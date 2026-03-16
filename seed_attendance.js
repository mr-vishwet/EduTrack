const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

// Uses the same serviceAccountKey.json as setup_firebase.js
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');

// ----------------------------------------------------------------
// Configuration
// ----------------------------------------------------------------
const START_DATE = '2026-03-01';
const END_DATE   = '2026-03-16'; // today (inclusive)

// All 20 classes (1A–10B). Must match existing Firestore 'classes' documents.
const CLASSES = [];
for (let std = 1; std <= 10; std++) {
  for (const div of ['A', 'B']) {
    CLASSES.push({ standard: std.toString(), division: div, classId: `${std}${div}` });
  }
}

// Weekdays only: Mon-Fri (no attendance on Sat/Sun)
function getSchoolDays(start, end) {
  const days = [];
  const cur = new Date(start + 'T00:00:00Z');
  const last = new Date(end + 'T00:00:00Z');
  while (cur <= last) {
    const dow = cur.getUTCDay(); // 0=Sun, 6=Sat
    if (dow !== 0 && dow !== 6) {
      days.push(cur.toISOString().slice(0, 10));
    }
    cur.setUTCDate(cur.getUTCDate() + 1);
  }
  return days;
}

// Fetch all students for a class
async function getStudentsForClass(standard, division) {
  const snap = await db.collection('students')
    .where('standard', '==', standard)
    .where('division', '==', division)
    .get();
  return snap.docs.map(d => d.id); // studentId = document id
}

// Generate a realistic attendance map (85–98% present)
function generateStatuses(studentIds) {
  const statuses = {};
  for (const id of studentIds) {
    // ~90% chance present, 10% absent
    statuses[id] = Math.random() < 0.90;
  }
  return statuses;
}

async function seedAttendance() {
  console.log('🚀 Seeding attendance from', START_DATE, 'to', END_DATE);

  const schoolDays = getSchoolDays(START_DATE, END_DATE);
  console.log(`📅 ${schoolDays.length} school days:`, schoolDays.join(', '));

  let totalWritten = 0;
  let skipped = 0;

  for (const cls of CLASSES) {
    const studentIds = await getStudentsForClass(cls.standard, cls.division);
    if (studentIds.length === 0) {
      console.warn(`⚠️  No students found for class ${cls.classId}, skipping.`);
      continue;
    }

    const batch = db.batch();
    let batchCount = 0;

    for (const date of schoolDays) {
      const docId = `${date}_${cls.classId}`;
      const docRef = db.collection('attendance_records').doc(docId);

      // Skip if record already exists
      const existing = await docRef.get();
      if (existing.exists) {
        skipped++;
        continue;
      }

      const statuses = generateStatuses(studentIds);

      batch.set(docRef, {
        date,
        standard: cls.standard,
        division: cls.division,
        statuses,
        timestamp: admin.firestore.Timestamp.now()
      });
      batchCount++;

      // Firestore batch max = 500
      if (batchCount >= 400) {
        await batch.commit();
        totalWritten += batchCount;
        batchCount = 0;
        console.log(`   ✅ Batch committed for class ${cls.classId}`);
      }
    }

    if (batchCount > 0) {
      await batch.commit();
      totalWritten += batchCount;
    }

    console.log(`✅ Class ${cls.classId} (${studentIds.length} students) — ${batchCount > 0 ? schoolDays.length - skipped : 'batched'} records`);
  }

  console.log(`\n✨ Done! ${totalWritten} records written, ${skipped} already existed and were skipped.`);
}

seedAttendance().catch(err => {
  console.error('❌ Error:', err);
  process.exit(1);
});
