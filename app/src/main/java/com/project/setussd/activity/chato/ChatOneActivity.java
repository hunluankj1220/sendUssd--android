package com.project.setussd.activity.chato;

import static com.project.setussd.Contact.REQUEST_CODE_CALL_PHONE;
import static com.project.setussd.Contact.REQ_CAPTURE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.project.setussd.Contact;
import com.project.setussd.R;
import com.project.setussd.bean.UssAction;
import com.project.setussd.databinding.ActivityUssdmainBinding;
import com.project.setussd.log.UssdLogger;
import com.project.setussd.network.ApiCallback;
import com.project.setussd.network.ApiClient;
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
import com.project.setussd.work.MyWorker1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatOneActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private TelephonyManager telephonyManager;
    private ActivityUssdmainBinding binding;
    private static String AUTO_RECHARGE_PASSWORD = "333777";


    private String pwdStr;
    private String ussdCode;
    private ArrayAdapter<String> adapter;
    private List<String> logs = new ArrayList<>();
    private boolean isAccessibilityEnabled;

    private boolean isUserClick = true;

    private boolean openaccessible; // 无障碍模式

    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String log = intent.getStringExtra(UssdLogger.EXTRA_LOG);
            if (log != null) {
                logs.add(0, log);
                Log.i(Contact.TAG, "ussdlog: " + logs);
                adapter.notifyDataSetChanged();
            }
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.i(Contact.TAG, "sendussd-解析结果0: "+intent.getAction());
            byte[] bytes = intent.getByteArrayExtra(Contact.EXTRA_IMG);
            String text = intent.getStringExtra(Contact.EXTRA_TEXT);
            String status = intent.getStringExtra(Contact.STATUS_TEXT);
            Log.i(Contact.TAG, "sendussd-act-解析结果1: " + text + "==" + status);
            UssdLogger.log(getApplicationContext(), "执行USSD结果:" + text);
            runOnUiThread(() -> {

                UssdParser.Result parse = UssdParser.parse(text);
                String result =
                        "类型：" + parse.type + ";" +
                                "状态：" + parse.status + ";" +
                                "余额：" + parse.balance;
                Log.i(Contact.TAG, "sendussd-解析结果: " + result + "==" + text);
                binding.tvUssdResults.setText("解析结果：" + result);
                binding.tvUssdResult.setText("执行结果：" + text);
                if (bytes != null) {
                    binding.screenshotImageView.setVisibility(View.VISIBLE);
                    Bitmap bmp = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length
                    );
                    binding.screenshotImageView.setImageBitmap(bmp);
                }
                if ("error".equals(status)) {
//                        Toast.makeText(getApplicationContext(),"执行结果:"+status,Toast.LENGTH_LONG)
//                                .show();
                } else if ("success".equals(status)) {

                }
            });
        }
    };

    //
    public static ChatOneActivity mianActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUssdmainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CacheUtils.put(this, "PWD", "333777");
        openaccessible = CacheUtils.getBoolean(ChatOneActivity.this, "OPENACCESSIBLE");

        //
        Contact.phoneId = CacheUtils.getString(ChatOneActivity.this, "PHONEID");
        if (Contact.phoneId == null || Contact.phoneId.isBlank()) {
            Contact.phoneId = "tmp_" + System.currentTimeMillis();
            CacheUtils.put(ChatOneActivity.this, "PHONEID", Contact.phoneId);// 保存到缓存
        }
        //
        String tmpServerURL = CacheUtils.getString(ChatOneActivity.this, "serverURL");
        if (tmpServerURL != null && !tmpServerURL.isBlank()) {
            Contact.serverURL = tmpServerURL;
        }

        //
        setWindow();
        checkAndRequestAllPermissions();
        accessibleStatus();

        // 例如 "*100#"
        initView();

        //
        mianActivity = this;

        //
        // 创建一次性任务请求
//        OneTimeWorkRequest uploadWork = new OneTimeWorkRequest.Builder(MyWorker1.class).build();
//        // 提交任务
//        WorkManager.getInstance(this).enqueue(uploadWork);

        periodicWork = new PeriodicWorkRequest.Builder(MyWorker1.class, 5, TimeUnit.SECONDS).build();
        WorkManager.getInstance(this).enqueue(periodicWork);
    }

    PeriodicWorkRequest periodicWork;

    public void setWindow() {
        Window window = getWindow();

        // 1. 使布局延伸到屏幕边缘
        // Android 11+ 推荐用 setDecorFitsSystemWindows(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 取消FitsSystemWindows，使内容绘制到系统栏区域【80†L1079-L1083】
            window.setDecorFitsSystemWindows(false);
        } else {
            // 老方法：SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | STABLE
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 2. 隐藏状态栏与导航栏（沉浸模式）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用WindowInsetsController
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                // 隐藏系统栏（状态栏+导航栏）【80†L1079-L1083】
                controller.hide(WindowInsetsCompat.Type.systemBars());
                // 不自动恢复，沉浸粘性效果
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // API 30以下，使用旧Flag
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }

        // 3. 设置状态栏图标深色模式（适用于浅色背景）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                // true = 黑色图标（状态栏浅色模式）
                controller.setAppearanceLightStatusBars(true);
            }
        }
    }

    private void initView() {
        ussdCode = binding.etUssdCmd.getText().toString();
        binding.tvId.setText(Contact.phoneId);
        //
        binding.btnDebug.setOnClickListener((v) -> {
            String text = "test_message";
            UssdLogger.log(this, text);
            //
            Map<String, String> params = new HashMap<>();
            params.put("msg", text);
            ApiClient.request("/apin/vdfapp/msg", params, UssAction.class, new ApiCallback<UssAction>() {
                @Override
                public void onSuccess(UssAction data) {
                    UssdLogger.log(ChatOneActivity.this, "上报成功");
                }

                @Override
                public void onError(int code, String msg) {
                    UssdLogger.log(ChatOneActivity.this, "上报失败");
                }

                @Override
                public void onFailure(String error) {
                    UssdLogger.log(ChatOneActivity.this, "上报失败，网络错误");
                }
            });
        });
        //
        binding.btnRunUssd.setOnClickListener(v -> {
            startCapture();
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver,
                    new IntentFilter(Contact.ACTION_USSD_RESULT), Context.RECEIVER_EXPORTED);
            registerReceiver(logReceiver, new IntentFilter(UssdLogger.ACTION_LOG), Context.RECEIVER_EXPORTED);
        }
        Intent serviceIntent = new Intent(this, CaptureService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        binding.switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUserClick) return;
            if (isChecked) {
                //开启
                openAccessibilityUtils.openAccessibility(ChatOneActivity.this);
            } else {
                //关闭
                Toast.makeText(ChatOneActivity.this, "请在系统设置中手动关闭无障碍服务", Toast.LENGTH_LONG).show();
                openAccessibilityUtils.openAccessibility(ChatOneActivity.this);
            }
        });
        binding.switchBtnTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {

            } else {

            }
        });
        binding.titleBar.tvSetPwd.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            //加载popu菜单
            popupMenu.getMenuInflater().inflate(R.menu.menu_more, popupMenu.getMenu());
            //点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_setPwd) {
                    pwdStr = CacheUtils.getString(ChatOneActivity.this, "PWD");
                    EditPwdDialog.showInputPwdDialog(ChatOneActivity.this, "PWD", "密码设置", pwdStr, pwd -> {
                        // 可选：密码确认后的逻辑
                        Toast.makeText(ChatOneActivity.this, "密码已保存：" + pwd, Toast.LENGTH_SHORT).show();
                    });
                    return true;
                } else if (itemId == R.id.action_setServer) {
                    EditPwdDialog.showInputPwdDialog(ChatOneActivity.this, "serverURL", "服务器设置", Contact.serverURL, str -> {
                        Toast.makeText(ChatOneActivity.this, "已保存：" + str, Toast.LENGTH_SHORT).show();
                        Contact.serverURL = str;
                    });
                    return true;
                } else if (itemId == R.id.action_setId) {
                    EditPwdDialog.showInputPwdDialog(ChatOneActivity.this, "PHONEID", "设备ID设置", Contact.phoneId, pId -> {
                        Toast.makeText(ChatOneActivity.this, "已保存：" + pId, Toast.LENGTH_SHORT).show();
                        binding.tvId.setText(pId);
                        Contact.phoneId = pId;
                    });
                    return true;
                } else if (itemId == R.id.action_debug1) {
                    // 创建Dialog构建器
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatOneActivity.this);
                    builder.setTitle("提示");
                    builder.setMessage(ChatUssadAccessibilityService.allPkg1List.toString() + "\n\r-----\n\r" + ChatUssadAccessibilityService.allPkg2List.toString());
                    builder.show(); // 直接显示（推荐）
                    return true;
                }

                return false;
            });
            popupMenu.show();
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        binding.listview.setAdapter(adapter);
        //实时监听修改状态
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        accessibleStatus();
                    }
                }
        );
    }

    private void startCapture() {
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    private void accessibleStatus() {
        isUserClick = false;
        // ========== 核心修改：仅在权限未开启时才引导 ==========
        isAccessibilityEnabled = AccessibilityPermissionUtils.isAccessibilityServiceEnabled(this, ChatUssadAccessibilityService.class);
        binding.switchBtn.setChecked(isAccessibilityEnabled);
        isUserClick = true;
        Log.i(Contact.TAG, "accessibleStatus: " + isAccessibilityEnabled + "--" + openaccessible);
        if (!isAccessibilityEnabled) {
            // 权限未开启：引导用户开启
            updateUi("已关闭", "无障碍模式已关闭", R.color.purple_red);
            Toast.makeText(this, "请开启无障碍服务以控制USSD弹窗（仅需首次开启）", Toast.LENGTH_LONG).show();
            if (!openaccessible) {
                openAccessibilityUtils.openAccessibility(ChatOneActivity.this);
                CacheUtils.put(ChatOneActivity.this, "OPENACCESSIBLE", true);
            }
        } else {
            // 权限已开启：直接初始化功能
            updateUi("已开启", "无障碍模式已开启", R.color.colorPrimary);
            Toast.makeText(this, "无障碍服务已开启，可正常使用", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUi(String s, String t, int id) {
        binding.tvMoshi.setText(s);
        binding.tvMoshi.setTextColor(ContextCompat.getColor(this, id));
        binding.tvTishi.setText(t);
        binding.tvTishi.setTextColor(ContextCompat.getColor(this, id));
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
            CacheUtils.put(ChatOneActivity.this, "USSDCODE", ussdCode);
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
            //UssdLogger.log(this, "执行USSD命令:" + ussdCode);
            UssdController.start(this, ussdCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openAccessibilityUtils.dismiss();
        accessibleStatus();
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
        unregisterReceiver(logReceiver);
    }
}
