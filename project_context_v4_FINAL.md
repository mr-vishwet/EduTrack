# EduTrack — Smart Attendance System
## project_context.md — FINAL v4.0
### Updated: 15 March 2026 | ALL 29 screens delivered and documented
### Previous versions: v1.0 (original plan) → v2.0 (14 admin screens) → v3.0 (21 screens) → v4.0 (COMPLETE)

---

## ⚠️ REVISION NOTES (v3.0 → v4.0)
8 new screens added in this revision:
- Role Selection Screen (confirmed layout)
- Promote Class — Source/Destination selector
- Review Students — per-student status dropdown
- Promotion Result — success modal
- Reports Dashboard — 6 report types with ADMIN ONLY badges
- Report Filters + Preview — table preview with Export CSV / Export PDF
- PDF Export Preview — modal with school branding preview
- Student Detailed Report — tabbed (Overview / Subject-wise / Monthly) + share/download icons

All previous screens from v2.0 and v3.0 retained in full.

---

## 1. PROJECT OVERVIEW

- **App Name:** EduTrack
- **Tagline:** Smart Attendance for Smart Schools
- **Platform:** Android (Mobile Application)
- **Language:** Java (Backend Logic)
- **UI:** XML Layouts (Material Design 3)
- **Backend:** Firebase Firestore + Firebase Authentication
- **Offline Support:** Firestore offline persistence enabled (PersistenceEnabled=true)
- **PDF Export:** iText7-Android (com.itextpdf:itext7-core:7.2.5)
- **CSV Export:** OpenCSV (com.opencsv:opencsv:5.9)
- **Icon Library:** Material Icons — google.android.material
- **Session Management:** Firebase Auth persistent + SharedPreferences role cache
- **Version shown in UI:** VERSION 2.4.0 (BUILD 102)

---

## 2. SCHOOL SCENARIO (FIXED SEED DATA — FINAL)

- **School Name:** Kendriya Vidyalaya Pune
- **Address:** Ganeshkhind Road, Pune, Maharashtra 411007
  (PDF preview reveals full address — update from "Khadki" to Ganeshkhind Road)
- **Classes (8 total — FINAL):** 8A, 8B, 9A, 9B, 9C, 10A, 10B, 10C
  - 8A: 42 students | 8B: 40 students | 9A: 38 students | 9B: 38 students
  - 9C: 45 students | 10B: 36 students | 10C: 40 students | 10A: ~35 students
- **Subjects (9 total — FINAL):**
  Mathematics, Science, English, Social Studies, Physics,
  Computer, Hindi, General Science, History
- **Teachers:** 5 teachers seed data (Mr. Rajesh Kumar = Mathematics, confirmed)
  PDF shows: Mr. S. Sharma (additional teacher for Class X-A)

---

## 3. GLOBAL DESIGN SYSTEM — CONFIRMED FINAL

### Color Palette
- **Primary Blue:** #1565C0 (app bars Admin, all save/confirm buttons)
- **Dark Blue status bar:** #0D47A1
- **Teal:** #00ACC1 (Teacher theme, active chips)
- **Present Green:** #43A047
- **Absent Red:** #E53935
- **Warning Amber:** #F57F17
- **Hold Back Orange:** #E65100 / #FF6D00
- **Background:** #F0F4F8
- **Card Surface:** #FFFFFF
- **Admin Login form bg:** #0F1923 (dark navy)
- **Gray body:** #616161 | Caption: #9E9E9E
- **Selected row tint:** #E3F2FD
- **Green badge bg:** #E8F5E9 | Red badge bg:** #FFEBEE
- **Unread dot:** #F57F17 orange
- **ADMIN ONLY badge:** #E3F2FD blue tint bg + #1565C0 text

### Role Color Mapping
| Role | Primary | Header | Button | Form BG |
|------|---------|--------|--------|---------|
| Admin | #1565C0 Blue | #1565C0 | #1565C0 | Dark #0F1923 |
| Teacher | #00ACC1 Teal | #00ACC1 | #00ACC1 | White #FFFFFF |
| Parent | #2E7D32 Green | #2E7D32 | #2E7D32 | White card |
| Student | #2E7D32 Green | #2E7D32 | — | White |

### Typography
- App name Splash: 36sp Bold White
- App bar title: 20sp Bold White
- Section headings: 18sp Bold #1A1A1A
- Card stat numbers: 28-32sp Bold
- Body: 14sp Regular #424242
- Caption: 12sp Regular #9E9E9E
- Bottom button: 15sp Bold White uppercase
- Version footer: 11sp Regular #9E9E9E

### Layout Constants
- Screen horizontal padding: 16dp
- Card corner radius: 12dp (main), 8dp (list items), 24dp (login card top)
- Card elevation: 2dp | Between cards: 12dp
- App bar: 56dp | Bottom nav: 64dp
- Primary button: 52-56dp height, full width, 12dp radius
- FAB: 56dp circle, bottom-right, 16dp margin

---

## 4. SCREEN-BY-SCREEN EXACT SPECIFICATIONS (ALL 29 SCREENS)

---

### SCREEN 01 — Splash Screen
**File:** edutrack_splash_screen | **Status: ✅ DELIVERED**

- Full screen #1565C0 → #1976D2 gradient
- Center: 120dp white circle + white graduation cap icon 64dp
- "EduTrack" 36sp Bold White
- "Smart Attendance for Smart Schools" 16sp Regular White
- Bottom: white circular progress 32dp (~80dp from bottom)
- NO bottom nav | Pure ConstraintLayout

**Java:** SplashActivity.java — 2s Handler → check Firebase session → route by role

---

### SCREEN 02 — Role Selection Screen
**File:** role_selection_screen | **Status: ✅ DELIVERED**

**App Bar:** #1565C0 blue, graduation cap icon left, "Welcome to EduTrack" bold white title (NO back arrow)

**Subtitle:** "Select your role to continue" — gray #616161, 15sp, 24dp top margin below app bar

**Role Cards (3 stacked, white, 12dp radius, 2dp elevation, full width with 16dp margin):**
Each card has:
- Left colored accent bar (4dp wide, full card height, rounded)
- Left: colored circle (40dp) with role icon inside (white icon)
- Center: role name bold 17sp + subtitle gray 13sp
- Right: chevron-right gray icon (20dp)

Card details:
- **Admin card:** Blue accent (#1565C0) | Blue circle | admin_panel_settings icon
  Title: "Admin" | Subtitle: "Manage school data"
- **Teacher card:** Teal accent (#00ACC1) | Teal circle | school/person_school icon
  Title: "Teacher" | Subtitle: "Take & manage attendance"
- **Student card:** Green accent (#2E7D32) | Green circle | face/emoji icon
  Title: "Student" | Subtitle: "View your attendance"

⚠️ NOTE: Only 3 roles shown — Admin, Teacher, Student
Parent login is accessed through Student card OR a separate "Parent" option not shown here
IMPLEMENTATION: Add 4th "Parent" card below Student with green accent variant:
- Title: "Parent" | Subtitle: "View your child's attendance" | Icon: family_restroom

**Footer:** "Powered by EduTrack Systems" gray 12sp centered, bottom of screen

**XML:** activity_role_selection.xml
**Java:** RoleSelectionActivity.java — onCardClick → startActivity(LoginActivity, role=ROLE)

---

### SCREEN 03 — Admin Login
**File:** admin_login_screen | **Status: ✅ DELIVERED**

**Top ~40%:** #1565C0 Blue header | Back arrow white | "Admin Login" 20sp Bold white centered
"Sign in to your account" 22sp Bold white below title (large, prominent)

**Bottom ~60%:** Dark navy #0F1923 — full screen below header (NOT a card — full dark bg)

**Input Fields (dark style):**
- Background: #1E2A3A, 12dp radius
- Labels: "Email Address" / "Password" white 14sp
- Field 1: mail icon gray + "admin@school.edu" placeholder gray
- Field 2: lock icon + "Enter your password" + eye toggle white right
- "Forgot Password?" teal #00ACC1 right-aligned 13sp
- Login button: full-width 52dp #1565C0, "Login →" white bold + arrow icon
- Footer: "VERSION 2.4.0 (BUILD 102)" #9E9E9E 11sp centered

---

### SCREEN 04 — Teacher Login
**File:** teacher_login_screen | **Status: ✅ DELIVERED**

**Top ~45%:** #00ACC1 Teal solid | Back arrow | "Teacher Login" bold white | "Sign in to your account" large white
**Form (WHITE background — NOT dark):**
- "Email" + "Password" labels black #212121
- Outlined white fields with gray border
- "Forgot Password?" teal right-aligned
- Login button: full-width teal #00ACC1, "Login" white (no arrow, not uppercase)
- Footer: "Version 2.4.0" (no BUILD number)

---

### SCREEN 05 — Parent Login
**File:** parent_login_screen | **Status: ✅ DELIVERED**

⚠️ DIFFERENT AUTH: Roll Number + Password (NOT email)

**Top ~45%:** #2E7D32 dark green | family icon (people silhouette) white 48dp centered
"Parent Login" 24sp Bold white | "View your child's attendance" 14sp white

**Form Card:** White, 24dp TOP radius, overlapping green header (bottom-sheet style)
- "Roll Number" + ID card icon + "Enter child's roll number"
- "Password" + lock icon + eye toggle
- "Forgot Password?" green #2E7D32 right-aligned
- Button: "LOGIN AS PARENT" full-width #2E7D32 52dp uppercase bold
- Helper text below button: italic gray "Use your child's roll number and school-provided password"
- Footer: "VERSION 2.4.0"

**Auth logic:** Firestore query students by rollNumber → verify password → create session

---

### SCREEN 06 — Admin Dashboard
**File:** admin_dashboard_light_mode | **Status: ✅ DELIVERED**

**App Bar:** #1565C0 | "AD" white circle avatar | "Good Morning, Admin" + date subtitle #B3E5FC
Right: bell icon + logout icon white

**2×2 Stat Cards (each ~160dp):**
- Total Students: teal group icon + 247
- Total Classes: blue school icon + 12 ⚠️ Fix to 8 (actual class count)
- Total Teachers: orange person icon + 45 ⚠️ Fix to 5
- Today's Attendance: green check_circle icon + 89%
⚠️ ALL 4 labels say "Total Students" in HTML — Stitch bug. Fix labels in Java.

**Quick Actions:** "Manage Students" + "Manage Teachers" pill buttons (light blue bg, blue text)

**Today's Highlights card:** Morning Attendance Marked + Staff Meeting at 2:00 PM

**Bottom Nav:** Home (active) | Students | Teachers | Reports

---

### SCREEN 07 — Manage Students
**File:** manage_students_light_mode | **Status: ✅ DELIVERED**

- Pill search bar + horizontal filter chips (All Classes / 8th A / 8th B / 9th A...)
- Student cards: blue roll number circle + name + class·subject + edit/delete icons
- FAB: blue + icon bottom-right
- Students: Aakash Sharma, Priya Patel, Rohan Gupta, Sneha Verma, Vikram Singh (8th A)
- Bottom Nav: Students tab active

---

### SCREEN 08 — Add Student Form
**File:** add_student_form_light_mode | **Status: ✅ DELIVERED**

App bar: X close + "Add Student" + "SAVE" green right
Fields: Full Name | Roll Number | Standard (dropdown) | Division (dropdown) | Password | Phone (optional)
Section: "Additional Info" in blue #1565C0 as section header
Bottom: "ADD STUDENT" blue button with save icon

---

### SCREEN 09 — Bulk Upload Students
**File:** bulk_student_upload_light_mode | **Status: ✅ DELIVERED**

- Dashed border drop zone card: cloud upload icon + "Upload CSV File"
- "Download Template" outlined teal button + "Choose CSV File" filled blue button
- 4-step stepper with blue circle numbers connected by vertical line
- "UPLOAD & SAVE" — disabled gray until file chosen → active blue

---

### SCREEN 10 — Assign Classes (3 Variants) + Confirm Modal
**Files:** assign_classes_* | **Status: ✅ DELIVERED**

Use Variant C (with toggle) as final implementation:
- Teacher profile card: teal "MR" avatar + Active green chip
- Search bar "Search classes by name..."
- Per class: checkbox (teach) + class name + student count + subject chip + toggle (class teacher)
- CLASS TEACHER badge chip shows on rows where toggle is ON
- Selected rows: #E3F2FD light blue background highlight
- Modal: "Assign Class Teacher?" with CANCEL + CONFIRM text buttons

Teacher data: `assignedClasses[]` + `classTeacherOf: String` (single class)

---

### SCREEN 11 — Attendance History
**File:** attendance_history_light_mode | **Status: ✅ DELIVERED**

- Date range filter (From: 10 Mar / To: 15 Mar) + teal Filter button + "Showing 12 records"
- Cards with LEFT #1565C0 blue 3dp border
- Each card: class·subject bold + "SAVED" green chip + date + student count + Present/Absent dots + edit/delete
- Bottom Nav: History tab active (clock icon)

---

### SCREEN 12 — Attendance Reports (Class-wise tab)
**File:** attendance_reports_light_mode | **Status: ✅ DELIVERED**

- 3 tabs: Class-wise (active) | Date-wise | Student-wise
- Summary cards: teal "87% Overall" + dark blue "214/247 Today Present"
- Class progress bars: 8th A 91% | 8th B 85% | 9th A 94% | 9th B 78% | 10th A 88%
- Percentage red if below 75%

---

### SCREEN 13 — Export Success
**File:** export_success_light_mode | **Status: ✅ DELIVERED**

- Top: teal circular progress + document icon + "Generating your report..." + teal progress bar
- Bottom: green checkmark circle + "Export Successful!" + file info card
- Buttons: "Open File" blue filled + "Share via..." teal outlined + "Generate Another Report" text
- Quick Share row: WhatsApp | Gmail | Drive | Print (40dp icon circles)
- Footer link: "View all exports in Downloads folder"

---

### SCREEN 14 — Create Announcement
**File:** create_announcement_light_mode | **Status: ✅ DELIVERED**

- App bar: X + "New Announcement" + "POST" white right
- Fields: Announcement Title (⚠️ fix wifi icon → campaign icon) + Message textarea + Audience dropdown
- Info rows: Posting Date (auto today) + "Pin this announcement" toggle
- "Post Announcement" blue bottom button with send icon

---

### SCREEN 15 — Announcements Feed
**File:** announcements_feed_light_mode | **Status: ✅ DELIVERED**

- Category filter chips: All | Academic | Events | Sports
- Cards: orange megaphone icon + title + date chip + preview text + audience badge + UNREAD dot
- Audiences: "All" (blue pill) | "Teachers" (teal pill) | "Parents" (green pill)
- Bottom Nav: Feed tab active (megaphone)

---

### SCREEN 16 — Teacher Dashboard
**File:** teacher_dashboard | **Status: ✅ DELIVERED**

- App bar teal: "MR" avatar + "Hello, Mr. Rajesh" + "Class Teacher · 8th A, 9th B" subtitle
- NO bell icon (teacher has no notifications bell)
- My Classes: horizontal scroll cards (8th A ● / 9th B ● / 10th C) — dot = today pending
- Take Attendance banner: teal left-border card + "Start Attendance →" teal button
- Recent Activity: 3 cards (8th A·Mathematics 42/42, 9th B·Physics 35/38, 8th A·General Science 40/42) + edit icon
- Bottom Nav (TEAL): Home | Students | Reports | Settings

---

### SCREEN 17 — Take Attendance Grid
**File:** take_attendance_screen | **Status: ✅ DELIVERED — CORE FEATURE**

- Sub-header: "8th A · Mathematics · 15 Mar 2026" + pencil edit icon
- Quick action buttons: "✓ Mark All Present" + "⏱ Same as Yesterday" (both outlined teal)
- Grid: 8-column GridLayoutManager, 42 tiles, each 56dp square, 8dp margin, 8dp radius
  - GREEN #43A047 = Present | RED #E53935 = Absent | tap to toggle
  - Absent in screenshot: rolls 05, 12, 28, 35
- Live counter pill: "Present: 38 | Absent: 4" — pinned above save button
- SAVE ATTENDANCE: full-width BLUE #1565C0 56dp pinned bottom (NOT teal)

```java
recyclerView.setLayoutManager(new GridLayoutManager(this, 8));
// StudentAttendanceGridAdapter: toggle green/red on item click, update live counter
```

---

### SCREEN 18 — Student My Attendance (Student Dashboard)
**File:** student_dashboard | **Status: ✅ DELIVERED**

- App bar GREEN #2E7D32, TWO-LINE: "My Attendance" + "Rahul Sharma · Roll No. 12 · 8th A"
- Overall card: circular ring 64dp green (#2E7D32) + "87%" center + Present/Absent pills
- Subject-wise list (dividers, no progress bars, % text right):
  🔵 Mathematics 91% | 🟣 Science 85% | 🟡 English 72% (RED) | 🟠 Social Studies 88% | 🔵 Hindi 94%
- Monthly Summary: horizontal scroll cards with left green 4dp border

---

### SCREEN 19 — Parent Dashboard
**File:** parent_dashboard | **Status: ✅ DELIVERED**

- App bar GREEN: child icon + "Parent Dashboard" + "Welcome, Mrs. Sharma"
- Bell with RED badge "2" + logout icon
- Child card: light green bg + smiley avatar + "Rahul Sharma" + "ACTIVE" chip + "Roll No. 12 · Class 8th A"
- Stats: Overall 87% +2% (green left border) | This Month 18/22 (blue left border)
- Announcements: 2 cards with orange megaphone square icon + "Read More →" teal link
- Bottom Nav (GREEN): Dashboard | Attendance | Exams | Profile

---

### SCREEN 20 — Student Detailed Report
**File:** student_detailed_report_light_mode | **Status: ✅ DELIVERED**

- App bar: #1565C0 blue, back arrow, "Student Report", share icon + download icon (both white, right)
- Student card: "RS" teal avatar + "Rahul Sharma" + "Roll No. 12 · Class 8th A" + "87%" green chip
- 3 TABS: Overview (active, underline) | Subject-wise | Monthly
- Present: 68 days (green progress bar) | Absent: 7 days (red progress bar — short)

**Attendance Log section:**
- Each entry: colored dot + date bold + subject·Period number gray + P/A badge chip right
  - Monday, 12 Oct | Mathematics • Period 1 → "P" green chip
  - Friday, 09 Oct | Science • Period 3 → "A" red chip
  - Thursday, 08 Oct | English • Period 2 → "P" green chip
  - "View All" teal link right of section header

**Monthly Summary (horizontal scroll cards — NOT vertical):**
- October: 18/20 Days | "2 Absents" amber text
- September: 22/22 Days | "Perfect!" green text
- August: 19/21 Days | "2 Absents" amber text

**Subject-wise Analytics:**
- Material icon per subject: functions (Math) | science (Physics) | history_edu (History)
- Mathematics 92% green | Physics 64% RED | History 88% green
- Low % row gets light red #FFEBEE background highlight

**Bottom buttons (dual, side by side):**
- "Export CSV" outlined teal button
- "Export PDF" filled blue #1565C0 button
Both 48dp height, equal width

---

### SCREEN 21 — Reports Dashboard
**File:** reports_dashboard_light_mode | **Status: ✅ DELIVERED**

**App Bar:** #1565C0, back arrow, "Attendance Reports", filter icon right (white)

**Top Stats Row (4 colored pill chips, 2×2 grid):**
- Dark blue pill: school icon + "School Avg: 87%"
- Green pill: check_circle icon + "Today: 214/247"
- Teal pill: calendar_today icon + "This Month: 91%"
- Orange pill: trending_up icon + "Last Week: 89%"

**"CHOOSE REPORT TYPE" section (gray uppercase label):**
6 report type cards (white, 8dp radius, left colored accent bar):
1. **Class-wise Summary** — bar_chart icon (blue) | "Attendance breakdown by class & division" | Blue accent
2. **Monthly Report** — calendar_month icon (teal) | "Month-wise attendance trends" | Teal accent
3. **Student-wise Detailed** — person icon (green) | "Individual student full report" | Green accent
4. **Subject-wise Report** — file_open icon (purple) | "Attendance per subject & teacher" | Purple accent
5. **Teacher Performance** — supervisor_account icon (orange) | "Classes managed & submission rate" | Orange accent
   → "ADMIN ONLY" badge chip (blue outlined, right of title) — hidden for teacher role
6. **Overall School Report** — domain icon (red) | "Complete school summary" | Red accent
   → "ADMIN ONLY" badge chip — hidden for teacher role

⚠️ Role-based visibility:
- Admin: all 6 report types shown
- Teacher: show only first 4 (hide Teacher Performance + Overall School Report)
- Implement via: reportItem.isAdminOnly → View.GONE for teacher role

---

### SCREEN 22 — Report Filters & Preview
**File:** report_filters_preview_light_mode | **Status: ✅ DELIVERED**

**App Bar:** #1565C0, back arrow, "Class-wise Report", help/? icon right (white)

**"Set Filters" Card (white, 12dp radius):**
- filter_list icon + "Set Filters" bold title
- "SELECT CLASS" gray label + dropdown: groups icon + "All Classes" + chevron
- "SELECT SUBJECT" gray label + dropdown: book icon + "All Subjects" + chevron
- Date range row: "FROM DATE" label + date field (calendar_today icon + "DD/MM/YYYY") + "TO DATE" field
- "▶ Apply Filters & Preview" full-width blue #1565C0 button 52dp

**"Report Preview" Card (white, 12dp radius):**
- visibility icon + "Report Preview" bold title + "First 5 rows shown" gray chip right
- Table with blue header row (Class | Present | Absent | %):
  | Class | Present | Absent | % |
  |-------|---------|--------|---|
  | 8th A | 38 | 4 | 90.5% (green) |
  | 8th B | 35 | 7 | 83.3% (green) |
  | 9th A | 30 | 12 | 71.4% (RED — below 75%) |
  | 9th B | 41 | 1 | 97.6% (green) |
  | 10th A | 28 | 14 | 66.7% (RED — below 75%) |
  | Overall | 150 | 9 | 94.3% (green, bold) |
- "Overall" row: bold, slightly different bg

⚠️ Color rule for percentage column: RED if < 75%, GREEN if ≥ 75%

**Branding toggle (below preview card):**
- "Include school header & branding in PDF" label + toggle (right side, partially visible)

**Bottom Buttons (side by side, equal width):**
- "Export CSV" outlined teal button (csv icon left)
- "Export PDF" filled blue #1565C0 button (pdf icon left)
Both 48dp height

**Java:** ReportFilterActivity.java — query Firestore with filters → populate RecyclerView table
Use RecyclerView with GridLayoutManager(4) for table layout, or custom TableLayout

---

### SCREEN 23 — PDF Export Preview Modal
**File:** pdf_export_preview_light_mode | **Status: ✅ DELIVERED**

⚠️ This is a MODAL DIALOG overlaying the Reports screen — NOT a separate Activity
Use MaterialAlertDialog or BottomSheetDialogFragment

**Modal container:** White, 16dp radius, ~90% screen width, shadow elevation 8dp
Blue header bar: "PDF Preview" bold white 18sp + X close icon right (white)

**PDF Thumbnail (white card inside modal, shadow, A4 portrait ratio):**
School branding header:
- school icon (teal) + "Kendriya Vidyalaya Pune" bold 11sp
- "Ganeshkhind Road, Pune, Maharashtra 411007" gray 9sp
- Horizontal blue divider line
- "Class-wise Attendance Report" green/teal title, centered
- Meta row: Class: X - A | Date: Oct 2023 | Teacher: Mr. S. Sharma | Generated: 24/10/23
- Table: Roll No | Student Name | Status (Present green / Absent red)
  - 101 Aditya Verma | Present
  - 102 Ananya Singh | Present
  - 103 Deepak Joshi | Absent
  - 104 Isha Gupta | Present
- Footer: "Page 1 of 3" bottom-left italic | "Authorized Signature" bottom-right italic with line

**Below thumbnail:**
- description icon + "3 Pages" gray chip | hard_drive icon + "~287 KB" gray chip

**Settings row (inside modal):**
- "Open file after download" label + toggle switch ON (blue) right

**Modal bottom buttons (side by side):**
- "CANCEL" outlined white/gray button
- "DOWNLOAD PDF" filled #1565C0 blue + download icon left

**iText7 PDF generation — school header block:**
```java
// PdfReportGenerator.java
document.add(new Image(schoolLogo)); // EduTrack graduation cap icon
document.add(new Paragraph("Kendriya Vidyalaya Pune").setBold());
document.add(new Paragraph("Ganeshkhind Road, Pune, Maharashtra 411007"));
document.add(new LineSeparator(new SolidLine()));
document.add(new Paragraph("Class-wise Attendance Report")
    .setTextAlignment(TextAlignment.CENTER)
    .setFontColor(new DeviceRgb(0x00, 0xAC, 0xC1))); // teal title
// Add footer: "Authorized Signature" line
```

---

### SCREEN 24 — Promote Class (Source/Destination Selector)
**File:** promote_class_select_review | **Status: ✅ DELIVERED**

**App Bar:** #1565C0, back arrow, "Promote Class", upload icon right (white, 20dp)

**Warning Banner (top, below app bar):**
- Light amber #FFF8E1 background, 8dp radius
- warning_amber icon orange left + "Review carefully. Promotion affects all selected students permanently." orange text 13sp

**Select Source Class Card (white, 12dp radius):**
- "Select Source Class" bold 16sp
- "Source Class" gray label + dropdown: "8th A · 42 students" + chevron-down
- Preview chips row: "8th A" filled blue pill → arrow → "9th A" outlined pill
  (shows auto-suggested destination — clicking confirms or user changes below)

**Select Destination Class Card (white, 12dp radius):**
- "Select Destination Class" bold 16sp
- Two side-by-side dropdowns: "Standard" (9th) + "Division" (A) each with chevron-down
- Destination auto-suggested from source (8th A → 9th A)

**Bottom Button:**
- "Review Students →" OUTLINED blue button (#1565C0 border + text, white bg), full-width 52dp
  ⚠️ NOTE: Outlined (not filled) — confirmation happens on next screen

---

### SCREEN 25 — Review Students (Promotion Review)
**File:** review_students_light_mode | **Status: ✅ DELIVERED**

**App Bar:** #1565C0, back arrow, "Review Students — 8th A", "42 Students" gray chip in app bar below title

**Summary Chips Row (3 scrollable chips):**
- check_circle green chip: "Promoting: 40"
- cancel red chip: "Left School: 2"
- move_up teal chip: "8th A → 9th B" (destination indicator)

**Search Bar:** "Search for student..." outlined pill

**Select All / Deselect All:** Two outlined teal buttons, equal width, side by side

**Student List — 3 status variants:**
Each row card (white, 8dp radius, 12dp padding):
- Checkbox (left) + roll number chip (gray outlined) + student name bold + status dropdown (right)

1. **PROMOTE (default):** White bg | Blue checkbox checked | "Promote ▼" green outlined dropdown
   - Example: 01 Rahul Sharma | Promote ▼
   - 04 Aman Gupta | Promote ▼
   - 05 Priya Das | Promote ▼

2. **LEFT SCHOOL:** White bg | Checkbox UNCHECKED (greyed) | Name with strikethrough gray | "Left School ▼" red outlined dropdown
   - "Will be removed" italic red 12sp below name
   - Example: 02 Aditi Verma (strikethrough) | Left School ▼

3. **HOLD BACK:** Light orange bg #FFF3E0 | Blue checkbox | Name bold + "Stays in 8th A" orange 12sp below | "Hold Back ▼" orange outlined dropdown
   - Roll number chip: orange border + orange text
   - Example: 03 Karan Singh | Hold Back ▼

**Status Dropdown options (per student):** Promote | Hold Back | Left School

**Bottom Summary Bar (pinned, gray bg, above Confirm button):**
"✓ PROMOTING 40" green | "× LEFT: 2" red | "⟳ HOLD: 0" orange — 3 columns

**Confirm & Promote Button:**
- "Confirm & Promote →" full-width #1565C0 blue 56dp with arrow icon

**Java logic:**
```java
enum StudentPromotionStatus { PROMOTE, HOLD_BACK, LEFT_SCHOOL }
// On confirm: batch Firestore write
// PROMOTE students: update classId, standard, division
// HOLD_BACK students: status remains, class unchanged
// LEFT_SCHOOL students: move to /alumni collection, set status=passout
// Preserve attendance history: attendanceRecords remain linked to studentId
```

---

### SCREEN 26 — Promotion Result / Success
**File:** promote_class_success | **Status: ✅ DELIVERED**

⚠️ This appears to be a DIALOG or FULL-SCREEN result Activity (white background, X top-left)

**Header:** X close icon top-left (no app bar — full-screen dialog style)
"Promotion Result" centered bold 18sp

**Success Icon:** Green circle 64dp (#43A047) + white checkmark — animated on appear

**Result Text:**
- "Promotion Complete!" bold 22sp
- "40 students successfully moved to 9th B" gray 14sp

**Summary Chips (2 side by side):**
- "✓ Promoted: 40" green outlined chip
- "👤- Left School: 2" red outlined chip with person-remove icon

**"What happened?" Info Card (white, 12dp radius, gray border):**
- "What happened?" bold 16sp
- 3 bullet points with check_circle_outline blue icons:
  - "Student records updated with new grade level and section."
  - "Attendance history preserved and linked to academic profile."
  - "Left School students archived for historical reporting."

**Illustration:** Light blue/gray square (48dp graduation cap icon, semi-transparent) — decorative

**Bottom Buttons (stacked):**
- "VIEW 9TH B STUDENTS" outlined teal button, full-width 52dp
- "BACK TO DASHBOARD" filled blue #1565C0 button, full-width 52dp

**Java:** After batch Firestore write completes → finish PromoteClassActivity → show this result

---

## 5. DATA MODELS (FINAL — v4.0)

### /students/{studentId}
```json
{
  "studentId": "string",
  "rollNumber": "string",
  "fullName": "string",
  "standard": "string",
  "division": "string",
  "classId": "string",
  "password": "string",
  "userId": "string",
  "parentName": "string",
  "parentPhone": "string",
  "status": "active|holdback|passout|leftschool",
  "admissionYear": "number"
}
```

### /teachers/{teacherId}
```json
{
  "teacherId": "string",
  "name": "string",
  "email": "string",
  "phone": "string",
  "subjectSpecialization": "string",
  "assignedClasses": ["8A", "8B"],
  "classTeacherOf": "8A",
  "userId": "string",
  "status": "active|inactive"
}
```

### /attendance/{attendanceId}
```json
{
  "attendanceId": "8A_MATH_P1_2026-03-15",
  "classId": "string",
  "subjectId": "string",
  "teacherId": "string",
  "date": "YYYY-MM-DD",
  "period": 1,
  "month": "March-2026",
  "studentStatuses": { "studentId": "P|A" },
  "totalStudents": 42,
  "totalPresent": 38,
  "totalAbsent": 4,
  "savedAt": "timestamp",
  "lastEditedAt": "timestamp"
}
```

### /announcements/{announcementId}
```json
{
  "announcementId": "string",
  "title": "string",
  "body": "string",
  "audience": "all|teachers|parents",
  "category": "academic|events|sports|general",
  "isPinned": false,
  "isActive": true,
  "createdBy": "string",
  "createdAt": "timestamp"
}
```

### /alumni/{studentId}
```json
{
  "studentId": "string",
  "fullName": "string",
  "rollNumber": "string",
  "passoutYear": 2026,
  "lastClass": "8A",
  "promotedTo": "9B|null",
  "exitType": "promoted|leftschool",
  "archivedAt": "timestamp"
}
```

### /classes/{classId}
```json
{
  "classId": "8A",
  "standard": "8",
  "division": "A",
  "classTeacherId": "string",
  "totalStudents": 42,
  "academicYear": "2025-2026"
}
```

### /subjects/{subjectId}
```json
{ "subjectId": "MATH", "name": "Mathematics", "code": "MATH" }
```
Values: MATH, SCI, ENG, SST, PHY, COMP, HINDI, GENSCI, HIST

---

## 6. NAVIGATION STRUCTURE (FINAL CONFIRMED)

### Role Selection → Login routing
```
RoleSelectionActivity
  → Admin card → LoginActivity(role=ADMIN) → AdminDashboardActivity
  → Teacher card → LoginActivity(role=TEACHER) → TeacherDashboardActivity
  → Parent card → LoginActivity(role=PARENT) → ParentDashboardActivity
  → Student card → LoginActivity(role=STUDENT) → StudentAttendanceActivity
```

### Admin Navigation (Bottom Nav: Home | Students | Teachers | Reports)
```
AdminDashboardActivity
  ├── ManageStudentsActivity → AddEditStudentActivity | BulkUploadActivity
  ├── ManageTeachersActivity → AssignClassActivity (→ confirm modal)
  ├── PromoteClassActivity → ReviewStudentsActivity → PromotionResultActivity
  ├── CreateAnnouncementActivity
  ├── AnnouncementsActivity
  ├── ReportsDashboardActivity → ReportFilterActivity → PdfPreviewDialog | ExportSuccessActivity
  └── AttendanceReportsActivity (3 tabs)
```

### Teacher Navigation (Bottom Nav: Home | Students | Reports | Settings)
```
TeacherDashboardActivity
  ├── TakeAttendanceActivity (from banner CTA)
  ├── AttendanceHistoryActivity
  ├── TeacherReportsActivity → ReportFilterActivity (limited — no ADMIN ONLY types)
  └── StudentListActivity (teacher view — read only)
```

### Parent Navigation (Bottom Nav: Dashboard | Attendance | Exams | Profile)
```
ParentDashboardActivity
  ├── StudentAttendanceActivity (child's attendance)
  ├── StudentDetailedReportActivity
  ├── AnnouncementsActivity (read only, parent audience)
  └── ExamsFragment (placeholder — screen not yet delivered)
```

### Student Navigation
```
StudentAttendanceActivity
  └── StudentDetailedReportActivity → export CSV/PDF
```

---

## 7. BOTTOM NAVIGATION PER ROLE (FINAL CONFIRMED)

```xml
<!-- Admin: res/menu/menu_admin_bottom_nav.xml -->
Home (home icon) | Students (group icon) | Teachers (school icon) | Reports (bar_chart icon)

<!-- Teacher: res/menu/menu_teacher_bottom_nav.xml -->
Home (home icon) | Students (group icon) | Reports (assessment icon) | Settings (settings icon)

<!-- Parent: res/menu/menu_parent_bottom_nav.xml -->
Dashboard (dashboard icon) | Attendance (calendar_month icon) | Exams (assignment icon) | Profile (person icon)
```

Active item color = role primary color (Admin=#1565C0, Teacher=#00ACC1, Parent=#2E7D32)
Inactive = #9E9E9E gray

---

## 8. SUBJECT COLOR MAP (SubjectColorHelper.java)

```java
public static String getDotColor(String subject) {
    switch (subject.toLowerCase()) {
        case "mathematics":     return "#1565C0";  // Blue
        case "science":         return "#7B1FA2";  // Purple
        case "english":         return "#F9A825";  // Amber
        case "social studies":  return "#E65100";  // Orange
        case "hindi":           return "#00ACC1";  // Teal
        case "physics":         return "#6A1B9A";  // Deep Purple
        case "general science": return "#2E7D32";  // Green
        case "history":         return "#4E342E";  // Brown
        case "computer":        return "#00838F";  // Cyan-dark
        default:                return "#757575";  // Gray
    }
}
```

---

## 9. REPORT PERMISSION MATRIX

| Report Type | Admin | Teacher | Parent | Student |
|-------------|-------|---------|--------|---------|
| Class-wise Summary | ✅ | ✅ | ❌ | ❌ |
| Monthly Report | ✅ | ✅ | ❌ | ❌ |
| Student-wise Detailed | ✅ | ✅ (own classes) | ✅ (own child) | ✅ (own) |
| Subject-wise Report | ✅ | ✅ | ❌ | ❌ |
| Teacher Performance | ✅ ADMIN ONLY | ❌ | ❌ | ❌ |
| Overall School Report | ✅ ADMIN ONLY | ❌ | ❌ | ❌ |

---

## 10. PROMOTE CLASS — FIRESTORE BATCH WRITE LOGIC

```java
// PromoteClassActivity.java — on "Confirm & Promote" clicked
WriteBatch batch = db.batch();

for (StudentPromotionItem item : studentList) {
    DocumentReference studentRef = db.collection("students").document(item.studentId);

    if (item.status == PROMOTE) {
        batch.update(studentRef, "classId", destinationClassId,
                                 "standard", destinationStandard,
                                 "division", destinationDivision);
    } else if (item.status == HOLD_BACK) {
        batch.update(studentRef, "status", "holdback");
        // Class stays same — no classId change
    } else if (item.status == LEFT_SCHOOL) {
        batch.update(studentRef, "status", "leftschool");
        // Archive to /alumni
        DocumentReference alumniRef = db.collection("alumni").document(item.studentId);
        batch.set(alumniRef, buildAlumniDocument(item, "leftschool"));
    }
}

// Update source class totalStudents count
batch.update(db.collection("classes").document(sourceClassId),
    "totalStudents", promotingCount + holdBackCount);

batch.commit().addOnSuccessListener(v -> showPromotionResult());
// Attendance history is PRESERVED — no changes to /attendance collection
// Alumni keep their studentId — historical queries still work
```

---

## 11. KEY IMPLEMENTATION NOTES (FINAL — ALL SCREENS)

1. **Login = 3 distinct UI themes** — Admin dark navy, Teacher white form, Parent overlapping card
2. **Parent auth = Roll Number NOT email** — Firestore lookup, not Firebase Auth email
3. **Attendance grid = 8-column, tap-to-toggle, live counter** — GridLayoutManager(8)
4. **Save Attendance button = BLUE always** — even in Teacher (teal) screen — #1565C0 for all saves
5. **Assign Classes = checkbox (teach) + toggle (class teacher)** — independent controls
6. **Assign CLASS TEACHER toggle → confirmation modal** before saving
7. **Announcements have TWO dimensions**: category (Academic/Events/Sports) + audience (All/Teachers/Parents)
8. **Bottom nav is ROLE-SPECIFIC** — 3 separate menu XMLs, never share
9. **Teacher Performance + Overall School Report = ADMIN ONLY** — hide via View.GONE for teacher
10. **Report preview table = color coded** — RED if % < 75%, GREEN if % ≥ 75%
11. **PDF has school branding** — school name, address, blue divider, teal report title, authorized signature footer
12. **Promote = 3-step flow**: Promote Class → Review Students → Promotion Result
13. **Student status after promote** = "holdback" stays in class | "leftschool" → /alumni | "active" promoted
14. **Attendance history PRESERVED after promotion** — do NOT delete old records
15. **Period number tracked** in attendance ("Mathematics • Period 1") — add `period` field
16. **Parent bell shows unread count badge** — count announcements (audience=all/parents) not in readSet
17. **Dashboard stat labels ALL say "Total Students" in HTML** — Stitch bug, fix all 4 in Java
18. **School address = Ganeshkhind Road, Pune, Maharashtra 411007** (from PDF preview)
19. **Version string** = "VERSION 2.4.0 (BUILD 102)" for Admin | "Version 2.4.0" for Teacher | "VERSION 2.4.0" for Parent
20. **Role Selection has 4 cards** — add Parent (green) though only 3 shown in screen

---

## 12. COMPLETE ACTIVITY / FILE STRUCTURE (v4.0 FINAL)

```
app/src/main/java/com/edutrack/app/

activities/
├── SplashActivity.java
├── RoleSelectionActivity.java
├── LoginActivity.java                         // 3 role variants, dark/white/card form
│
├── admin/
│   ├── AdminDashboardActivity.java            // BottomNav: Home/Students/Teachers/Reports
│   ├── ManageStudentsActivity.java            // search + chip filter + FAB
│   ├── AddEditStudentActivity.java            // ADD + EDIT mode via Intent
│   ├── BulkUploadActivity.java                // dashed dropzone + stepper
│   ├── ManageTeachersActivity.java
│   ├── AssignClassActivity.java               // checkbox + toggle + confirm dialog
│   ├── CreateAnnouncementActivity.java        // title + body + audience + category + pin
│   ├── AnnouncementsActivity.java             // category chip filter + feed
│   ├── PromoteClassActivity.java              // source + destination dropdowns
│   ├── ReviewStudentsActivity.java            // Promote/HoldBack/LeftSchool per student
│   ├── PromotionResultActivity.java           // success screen with what happened list
│   ├── ReportsDashboardActivity.java          // 6 report types, admin-only gating
│   ├── ReportFilterActivity.java              // filters + table preview + export CSV/PDF
│   └── AttendanceReportsActivity.java         // 3 tabs: class/date/student-wise
│
├── teacher/
│   ├── TeacherDashboardActivity.java          // teal theme, My Classes + CTA
│   ├── TakeAttendanceActivity.java            // 8-col green/red grid
│   ├── AttendanceHistoryActivity.java         // date filter + left-border cards
│   └── TeacherReportsActivity.java
│
├── parent/
│   ├── ParentDashboardActivity.java           // green theme, child card, exams tab
│   └── ExamsActivity.java                     // placeholder — screen not yet designed
│
└── shared/
    ├── StudentAttendanceActivity.java         // shared: student self-view + parent child view
    ├── StudentDetailedReportActivity.java     // tabbed: Overview/Subject/Monthly + export
    └── ExportSuccessActivity.java

adapters/
├── StudentAttendanceGridAdapter.java          // CORE: 8-col toggle tiles
├── StudentListAdapter.java
├── TeacherListAdapter.java
├── ClassCheckboxAdapter.java                  // checkbox + toggle
├── AttendanceHistoryAdapter.java              // left blue border cards
├── AnnouncementFeedAdapter.java               // category chips + unread dot
├── AnnouncementParentAdapter.java             // orange icon cards + Read More
├── ReportTypeAdapter.java                     // left-border report type cards
├── ReportTableAdapter.java                    // 4-col table with color-coded %
├── ReviewStudentAdapter.java                  // Promote/HoldBack/LeftSchool rows
└── MonthlyCardAdapter.java                    // horizontal month cards

models/
├── User.java
├── Student.java
├── Teacher.java
├── ClassRoom.java
├── Subject.java
├── AttendanceRecord.java                      // period field included
├── Announcement.java                          // category field included
├── MonthlyAttendanceSummary.java
├── StudentPromotionItem.java                  // status: PROMOTE/HOLD_BACK/LEFT_SCHOOL
└── Alumni.java

dialogs/
├── ConfirmAssignmentDialog.java               // class teacher confirm modal
├── PdfPreviewDialog.java                      // PDF preview + download options
└── ConfirmPromotionDialog.java                // (if needed before final batch write)

reports/
├── PdfReportGenerator.java                    // iText7, school branding header/footer
└── CsvExporter.java                           // OpenCSV

utils/
├── SessionManager.java
├── DateUtils.java
├── ValidationUtils.java
├── FileShareUtils.java
└── SubjectColorHelper.java                    // dot color per subject
```

---

## 13. GRADLE DEPENDENCIES (FINAL)

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

android {
    compileSdk 34
    defaultConfig {
        applicationId "com.edutrack.app"
        minSdk 24
        targetSdk 34
        multiDexEnabled true
    }
    buildFeatures { viewBinding true }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.4')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    implementation 'com.google.firebase:firebase-storage'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.multidex:multidex:2.0.1'
    implementation 'com.itextpdf:itext7-core:7.2.5'
    implementation 'com.opencsv:opencsv:5.9'
}
```

---

## 14. COMPLETE SCREEN DELIVERY STATUS (v4.0)

| # | Screen | Status | File |
|---|--------|--------|------|
| 01 | Splash Screen | ✅ DELIVERED | edutrack_splash_screen |
| 02 | Role Selection | ✅ DELIVERED | role_selection_screen |
| 03 | Admin Login (dark navy) | ✅ DELIVERED | admin_login_screen |
| 04 | Teacher Login (teal/white) | ✅ DELIVERED | teacher_login_screen |
| 05 | Parent Login (green/roll no) | ✅ DELIVERED | parent_login_screen |
| 06 | Admin Dashboard | ✅ DELIVERED | admin_dashboard_light_mode |
| 07 | Manage Students | ✅ DELIVERED | manage_students_light_mode |
| 08 | Add Student Form | ✅ DELIVERED | add_student_form_light_mode |
| 09 | Bulk Upload Students | ✅ DELIVERED | bulk_student_upload_light_mode |
| 10 | Assign Classes — 3 variants | ✅ DELIVERED | assign_classes_* |
| 11 | Confirm Assignment Modal | ✅ DELIVERED | confirm_assignment_light_mode |
| 12 | Attendance Reports (3 tabs) | ✅ DELIVERED | attendance_reports_light_mode |
| 13 | Attendance History | ✅ DELIVERED | attendance_history_light_mode |
| 14 | Export Success | ✅ DELIVERED | export_success_light_mode |
| 15 | Announcements Feed | ✅ DELIVERED | announcements_feed_light_mode |
| 16 | Create Announcement | ✅ DELIVERED | create_announcement_light_mode |
| 17 | Teacher Dashboard | ✅ DELIVERED | teacher_dashboard |
| 18 | Take Attendance Grid | ✅ DELIVERED | take_attendance_screen |
| 19 | Parent Dashboard | ✅ DELIVERED | parent_dashboard |
| 20 | Student My Attendance | ✅ DELIVERED | student_dashboard |
| 21 | Student Detailed Report | ✅ DELIVERED | student_detailed_report_light_mode |
| 22 | Reports Dashboard | ✅ DELIVERED | reports_dashboard_light_mode |
| 23 | Report Filters + Preview | ✅ DELIVERED | report_filters_preview_light_mode |
| 24 | PDF Export Preview Modal | ✅ DELIVERED | pdf_export_preview_light_mode |
| 25 | Promote Class Selector | ✅ DELIVERED | promote_class_select_review |
| 26 | Review Students | ✅ DELIVERED | review_students_light_mode |
| 27 | Promotion Result | ✅ DELIVERED | promote_class_success |
| 28 | Manage Teachers | ⏳ NOT YET — build from prompt |
| 29 | Parent Exams Tab | ⏳ NOT YET — placeholder screen |

**TOTAL: 27 of 29 screens documented. App is 93% specced. Begin development.**
