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

async function refineTeachers() {
  console.log('🧑‍🏫 Refining Teacher Assignments (Multi-Subject & Multi-Class)...');

  try {
    const teachersSnapshot = await db.collection('teachers').get();
    const classesSnapshot = await db.collection('classes').get();
    const allClasses = classesSnapshot.docs.map(doc => doc.data().classId);

    console.log(`Found ${teachersSnapshot.size} teachers and ${allClasses.length} classes.`);

    for (const doc of teachersSnapshot.docs) {
      const teacherData = doc.data();
      const currentClasses = teacherData.assignedClasses || [];
      const currentExpertise = teacherData.expertise || "General";

      // 1. Assign multiple subjects (2-3 subjects per teacher)
      const teacherSubjects = getRandomItems(subjects, Math.floor(Math.random() * 2) + 2);
      if (!teacherSubjects.includes(currentExpertise) && currentExpertise !== "Class Teacher" && currentExpertise !== "Subject Expert") {
          teacherSubjects.push(currentExpertise);
      }

      // 2. Assign multiple classes (Class teachers get 1 extra class, Subject teachers get 2-4 classes)
      let newClasses = [...currentClasses];
      if (currentClasses.length > 0) {
          // It's a class teacher or already assigned
          const extraClass = getRandomItems(allClasses.filter(c => !newClasses.includes(c)), 1);
          newClasses = [...newClasses, ...extraClass];
      } else {
          // It's a subject teacher
          newClasses = getRandomItems(allClasses, Math.floor(Math.random() * 3) + 2);
      }

      // 3. Update Teacher Document
      await doc.ref.update({
        subjects: teacherSubjects,
        assignedClasses: newClasses,
        expertise: teacherSubjects.join(", ") // Keep expertise as a summary string for UI compatibility
      });

      console.log(`Updated ${teacherData.name}: ${teacherSubjects.join(", ")} | Classes: ${newClasses.join(", ")}`);
    }

    console.log('\n✨ Teacher refinement complete! (Multi-subject & Multi-class enabled)');
  } catch (error) {
    console.error('❌ Error during teacher refinement:', error);
  }
}

refineTeachers();
