const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

// Load service account
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Using the named database 'edu-track'
const db = getFirestore('edu-track');

async function deleteCollection(collectionPath, batchSize) {
  const collectionRef = db.collection(collectionPath);
  const query = collectionRef.orderBy('__name__').limit(batchSize);

  return new Promise((resolve, reject) => {
    deleteQueryBatch(query, resolve).catch(reject);
  });
}

async function deleteQueryBatch(query, resolve) {
  const snapshot = await query.get();

  const batchSize = snapshot.size;
  if (batchSize === 0) {
    resolve();
    return;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.delete(doc.ref);
  });
  await batch.commit();

  process.nextTick(() => {
    deleteQueryBatch(query, resolve);
  });
}

async function clearAll() {
  const collections = ['users', 'admins', 'teachers', 'parents', 'students', 'classes', 'attendance_records', 'announcements'];
  
  console.log('🗑️ Starting Firestore data cleanup for database "edu-track"...');
  
  for (const collection of collections) {
    try {
      console.log(`⏳ Clearing collection: ${collection}...`);
      await deleteCollection(collection, 100);
      console.log(`✅ ${collection} cleared.`);
    } catch (error) {
      console.error(`❌ Error clearing ${collection}:`, error.message);
    }
  }

  console.log('\n✨ Database cleanup complete!');
}

clearAll();
