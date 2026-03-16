const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

// Load service account
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');

const subjects = [
  "Mathematics", "Science", "History", "Geography", "English", "Hindi", "Marathi", "Sanskrit",
  "Computer Science", "Physical Education", "Arts & Crafts", "Music", "Library Science", "Physics", "Chemistry", "Biology"
];

function getRandomItems(array, count) {
  const shuffled = [...array].sort(() => 0.5 - Math.random());
  return shuffled.slice(0, count);
}

const auth = admin.auth();

async function refineTeachers() {
  console.log('🧑‍🏫 Refining Teacher Emails & Assignments...');

  try {
    const teachersSnapshot = await db.collection('teachers').get();
    const classesSnapshot = await db.collection('classes').get();
    const allClasses = classesSnapshot.docs.map(doc => doc.data().classId);

    console.log(`Found ${teachersSnapshot.size} teachers and ${allClasses.length} classes.`);

    for (const doc of teachersSnapshot.docs) {
      const teacherData = doc.data();
      const uid = doc.id;
      const currentName = teacherData.name || "";
      
      // 1. Standardize Email: name_surname@edutrack.com
      // Logic: Strip "sir"/"mam", split by space, join with underscore
      let parts = currentName.toLowerCase().replace("sir", "").replace("mam", "").trim().split(/\s+/);
      let newEmail = "";
      if (parts.length >= 2) {
          newEmail = `${parts[0]}.${parts[1]}@edutrack.com`;
      } else {
          newEmail = `${parts[0]}@edutrack.com`;
      }

      console.log(`Updating ${currentName}: New Email -> ${newEmail}`);

      // 2. Update Firebase Auth
      try {
          await auth.updateUser(uid, { email: newEmail });
      } catch (authError) {
          console.error(`  ⚠️ Could not update Auth for ${uid}: ${authError.message}`);
      }

      // 3. Assign multiple subjects (2-3 subjects per teacher)
      const currentExpertise = teacherData.expertise || "General";
      const teacherSubjects = getRandomItems(subjects, Math.floor(Math.random() * 2) + 2);
      if (!teacherSubjects.includes(currentExpertise) && currentExpertise !== "Class Teacher" && currentExpertise !== "Subject Expert") {
          teacherSubjects.push(currentExpertise);
      }

      // 4. Assign multiple classes
      const currentClasses = teacherData.assignedClasses || [];
      let newClasses = [...currentClasses];
      if (currentClasses.length > 0) {
          const extraClass = getRandomItems(allClasses.filter(c => !newClasses.includes(c)), 1);
          newClasses = [...newClasses, ...extraClass];
      } else {
          newClasses = getRandomItems(allClasses, Math.floor(Math.random() * 3) + 2);
      }

      // 5. Update Teacher Document
      await doc.ref.update({
        email: newEmail,
        subjects: teacherSubjects,
        assignedClasses: newClasses,
        expertise: teacherSubjects.join(", ")
      });

      // 6. Update Users Collection mapping
      await db.collection('users').doc(uid).update({
          email: newEmail
      });

      console.log(`  ✅ Done: ${newEmail} | Subjects: ${teacherSubjects.join(", ")}`);
    }

    console.log('\n✨ Teacher refinement complete!');
  } catch (error) {
    console.error('❌ Error during teacher refinement:', error);
  }
}

refineTeachers();
