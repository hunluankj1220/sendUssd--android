package com.project.setussd.utils.chato;


import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class DialogFinder {

    /**
     * 自动查找最可能的“USSD弹窗节点”
     */
    public static AccessibilityNodeInfo findDialog(AccessibilityNodeInfo root, DisplayMetrics dm) {

        if (root == null) return null;

        List<AccessibilityNodeInfo> candidates = new ArrayList<>();

        // 收集所有节点
        collect(root, candidates);

        AccessibilityNodeInfo best = null;
        int bestScore = 0;

        for (AccessibilityNodeInfo node : candidates) {

            Rect r = new Rect();
            node.getBoundsInScreen(r);

            // ❗过滤非法
            if (r.width() <= 0 || r.height() <= 0) continue;

            // ❗过滤全屏
            if (r.width() >= dm.widthPixels * 0.95 &&
                    r.height() >= dm.heightPixels * 0.95) {
                continue;
            }

            int score = 0;

            // ⭐ 规则1：在屏幕中间（加分）
            if (isCenter(r, dm)) score += 3;

            // ⭐ 规则2：包含文本（重要）
            if (hasText(node)) score += 3;

            // ⭐ 规则3：包含按钮（非常重要）
            if (hasButton(node)) score += 5;

            // ⭐ 规则4：大小合理
            if (r.width() > dm.widthPixels * 0.4) score += 1;
            if (r.height() > dm.heightPixels * 0.2) score += 1;

            if (score > bestScore) {
                bestScore = score;
                best = node;
            }
        }

        return best;
    }

    /**
     * 递归收集节点
     */
    private static void collect(AccessibilityNodeInfo node,
                                List<AccessibilityNodeInfo> list) {

        if (node == null) return;

        list.add(node);

        for (int i = 0; i < node.getChildCount(); i++) {
            collect(node.getChild(i), list);
        }
    }

    /**
     * 是否在屏幕中心区域
     */
    private static boolean isCenter(Rect r, DisplayMetrics dm) {

        int centerX = r.centerX();
        int centerY = r.centerY();

        return centerX > dm.widthPixels * 0.2 &&
                centerX < dm.widthPixels * 0.8 &&
                centerY > dm.heightPixels * 0.2 &&
                centerY < dm.heightPixels * 0.8;
    }

    /**
     * 是否包含文本
     */
    private static boolean hasText(AccessibilityNodeInfo node) {

        if (node.getText() != null && node.getText().length() > 0)
            return true;

        if (node.getContentDescription() != null &&
                node.getContentDescription().length() > 0)
            return true;

        for (int i = 0; i < node.getChildCount(); i++) {
            if (hasText(node.getChild(i))) return true;
        }

        return false;
    }

    /**
     * 是否包含按钮
     */
    private static boolean hasButton(AccessibilityNodeInfo node) {

        if ("android.widget.Button".contentEquals(node.getClassName()))
            return true;

        for (int i = 0; i < node.getChildCount(); i++) {
            if (hasButton(node.getChild(i))) return true;
        }

        return false;
    }
}