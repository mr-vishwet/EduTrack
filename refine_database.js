const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

// Load service account
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');
const auth = admin.auth();

const indianNames = {
  boys: ["Aarav", "Vihaan", "Vivaan", "Advait", "Kabir", "Arjun", "Aryan", "Sai", "Ishaan", "Reyansh", "Atharv", "Shaurya", "Ayush", "Rudra", "Om", "Aaryan", "Ishan", "Aadi", "Sarthak", "Pranav", "Darsh", "Veer", "Rishi", "Yuvraj", "Karthik", "Rohan", "Abhishek", "Rishabh", "Varun", "Karan"],
  girls: ["Ananya", "Diya", "Ishani", "Myra", "Reva", "Saanvi", "Shanaya", "Navya", "Aadhya", "Zara", "Amyra", "Anvi", "Pari", "Vanya", "Kaira", "Siya", "Riya", "Aavya", "Inaya", "Ira", "Kiara", "Prisha", "Ishi", "Gia", "Sara", "Kyira", "Anushka", "Ruchi", "Tanya", "Ritu"],
  men: ["Rajesh", "Suresh", "Amit", "Vijay", "Nitin", "Sanjay", "Mahesh", "Ramesh", "Anil", "Sunil", "Pankaj", "Deepak", "Manoj", "Vikram", "Rahul", "Sameer", "Arvind", "Kishore", "Ganesh", "Ashok", "Sandeep", "Pradeep", "Sudhir", "Narendra", "Yogesh", "Abhay", "Milind", "Satish", "Mohan", "Dilip", "Vinod"],
  women: ["Ashwini", "Priya", "Snehal", "Deepali", "Kavita", "Sunita", "Megha", "Shweta", "Pooja", "Nisha", "Anita", "Surekha", "Manali", "Swati", "Pallavi", "Anjali", "Varsha", "Rekha", "Suman", "Maya", "Lata", "Usha", "Geeta", "Seema", "Vidya", "Asha", "Neeta", "Kiran", "Saritha", "Uma", "Vandana"],
  surnames: ["Patil", "Sharma", "Kulkarni", "Deshmukh", "Joshi", "Verma", "Singh", "Gadvi", "More", "Rathod", "Shah", "Pawar", "Gupta", "Tiwari", "Iyer", "Saxena", "Reddy", "Malhotra", "Choudhury", "Jha", "Bhide", "Apte", "Modi", "Khanna", "Bansal", "Mehta", "Kapoor", "Jain", "Aggarwal", "Pandey", "Chatterjee", "Mukherjee", "Nair", "Pillai", "Shetty", "Rao", "Kadam", "Shinde", "Gaikwad", "Thorat", "Chavan"]
};

function getRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

async function refineData() {
  console.log('🔄 Starting Data Refinement (Students & Parents)...');

  try {
    const studentsSnapshot = await db.collection('students').get();
    console.log(`Found ${studentsSnapshot.size} students to refine.`);

    let count = 0;
    for (const doc of studentsSnapshot.docs) {
      const studentData = doc.data();
      const parentUid = studentData.parentUid;

      // 1. Generate new names
      const isBoy = Math.random() > 0.5;
      const sFirstName = isBoy ? getRandom(indianNames.boys) : getRandom(indianNames.girls);
      const familySurname = getRandom(indianNames.surnames);
      const studentName = `${sFirstName} ${familySurname}`;
      
      const pFirstName = getRandom(indianNames.men);
      const parentName = `${pFirstName} ${familySurname}`;

      // 2. Update Student Document
      await doc.ref.update({
        name: studentName
      });

      // 3. Update Parent & User documents if parent exists
      if (parentUid) {
        // Update parents collection
        await db.collection('parents').doc(parentUid).update({
          name: parentName
        });

        // Update users (routing) collection
        await db.collection('users').doc(parentUid).update({
          name: parentName
        });

        // Optionally update Auth display name
        try {
            await auth.updateUser(parentUid, {
                displayName: parentName
            });
        } catch (e) {
            // Might be a test UID not in Auth, skip
        }
      }

      count++;
      if (count % 50 === 0) console.log(`Processed ${count} students...`);
    }

    console.log(`\n✨ Successfully refined ${count} student-parent sets.`);
  } catch (error) {
    console.error('❌ Error during refinement:', error);
  }
}

refineData();
