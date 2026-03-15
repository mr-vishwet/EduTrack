const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');
const auth = admin.auth();

const indianNames = [
  "Ashwini Patil Mam", "Suyog Sharma Sir", "Anjali Kulkarni Mam", "Sanjay Deshpande Sir", "Kavita Iyer Mam",
  "Rajesh Patil Sir", "Sunita Gadkari Mam", "Amitabh Joshi Sir", "Kiran Rao Mam", "Meera Shinde Mam",
  "Arjun Varma Sir", "Sneha Kulkarni Mam", "Rohan Deshpande Sir", "Deepa Patil Mam", "Vikas Gupta Sir",
  "Nandini Sharma Mam", "Suresh More Sir", "Kavita Dixit Mam", "Rahul Kulkarni Sir", "Shantanu Pawar Sir",
  "Pankaj Kadam Sir", "Sushma Swaraj Mam", "Manish Pandey Sir", "Anushka Rao Mam", "Vinay Pathak Sir",
  "Vidya Iyer Mam", "Akshay Deshmukh Sir", "Priyanka Naik Mam", "Ranveer Singh Sir", "Deepika Padukone Mam"
];

const subjects = [
  "Mathematics", "Science", "English", "Hindi", "Marathi", "History", "Geography", 
  "Sanskrit", "Computer Science", "Physics", "Chemistry", "Biology", "Physical Education", "Art"
];

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

async function setupTeachers() {
  console.log('🇮🇳 Starting Indian Teachers Population...');
  
  const classes = [];
  for (let std = 1; std <= 10; std++) {
    classes.push(`${std}A`, `${std}B`);
  }

  try {
    // We have 30 names, we need at least 20 for Class Teachers
    for (let i = 0; i < 30; i++) {
        const name = indianNames[i];
        const email = `teacher_${i + 1}@edutrack.com`;
        const uid = await createAuthUser(email, 'password123', name);
        
        let assignedClasses = [];
        let roleInClass = "Subject Teacher";

        // Assign first 20 as Class Teachers for 1A to 10B
        if (i < 20) {
            const classId = classes[i];
            assignedClasses = [classId];
            roleInClass = "Class Teacher";

            // Update Class document to point to this teacher
            await db.collection('classes').doc(classId).set({
                teacherUid: uid,
                classTeacherName: name
            }, { merge: true });
            
            console.log(`✅ ${name} assigned as Class Teacher for ${classId}`);
        } else {
            // Remaining 10 are floating Subject Teachers
            assignedClasses = ["General"];
            console.log(`✅ ${name} added as Specialist/Subject Teacher`);
        }

        await db.collection('users').doc(uid).set({
            uid: uid,
            name: name,
            email: email,
            role: 'TEACHER',
            metadata: { 
                assignedClasses: assignedClasses,
                designation: roleInClass,
                subject: subjects[i % subjects.length], // Cyclic assignment
                experience: `${Math.floor(Math.random() * 15) + 2} years`
            },
            createdAt: Date.now()
        });
    }

    // Initialize Attendance Schema with some sample data for today
    console.log('📅 Initializing Attendance sample data...');
    const today = new Date().toISOString().split('T')[0];
    
    // Create attendance records for class 1A (Roll 1-5) as a sample
    const sampleStudents = [1, 2, 3, 4, 5];
    for (const roll of sampleStudents) {
        const studentId = `ROLL_${roll}_1A`;
        const attendanceId = `${studentId}_${today}`;
        
        await db.collection('attendance').doc(attendanceId).set({
            attendanceId: attendanceId,
            studentId: studentId,
            classId: '1A',
            date: today,
            status: Math.random() > 0.1 ? 'PRESENT' : 'ABSENT',
            markedBy: 'teacher_1@edutrack.com',
            timestamp: Date.now()
        });
    }

    console.log('\n✨ Teacher Population Complete!');
    console.log('30 Teachers created (20 Class Teachers + 10 Subject Specialists).');
    console.log('Attendance initialized for 1A for today.');

  } catch (error) {
    console.error('❌ Error during teacher setup:', error);
  }
}

setupTeachers();
