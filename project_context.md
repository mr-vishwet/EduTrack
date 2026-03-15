# EduTrack — Smart Attendance System
## Project Context File (project_context.md)
### Version 1.0 | Generated: 15 March 2026

---

## 1. PROJECT OVERVIEW

- **App Name:** EduTrack
- **Platform:** Android (Mobile Application)
- **Language:** Java (Backend Logic)
- **UI:** XML Layouts (Material Design 3)
- **Backend:** Firebase (Firestore + Authentication)
- **Offline Support:** Firebase Firestore offline persistence enabled
- **Export:** iText7-Android (PDF) + OpenCSV (CSV)
- **Icon Library:** Material Icons (com.google.android.material — built-in Android)
- **Session Management:** Firebase Auth persistent session + SharedPreferences (role caching)
- **Target:** Academic Android mini project — real school scenario

---

## 2. SCHOOL SCENARIO (FIXED DATA)

- **School Name:** Kendriya Vidyalaya Pune
- **Address:** Khadki, Pune - 411003, Maharashtra
- **Classes:** 8th A, 8th B, 9th A, 9th B, 10th A
- **Subjects:** Mathematics, Science, English, Social Studies, Computer
- **Teachers:** 5 teachers (each handles multiple classes)
- **Students per class:** 35 to 60 students
- **Each student has:** Roll Number, Full Name, Standard, Division, Password

---

## 3. GLOBAL DESIGN SYSTEM

### Color Palette
- **Primary Blue:** #1565C0
- **Primary Dark Blue (status bar):** #0D47A1
- **Secondary Teal:** #00ACC1
- **Present Green:** #43A047
- **Absent Red:** #E53935
- **Warning Amber:** #F57C00
- **Background:** #F5F7FA
- **Card Surface:** #FFFFFF
- **Gray Text:** #757575
- **Light Blue Tint:** #E3F2FD
- **Light Green Tint:** #E8F5E9
- **Light Red Tint:** #FFEBEE
- **Light Amber Tint:** #FFF8E1

### Typography (Roboto Font)
- **Headline Bold:** 22sp
- **Section Title Bold:** 16sp
- **Body Regular:** 14sp
- **Caption / Label:** 12sp
- **Small Note:** 10sp

### Layout Rules
- Card corner radius: 12dp (large cards), 8dp (list items)
- Card elevation shadow: 2dp–4dp
- Screen padding: 16dp
- Between-card spacing: 12dp
- App bar height: 56dp
- Button height: 48dp (standard), 56dp (primary save/action)
- FAB: 56dp, bottom-right, #1565C0 blue
- Status bar color: #0D47A1

### Components
- Inputs: Material outlined text fields, 8dp radius, icon inside field
- Dropdowns: Material exposed dropdown menus
- Buttons: Material rounded buttons (8dp radius)
- Chips: Material chips (filled for selected, outlined for default)
- Cards: MaterialCardView with strokeWidth=0, cardElevation=2dp
- RecyclerView list items: white cards, 8dp radius
- Tab bars: Material TabLayout with #1565C0 indicator
- Bottom nav: Material BottomNavigationView
- Snackbar for success/error feedback (no AlertDialog spam)

---

## 4. USER ROLES & LOGIN CREDENTIALS STRUCTURE

### Role 1 — Admin
- **Login:** Email + Password
- **Header Color:** #1565C0 Blue
- **Firebase Auth:** Separate admin UID with role="admin" in Firestore
- **Access:** Full access to all data

### Role 2 — Teacher
- **Login:** Email + Password
- **Header Color:** #00ACC1 Teal
- **Firebase Auth:** UID with role="teacher"
- **Access:** Only assigned classes

### Role 3 — Parent (Student Portal)
- **Login:** Roll Number + School-provided Password
- **Header Color:** #2E7D32 Dark Green
- **Firebase Auth:** UID with role="parent", linked to studentId
- **Access:** Own child's data only — READ ONLY

---

## 5. FIREBASE FIRESTORE DATA MODEL

### Collection: /users/{uid}
```
{
  uid: string,
  role: "admin" | "teacher" | "parent",
  name: string,
  email: string,
  phone: string,
  createdAt: timestamp
}
```

### Collection: /teachers/{teacherId}
```
{
  teacherId: string,
  name: string,
  email: string,
  phone: string,
  subjectSpecialization: string,
  assignedClasses: ["8A", "9B"],  // array of classIds
  userId: string,  // Firebase Auth UID
  status: "active" | "inactive"
}
```

### Collection: /classes/{classId}  (classId = "8A", "8B", "9A", "9B", "10A")
```
{
  classId: string,
  standard: "8" | "9" | "10",
  division: "A" | "B",
  classTeacherId: string,
  totalStudents: number,
  academicYear: "2025-2026"
}
```

### Collection: /students/{studentId}
```
{
  studentId: string,
  rollNumber: string,
  fullName: string,
  standard: string,
  division: string,
  classId: string,
  password: string,  // hashed
  userId: string,    // Firebase Auth UID
  parentName: string,
  parentPhone: string,
  status: "active" | "inactive" | "passout" | "holdback",
  admissionYear: string
}
```

### Collection: /subjects/{subjectId}
```
{
  subjectId: string,
  name: "Mathematics" | "Science" | "English" | "Social Studies" | "Computer",
  code: string
}
```

### Collection: /announcements/{announcementId}
```
{
  announcementId: string,
  title: string,
  body: string,
  audience: "all" | "teachers" | "parents",
  createdBy: string,  // admin UID
  createdAt: timestamp,
  isPinned: boolean,
  isActive: boolean
}
```

### Collection: /attendance/{attendanceId}
```
{
  attendanceId: string,  // composite: classId_subjectId_date
  classId: string,
  subjectId: string,
  teacherId: string,
  date: string,  // "YYYY-MM-DD"
  month: string, // "March-2026"
  studentStatuses: {
    "studentId1": "P",
    "studentId2": "A",
    ...
  },
  totalStudents: number,
  totalPresent: number,
  totalAbsent: number,
  savedAt: timestamp,
  lastEditedAt: timestamp
}
```

### Collection: /alumni/{studentId}  (for passed-out / promoted-out students)
```
{
  studentId: string,
  fullName: string,
  rollNumber: string,
  passoutYear: string,
  lastClass: string,
  archivedAt: timestamp
}
```

---

## 6. FIREBASE CONFIGURATION

### gradle dependencies
```
implementation 'com.google.firebase:firebase-auth:22.3.1'
implementation 'com.google.firebase:firebase-firestore:24.10.3'
implementation 'com.google.android.material:material:1.11.0'
implementation 'com.itextpdf:itext7-core:7.2.5'
implementation 'com.opencsv:opencsv:5.9'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'com.google.firebase:firebase-storage:20.3.0'
```

### Firestore Offline Persistence (Application class)
```java
FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
    .build();
FirebaseFirestore.getInstance().setFirestoreSettings(settings);
```

### Firestore Security Rules
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }
    match /attendance/{doc} {
      allow read: if request.auth != null;
      allow write: if get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "teacher"
                   || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }
    match /students/{doc} {
      allow read: if request.auth != null;
      allow write: if get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }
    match /announcements/{doc} {
      allow read: if request.auth != null;
      allow write: if get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }
  }
}
```

---

## 7. PROJECT STRUCTURE

```
app/src/main/java/com/edutrack/
│
├── EduTrackApplication.java          // Firebase init, offline persistence
│
├── activities/
│   ├── SplashActivity.java
│   ├── RoleSelectionActivity.java
│   ├── LoginActivity.java            // handles all 3 role logins
│   ├── admin/
│   │   ├── AdminDashboardActivity.java
│   │   ├── ManageStudentsActivity.java
│   │   ├── AddEditStudentActivity.java
│   │   ├── BulkUploadActivity.java
│   │   ├── ManageTeachersActivity.java
│   │   ├── AssignClassActivity.java
│   │   ├── ManageClassesActivity.java
│   │   ├── PromoteClassActivity.java       // 3-step flow
│   │   ├── CreateAnnouncementActivity.java
│   │   └── AdminReportsActivity.java
│   ├── teacher/
│   │   ├── TeacherDashboardActivity.java
│   │   ├── TakeAttendanceActivity.java
│   │   ├── AttendanceHistoryActivity.java
│   │   └── TeacherReportsActivity.java
│   └── parent/
│       ├── ParentDashboardActivity.java
│       └── ChildAttendanceDetailActivity.java
│
├── adapters/
│   ├── StudentGridAdapter.java        // RecyclerView 8-col grid, green/red toggle
│   ├── StudentListAdapter.java
│   ├── TeacherListAdapter.java
│   ├── AttendanceHistoryAdapter.java
│   ├── AnnouncementAdapter.java
│   └── ReportListAdapter.java
│
├── models/
│   ├── User.java
│   ├── Student.java
│   ├── Teacher.java
│   ├── ClassRoom.java
│   ├── Subject.java
│   ├── AttendanceRecord.java
│   └── Announcement.java
│
├── firebase/
│   ├── FirebaseAuthHelper.java        // login, logout, session
│   └── FirestoreHelper.java           // all CRUD operations
│
├── reports/
│   ├── PdfReportGenerator.java        // iText7 PDF with school branding
│   └── CsvExporter.java               // OpenCSV export
│
└── utils/
    ├── SessionManager.java            // SharedPreferences role + uid cache
    ├── DateUtils.java
    ├── ValidationUtils.java
    └── FileShareUtils.java            // share PDF/CSV via intent
```

---

## 8. SCREEN INVENTORY (37 TOTAL FRAMES)

### Common Screens (3)
- Screen 1: Splash Screen
- Screen 2: Role Selection Screen
- Screen 3: Login Screen (3 color variants: Admin=Blue, Teacher=Teal, Parent=Green)

### Admin Flow (16 screens)
- Screen 4: Admin Dashboard
- Screen 5: Manage Students List
- Screen 6: Bulk CSV Upload
- Screen 7: Manage Teachers List
- Screen 8: Manage Classes
- Screen 9: Create Announcement
- Screen 13: Assign Teacher to Class
- Screen 16: Announcements Feed (Admin — can edit/delete)
- Screen 17: Add/Edit Student Form
- Screen 18A: Promote Class — Step A (Select Source + Destination)
- Screen 18B: Promote Class — Step B (Review & Manage Students)
- Screen 18C: Promote Class — Step C (Confirm Modal + Success)
- Screen 19: Reports Dashboard
- Screen 20: Report Filter & Preview
- Screen 21: PDF Export Preview Modal
- Screen 22: Export Success Screen

### Teacher Flow (10 screens)
- Screen 10: Teacher Dashboard
- Screen 11: Take Attendance (default all-green state)
- Screen 11B: Take Attendance (in-progress mixed state)
- Screen 11C: Take Attendance (save confirmation state)
- Screen 12: Attendance History / Edit Records
- Screen 16: Announcements Feed (Teacher — read only)
- Screen 19: Reports Dashboard (Teacher variant — 4 types only)
- Screen 20: Report Filter & Preview (own classes only)
- Screen 21: PDF Preview Modal
- Screen 22: Export Success Screen

### Parent Flow (7 screens)
- Screen 14: Parent Login
- Screen 15: Parent Dashboard
- Screen 23: Child Attendance Detail — Overview Tab
- Screen 23B: Child Attendance Detail — Subject-wise Tab
- Screen 23C: Child Attendance Detail — Monthly Tab
- Screen 16: Announcements Feed (Parent — read only, filtered)
- Screen 22: Export Success (child report only)

---

## 9. ATTENDANCE SYSTEM — DETAILED LOGIC

### Take Attendance Flow
1. Teacher selects: Standard → Division → Subject → Date (default = today)
2. System checks Firestore: if attendance already exists for classId+subjectId+date → show edit warning
3. Fetch all students for selected classId ordered by rollNumber
4. Generate RecyclerView grid: GridLayoutManager(8 columns)
5. Each cell = MaterialCardView (40×40dp, 6dp radius, 4dp margin)
   - Default state: green #43A047, white bold roll number text = PRESENT
   - Tapped state: red #E53935, white roll number text = ABSENT
   - Tapping toggles between green ↔ red
6. Summary badge above save button: "Present: 38 | Absent: 4" — updates on every tap
7. Two action buttons above grid:
   - "Mark All Present" → set all cards green
   - "Same as Yesterday" → fetch previous attendance for same class+subject, apply same pattern
8. "Save Attendance" pinned button → writes to Firestore
9. Attendance document ID = "{classId}_{subjectId}_{date}" — prevents duplicates naturally

### Duplicate Prevention
- On TakeAttendanceActivity load: query Firestore for document ID = classId_subjectId_date
- If exists: show Snackbar "Attendance already saved. Tap to Edit." with EDIT action button
- Do NOT silently overwrite

### Attendance Percentage Calculation (Java-side from Firestore data)
```
totalPresent = sum of all "P" values for studentId across date range
totalClasses = count of attendance documents in date range
percentage = (totalPresent / totalClasses) * 100
```

---

## 10. PROMOTE CLASS — DETAILED LOGIC

### Step A — Select Screen
- Source class dropdown (e.g., 8th A)
- Destination standard dropdown (9th / 10th / Passout)
- Destination division dropdown (A / B)
- If Passout selected → division field hides, shows red archive warning chip

### Step B — Review Students Screen
- Fetch all students where classId = selected source
- Display as scrollable RecyclerView list
- Each student row has 3-state status chip:
  - "Promote" (green) → student.standard++ , student.classId = destination
  - "Left School" (red) → student.status = "inactive", removed from class
  - "Hold Back" (orange) → student stays in same class, no changes
- "Left School" rows: dimmed 60%, red strikethrough on name
- "Hold Back" rows: orange tinted background
- If destination has mixed divisions → per-student division chip appears
- Counter bar: "Promoting: 40 | Left: 2 | Hold: 0"

### Step C — Confirm + Success
- Modal with checkbox "I understand this is permanent" before enabling confirm
- On confirm: batch Firestore write updating all selected students
- 10th Passout students → moved to /alumni collection
- Attendance history NEVER deleted — preserved with original classId reference
- Success screen shows summary + "View New Class" button

---

## 11. REPORTS SYSTEM

### Report Types
| Type | Admin | Teacher | Parent |
|------|-------|---------|--------|
| Class-wise Summary | ✅ All classes | ✅ Own classes | ❌ |
| Monthly Report | ✅ | ✅ Own | ❌ |
| Student-wise Detailed | ✅ | ✅ Own students | ✅ Own child |
| Subject-wise | ✅ | ✅ Own | ❌ |
| Teacher Performance | ✅ | ❌ | ❌ |
| Overall School | ✅ | ❌ | ❌ |

### PDF Branding (All Reports)
- Header: School logo + "Kendriya Vidyalaya Pune" + address + divider
- Report title (teal, centered)
- Metadata: Generated on, Generated by, Report ID
- Data table with alternating row colors (#F5F7FA and white)
- Percentage column: green if ≥75%, red if below
- Footer: Page X of Y + Authorized Signature line
- Report ID format: ATT-2026-MAR-001

### CSV Naming Convention
- ClassReport_8thA_Mar2026.csv
- MonthlyReport_SchoolWide_Mar2026.csv
- StudentReport_RahulSharma_Mar2026.csv
- SubjectReport_Mathematics_8thA_Mar2026.csv

### Export Libraries
- PDF: implementation 'com.itextpdf:itext7-core:7.2.5'
- CSV: implementation 'com.opencsv:opencsv:5.9'

### Share Options After Export
- WhatsApp, Gmail, Google Drive, Print, Save to Downloads
- Use Android native share sheet (Intent.ACTION_SEND)

---

## 12. ANNOUNCEMENTS MODULE

### Create (Admin only)
- Title, Body (multiline), Audience (All / Teachers Only / Parents Only)
- Auto-set posting date = today
- Pin toggle (pinned = always top)

### Display (Teacher + Parent)
- Notification bell with red unread badge count
- Announcements feed: newest on top, pinned first
- Each card: megaphone icon, title, date chip, body preview, audience badge chip
- "Unread" orange dot if not yet seen (track in SharedPreferences per UID)
- "Read More" for long announcements

### Firestore Query
```java
db.collection("announcements")
  .whereEqualTo("isActive", true)
  .whereIn("audience", Arrays.asList("all", userAudienceType))
  .orderBy("isPinned", Direction.DESCENDING)
  .orderBy("createdAt", Direction.DESCENDING)
  .get()
```

---

## 13. BULK STUDENT UPLOAD

### CSV Template Format
```
rollNumber,fullName,standard,division,parentName,parentPhone,password
01,Rahul Sharma,8,A,Mrs. Sharma,9876543210,student123
02,Priya Patel,8,A,Mr. Patel,9876543211,student456
```

### Upload Flow
1. Admin downloads CSV template
2. Fills in student data
3. Uploads CSV via file picker
4. App parses CSV using OpenCSV
5. Preview table shown (first 5 rows)
6. Admin taps "Upload & Save"
7. Batch write to Firestore /students collection
8. Auto-create Firebase Auth accounts for each student (email = rollNumber@school.edu)

---

## 14. SESSION MANAGEMENT

```java
// SharedPreferences keys
PREF_KEY_ROLE = "user_role"         // "admin" | "teacher" | "parent"
PREF_KEY_UID = "user_uid"
PREF_KEY_NAME = "user_name"
PREF_KEY_CLASS = "user_class"       // for teachers and parents

// On app launch → check Firebase Auth currentUser
// If not null → read role from SharedPreferences → route to correct dashboard
// Logout → Firebase Auth signOut() + SharedPreferences.clear()
```

---

## 15. KEY CONSTRAINTS

- App must work OFFLINE — Firestore caches all data locally
- No internet = read from cache, write queued and synced on reconnect
- Students/Parents CANNOT edit any data — enforce in UI (no edit buttons) + Firestore rules
- Duplicate attendance prevention — document ID collision strategy
- Attendance history is NEVER deleted on class promotion
- 10th passout students go to /alumni collection, not deleted
- PDF reports must include school name, address, signature line
- All monetary/sensitive data: none (school attendance app only)
