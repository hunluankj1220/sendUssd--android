package com.project.setussd.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * 无障碍模式
 */
public class openAccessibilityUtils {

    private static MaterialDialog materialDialog;

    public static void openAccessibility(Context context) {
        materialDialog = new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR());
        materialDialog.title(null, "info");
        materialDialog.message(null, "plese open accessibility switch", null);
        materialDialog.positiveButton(null, "openSetting", dialog -> {
            openAccessibilitySettings(context);
            dialog.dismiss();
            return null;
        });
        materialDialog.negativeButton(null, "Cancel", null);
        materialDialog.show();
    }

    private static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    public static void dismiss(){
        if (materialDialog !=null && materialDialog.isShowing()){
            materialDialog.dismiss();
            materialDialog = null;
        }
    }
}
