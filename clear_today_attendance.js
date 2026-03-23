const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = getFirestore('edu-track');

async function clearTodayAttendance() {
  const today = new Date().toISOString().split('T')[0];  // e.g., "2026-03-23"
  const classId = '2A'; // <-- Change this if needed
  const docId = `${today}_${classId}`;

  console.log(`🗑️  Deleting attendance record: ${docId} ...`);
  try {
    await db.collection('attendance_records').doc(docId).delete();
    console.log(`✅ Deleted: ${docId}`);
  } catch (err) {
    console.error('❌ Error:', err.message);
  }
}

clearTodayAttendance();
