const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = getFirestore('edu-track');

const SUBJECT_IDS = ['mathematics', 'science', 'english', 'hindi', 'marathi', 'history', 'geography', 'physics', 'chemistry', 'biology', 'computer', 'arts', 'music', 'pe', 'sanskrit'];

async function seedClassTeachers() {
  console.log('🚀 Seeding Proper Class Teachers...');

  try {
    const classesSnap = await db.collection('classes').get();
    const teachersSnap = await db.collection('teachers').get();
    
    // Build actual class IDs (standard + division)
    const allClasses = [];
    classesSnap.forEach(doc => {
        const data = doc.data();
        if (data.standard && data.division) {
            allClasses.push(data.standard + data.division);
        }
    });
    
    const allTeachers = [];
    teachersSnap.forEach(doc => {
        allTeachers.push({ id: doc.id, ...doc.data() });
    });

    console.log(`Found ${allClasses.length} classes and ${allTeachers.length} teachers.`);

    if (allTeachers.length === 0) {
        console.log('❌ No teachers found in database!');
        return;
    }

    const batch = db.batch();
    
    // Shuffle teachers to pick randomly
    const shuffledTeachers = [...allTeachers].sort(() => 0.5 - Math.random());
    
    const classToTeacherMap = {}; // track who is assigned to what

    // Assign one class teacher per class
    for (let i = 0; i < allClasses.length; i++) {
        const classId = allClasses[i];
        const teacher = shuffledTeachers[i % shuffledTeachers.length];
        
        console.log(`Assigning ${teacher.name} (${teacher.id}) as Class Teacher for ${classId}`);
        
        // Ensure teacher has this class in assignedClasses
        let assigned = teacher.assignedClasses || [];
        if (!assigned.includes(classId)) assigned.push(classId);
        
        // Ensure teacher has at least one subjectId
        let sIds = teacher.subjectIds || [];
        if (!sIds || sIds.length === 0) {
            // Pick a random subject if none
            sIds = [SUBJECT_IDS[Math.floor(Math.random() * SUBJECT_IDS.length)]];
        }
        
        // Create human readable expertise string
        const expertise = sIds.map(id => id.charAt(0).toUpperCase() + id.slice(1)).join(", ");
        
        batch.update(db.collection('teachers').doc(teacher.id), {
            classTeacher: classId,
            assignedClasses: assigned,
            subjectIds: sIds,
            expertise: expertise,
            role: "TEACHER",
            isActive: true
        });
        
        // Also update the users collection to be safe
        batch.set(db.collection('users').doc(teacher.id), {
            uid: teacher.id,
            role: "TEACHER",
            classTeacher: classId,
            assignedClasses: assigned,
            subjectIds: sIds,
            expertise: expertise
        }, { merge: true });

        classToTeacherMap[teacher.id] = classId;
    }
    
    // For teachers who are NOT class teachers in this run, clear their classTeacher field
    for (const t of allTeachers) {
        if (!classToTeacherMap[t.id]) {
            batch.update(db.collection('teachers').doc(t.id), {
                classTeacher: "" 
            });
            batch.update(db.collection('users').doc(t.id), {
                classTeacher: ""
            });
        }
    }

    await batch.commit();
    console.log('✅ Successfully seeded class teachers!');
  } catch (error) {
    console.error('❌ Error seeding:', error);
  }
}

seedClassTeachers();
