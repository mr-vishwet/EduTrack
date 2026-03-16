package com.edu.track.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportManager {

    private static final String TAG = "ReportManager";
    private static final String BASE_FOLDER = "EduTrack";

    public interface ExportCallback {
        void onSuccess(String filePath);
        void onFailure(Exception e);
    }

    private static File getExportDirectory(String subFolder) throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File eduTrackDir = new File(downloadsDir, BASE_FOLDER + File.separator + subFolder);

        if (!eduTrackDir.exists()) {
            if (!eduTrackDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + eduTrackDir.getAbsolutePath());
            }
        }
        return eduTrackDir;
    }

    public static void exportToCSV(Context context, String fileName, String subFolder, String csvContent, ExportCallback callback) {
        if (!isExternalStorageWritable()) {
            callback.onFailure(new IOException("External storage is not writable"));
            return;
        }

        try {
            File eduTrackDir = getExportDirectory(subFolder);
            File file = new File(eduTrackDir, fileName + ".csv");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(csvContent.getBytes());
                fos.flush();
                callback.onSuccess(file.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing CSV", e);
            callback.onFailure(e);
        }
    }

    public static void exportToPDF(Activity activity, String title, String fileName, String subFolder, List<String[]> data, ExportCallback callback) {
        if (!isExternalStorageWritable()) {
            callback.onFailure(new IOException("External storage is not writable"));
            return;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();
        Paint borderPaint = new Paint();

        int pageWidth = 595;
        int pageHeight = 842;
        int xMargin = 40;
        int yMargin = 50;
        int lineSpacing = 25;

        int currentY = yMargin;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Draw Logo at top right
        try {
            android.graphics.Bitmap logo = android.graphics.BitmapFactory.decodeResource(activity.getResources(), com.edu.track.R.mipmap.ic_launcher);
            if (logo != null) {
                android.graphics.Bitmap scaledLogo = android.graphics.Bitmap.createScaledBitmap(logo, 50, 50, false);
                canvas.drawBitmap(scaledLogo, pageWidth - xMargin - 50, currentY, paint);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error drawing logo", e);
        }

        // Draw Title
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.parseColor("#1565C0")); // Primary dark blue
        canvas.drawText(title, xMargin, currentY + 15, titlePaint);
        currentY += 40;

        // Draw Organization Name & Date
        paint.setTextSize(10);
        paint.setColor(Color.DKGRAY);
        canvas.drawText("EduTrack - Educational Management System", xMargin, currentY, paint);
        currentY += 15;
        
        String date = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Report Generated: " + date, xMargin, currentY, paint);
        currentY += 30;

        // Table settings
        headerPaint.setTextSize(12);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(Color.WHITE);
        
        borderPaint.setColor(Color.LTGRAY);
        borderPaint.setStrokeWidth(1f);
        borderPaint.setStyle(Paint.Style.STROKE);

        paint.setTextSize(11);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);

        if (data == null || data.isEmpty()) {
            document.finishPage(page);
            document.close();
            callback.onFailure(new Exception("No data to export"));
            return;
        }

        int numCols = data.get(0).length;
        int colWidth = (pageWidth - (2 * xMargin)) / numCols;

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            
            // Pagination Check
            if (currentY + lineSpacing > pageHeight - yMargin) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                currentY = yMargin;
            }

            // Draw Header Background
            if (i == 0) {
                Paint headerBg = new Paint();
                headerBg.setColor(Color.parseColor("#1976D2"));
                headerBg.setStyle(Paint.Style.FILL);
                canvas.drawRect(xMargin, currentY - 15, pageWidth - xMargin, currentY + 10, headerBg);
            }

            Paint rowPaint = (i == 0) ? headerPaint : paint;
            for (int j = 0; j < row.length; j++) {
                String cell = row[j] != null ? row[j] : "";
                // Handle text wrapping if cell is too long (simplified approximation here)
                canvas.drawText(cell, xMargin + (j * colWidth) + 5, currentY, rowPaint);
                
                // Draw vertical borders
                canvas.drawLine(xMargin + (j * colWidth), currentY - 15, xMargin + (j * colWidth), currentY + 10, borderPaint);
            }
            // Draw last vertical border
            canvas.drawLine(pageWidth - xMargin, currentY - 15, pageWidth - xMargin, currentY + 10, borderPaint);
            
            // Draw horizontal borders
            if (i == 0) {
                canvas.drawLine(xMargin, currentY - 15, pageWidth - xMargin, currentY - 15, borderPaint);
            }
            canvas.drawLine(xMargin, currentY + 10, pageWidth - xMargin, currentY + 10, borderPaint);
            
            currentY += lineSpacing;
        }

        document.finishPage(page);

        try {
            File eduTrackDir = getExportDirectory(subFolder);
            File file = new File(eduTrackDir, fileName + ".pdf");
            document.writeTo(new FileOutputStream(file));
            callback.onSuccess(file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF", e);
            callback.onFailure(e);
        } finally {
            document.close();
        }
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static void showExportSuccessDialog(Activity activity, String filePath) {
        File file = new File(filePath);
        // Show a clean path starting from Downloads/
        String displayPath = filePath;
        int downloadsIdx = filePath.indexOf("Downloads");
        if (downloadsIdx >= 0) {
            displayPath = filePath.substring(downloadsIdx);
        }
        new AlertDialog.Builder(activity)
                .setTitle("✅ Report Exported")
                .setMessage("File saved to:\n\n" + displayPath)
                .setPositiveButton("📂 View File", (dialog, which) -> openFile(activity, file))
                .setNegativeButton("Done", null)
                .show();
    }

    private static void openFile(Activity activity, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Intent chooser = Intent.createChooser(intent, "Open with");
            activity.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file", e);
            Toast.makeText(activity, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    @Deprecated
    public static void showExportToast(Context context, String filePath) {
        Toast.makeText(context, "Report saved to: " + filePath, Toast.LENGTH_LONG).show();
    }
}
