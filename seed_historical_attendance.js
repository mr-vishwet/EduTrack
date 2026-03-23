const admin = require('firebase-admin');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = getFirestore('edu-track');

async function seedHistoricalAttendance() {
  console.log('🚀 Seeding Historical Attendance Data (Last 60 Days)...');

  try {
    // 1. Fetch Students to know who is in what class
    const studentsSnap = await db.collection('students').where('isActive', '==', true).get();
    const studentsByClass = {}; // key: "8thA", value: [studentId1, studentId2, ...]

    studentsSnap.forEach(doc => {
      const data = doc.data();
      if (data.standard && data.division) {
        const classId = data.standard + data.division;
        if (!studentsByClass[classId]) {
          studentsByClass[classId] = [];
        }
        studentsByClass[classId].push(doc.id);
      }
    });

    console.log(`Found students in ${Object.keys(studentsByClass).length} classes.`);

    if (Object.keys(studentsByClass).length === 0) {
      console.log('❌ No active students found in any class!');
      return;
    }

    // 2. Clear old attendance records to prevent massive duplication
    console.log('Clearing old attendance records...');
    const oldRecordsSnap = await db.collection('attendance_records').get();
    const deleteBatch = db.batch();
    let deleteCount = 0;
    oldRecordsSnap.forEach(doc => {
        deleteBatch.delete(doc.ref);
        deleteCount++;
    });
    if (deleteCount > 0) {
        await deleteBatch.commit();
        console.log(`✅ Deleted ${deleteCount} old attendance records.`);
    }

    // 3. Generate 60 days of data ending today
    let batch = db.batch();
    let opCount = 0;
    let totalCreated = 0;

    const today = new Date();

    for (let i = 0; i < 60; i++) {
        const targetDate = new Date();
        targetDate.setDate(today.getDate() - i);
        
        // Skip weekends (0 = Sunday, 6 = Saturday)
        const dayOfWeek = targetDate.getDay();
        if (dayOfWeek === 0 || dayOfWeek === 6) continue;

        // format date as YYYY-MM-DD
        const yyyy = targetDate.getFullYear();
        const mm = String(targetDate.getMonth() + 1).padStart(2, '0');
        const dd = String(targetDate.getDate()).padStart(2, '0');
        const dateStr = `${yyyy}-${mm}-${dd}`;

        // Create a record for each class
        for (const [classId, studentIds] of Object.entries(studentsByClass)) {
            const standard = classId.replace(/[^0-9]/g, '');
            const division = classId.replace(/[0-9]/g, '');
            
            const statuses = {};
            let presentCount = 0;
            let totalCount = studentIds.length;

            studentIds.forEach(uid => {
                // 90% chance of being present
                const isPresent = Math.random() < 0.90;
                statuses[uid] = isPresent;
                if (isPresent) presentCount++;
            });

            const recordRef = db.collection('attendance_records').doc();
            batch.set(recordRef, {
                standard: standard || classId,
                division: division || "",
                date: dateStr,
                timestamp: FieldValue.serverTimestamp(),
                statuses: statuses,
                presentCount: presentCount,
                totalCount: totalCount,
                markedBy: 'SYSTEM_SEED',
                createdAt: targetDate.getTime()
            });

            opCount++;
            totalCreated++;

            // Firestore batch limit is 500 operations
            if (opCount >= 400) {
                await batch.commit();
                batch = db.batch();
                opCount = 0;
            }
        }
    }

    if (opCount > 0) {
        await batch.commit();
    }

    console.log(`✅ Successfully seeded ${totalCreated} attendance records spanning 60 days (excluding weekends)!`);
  } catch (error) {
    console.error('❌ Error seeding:', error);
  }
}

seedHistoricalAttendance();
