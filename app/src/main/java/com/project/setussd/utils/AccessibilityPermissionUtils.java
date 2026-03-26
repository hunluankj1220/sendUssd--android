package com.project.setussd.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 无障碍权限检测工具类（全局生效，首次开启后永久有效）
 */
public class AccessibilityPermissionUtils {

    // 检查无障碍服务是否开启（核心方法）
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        try {
            // 读取系统无障碍全局设置
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
            );

            // 权限未开启直接返回false
            if (accessibilityEnabled != 1) {
                return false;
            }

            // 读取已开启的无障碍服务列表
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            if (TextUtils.isEmpty(enabledServices)) {
                return false;
            }

            // 拼接当前服务的完整名称（包名+类名）
            String serviceName = context.getPackageName() + "/" + serviceClass.getName();

            // 检查当前服务是否在启用列表中
            String[] services = enabledServices.split(":");
            for (String service : services) {
                if (service.trim().equals(serviceName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}