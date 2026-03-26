package com.project.setussd.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

import com.project.setussd.R;

public class EditPwdDialog {

    /**
     * 显示密码输入弹窗
     * @param context 上下文（Activity）
     * @param defaultPwd 默认密码
     * @param confirmListener 确认按钮回调
     */
    public static void showInputPwdDialog(Context context, String defaultPwd, OnPwdConfirmListener confirmListener) {
        // 创建Dialog构建器
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_input_pwd, null);

        // 查找控件
        EditText inputField = dialogView.findViewById(R.id.et_pwd);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 设置布局和标题
        builder.setView(dialogView);
        builder.setTitle("密码设置");
        builder.setCancelable(false); // 禁止点击外部关闭

        // 创建并显示弹窗
        AlertDialog ussdDialog = builder.create();
        ussdDialog.show();

        // 设置默认密码
        if (defaultPwd != null && !defaultPwd.isEmpty()) {
            inputField.setText(defaultPwd);
        } else {
            inputField.setText("333777");
        }

        // 取消按钮
        btnCancel.setOnClickListener(v -> ussdDialog.dismiss());

        // 确认按钮：保存密码 + 回调
        btnConfirm.setOnClickListener(v -> {
            String inputPwd = inputField.getText().toString().trim();
            // 保存到缓存
            CacheUtils.put(context, "PWD", inputPwd);
            // 回调通知外部
            if (confirmListener != null) {
                confirmListener.onConfirm(inputPwd);
            }
            ussdDialog.dismiss();
        });
    }

    /**
     * 密码确认回调接口
     */
    public interface OnPwdConfirmListener {
        void onConfirm(String pwd);
    }
}
