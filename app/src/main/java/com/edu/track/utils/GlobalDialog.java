package com.edu.track.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.edu.track.R;

public class GlobalDialog {

    public interface DialogCallback {
        void onConfirm();
        default void onCancel() {}
    }

    public static void show(Context context, String title, String description, int iconRes, int iconTint, String positiveText, DialogCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_global_action, null);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivIcon = view.findViewById(R.id.dialog_icon);
        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvDesc = view.findViewById(R.id.dialog_description);
        View btnPos = view.findViewById(R.id.btn_positive);
        View btnNeg = view.findViewById(R.id.btn_negative);

        ivIcon.setImageResource(iconRes);
        if (iconTint != 0) ivIcon.setColorFilter(context.getColor(iconTint));
        
        tvTitle.setText(title);
        tvDesc.setText(description);
        
        if (positiveText != null && !positiveText.isEmpty()) {
            ((TextView)btnPos).setText(positiveText);
        }

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onConfirm();
        });

        btnNeg.setOnClickListener(v -> {
            dialog.dismiss();
            callback.onCancel();
        });

        dialog.show();
    }

    public static void showWarning(Context context, String title, String description, DialogCallback callback) {
        show(context, title, description, R.drawable.ic_notifications, R.color.absent_red, "YES, PROCEED", callback);
    }
}
