package com.edu.track.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

/**
 * Utility class to provide consistent access to Firebase services.
 */
public class FirebaseSource {
    private static FirebaseSource instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private FirebaseSource() {
        // Using the named database 'edu-track' as per user preference
        db = FirebaseFirestore.getInstance("edu-track");
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseSource getInstance() {
        if (instance == null) {
            instance = new FirebaseSource();
        }
        return instance;
    }

    public FirebaseFirestore getFirestore() {
        return db;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    // Collection References
    public CollectionReference getUsersRef() {
        return db.collection("users");
    }

    public CollectionReference getTeachersRef() {
        return db.collection("teachers");
    }

    public CollectionReference getParentsRef() {
        return db.collection("parents");
    }

    public CollectionReference getAdminsRef() {
        return db.collection("admins");
    }

    public CollectionReference getStudentsRef() {
        return db.collection("students");
    }

    public CollectionReference getClassesRef() {
        return db.collection("classes");
    }

    public CollectionReference getAttendanceRef() {
        return db.collection("attendance_records");
    }

    public CollectionReference getAnnouncementsRef() {
        return db.collection("announcements");
    }
}
