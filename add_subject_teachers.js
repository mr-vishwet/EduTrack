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

async function addSubjectTeachers() {
  console.log('👨‍🏫 Adding 15 Specialized Subject Teachers...');

  try {
    for (let i = 1; i <= 15; i++) {
        const isMale = Math.random() > 0.5;
        const firstName = isMale ? getRandom(indianNames.men) : getRandom(indianNames.women);
        const surname = getRandom(indianNames.surnames);
        const title = isMale ? "sir" : "mam";
        const fullName = `${firstName} ${surname} ${title}`;
        const email = `subject_teacher_${i}@edutrack.com`;
        const expertise = getRandom(subjects);

        console.log(`Creating: ${fullName} (${expertise})`);
        const uid = await createAuthUser(email, 'password123', fullName);
        
        // Entity Collection
        await db.collection('teachers').doc(uid).set({
            uid: uid,
            name: fullName,
            email: email,
            expertise: expertise,
            assignedClasses: [], // Specialization teachers
            createdAt: Date.now()
        });
        
        // Routing Collection
        await db.collection('users').doc(uid).set({
            uid: uid,
            name: fullName,
            role: 'TEACHER',
            email: email
        });
    }

    console.log('\n✨ All 15 subject teachers added successfully.');
  } catch (error) {
    console.error('❌ Error adding teachers:', error);
  }
}

addSubjectTeachers();
