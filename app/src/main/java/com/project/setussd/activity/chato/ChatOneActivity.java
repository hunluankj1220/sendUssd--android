package com.project.setussd.activity.chato;

import static com.project.setussd.Contact.REQUEST_CODE_CALL_PHONE;
import static com.project.setussd.Contact.REQ_CAPTURE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.project.setussd.Contact;
import com.project.setussd.databinding.ActivityUssdmainBinding;
import com.project.setussd.service.chato.CaptureService;
import com.project.setussd.service.chato.ChatUssadAccessibilityService;
import com.project.setussd.utils.AccessibilityPermissionUtils;
import com.project.setussd.utils.CacheUtils;
import com.project.setussd.utils.EditPwdDialog;
import com.project.setussd.utils.chato.ScreenCaptureManager;
import com.project.setussd.utils.chato.UssdController;
import com.project.setussd.utils.chato.UssdParser;
import com.project.setussd.utils.chato.UssdState;
import com.project.setussd.utils.openAccessibilityUtils;

public class ChatOneActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private TelephonyManager telephonyManager;
    private ActivityUssdmainBinding binding;
    private static String AUTO_RECHARGE_PASSWORD = "333777";
    private String pwdStr;
    private String ussdCode;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.i(Contact.TAG, "sendussd-解析结果0: "+intent.getAction());
            byte[] bytes = intent.getByteArrayExtra(Contact.EXTRA_IMG);
            String text = intent.getStringExtra(Contact.EXTRA_TEXT);
            String status = intent.getStringExtra(Contact.STATUS_TEXT);
            Log.i(Contact.TAG, "sendussd-act-解析结果1: "+text+"=="+status);

                runOnUiThread(() -> {
                    UssdParser.Result parse = UssdParser.parse(text);
                    String result =
                            "类型：" + parse.type + "\n" +
                                    "状态：" + parse.status + "\n" +
                                    "余额：" + parse.balance;
                    Log.i(Contact.TAG, "sendussd-解析结果: "+result+"=="+text);
                    binding.tvUssdResults.setText("解析结果：\n" + result);

                    binding.tvUssdResult.setText("执行结果："+text);
                    if (bytes != null) {
                        binding.screenshotImageView.setVisibility(View.VISIBLE);
                        Bitmap bmp = BitmapFactory.decodeByteArray(
                                bytes, 0, bytes.length
                        );
                        binding.screenshotImageView.setImageBitmap(bmp);
                    }
                    if ("error".equals(status)){
//                        Toast.makeText(getApplicationContext(),"执行结果:"+status,Toast.LENGTH_LONG)
//                                .show();
                    }else if ("success".equals(status)){

                    }
                });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUssdmainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkAndRequestAllPermissions();
        // ========== 核心修改：仅在权限未开启时才引导 ==========
        if (!AccessibilityPermissionUtils.isAccessibilityServiceEnabled(this, ChatUssadAccessibilityService.class)) {
            // 权限未开启：引导用户开启
            Toast.makeText(this, "请开启无障碍服务以控制USSD弹窗（仅需首次开启）", Toast.LENGTH_LONG).show();
            openAccessibilityUtils.openAccessibility(ChatOneActivity.this);
        } else {
            // 权限已开启：直接初始化功能
            Toast.makeText(this, "无障碍服务已开启，可正常使用", Toast.LENGTH_SHORT).show();
        }

        CacheUtils.put(this, "PWD", "333777");
        // 例如 "*100#"
        ussdCode = binding.etUssdCmd.getText().toString();
        binding.titleBar.tvSetPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pwdStr = CacheUtils.getString(ChatOneActivity.this, "PWD");
                EditPwdDialog.showInputPwdDialog(ChatOneActivity.this, pwdStr, pwd -> {
                    // 可选：密码确认后的逻辑
                    Toast.makeText(ChatOneActivity.this, "密码已保存：" + pwd, Toast.LENGTH_SHORT).show();
                });
            }
        });
        binding.btnRunUssd.setOnClickListener(v -> {
            startCapture();
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver,
                    new IntentFilter(Contact.ACTION_USSD_RESULT),Context.RECEIVER_EXPORTED);
        }
        Intent serviceIntent = new Intent(this, CaptureService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    private void startCapture() {
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }
    private void checkAndRequestAllPermissions() {
        // 1. 电话相关权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CODE_CALL_PHONE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_CALL_PHONE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    String trim = binding.tvCode.getText().toString().trim();
                } else {
                    Toast.makeText(this, "CALL_PHONE权限被拒绝，无法发送USSD指令", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQUEST_CODE_CALL_PHONE) {
//            String trim = binding.tvCode.getText().toString().trim();
        }
        if (req == REQ_CAPTURE && res == RESULT_OK) {

            MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

            MediaProjection projection =
                    mpm.getMediaProjection(res, data);

            DisplayMetrics dm = getResources().getDisplayMetrics();

            ScreenCaptureManager.init(
                    projection,
                    dm.widthPixels,
                    dm.heightPixels,
                    dm.densityDpi
            );
            ussdCode = binding.etUssdCmd.getText().toString();
            // ⭐ 执行USSD
            UssdState.reset();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CODE_CALL_PHONE);
                return;
            }
            ChatUssadAccessibilityService service =
                    ChatUssadAccessibilityService.getInstance();

            if (service != null) {
                service.startUssdTask();
            }
            UssdController.start(this, ussdCode);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
//        registerReceiver(receiver,
//                new IntentFilter(Contact.ACTION_USSD_RESULT),Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
