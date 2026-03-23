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

    public static void exportToCSV(Activity activity, String fileName, String subFolder, String csvContent, ExportCallback callback) {
        if (!isExternalStorageWritable()) {
            callback.onFailure(new IOException("External storage is not writable"));
            return;
        }

        if (csvContent != null && !csvContent.toLowerCase().contains("date range")) {
            csvContent = "Report Date Range: Complete History (All Time)\n\n" + csvContent;
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

        if (title != null && !title.contains("to") && !title.contains("(")) {
            title = title + " (Complete History)";
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
        
        int openParen = title.indexOf("(");
        if (openParen > 0) {
            String mainTitle = title.substring(0, openParen).trim();
            String subTitle = title.substring(openParen).trim();
            canvas.drawText(mainTitle, xMargin, currentY + 15, titlePaint);
            currentY += 25;
            titlePaint.setTextSize(14);
            canvas.drawText(subTitle, xMargin, currentY + 15, titlePaint);
            currentY += 30;
        } else {
            canvas.drawText(title, xMargin, currentY + 15, titlePaint);
            currentY += 40;
        }

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
        
        // Dynamic column weights to prevent wrapping in "Name" fields
        float[] colWidths = new float[numCols];
        float totalWeight = 0;
        float[] weights = new float[numCols];
        for (int j = 0; j < numCols; j++) {
            String header = data.get(0)[j].toLowerCase();
            if (header.contains("name")) weights[j] = 3f;
            else if (header.contains("roll") || header.contains("%")) weights[j] = 0.8f;
            else if (header.contains("date") || header.contains("standard")) weights[j] = 1.5f;
            else weights[j] = 1f;
            totalWeight += weights[j];
        }
        for (int j = 0; j < numCols; j++) {
            colWidths[j] = ((pageWidth - (2f * xMargin)) * weights[j]) / totalWeight;
        }

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            
            // Check if any cell in this row has multiple lines
            int maxLines = 1;
            for (String cell : row) {
                if (cell != null && cell.contains("\n")) maxLines = Math.max(maxLines, cell.split("\n").length);
            }
            
            int currentRowHeight = 15 + (maxLines * 15); // Adjust height based on lines (was fixed 25)

            // Pagination Check
            if (currentY + currentRowHeight > pageHeight - yMargin) {
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
                canvas.drawRect(xMargin, currentY - 15, pageWidth - xMargin, currentY + currentRowHeight - 15, headerBg);
            }

            Paint rowPaint = (i == 0) ? headerPaint : paint;
            float currentX = xMargin;
            for (int j = 0; j < row.length; j++) {
                String cell = row[j] != null ? row[j] : "";
                String[] lines = cell.split("\n");
                float colW = colWidths[j];
                
                for (int k = 0; k < lines.length; k++) {
                    String lineText = lines[k];
                    // Truncate line if it's too long
                    float maxWidth = colW - 10;
                    int charactersCount = rowPaint.breakText(lineText, true, maxWidth, null);
                    if (charactersCount < lineText.length()) {
                        lineText = lineText.substring(0, Math.max(0, charactersCount - 3)) + "...";
                    }
                    
                    canvas.drawText(lineText, currentX + 5, currentY + 11 + (k * 15), rowPaint);
                }
                
                // Draw vertical borders
                canvas.drawLine(currentX, currentY - 15, currentX, currentY + currentRowHeight - 15, borderPaint);
                currentX += colW;
            }
            // Draw last vertical border
            canvas.drawLine(pageWidth - xMargin, currentY - 15, pageWidth - xMargin, currentY + currentRowHeight - 15, borderPaint);
            
            // Draw horizontal borders
            if (i == 0) {
                canvas.drawLine(xMargin, currentY - 15, pageWidth - xMargin, currentY - 15, borderPaint);
            }
            canvas.drawLine(xMargin, currentY + currentRowHeight - 15, pageWidth - xMargin, currentY + currentRowHeight - 15, borderPaint);
            
            currentY += currentRowHeight;
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

    public static String formatTwoLineDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "N/A";
        // Handle yyyy-MM-dd (standard DB format)
        try {
            if (dateStr.length() == 10 && dateStr.charAt(4) == '-') {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date d = sdf.parse(dateStr);
                return new SimpleDateFormat("dd MMM\nyyyy", Locale.getDefault()).format(d);
            }
        } catch (Exception ignored) {}
        
        // Handle dd/MM/yyyy (display format in some activities)
        try {
            if (dateStr.contains("/")) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date d = sdf.parse(dateStr);
                return new SimpleDateFormat("dd MMM\nyyyy", Locale.getDefault()).format(d);
            }
        } catch (Exception ignored) {}

        return dateStr;
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
