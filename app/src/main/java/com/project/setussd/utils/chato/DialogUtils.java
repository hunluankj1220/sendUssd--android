package com.project.setussd.utils.chato;

import static com.project.setussd.utils.chato.NodeUtils.find;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

public class DialogUtils {

    public static String getBounds(AccessibilityNodeInfo node) {
        if (node == null) return "";

        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return r.left + "," + r.top + "," + r.width() + "," + r.height();
    }

    public static Rect parse(String s) {
        if (s == null || s.isEmpty()) return null;

        try {
            String[] arr = s.split(",");
            if (arr.length != 4) return null;

            return new Rect(
                    Integer.parseInt(arr[0]),
                    Integer.parseInt(arr[1]),
                    Integer.parseInt(arr[0]) + Integer.parseInt(arr[2]),
                    Integer.parseInt(arr[1]) + Integer.parseInt(arr[3])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
