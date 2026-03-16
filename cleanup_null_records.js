const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = getFirestore('edu-track');

async function cleanupNullRecords() {
  console.log('🧹 Scanning attendance_records for null standard/division...');

  const snap = await db.collection('attendance_records').get();
  let deleted = 0;
  let skipped = 0;

  const BATCH_SIZE = 400;
  let batch = db.batch();
  let count = 0;

  for (const doc of snap.docs) {
    const data = doc.data();
    const std = data.standard;
    const div = data.division;

    if (std === null || std === undefined || div === null || div === undefined ||
        std === 'null' || div === 'null' || std === '' || div === '') {
      batch.delete(doc.ref);
      deleted++;
      count++;

      if (count >= BATCH_SIZE) {
        await batch.commit();
        console.log(`  🗑️  Deleted ${count} records in this batch`);
        batch = db.batch();
        count = 0;
      }
    } else {
      skipped++;
    }
  }

  if (count > 0) {
    await batch.commit();
    console.log(`  🗑️  Deleted final ${count} records`);
  }

  console.log(`\n✅ Cleanup complete: ${deleted} null records deleted, ${skipped} valid records kept.`);
}

cleanupNullRecords().catch(err => {
  console.error('❌ Error:', err);
  process.exit(1);
});
