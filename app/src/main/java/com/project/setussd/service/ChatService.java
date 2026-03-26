package com.project.setussd.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class ChatService extends AccessibilityService {
    private static final String TAG = "UssdService";
    private static final String INPUT_STRING = "1234"; // 要自动填充的数字字符串示例

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // 配置AccessibilityService以捕获所有内容改变事件
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
        Log.d(TAG, "Accessibility Service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 仅处理USSD对话框窗口事件
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;

        CharSequence className = event.getClassName();
        String textContent = event.getText().toString();
        Log.d(TAG, "USSD Event: class=" + className + ", text=" + textContent);

        // 寻找输入框（焦点模式）
        AccessibilityNodeInfo inputNode = source.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (inputNode != null) {
            // 设置文本值
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, INPUT_STRING);
            boolean result = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            Log.d(TAG, "Set input text " + (result ? "succeeded" : "failed"));
        }

        // 寻找“发送”或“确定”按钮并点击
        List<AccessibilityNodeInfo> sendButtons = source.findAccessibilityNodeInfosByText("Send");
        sendButtons.addAll(source.findAccessibilityNodeInfosByText("OK"));
        for (AccessibilityNodeInfo node : sendButtons) {
            boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked button: " + node.getText() + " result=" + clicked);
            if (clicked) break;
        }
    }

    @Override
    public void onInterrupt() {
        // 可在此处理服务被中断情况
    }
}