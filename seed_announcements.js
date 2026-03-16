const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = getFirestore('edu-track');

const dummyAnnouncements = [
    {
        title: "Term 1 Final Examination Schedule",
        content: "Dear Parents and Students, the final examination for Term 1 will commence from next week. Please refer to the PDF sent via email for the detailed timetable. All students must report 15 mins early.",
        audience: "All",
        isPinned: true,
        author: "Admin",
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    },
    {
        title: "Upcoming PTA Meeting",
        content: "A gentle reminder that the Parent Teacher Association (PTA) meeting for standards 5 through 10 will be held this Saturday in the main auditorium. Please ensure your presence.",
        audience: "Parents Only",
        isPinned: false,
        author: "Admin",
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    },
    {
        title: "Teachers Training Workshop",
        content: "All teaching staff are required to attend the mandatory workshop on 'Modern Assessment Strategies' this Friday at 3 PM in the staff room. Attendance is strictly compulsory.",
        audience: "Teachers Only",
        isPinned: false,
        author: "Admin",
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    },
    {
        title: "Annual Sports Day Registration",
        content: "Registration for the Annual Sports Day is now open! Students interested in participating in track and field events should give their names to their respective class teachers by tomorrow.",
        audience: "All",
        isPinned: true,
        author: "Admin",
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    }
];

async function seedAnnouncements() {
    try {
        console.log("Adding dummy announcements...");
        const batch = db.batch();
        const announcementsRef = db.collection('announcements');
        
        for (const data of dummyAnnouncements) {
            const docRef = announcementsRef.doc();
            batch.set(docRef, data);
        }
        
        await batch.commit();
        console.log(`Successfully added ${dummyAnnouncements.length} announcements.`);
        process.exit(0);
    } catch (error) {
        console.error("Error seeding announcements:", error);
        process.exit(1);
    }
}

seedAnnouncements();
