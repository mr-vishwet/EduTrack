const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = getFirestore('edu-track');

// ── Define the Subjects Collection ──────────────────────────────────────────
const SUBJECTS = [
  { id: 'mathematics',  name: 'Mathematics',        code: 'MATH' },
  { id: 'science',      name: 'Science',             code: 'SCI'  },
  { id: 'english',      name: 'English',             code: 'ENG'  },
  { id: 'hindi',        name: 'Hindi',               code: 'HIN'  },
  { id: 'marathi',      name: 'Marathi',             code: 'MAR'  },
  { id: 'history',      name: 'History',             code: 'HIS'  },
  { id: 'geography',    name: 'Geography',           code: 'GEO'  },
  { id: 'physics',      name: 'Physics',             code: 'PHY'  },
  { id: 'chemistry',    name: 'Chemistry',           code: 'CHEM' },
  { id: 'biology',      name: 'Biology',             code: 'BIO'  },
  { id: 'computer',     name: 'Computer Science',    code: 'CS'   },
  { id: 'arts',         name: 'Arts & Crafts',       code: 'ART'  },
  { id: 'music',        name: 'Music',               code: 'MUS'  },
  { id: 'pe',           name: 'Physical Education',  code: 'PE'   },
  { id: 'sanskrit',     name: 'Sanskrit',            code: 'SAN'  },
];

// Map display name → subject id (for matching teachers)
const EXPERTISE_TO_ID = {};
for (const s of SUBJECTS) {
  EXPERTISE_TO_ID[s.name.toLowerCase()] = s.id;
  EXPERTISE_TO_ID[s.code.toLowerCase()] = s.id;
}

async function run() {
  console.log('🚀 Setting up subjects collection and updating teacher SubjectIds...');

  // 1. Write subjects to Firestore
  const batch1 = db.batch();
  for (const subj of SUBJECTS) {
    batch1.set(db.collection('subjects').doc(subj.id), {
      ...subj,
      createdAt: admin.firestore.Timestamp.now()
    });
  }
  await batch1.commit();
  console.log(`✅ ${SUBJECTS.length} subjects written to 'subjects' collection`);

  // 2. Update teachers with subjectIds + classTeacher field
  const teachersSnap = await db.collection('teachers').get();
  console.log(`📋 Found ${teachersSnap.size} teachers — updating subjectIds & classTeacher...`);

  const batch2 = db.batch();
  for (const doc of teachersSnap.docs) {
    const data = doc.data();
    const expertise = (data.expertise || '').trim();
    const assignedClasses = data.assignedClasses || [];

    // Derive subjectId from expertise text
    const subjectId = EXPERTISE_TO_ID[expertise.toLowerCase()] || null;
    const subjectIds = subjectId ? [subjectId] : [];

    // Determine classTeacher: if exactly 1 assigned class (typical class teachers in our setup)
    const classTeacher = assignedClasses.length === 1 ? assignedClasses[0] : null;

    batch2.update(doc.ref, {
      subjectIds,
      classTeacher
    });
  }
  await batch2.commit();
  console.log(`✅ Updated ${teachersSnap.size} teachers with subjectIds and classTeacher`);

  console.log('\n✨ Done! Run the app and check the Manage Teachers screen.');
}

run().catch(err => {
  console.error('❌ Error:', err);
  process.exit(1);
});
