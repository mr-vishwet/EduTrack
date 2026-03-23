const admin = require('firebase-admin');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ 
    credential: admin.credential.cert(serviceAccount) 
});

const db = getFirestore('edu-track');

async function clearAndSeedAnnouncements() {
    console.log('🧹 Clearing existing announcements...');
    
    try {
        const snap = await db.collection('announcements').get();
        const batch = db.batch();
        
        snap.forEach(doc => {
            batch.delete(doc.ref);
        });
        
        await batch.commit();
        console.log(`✅ Deleted ${snap.size} announcements.`);

        console.log('🌱 Seeding new categorized announcements...');
        
        const announcements = [
            // --- ACADEMIC ---
            {
                title: "Final Examination Schedule 2026",
                content: "The final examination schedule for the academic year 2025-2026 has been released. Please check the notice board or student portal for subject-wise dates. Exams begin April 15th.",
                category: "Academic",
                audience: "All",
                targetType: "school",
                isPinned: true,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },
            {
                title: "Mathematics Unit Test - Std 10",
                content: "Reminder: The Mathematics Unit Test on 'Quadratic Equations' is scheduled for next Monday. Prepare well, students!",
                category: "Academic",
                audience: "Students",
                targetType: "class",
                classId: "10A",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "Teacher_UID_Placeholder" // Ideally replaced by real teacher UID if needed
            },
            {
                title: "Science Lab Practical Session",
                content: "All Grade 9 students must bring their lab coats and journals for the upcoming practical session on Thursday.",
                category: "Academic",
                audience: "Students",
                targetType: "class",
                classId: "9A",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "Teacher_UID_Placeholder"
            },

            // --- EVENTS ---
            {
                title: "Annual Cultural Fest - 'Pratibha'",
                content: "Get ready for our annual cultural fest 'Pratibha'! Registration for solo and group dance performances is now open at the activity office.",
                category: "Events",
                audience: "All",
                targetType: "school",
                isPinned: true,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },
            {
                title: "Parent-Teacher Meeting (Term 2)",
                content: "A mandatory PTM is scheduled for Saturday, March 28th, from 9 AM to 12 PM to discuss student progress and upcoming final exams.",
                category: "Events",
                audience: "Parents",
                targetType: "school",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },
            {
                title: "Science Fair 2026 Participation",
                content: "Students interested in participating in the Inter-School Science Fair should submit their project abstracts by March 30th.",
                category: "Events",
                audience: "Students",
                targetType: "school",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },

            // --- SPORTS ---
            {
                title: "Inter-School Football Championship",
                content: "Congratulations to our school football team for reaching the finals! The final match against DAV Public School is on Friday at 4 PM.",
                category: "Sports",
                audience: "All",
                targetType: "school",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },
            {
                title: "Basketball Team Selections (Under-16)",
                content: "Selections for the Under-16 Girls Basketball team will be held tomorrow morning at 7:30 AM in the school gym.",
                category: "Sports",
                audience: "Students",
                targetType: "school",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            },
            {
                title: "Annual Sports Day Winners!",
                content: "The winners list for the 2025 Annual Sports Day has been published. Certificates and medals will be distributed in the morning assembly.",
                category: "Sports",
                audience: "All",
                targetType: "school",
                isPinned: false,
                timestamp: FieldValue.serverTimestamp(),
                author: "ADMIN"
            }
        ];

        const writeBatch = db.batch();
        announcements.forEach(a => {
            const docRef = db.collection('announcements').doc();
            writeBatch.set(docRef, a);
        });

        await writeBatch.commit();
        console.log(`✅ Successfully seeded ${announcements.length} categorized announcements!`);

    } catch (error) {
        console.error('❌ Error during seeding:', error);
    } finally {
        process.exit();
    }
}

clearAndSeedAnnouncements();
