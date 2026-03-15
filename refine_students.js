const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');

const firstNames = [
  "Aarav", "Vihaan", "Aditya", "Arjun", "Aryan", "Reyansh", "Krishna", "Ishaan", "Shaurya", "Atharv",
  "Ananya", "Diya", "Pari", "Myra", "Aadhya", "Saanvi", "Anvi", "Aavya", "Kyra", "Ishani",
  "Rohan", "Siddharth", "Varun", "Kabir", "Aaryan", "Advait", "Tushar", "Rahul", "Yash", "Om",
  "Isha", "Riya", "Sia", "Tara", "Kiara", "Zara", "Navya", "Avani", "Tanvi", "Prisha"
];

const surnames = [
  "Patil", "Sharma", "Kulkarni", "Deshpande", "Iyer", "Mehra", "Gupta", "Shinde", "Joshi", "Rao",
  "More", "Pawar", "Kadam", "Pandey", "Deshmukh", "Naik", "Gadkari", "Kulkarni", "Dixit", "Pathak"
];

const parentFirstNames = [
  "Rajesh", "Suresh", "Manish", "Amit", "Vikram", "Sanjay", "Anil", "Sunil", "Vinay", "Sandeep",
  "Sunita", "Anita", "Priya", "Kavita", "Meena", "Deepa", "Sneha", "Anjali", "Swati", "Vidya"
];

async function refineStudents() {
  console.log('✨ Refining Student and Parent names...');
  
  try {
    const studentsSnapshot = await db.collection('students').get();
    console.log(`Found ${studentsSnapshot.size} students to refine.`);

    let count = 0;
    const batchSize = 400; // Firestore batch limit is 500
    let batch = db.batch();

    for (const doc of studentsSnapshot.docs) {
      const studentData = doc.data();
      const surname = surnames[Math.floor(Math.random() * surnames.length)];
      const studentFirstName = firstNames[Math.floor(Math.random() * firstNames.length)];
      const studentFullName = `${studentFirstName} ${surname}`;
      
      const parentFirstName = parentFirstNames[Math.floor(Math.random() * parentFirstNames.length)];
      const parentFullName = `${parentFirstName} ${surname}`;

      // 1. Update Student doc
      batch.update(doc.ref, { name: studentFullName });

      // 2. Update Parent User doc
      if (studentData.parentUid) {
        const parentRef = db.collection('users').doc(studentData.parentUid);
        batch.update(parentRef, { name: parentFullName });
      }

      count++;
      if (count % batchSize === 0) {
        await batch.commit();
        batch = db.batch();
        console.log(`Processed ${count} students...`);
      }
    }

    if (count % batchSize !== 0) {
      await batch.commit();
    }

    console.log(`\n✅ Successfully refined ${count} students and their parents with proper Indian names.`);
  } catch (error) {
    console.error('❌ Error during refinement:', error);
  }
}

refineStudents();
