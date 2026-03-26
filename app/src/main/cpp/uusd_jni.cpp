#include <jni.h>
#include <string>
#include <cstdlib>
#include <cstdio>
#include <sstream>
#include <iomanip>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "uusd_jni", __VA_ARGS__)
#define LOG_TAG "UUSD_JNI"

// URL 编码函数：将 # 等特殊字符转换为 %23 等形式
std::string urlEncode(const std::string& str) {
    std::ostringstream encoded;
    for (unsigned char c : str) {
        if (isalnum(c) || c == '*' || c == '+' || c == ',' || c == ';' || c == '@') {
            encoded << c;
        } else {
            encoded << '%' << std::uppercase << std::hex << std::setw(2)
                    << std::setfill('0') << static_cast<int>(c);
        }
    }
    return encoded.str();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_project_setussd_service_UusdUtils_execUusdCommand(JNIEnv *env, jclass thiz, jstring code) {
    const char *cstr = env->GetStringUTFChars(code, JNI_FALSE);
    std::string rawCode = cstr;
    env->ReleaseStringUTFChars(code, cstr);

    // 编码 USSD 命令中的特殊字符
    std::string encodedCode = urlEncode(rawCode);
    std::string uri = "tel:" + encodedCode;
    LOGI("Encoded USSD URI: %s", uri.c_str());

    // 获取应用上下文
    jclass appClass = env->FindClass("com/project/setussd/MyApplication");
    jmethodID getCtx = env->GetStaticMethodID(appClass, "getContext", "()Landroid/content/Context;");
    jobject ctx = env->CallStaticObjectMethod(appClass, getCtx);

    // 构造 Intent(android.intent.action.CALL, Uri.parse("tel:xxx"))
    jclass intentCls = env->FindClass("android/content/Intent");
    jmethodID ctor = env->GetMethodID(intentCls, "<init>", "(Ljava/lang/String;Landroid/net/Uri;)V");
    jstring actionCall = env->NewStringUTF("android.intent.action.CALL");

    jclass uriCls = env->FindClass("android/net/Uri");
    jmethodID parse = env->GetStaticMethodID(uriCls, "parse", "(Ljava/lang/String;)Landroid/net/Uri;");
    jobject uriObj = env->CallStaticObjectMethod(uriCls, parse, env->NewStringUTF(uri.c_str()));
    jobject intent = env->NewObject(intentCls, ctor, actionCall, uriObj);

    // 添加 FLAG_ACTIVITY_NEW_TASK
    jmethodID addFlags = env->GetMethodID(intentCls, "addFlags", "(I)Landroid/content/Intent;");
    const int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    env->CallObjectMethod(intent, addFlags, FLAG_ACTIVITY_NEW_TASK);

    // 启动拨号 Intent
    jclass ctxCls = env->FindClass("android/content/Context");
    jmethodID startAct = env->GetMethodID(ctxCls, "startActivity", "(Landroid/content/Intent;)V");
    env->CallVoidMethod(ctx, startAct, intent);

    LOGI("USSD Intent sent: %s", uri.c_str());
    return 0;
}
//#include <jni.h>
//#include <string>
//#include <cstdlib>
//#include <cstdio>
//
//extern "C"
//JNIEXPORT jstring JNICALL
//Java_com_project_senussd_service_UusdUtils_execUusdCommand(JNIEnv *env, jclass clazz, jstring command) {
//    const char *nativeCmd = env->GetStringUTFChars(command, 0);
//    FILE *fp;
//    char buffer[128];
//    std::string result;
//
//    // 调用带 su 权限的命令（如有 root）
//    std::string fullCmd = "su -c \"" + std::string(nativeCmd) + "\"";
//
//    fp = popen(fullCmd.c_str(), "r");
//    if (fp == nullptr) {
//        env->ReleaseStringUTFChars(command, nativeCmd);
//        return env->NewStringUTF("执行失败");
//    }
//
//    while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
//        result += buffer;
//    }
//
//    pclose(fp);
//    env->ReleaseStringUTFChars(command, nativeCmd);
//
//    return env->NewStringUTF(result.c_str());
//}