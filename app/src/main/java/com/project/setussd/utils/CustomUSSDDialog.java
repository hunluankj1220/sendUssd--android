package com.project.setussd.utils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.project.setussd.R;

public class CustomUSSDDialog extends AlertDialog {
    private EditText etPassword;
    private Button btnSend, btnCancel;
    private OnCustomUSSDListener listener;
    private String autoPassword;

    public CustomUSSDDialog(Context context, String autoPassword) {
        super(context);
        this.autoPassword = autoPassword;
    }

    public void setOnCustomUSSDListener(OnCustomUSSDListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_custom_ussd);

        etPassword = findViewById(R.id.et_ussd_password);
        btnSend = findViewById(R.id.btn_ussd_send);
        btnCancel = findViewById(R.id.btn_ussd_cancel);

        // 自动填充充值密码
        etPassword.setText(autoPassword);
        // 光标移到最后
        etPassword.setSelection(etPassword.getText().length());

        // 发送按钮事件
        btnSend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSend(etPassword.getText().toString().trim());
            }
            dismiss();
        });

        // 取消按钮事件
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });

        // 禁止点击外部关闭
        setCanceledOnTouchOutside(false);
    }

    // 自定义回调接口
    public interface OnCustomUSSDListener {
        void onSend(String password);
        void onCancel();
    }
}