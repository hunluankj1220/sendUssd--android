package com.project.setussd.utils.chato;

import android.view.accessibility.AccessibilityNodeInfo;

import com.project.setussd.Contact;

import java.util.ArrayList;
import java.util.List;

public class NodeUtils {

    public static AccessibilityNodeInfo find(AccessibilityNodeInfo root, String id) {
        if (root == null) return null;

        List<AccessibilityNodeInfo> list =
                root.findAccessibilityNodeInfosByViewId(id);

        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

//    public static void click(AccessibilityNodeInfo root, String id) {
//        AccessibilityNodeInfo node = find(root, id);
//        if (node != null) {
//            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }
//    }

    public static String getAllText(AccessibilityNodeInfo node) {

        StringBuilder sb = new StringBuilder();

        collectText(node);

        return sb.toString();
    }

    private static String  collectText(AccessibilityNodeInfo node) {

//        if (node == null) return;
//
//        if (node.getText() != null) {
//            sb.append(node.getText()).append("\n");
//        }
//
//        for (int i = 0; i < node.getChildCount(); i++) {
//            collectText(node.getChild(i), sb);
//        }
        if (node == null) return "";

        StringBuilder sb = new StringBuilder();

        if (node.getText() != null) {
            sb.append(node.getText()).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            sb.append(collectText(node.getChild(i)));
        }

        return sb.toString();
    }
public static boolean click(AccessibilityNodeInfo root, String id) {

//        AccessibilityNodeInfo node = find(root, id);
//
//        if (node != null) {
//            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }
    if (root == null) return false;

    if (id.equals(root.getViewIdResourceName())) {
        return root.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    for (int i = 0; i < root.getChildCount(); i++) {
        if (click(root.getChild(i), id)) return true;
    }

    return false;
}
    public static boolean clickByText(AccessibilityNodeInfo node, String text) {

        if (node == null) return false;

        if (node.getText() != null) {

            String t = node.getText().toString().toLowerCase();

            if (t.contains(text.toLowerCase()) && node.isClickable()) {

                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickByText(node.getChild(i), text)) {
                return true;
            }
        }

        return false;
    }
    public static String getUssdResultText(AccessibilityNodeInfo root,
                                     AccessibilityNodeInfo dialog) {
        if (root == null) return null;

        List<AccessibilityNodeInfo> list =
                root.findAccessibilityNodeInfosByViewId("android:id/message");

        if (list != null && !list.isEmpty()) {
            CharSequence cs = list.get(0).getText();
            if (cs != null) return cs.toString();
        }

        return collectText(root);
    }

}