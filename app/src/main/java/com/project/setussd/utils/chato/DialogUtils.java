package com.project.setussd.utils.chato;

import static com.project.setussd.utils.chato.NodeUtils.find;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

public class DialogUtils {

    public static String getBounds(AccessibilityNodeInfo node) {
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        return r.left + "," + r.top + "," + r.width() + "," + r.height();
    }

    public static Rect parse(String s) {
        try {
            String[] arr = s.split(",");
            return new Rect(
                    Integer.parseInt(arr[0]),
                    Integer.parseInt(arr[1]),
                    Integer.parseInt(arr[0]) + Integer.parseInt(arr[2]),
                    Integer.parseInt(arr[1]) + Integer.parseInt(arr[3])
            );
        } catch (Exception e) {
            return null;
        }
    }

}