# PROJECT REPORT ON
# EduTrack: Smart Attendance Tracking System

**Submitted to:**
Department of Computer Science
Pune Institute of Technology
Savitribai Phule Pune University

**Under the guidance of:**
Prof. Rajesh Kulkarni
Assistant Professor

**Submitted by:**
Vishwet
Roll No: PIT-CS-2026-42

---

This is to certify that the project entitled **"EduTrack: Smart Attendance Tracking System"** is a bona fide work carried out by **Vishwet** in partial fulfillment of the requirements for the degree of Bachelor of Technology from **Pune Institute of Technology** during the academic year 2025-2026.

The project has been approved and accepted for presentation.

<br><br><br>

**[Internal Guide Name]**  
(Project Guide)

<br><br><br>

**[Head of Department]**  
(HOD, Dept of CS/IT)

---

## DECLARATION

I hereby declare that the project titled **"EduTrack: Smart Attendance Tracking System"** is an original piece of work conducted by me. The information and data collected and presented in this report are true to the best of my knowledge and have not been submitted for any other degree or professional qualification.

<br><br><br>

**Vishwet**
Date: 16 March 2026

---

## ACKNOWLEDGEMENT

I wish to express my deep sense of gratitude to my guide **Prof. Rajesh Kulkarni**, for their constant encouragement, technical insights, and valuable suggestions during the project work.

I also thank the Principal and the Head of the Department for providing the necessary facilities, infrastructure, and support required to complete this project.

Finally, I thank my parents and friends for their continuous support and motivation throughout the development of this system.

---

## ABSTRACT

The **EduTrack Smart Attendance System** is a robust Android-based mobile application designed to modernize and automate the traditional manual attendance marking process within educational institutions. Utilizing modern mobile technologies and the Firebase Cloud infrastructure, the system provides a specialized interface for three distinct user roles: Administrators, Teachers, and Parents/Students.

The application features a high-performance 8-column attendance grid, real-time Firestore synchronization for cross-role data availability, and professional PDF/CSV report generation modules tailored for academic audits. Key innovations include student promotion management, bulk data migration utilities, and a secure cloud-synced communication channel via the announcements module. The system emphasizes low latency, offline persistence, and Material Design 3 aesthetics, providing a premium user experience while ensuring data integrity.

---

## TABLE OF CONTENTS

1. **Chapter 1: Introduction**
   - 1.1 Motivation
   - 1.2 Objective
   - 1.3 Problem Statement
2. **Chapter 2: Literature Survey**
   - 2.1 Existing System
   - 2.2 Proposed System
3. **Chapter 3: System Requirements Specification (SRS)**
   - 3.1 Hardware Requirements
   - 3.2 Software Requirements
   - 3.3 Functional Requirements
   - 3.4 Non-Functional Requirements
4. **Chapter 4: System Design**
   - 4.1 System Architecture
   - 4.2 Database Design (NoSQL Schema)
   - 4.3 UI Design Principles
5. **Chapter 5: Implementation**
   - 5.1 Technology Stack
   - 5.2 Key Algorithms (Attendance Aggregation)
   - 5.3 Security & Persistence
6. **Chapter 6: Results & Screenshots**
   - 6.1 Administrator Workflow
   - 6.2 Teacher Workflow
   - 6.3 Parent/Student Insights
7. **Chapter 7: Conclusion & Future Scope**
   - 7.1 Conclusion
   - 7.2 Future Enhancements
8. **References**

---

## CHAPTER 1: INTRODUCTION

### 1.1 Motivation
Traditional attendance management is heavily dependent on manual registers, which are prone to physical damage, data errors, and manual tallying delays. The motivation behind **EduTrack** is to leverage the ubiquity of smartphones to create a real-time bridge between the classroom and the parents' home.

### 1.2 Objective
- **Efficiency:** Reduce attendance marking time to under 60 seconds per class.
- **Transparency:** Provide parents with instant visibility into their child's daily presence.
- **Digitization:** Eliminate paper usage through cloud-based storage and digital exports.
- **Reporting:** Generate automated analytics for identifying low-attendance trends.

### 1.3 Problem Statement
The current manual system lack scalability and real-time alerts. Teachers spend significant time on administrative tasks instead of instruction. Parents remain unaware of student absenteeism until the end of the semester. EduTrack addresses these gaps through an integrated mobile ecosystem.

---

## CHAPTER 2: LITERATURE SURVEY

### 2.1 Existing System
- **Manual Attendance:** Susceptible to manual errors, difficult to retrieve historical data, no parental notification mechanism.
- **Biometric Systems:** High installation cost, maintenance issues (fingerprint smudge), and slow throughput during peak school hours.

### 2.2 Proposed System
- **Cloud-Native:** No local server maintenance required (BaaS).
- **Role-Based Access:** Encapsulated dashboards for distinct user needs.
- **Offline First:** Local caching ensures functionality in low-connectivity zones.
- **Branded Exports:** Native PDF generation for professional record keeping.

---

## CHAPTER 3: SRS (SYSTEM REQUIREMENT SPECIFICATION)

### 3.1 Hardware Requirements
- **Development Platform:** PC with 16GB RAM, SSD, Java 17 Support.
- **User Device:** Android Smartphone (Minimum Android 8.0, 2GB RAM).

### 3.2 Software Requirements
- **Language:** Java 8+ (Android SDK).
- **Layout:** Android XML (Material Components).
- **Backend:** Firebase Firestore (NoSQL), Firebase Authentication.
- **Libraries:** iText7 (PDF), OpenCSV (CSV).

### 3.3 Functional Requirements
- **Authentication:** Role-aware login for Admin, Teacher, and Parent.
- **Attendance Marking:** Optimized Grid view with toggle states.
- **Management:** CRUD modules for Students, Teachers, and Classes.
- **Reports:** Date-filtered CSV/PDF exports.
- **Promotions:** Automated academic year migration logic.

---

## CHAPTER 4: SYSTEM DESIGN

### 4.1 System Architecture
The application follows a specialized **Client-Server-Cloud Architecture**:
1. **Client App:** Android Native (Java/XML).
2. **Sync Engine:** Firebase Firestore Real-time Listeners.
3. **Cloud Storage:** Firebase Storage for branding/logos.
4. **Auth Layer:** Firebase Identity Platform.

### 4.2 Database Design (Firestore)
- **`students`:** `fullName`, `rollNumber`, `standard`, `division`, `parentEmail`.
- **`attendance_records`:** Map of student IDs to "P/A" status, indexed by `{classId}_{subject}_{date}`.
- **`announcements`:** Broadcast documents with role-specific visibility filters.

---

## CHAPTER 5: IMPLEMENTATION

### 5.1 Technology Stack
- **Android SDK:** Core framework.
- **Firestore:** Global state synchronization.
- **OpenCSV:** Specialized parsing for bulk student uploads (Student Import).
- **iText7:** Dynamic PDF table generation with nested school branding.

### 5.2 Module Description
- **Admin:** Handles school structure, teacher assignments, and global auditing.
- **Teacher:** Focuses on class-subject attendance and recent history.
- **Parent:** Read-only dashboard with child performance graphs and school notices.

---

## CHAPTER 6: RESULTS & SNAPSHOTS

**EduTrack** successfully delivers a premium UI experience:
- **Admin Dashboard:** Overview of total students, teacher attendance, and quick management links.
- **Attendance Grid:** Optimized 8-column layout reducing screen scrolling.
- **PDF Preview:** Interactive modal ensuring report data accuracy before generation.
- **Success States:** Animated feedback for data saves and exports.

---

## CHAPTER 7: CONCLUSION & FUTURE SCOPE

### 7.1 Conclusion
The EduTrack system provides a comprehensive solution for digital attendance tracking. It removes the friction of manual bookkeeping and ensures a 100% transparent environment for both school staff and parents.

### 7.2 Future Enhancements
- Inclusion of **Geo-fencing** to ensure attendance is only marked within school premises.
- **Facial Recognition** via OpenCV for hands-free marking.
- **Auto-SMS/WhatsApp** alerts for absent students.

---

## REFERENCES
1. Google Developers. (2024). *Android Architecture Components*.
2. Firebase Console Documentation. (2024). *Cloud Firestore Data Modeling*.
3. iText Group. (2024). *iText 7 for Android Developer Guide*.
4. Material Design 3 Guidelines (m3.material.io).
