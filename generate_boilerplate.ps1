$basePkg = "com.edu.track"
$javaResDir = "d:\Vishwet\Edu track app\EduTrack\app\src\main\java\com\edu\track\activities"
$layoutDir = "d:\Vishwet\Edu track app\EduTrack\app\src\main\res\layout"

$activities = @(
    @{ Name="SplashActivity"; SubPkg=""; Layout="activity_splash"; IsMain=$true },
    @{ Name="RoleSelectionActivity"; SubPkg=""; Layout="activity_role_selection"; IsMain=$false },
    @{ Name="LoginActivity"; SubPkg=""; Layout="activity_login"; IsMain=$false },
    @{ Name="AdminDashboardActivity"; SubPkg="admin"; Layout="activity_admin_dashboard"; IsMain=$false },
    @{ Name="ManageStudentsActivity"; SubPkg="admin"; Layout="activity_manage_students"; IsMain=$false },
    @{ Name="AddEditStudentActivity"; SubPkg="admin"; Layout="activity_add_edit_student"; IsMain=$false },
    @{ Name="BulkUploadActivity"; SubPkg="admin"; Layout="activity_bulk_upload"; IsMain=$false },
    @{ Name="ManageTeachersActivity"; SubPkg="admin"; Layout="activity_manage_teachers"; IsMain=$false },
    @{ Name="AssignClassActivity"; SubPkg="admin"; Layout="activity_assign_class"; IsMain=$false },
    @{ Name="ManageClassesActivity"; SubPkg="admin"; Layout="activity_manage_classes"; IsMain=$false },
    @{ Name="PromoteClassActivity"; SubPkg="admin"; Layout="activity_promote_class"; IsMain=$false },
    @{ Name="CreateAnnouncementActivity"; SubPkg="admin"; Layout="activity_create_announcement"; IsMain=$false },
    @{ Name="AdminReportsActivity"; SubPkg="admin"; Layout="activity_admin_reports"; IsMain=$false },
    @{ Name="TeacherDashboardActivity"; SubPkg="teacher"; Layout="activity_teacher_dashboard"; IsMain=$false },
    @{ Name="TakeAttendanceActivity"; SubPkg="teacher"; Layout="activity_take_attendance"; IsMain=$false },
    @{ Name="AttendanceHistoryActivity"; SubPkg="teacher"; Layout="activity_attendance_history"; IsMain=$false },
    @{ Name="TeacherReportsActivity"; SubPkg="teacher"; Layout="activity_teacher_reports"; IsMain=$false },
    @{ Name="ParentDashboardActivity"; SubPkg="parent"; Layout="activity_parent_dashboard"; IsMain=$false },
    @{ Name="ChildAttendanceDetailActivity"; SubPkg="parent"; Layout="activity_child_attendance_detail"; IsMain=$false }
)

if (!(Test-Path $layoutDir)) { New-Item -ItemType Directory -Force -Path $layoutDir | Out-Null }

foreach($act in $activities) {
    $subDir = if ($act.SubPkg) { "\$($act.SubPkg)" } else { "" }
    $pkgName = if ($act.SubPkg) { "$basePkg.activities.$($act.SubPkg)" } else { "$basePkg.activities" }
    
    $actDir = "$javaResDir$subDir"
    if (!(Test-Path $actDir)) { New-Item -ItemType Directory -Force -Path $actDir | Out-Null }
    
    $javaPath = "$actDir\$($act.Name).java"
    $layoutPath = "$layoutDir\$($act.Layout).xml"
    
    $javaContent = "package $pkgName;`r`n`r`nimport android.os.Bundle;`r`nimport androidx.appcompat.app.AppCompatActivity;`r`nimport com.edu.track.R;`r`n`r`npublic class $($act.Name) extends AppCompatActivity {`r`n`r`n    @Override`r`n    protected void onCreate(Bundle savedInstanceState) {`r`n        super.onCreate(savedInstanceState);`r`n        setContentView(R.layout.$($act.Layout));`r`n    }`r`n}`r`n"
    [System.IO.File]::WriteAllText($javaPath, $javaContent)
    
    $layoutContent = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`r`n<LinearLayout xmlns:android=`"http://schemas.android.com/apk/res/android`"`r`n    android:layout_width=`"match_parent`"`r`n    android:layout_height=`"match_parent`"`r`n    android:orientation=`"vertical`"`r`n    android:gravity=`"center`">`r`n`r`n    <TextView`r`n        android:layout_width=`"wrap_content`"`r`n        android:layout_height=`"wrap_content`"`r`n        android:text=`"$($act.Name)`"`r`n        android:textSize=`"24sp`" />`r`n`r`n</LinearLayout>"
    [System.IO.File]::WriteAllText($layoutPath, $layoutContent)
}

Write-Host "Success: Generated 19 Activities and 19 Layouts."
