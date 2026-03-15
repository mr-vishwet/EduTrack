const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

// 1. DOWNLOAD YOUR SERVICE ACCOUNT KEY
// Go to Firebase Console > Project Settings > Service Accounts
// Click "Generate new private key". Rename it to 'serviceAccountKey.json' 
// and place it in the SAME folder as this script.

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Using the specific database 'edu-track'
const db = getFirestore('edu-track');
const auth = admin.auth();

const indianNames = {
  boys: ["Aarav", "Vihaan", "Vivaan", "Advait", "Kabir", "Arjun", "Aryan", "Sai", "Ishaan", "Reyansh", "Atharv", "Shaurya", "Ayush", "Rudra", "Om", "Aaryan", "Ishan", "Aadi", "Sarthak", "Pranav", "Darsh", "Veer", "Rishi", "Yuvraj", "Karthik", "Rohan", "Abhishek", "Rishabh", "Varun", "Karan"],
  girls: ["Ananya", "Diya", "Ishani", "Myra", "Reva", "Saanvi", "Shanaya", "Navya", "Aadhya", "Zara", "Amyra", "Anvi", "Pari", "Vanya", "Kaira", "Siya", "Riya", "Aavya", "Inaya", "Ira", "Kiara", "Prisha", "Ishi", "Gia", "Sara", "Kyira", "Anushka", "Ruchi", "Tanya", "Ritu"],
  men: ["Rajesh", "Suresh", "Amit", "Vijay", "Nitin", "Sanjay", "Mahesh", "Ramesh", "Anil", "Sunil", "Pankaj", "Deepak", "Manoj", "Vikram", "Rahul", "Sameer", "Arvind", "Kishore", "Ganesh", "Ashok", "Sandeep", "Pradeep", "Sudhir", "Narendra", "Yogesh", "Abhay", "Milind", "Satish", "Mohan", "Dilip", "Vinod"],
  women: ["Ashwini", "Priya", "Snehal", "Deepali", "Kavita", "Sunita", "Megha", "Shweta", "Pooja", "Nisha", "Anita", "Surekha", "Manali", "Swati", "Pallavi", "Anjali", "Varsha", "Rekha", "Suman", "Maya", "Lata", "Usha", "Geeta", "Seema", "Vidya", "Asha", "Neeta", "Kiran", "Saritha", "Uma", "Vandana"],
  surnames: ["Patil", "Sharma", "Kulkarni", "Deshmukh", "Joshi", "Verma", "Singh", "Gadvi", "More", "Rathod", "Shah", "Pawar", "Gupta", "Tiwari", "Iyer", "Saxena", "Reddy", "Malhotra", "Choudhury", "Jha", "Bhide", "Apte", "Modi", "Khanna", "Bansal", "Mehta", "Kapoor", "Jain", "Aggarwal", "Pandey", "Chatterjee", "Mukherjee", "Nair", "Pillai", "Shetty", "Rao", "Kadam", "Shinde", "Gaikwad", "Thorat", "Chavan"]
};

const subjects = [
  "Mathematics", "Science", "History", "Geography", "English", "Hindi", "Marathi", "Sanskrit",
  "Computer Science", "Physical Education", "Arts & Crafts", "Music", "Library Science", "Physics", "Chemistry", "Biology"
];

function getRandom(array) {
  return array[Math.floor(Math.random() * array.length)];
}

async function createAuthUser(email, password, displayName) {
  try {
    const userRecord = await auth.createUser({
      email: email,
      password: password,
      displayName: displayName
    });
    return userRecord.uid;
  } catch (error) {
    if (error.code === 'auth/email-already-exists') {
      const user = await auth.getUserByEmail(email);
      return user.uid;
    }
    throw error;
  }
}

async function setup() {
  console.log('🚀 Starting Enriched Firebase Setup (Phase 9)...');

  try {
    // 1. Create Admin
    const adminUid = await createAuthUser('admin@edutrack.com', 'password123', 'School Administrator');
    await db.collection('admins').doc(adminUid).set({
      uid: adminUid,
      name: 'School Administrator',
      email: 'admin@edutrack.com',
      createdAt: Date.now()
    });
    await db.collection('users').doc(adminUid).set({
      uid: adminUid,
      role: 'ADMIN',
      email: 'admin@edutrack.com'
    });
    console.log('✅ Admin ready.');

    // 2. Create pool of Subject Teachers (15 teachers)
    console.log('⏳ Seeding Subject Teachers...');
    for (let i = 1; i <= 15; i++) {
        const isMale = Math.random() > 0.5;
        const firstName = isMale ? getRandom(indianNames.men) : getRandom(indianNames.women);
        const surname = getRandom(indianNames.surnames);
        const title = isMale ? "sir" : "mam";
        const fullName = `${firstName} ${surname} ${title}`;
        const email = `subject_teacher_${i}@edutrack.com`;
        const expertise = getRandom(subjects);

        const uid = await createAuthUser(email, 'password123', fullName);
        
        await db.collection('teachers').doc(uid).set({
            uid: uid,
            name: fullName,
            email: email,
            expertise: expertise,
            assignedClasses: [], // Specialization teachers
            createdAt: Date.now()
        });
        await db.collection('users').doc(uid).set({
            uid: uid,
            role: 'TEACHER',
            email: email
        });
    }
    console.log('✅ 15 Subject Teachers seeded.');

    // 3. Create Class Teachers and Students (1 to 10, A and B)
    for (let std = 1; std <= 10; std++) {
      for (const div of ['A', 'B']) {
        const classId = `${std}${div}`;
        
        // Class Teacher
        const isMale = Math.random() > 0.5;
        const tFirstName = isMale ? getRandom(indianNames.men) : getRandom(indianNames.women);
        const tSurname = getRandom(indianNames.surnames);
        const tTitle = isMale ? "sir" : "mam";
        const tName = `${tFirstName} ${tSurname} ${tTitle}`;
        const tEmail = `teacher_${classId.toLowerCase()}@edutrack.com`;

        const tUid = await createAuthUser(tEmail, 'password123', tName);
        await db.collection('teachers').doc(tUid).set({
          uid: tUid,
          name: tName,
          email: tEmail,
          expertise: "Class Teacher",
          assignedClasses: [classId],
          createdAt: Date.now()
        });
        await db.collection('users').doc(tUid).set({
          uid: tUid,
          role: 'TEACHER',
          email: tEmail
        });

        // Students
        const numStudents = Math.floor(Math.random() * (45 - 30 + 1)) + 30;
        await db.collection('classes').doc(classId).set({
          classId: classId,
          standard: std.toString(),
          division: div,
          teacherUid: tUid,
          studentCount: numStudents
        });

        for (let i = 1; i <= numStudents; i++) {
          const rollNo = i;
          const studentId = `ROLL_${rollNo}_${classId}`;
          const loginId = `ROLL_NO_${rollNo}_${classId}`;
          const parentEmail = `${loginId.toLowerCase()}@edutrack.com`;
          
          const isBoy = Math.random() > 0.5;
          const sFirstName = isBoy ? getRandom(indianNames.boys) : getRandom(indianNames.girls);
          const familySurname = getRandom(indianNames.surnames);
          const studentName = `${sFirstName} ${familySurname}`;
          
          const pFirstName = getRandom(indianNames.men); // Simple assumption for parent name
          const parentName = `${pFirstName} ${familySurname}`;

          const pUid = await createAuthUser(parentEmail, 'password123', parentName);
          await db.collection('parents').doc(pUid).set({
            uid: pUid,
            name: parentName,
            email: parentEmail,
            childrenUids: [studentId],
            createdAt: Date.now()
          });
          await db.collection('users').doc(pUid).set({
            uid: pUid,
            role: 'PARENT',
            email: parentEmail
          });

          await db.collection('students').doc(studentId).set({
            studentId: studentId,
            name: studentName,
            rollNumber: rollNo,
            standard: std.toString(),
            division: div,
            parentUid: pUid,
            isActive: true
          });
        }
        console.log(`✅ Class ${classId} populated (${numStudents} students).`);
      }
    }

    console.log('\n✨ Realistic Seeding Complete! Database is now "Crystal Clear" and diverse.');

  } catch (error) {
    console.error('❌ Error during setup:', error);
  }
}

setup();
