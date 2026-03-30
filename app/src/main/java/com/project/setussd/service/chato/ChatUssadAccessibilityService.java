package com.project.setussd.service.chato;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.project.setussd.Contact;
import com.project.setussd.bean.UssAction;
import com.project.setussd.log.UssdLogger;
import com.project.setussd.network.ApiCallback;
import com.project.setussd.network.ApiClient;
import com.project.setussd.utils.CacheUtils;
import com.project.setussd.utils.chato.DialogFinder;
import com.project.setussd.utils.chato.NodeUtils;
import com.project.setussd.utils.chato.ScreenCaptureManager;
import com.project.setussd.utils.chato.UssdController;
import com.project.setussd.utils.chato.UssdState;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChatUssadAccessibilityService extends AccessibilityService {

    private boolean hasCaptured = false;
    // =========================
    // ⭐ 防止重复识别同一个弹窗
    // =========================
    private int lastDialogHash = 0;
    // ⭐ 状态控制
    private boolean isProcessing = false;
    private long lastTextChangeTime = 0;
    private String lastText = "";
    private static final long STABLE_TIME = 1200; // 文本稳定时间
    private boolean isTaskRunning = false;
    private boolean isFinished = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private static final String[] USSD_PACKAGES = {
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.huawei.android.dialer",
            "com.miui.phone"
    };
    private static ChatUssadAccessibilityService instance;

    public static ChatUssadAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // ❗ 已结束，不再处理（解决问题1）
        if (isFinished) return;

        // ❗ 没启动任务，不处理
        if (!isTaskRunning) return;

        // ❗ 包名过滤（解决问题2）
        if (!isUssdPackage(event.getPackageName())) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        UssdLogger.log(this, "检测到USSD弹窗");
        DisplayMetrics dm = getResources().getDisplayMetrics();

        // ⭐ 1. 自动找弹窗
        AccessibilityNodeInfo dialog =
                DialogFinder.findDialog(root, dm);
//        AccessibilityNodeInfo dialog = findUssdDialog(root);
        if (dialog == null) return;

        // ⭐ 2. 提取文本
//        String text = NodeUtils.getAllText(dialog);
        String text = NodeUtils.getUssdResultText(root, dialog);
        if (text.contains("running")) {
            text.contains("processing");
            return;
        }
        if (text == null || text.trim().isEmpty()) return;
        // ❗ 排除系统截图授权弹窗（关键）
        if (isScreenCaptureDialog(root)) return;
        // ⭐ 3. 找输入框（增强版）
        AccessibilityNodeInfo input = findUssdInput(dialog);

        // =========================
        // ⭐ 输入阶段（重点优化）
        // =========================
        extracted(input, dialog, text, dm);
    }

    // =========================
    // ⭐ 开始任务（外部调用）
    // =========================
    public void startUssdTask() {
        isTaskRunning = true;
        isFinished = false;
    }

    // =========================
    // ⭐ 结束任务（关键）
    // =========================
    private void finishTask(String result) {

        if (isFinished) return;

        isFinished = true;
        isTaskRunning = false;
//        sendBroadcastResult(result, null);
        sendResult(null, result);
        UssdLogger.log(this, "任务结束");
        Log.i("USSD", "任务结束：" + result);
    }

    // =========================
    // ⭐ 判断是否USSD页面
    // =========================
    private boolean isUssdPackage(CharSequence pkg) {

        if (pkg == null) return false;

        String p = pkg.toString();

        for (String s : USSD_PACKAGES) {
            if (p.contains(s)) return true;
        }

        return false;
    }

    private void extracted(AccessibilityNodeInfo input, AccessibilityNodeInfo dialog, String text, DisplayMetrics dm) {
        if (input != null) {
            //截第二个输入框
            UssdLogger.log(this, "发现输入框，准备输入密码");
            UssdLogger.log(this, "输入框内容：" + text);
            if (!isSameDialog(dialog)) {
                UssdState.inputCount++;
//                UssdState.hasCaptured = true;
            }
            // ===== Activity 传入参数 =====
            Map<String, String> params = new HashMap<>();
            params.put("msg", text);
            UssdLogger.log(this, "请求接口,获取密码或者其他");
            ApiClient.request("/use/pwd", params, UssAction.class, new ApiCallback<UssAction>() {
                @Override
                public void onSuccess(UssAction data) {
                    String password = data.password;
                    //第一个输入框->自动输入+发送
                    Toast.makeText(getApplicationContext(), "sendussd-inputCount:" + UssdState.inputCount + "--" + UssdState.handledFirst, Toast.LENGTH_LONG).show();
                    Log.i(Contact.TAG, "onAccessibilityEvent-inputCount: " + UssdState.inputCount + "--" + UssdState.handledFirst);
                    if (UssdState.inputCount == 1 && !UssdState.handledFirst) {
                        UssdState.handledFirst = true;
                        // ⭐ 自动输入
                        if (!UssdController.hasInput) {
                            UssdController.hasInput = true;
                            //获取设置的密码
//                            String passwoard = CacheUtils.getString(getApplicationContext(), "PWD");
//                            if (passwoard.isEmpty()){
//                                passwoard =  Contact.AUTO_RECHARGE_PASSWORD;
//                            }
                            String finalPasswoard = password;
                            Log.i(Contact.TAG, "onAccessibilityEvent-finalPasswoard: " + finalPasswoard);
//                          Toast.makeText(getApplicationContext(),"sendussd-pwd:"+finalPasswoard,Toast.LENGTH_LONG).show();
                            handler.postDelayed(() -> {
                                UssdLogger.log(ChatUssadAccessibilityService.this, "自动输入密码完成");
                                boolean success = inputText(input, finalPasswoard);

                                if (success) {
                                    handler.postDelayed(() -> {
                                        UssdLogger.log(ChatUssadAccessibilityService.this, "点击发送按钮");
                                        boolean b = clickSend(dialog);
                                        if (!b) {
                                            UssdLogger.log(ChatUssadAccessibilityService.this, "发送失败");
                                            handleSendFail("发送失败");
                                        }

                                    }, 800);

                                }

                            }, 1200); // ⭐ 埃及网络必须延迟

                        }

                        return;
                    }
                    //第二个输入框截图+点击取消
                    if (UssdState.inputCount == 2 && !UssdState.handledSecond) {
                        UssdState.handledSecond = true;
                        handler.postDelayed(() -> {
                            UssdLogger.log(ChatUssadAccessibilityService.this, "正在截图");
                            // ⭐ 先截图（解决问题1）
                            captureAndSend(dialog, text, dm);
                            clickCancel(dialog);

                        }, 800);
                        return;
                    }
                }

                @Override
                public void onError(int code, String msg) {
                    UssdLogger.log(ChatUssadAccessibilityService.this, "请求获取失败：" + msg);
                }

                @Override
                public void onFailure(String error) {
                    UssdLogger.log(ChatUssadAccessibilityService.this, "网络错误：" + error);
                }
            });

            return;
        }

        // =========================
        // ⭐ 结果阶段
        // =========================
        // ❗ 过滤“正在运行USSD代码”
        Log.i(Contact.TAG, "onAccessibilityEvent0: " + text);
        if (text.contains("正在运行") || text.contains("running")) {
            UssdLogger.log(this, "正在运行USSD代码");
            return;
        }

        // ❗ 只处理错误 / 成功结果
        // =========================
        // ⭐ 2. 华为等错误直接命中
        // =========================
        if (isImmediateError(text)) {
            if (isFinishedFail(text)){
                //执行失败结果
                handleSendFail(text);
            }
            UssdLogger.log(this, "收到USSD结果：" + text);
//            handleResult(dialog, text, "ERROR", dm);
            captureAndSend(dialog, text, dm);
            finishTask(text); // ⭐ 关键：终止流程
            return;
        }

        // =========================
        // ⭐ 3. 自动判断完成（核心）
        // =========================
        handleFinal(dialog, text, dm);

        //没有输入框->允许识别新弹窗
        lastDialogHash = 0;
    }

    /**
     * 发送失败，上传后台。
     * @param text
     */
    private void handleSendFail(String text) {
        UssdLogger.log(this,"发送失败，上传后台："+text);
        Map<String, String> params = new HashMap<>();
        params.put("msg", text);
        ApiClient.request("user/up", params, UssAction.class, new ApiCallback<UssAction>() {
            @Override
            public void onSuccess(UssAction data) {
                UssdLogger.log(ChatUssadAccessibilityService.this,"上报成功");
                handler.post(()->{
                   executeAction(data);
                });
            }

            @Override
            public void onError(int code, String msg) {
                UssdLogger.log(ChatUssadAccessibilityService.this,"上报失败");
            }

            @Override
            public void onFailure(String error) {
                UssdLogger.log(ChatUssadAccessibilityService.this,"上报失败，网络错误");
            }
        });
    }

    /**
     * 根据返回数据，自行进行下一步处理
     * @param data
     */
    private void executeAction(UssAction data) {
        if (data == null) return;
        switch (data.action){
            case "1":
                //继续
                UssdLogger.log(this,"继续输入密码");
                break;
            case "2":
                //重新发起ussd
                UssdLogger.log(this,"重新发起ussd");
                UssdState.reset();
                String ussdCode = CacheUtils.getString(getApplicationContext(), "USSDCODE");
                UssdController.start(this, ussdCode);
                break;
            case "3":
                //终止任务
                UssdLogger.log(this,"终止任务");
//                finishTask("终止执行"); // ⭐ 关键：终止流程
                break;
        }
    }

    private boolean isScreenCaptureDialog(AccessibilityNodeInfo root) {

        if (root == null) return false;

        List<AccessibilityNodeInfo> list =
                root.findAccessibilityNodeInfosByText("开始录制");

        if (list != null && !list.isEmpty()) {
            return true;
        }

        list = root.findAccessibilityNodeInfosByText("Start now");

        return list != null && !list.isEmpty();
    }

    /**
     * 识别输入框弹出窗
     *
     * @param node
     * @return
     */
    private boolean isSameDialog(AccessibilityNodeInfo node) {

        Rect r = new Rect();
        node.getBoundsInScreen(r);

        int hash = r.toShortString().hashCode();

        if (hash == lastDialogHash) {
            return true;
        }

        lastDialogHash = hash;
        return false;
    }

    private AccessibilityNodeInfo findUssdInput(AccessibilityNodeInfo node) {

        if (node == null) return null;

        // ⭐ 包名过滤（必须）
        CharSequence pkg = node.getPackageName();

        if (pkg == null ||
                !(pkg.toString().contains("phone") ||
                        pkg.toString().contains("telecom"))) {
            return null;
        }

        // ⭐ EditText 才算输入框
        if ("android.widget.EditText".contentEquals(node.getClassName())) {
            return node;
        }

        // ⭐ 递归查找
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findUssdInput(node.getChild(i));
            if (result != null) return result;
        }

        return null;
    }

    private boolean inputText(AccessibilityNodeInfo node, String text) {

        try {
            Bundle b = new Bundle();
            b.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
            );

            return node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT, b
            );

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 点击发送
     *
     * @param dialog
     */
    private boolean clickSend(AccessibilityNodeInfo dialog) {

        if (dialog == null) return false;

        // =========================
        // ⭐ 1. 系统默认按钮（最稳）
        // =========================
        if (NodeUtils.click(dialog, "android:id/button1")) {
            UssdLogger.log(this, "点击发送（系统按钮button1）");
            return true;
        }

        // =========================
        // ⭐ 2. 厂商按钮ID（华为/小米）
        // =========================
        if (NodeUtils.click(dialog, "android:id/button2")) {
            UssdLogger.log(this, "点击发送（系统按钮button2）");
            return true;
        }

        // =========================
        // ⭐ 3. 查找可点击按钮（最关键）
        // =========================
        AccessibilityNodeInfo btn = findClickableButton(dialog);

        if (btn != null) {

            CharSequence txt = btn.getText();

            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            UssdLogger.log(this, "点击发送（自动识别按钮）:" + txt);

            return true;
        }

        // =========================
        // ⭐ 4. 多语言文本匹配（埃及重点）
        // =========================
        if (NodeUtils.clickByText(dialog, "Send")
                || NodeUtils.clickByText(dialog, "OK")
                || NodeUtils.clickByText(dialog, "发送")
                || NodeUtils.clickByText(dialog, "确定")
                || NodeUtils.clickByText(dialog, "إرسال")) { // 阿拉伯语

            UssdLogger.log(this, "点击发送（文本匹配）");
            return true;
        }

        // =========================
        // ❗ 5. 兜底失败
        // =========================
        UssdLogger.log(this, "点击发送失败（未找到按钮）");

        return false;
    }

    private AccessibilityNodeInfo findClickableButton(AccessibilityNodeInfo node) {

        if (node == null) return null;

        // ⭐ 是按钮且可点击
        if ("android.widget.Button".contentEquals(node.getClassName())
                && node.isClickable()) {
            return node;
        }

        // ⭐ 某些ROM按钮是TextView
        if ("android.widget.TextView".contentEquals(node.getClassName())
                && node.isClickable()) {

            CharSequence text = node.getText();

            if (text != null) {
                String t = text.toString().toLowerCase();

                if (t.contains("send") ||
                        t.contains("ok") ||
                        t.contains("yes") ||
                        t.contains("确认") ||
                        t.contains("إرسال")) {
                    return node;
                }
            }
        }

        // ⭐ 递归查找
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo res = findClickableButton(node.getChild(i));
            if (res != null) return res;
        }

        return null;
    }

    /**
     * 取消点击
     *
     * @param dialog
     */
    private void clickCancel(AccessibilityNodeInfo dialog) {

        // button1（发送）
        if (NodeUtils.click(dialog, "android:id/button2")) return;

        // 文本匹配（兜底）
        NodeUtils.clickByText(dialog, "Cancel");
        NodeUtils.clickByText(dialog, "取消");
    }

    /**
     * 不处理文本
     *
     * @param dialog
     * @param text
     * @param dm
     */
//    private void captureAndSend(AccessibilityNodeInfo dialog,
//                                DisplayMetrics dm) {
//
//        Rect rect = new Rect();
//        dialog.getBoundsInScreen(rect);
//
//        Bitmap full = ScreenCaptureManager.capture();
//        if (full == null) return;
//
//        float scaleX = full.getWidth() * 1f / dm.widthPixels;
//        float scaleY = full.getHeight() * 1f / dm.heightPixels;
//
//        Rect scaled = new Rect(
//                (int)(rect.left * scaleX),
//                (int)(rect.top * scaleY),
//                (int)(rect.right * scaleX),
//                (int)(rect.bottom * scaleY)
//        );
//
//        Bitmap crop = safeCrop(full, scaled);
//
//        if (crop != null) {
//            sendResult(crop, "输入框截图");
//        }
//    }
    private void captureAndSend(AccessibilityNodeInfo dialog,
                                String text,
                                DisplayMetrics dm) {

        Rect rect = new Rect();
        dialog.getBoundsInScreen(rect);

        Bitmap full = ScreenCaptureManager.capture();

        if (full == null) return;

        float scaleX = full.getWidth() * 1f / dm.widthPixels;
        float scaleY = full.getHeight() * 1f / dm.heightPixels;

        Rect scaled = new Rect(
                (int) (rect.left * scaleX),
                (int) (rect.top * scaleY),
                (int) (rect.right * scaleX),
                (int) (rect.bottom * scaleY)
        );

        Bitmap crop = safeCrop(full, scaled);

        if (crop != null) {
            sendResult(crop, text);
        }
    }


    private Bitmap safeCrop(Bitmap full, Rect rect) {

        if (full == null || rect == null) return null;

        int bitmapWidth = full.getWidth();
        int bitmapHeight = full.getHeight();

        int left = Math.max(0, rect.left);
        int top = Math.max(0, rect.top);

        int right = Math.min(bitmapWidth, rect.right);
        int bottom = Math.min(bitmapHeight, rect.bottom);

        int width = right - left;
        int height = bottom - top;

        // 防止非法值
        if (width <= 0 || height <= 0) return null;

        try {
            return Bitmap.createBitmap(full, left, top, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendResult(Bitmap bmp, String text) {

        try {
            Log.i(Contact.TAG, "sendussd-结果1: " + text + "==" + bmp);
            Intent intent = new Intent(Contact.ACTION_USSD_RESULT);
            // ⭐ 状态判断
            String status = "result";

            if (text.startsWith("ERROR")) {
                status = "error";
            } else if (text.startsWith("SUCCESS")) {
                status = "success";
            }
            intent.putExtra(Contact.STATUS_TEXT, status);
            intent.putExtra(Contact.EXTRA_TEXT, text);


            if (bmp != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // ⭐ 压缩（防止超1MB）
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] bytes = baos.toByteArray();
                intent.putExtra(Contact.EXTRA_IMG, bytes);
            }

            intent.setPackage(this.getPackageName());
            sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Log.i(Contact.TAG, "sendussd-e: " + e.getMessage());
        }
    }

    private Bitmap compressBitmap(Bitmap src) {

        int maxWidth = 800;

        if (src.getWidth() <= maxWidth) return src;

        float scale = maxWidth * 1f / src.getWidth();

        return Bitmap.createScaledBitmap(
                src,
                maxWidth,
                (int) (src.getHeight() * scale),
                true
        );
    }

    private List<AccessibilityNodeInfo> findAllButtons(AccessibilityNodeInfo node) {

        List<AccessibilityNodeInfo> list = new ArrayList<>();
        collectButtons(node, list);
        return list;
    }

    private void collectButtons(AccessibilityNodeInfo node,
                                List<AccessibilityNodeInfo> list) {

        if (node == null) return;

        if ("android.widget.Button".contentEquals(node.getClassName())
                && node.isClickable()) {

            list.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectButtons(node.getChild(i), list);
        }
    }

    private AccessibilityNodeInfo findUssdDialog(AccessibilityNodeInfo root) {

        if (root == null) return null;

        // ⭐ 方式1：直接找 message（最稳）
        AccessibilityNodeInfo msg = findByIds(root,
                "android:id/message",
                "com.android.phone:id/message"
        );

        if (msg != null) {
            return msg.getParent();
        }

        // ⭐ 方式2：找按钮少的容器（弹窗特征）
        return findDialogByStructure(root);
    }

    private AccessibilityNodeInfo findDialogByStructure(AccessibilityNodeInfo node) {

        if (node == null) return null;

        List<AccessibilityNodeInfo> buttons = findButtons(node);

        if (buttons.size() > 0 && buttons.size() <= 3) {

            String text = NodeUtils.getAllText(node);

            if (text != null && text.length() < 300) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo res = findDialogByStructure(node.getChild(i));
            if (res != null) return res;
        }

        return null;
    }

    private void handleFinal(AccessibilityNodeInfo dialog, String text, DisplayMetrics dm) {

        long now = System.currentTimeMillis();

        if (!text.equals(lastText)) {

            lastText = text;
            lastTextChangeTime = now;
            return;
        }

        if (now - lastTextChangeTime < STABLE_TIME) {
            return;
        }

        if (!isFinalDialog(dialog)) return;

        handleResult(dialog, text, "RESULT", dm);
    }

    private boolean isFinalDialog(AccessibilityNodeInfo dialog) {

        if (findUssdInput(dialog) != null) return false;

        List<AccessibilityNodeInfo> buttons = findButtons(dialog);

        if (buttons.size() == 1) return true;

        for (AccessibilityNodeInfo btn : buttons) {

            CharSequence t = btn.getText();

            if (t != null && t.toString().toLowerCase().contains("send")) {
                return false;
            }
        }

        return true;
    }

    private AccessibilityNodeInfo findByIds(AccessibilityNodeInfo node,
                                            String... ids) {

        for (String id : ids) {

            List<AccessibilityNodeInfo> list =
                    node.findAccessibilityNodeInfosByViewId(id);

            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }

        return null;
    }

    private List<AccessibilityNodeInfo> findButtons(AccessibilityNodeInfo node) {

        List<AccessibilityNodeInfo> list = new ArrayList<>();
        collect(node, list);
        return list;
    }

    private void collect(AccessibilityNodeInfo node,
                         List<AccessibilityNodeInfo> list) {

        if (node == null) return;

        if ("android.widget.Button".contentEquals(node.getClassName())
                && node.isClickable()) {

            list.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collect(node.getChild(i), list);
        }
    }
    private boolean isFinishedFail(String text){
        if (text == null) return false;

        return  text.contains("失败")
                || text.contains("错误")
                || text.contains("无效")
                || text.contains("MMI")
                || text.contains("连接问题")
                || text.contains("无法连接")
                || text.contains("network")
                || text.contains("invalid")
                || text.contains("无法访问")
                || text.toLowerCase().contains("error")
                || text.contains("خطأ")
                || text.contains("غير صالح") || text.contains("Connection issue")
                || text.contains("مشكلة في الاتصال")
                || text.contains("Cannot connect")
                || text.contains("لا يمكن الاتصال")
                || text.contains("Network exception")
                || text.contains("استثناء في الشبكة")
                || text.contains("Password error")
                || text.contains("خطأ في كلمة المرور")
                || text.contains("Password expired")
                || text.contains("كلمة المرور منتهية الصلاحية")
                || text.contains("Interrupted")
                || text.contains("مقطوع")
                ;
    }
    private boolean isImmediateError(String text) {
        if (text == null) return false;

        return text.contains("成功")
                ||text.contains("success")
                ||text.contains("النجاح")
                ||text.contains("نجحت العملية")
                ||text.contains("تمت العملية بنجاح")
                || text.contains("失败")
                || text.contains("错误")
                || text.contains("无效")
                || text.contains("MMI")
                || text.contains("连接问题")
                || text.contains("无法连接")
                || text.contains("network")
                || text.contains("invalid")
                || text.contains("无法访问")
                || text.toLowerCase().contains("error")
                || text.contains("خطأ")
                || text.contains("غير صالح") || text.contains("Connection issue")
                || text.contains("مشكلة في الاتصال")
                || text.contains("Cannot connect")
                || text.contains("لا يمكن الاتصال")
                || text.contains("Network exception")
                || text.contains("استثناء في الشبكة")
                || text.contains("Password error")
                || text.contains("خطأ في كلمة المرور")
                || text.contains("Password expired")
                || text.contains("كلمة المرور منتهية الصلاحية")
                || text.contains("Interrupted")
                || text.contains("مقطوع")
                ;
    }

    private void handleResult(AccessibilityNodeInfo dialog,
                              String text,
                              String type, DisplayMetrics dm) {

        handler.postDelayed(() -> {

//            Bitmap bmp = captureAndSend(dialog, text, dm);

            sendResult(null, type + ":" + text);

            clickClose(dialog);

            resetState();

        }, 500);
    }

    private void clickClose(AccessibilityNodeInfo dialog) {

        // ⭐ 系统OK按钮（最稳）
        if (NodeUtils.click(dialog, "android:id/button1")) return;

        // ⭐ 找唯一按钮点击
        List<AccessibilityNodeInfo> buttons = findAllButtons(dialog);

        if (buttons != null && buttons.size() == 1) {
            buttons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return;
        }

        // ⭐ fallback
        NodeUtils.clickByText(dialog, "OK");
        NodeUtils.clickByText(dialog, "确定");
    }

    private void resetState() {

        isProcessing = false;
        lastText = "";
        lastTextChangeTime = 0;

        UssdState.inputCount = 0;
        UssdState.handledFirst = false;
        UssdState.handledSecond = false;

        UssdController.hasInput = false;
        lastDialogHash = 0;
    }

    @Override
    public void onInterrupt() {
    }

}