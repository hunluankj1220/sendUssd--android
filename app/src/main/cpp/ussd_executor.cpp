#include <jni.h>
#include <string>
#include <android/log.h>
#include <sys/system_properties.h>
#include <dlfcn.h>
#include <unistd.h>
#include <sys/wait.h>

#define LOG_TAG "USSDExecutor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void* ril_handle = nullptr;

// 函数声明
std::string executeIMEIQuery();
std::string executeGenericUSSD(const char* ussd_code);
std::string executeATCommand(const char* ussd_code);
std::string executeShellUSSD(const char* ussd_code);

// 仅 JNI 接口在 extern "C" 中
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_project_setussd_activity_UssdMainActivity_initializeUSSD(JNIEnv *env, jobject thiz) {
    LOGI("Initializing USSD JNI");

    const char* ril_libs[] = {
            "/system/lib/libril.so",
            "/system/lib64/libril.so",
            "/vendor/lib/libril.so",
            "/vendor/lib64/libril.so"
    };

    for (int i = 0; i < sizeof(ril_libs) / sizeof(ril_libs[0]); i++) {
        ril_handle = dlopen(ril_libs[i], RTLD_LAZY);
        if (ril_handle) {
            LOGI("Successfully loaded RIL library: %s", ril_libs[i]);
            break;
        }
    }

    if (!ril_handle) {
        LOGE("Failed to load RIL library");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_project_setussd_activity_UssdMainActivity_executeUSSDCommand(JNIEnv *env, jobject thiz, jstring command) {
    if (!command) {
        return env->NewStringUTF("Invalid command");
    }

    const char* ussd_code = env->GetStringUTFChars(command, nullptr);
    LOGI("Executing USSD command: %s", ussd_code);

    std::string result;

    if (strncmp(ussd_code, "*#06#", 5) == 0) {
//        result = executeIMEIQuery();
        result = executeGenericUSSD(ussd_code);
    } else {
        result = executeGenericUSSD(ussd_code);
    }

    env->ReleaseStringUTFChars(command, ussd_code);

    if (result.empty()) {
        return env->NewStringUTF("USSD execution failed");
    }

    return env->NewStringUTF(result.c_str());

//    const char *ussd = env->GetStringUTFChars(commands, nullptr);
//    char command[256];
//    snprintf(command, sizeof(command), "su -c \"uusd %s\"", ussd);
//    FILE *pipe = popen(command, "r");
//    std::string result;
//    if (pipe) {
//        char buffer[128];
//        while (fgets(buffer, sizeof(buffer), pipe)) result += buffer;
//        pclose(pipe);
//    }
//    env->ReleaseStringUTFChars(commands, ussd);
//    return env->NewStringUTF(result.empty() ? "No response" : result.c_str());
}

JNIEXPORT void JNICALL
Java_com_project_setussd_activity_UssdMainActivity_cleanupUSSD(JNIEnv *env, jobject thiz) {
LOGI("Cleaning up USSD JNI");

if (ril_handle) {
dlclose(ril_handle);
ril_handle = nullptr;
}
}

} // extern "C" 结束

// 以下为 C++ 实现函数，放在 extern "C" 外
std::string executeIMEIQuery() {
    LOGI("Executing IMEI query");

    char imei1[PROP_VALUE_MAX];

    const char* imei_props[] = {
            "ro.ril.imei",
            "gsm.imei",
            "persist.radio.imei",
            "ro.gsm.imei",
            "ril.imei"
    };

    for (int i = 0; i < sizeof(imei_props) / sizeof(imei_props[0]); i++) {
        if (__system_property_get(imei_props[i], imei1) > 0) {
            LOGI("Found IMEI via property %s: %s", imei_props[i], imei1);
            return std::string("IMEI: ") + imei1;
        }
    }

    const char* imei_files[] = {
            "/sys/class/android_usb/android0/iSerial",
            "/proc/app_info",
            "/data/nvram/APCFG/APRDEB/BT_Addr"
    };

    for (int i = 0; i < sizeof(imei_files) / sizeof(imei_files[0]); i++) {
        FILE* file = fopen(imei_files[i], "r");
        if (file) {
            char buffer[256];
            if (fgets(buffer, sizeof(buffer), file)) {
                fclose(file);
                LOGI("Found IMEI from file %s", imei_files[i]);
                return std::string("IMEI: ") + buffer;
            }
            fclose(file);
        }
    }

    return "IMEI: 123456789012345";
}

std::string executeGenericUSSD(const char* ussd_code) {
//    LOGI("Executing generic USSD: %s", ussd_code);
//
//    std::string at_result = executeATCommand(ussd_code);
//    if (!at_result.empty()) {
//        return at_result;
//    }
//
//    std::string shell_result = executeShellUSSD(ussd_code);
//    if (!shell_result.empty()) {
//        return shell_result;
//    }
//
//    return std::string("USSD命令 ") + ussd_code + " 已发送到网络";
    LOGI("Executing generic USSD without UI: %s", ussd_code);

    // 直接用 uusd 命令发送 USSD 并捕获输出
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "su -c \"uusd %s\"", ussd_code);
    FILE* pipe = popen(cmd, "r");
    if (!pipe) {
        return "执行 USSD 失败：无法打开管道";
    }

    char buffer[1024];
    std::string result;
    while (fgets(buffer, sizeof(buffer), pipe)) {
        result += buffer;
    }
    pclose(pipe);

    // 去掉可能的换行
    if (!result.empty() && result.back() == '\n') {
        result.pop_back();
    }

    if (result.empty()) {
        return "未收到网络返回信息";
    }

    LOGI("USSD raw result: %s", result.c_str());
    return result;
}

std::string executeATCommand(const char* ussd_code) {
    const char* modem_devices[] = {
            "/dev/ttyUSB0",
            "/dev/ttyUSB1",
            "/dev/ttyUSB2",
            "/dev/ttyACM0",
            "/dev/smd0"
    };

    for (int i = 0; i < sizeof(modem_devices) / sizeof(modem_devices[0]); i++) {
        FILE* modem = fopen(modem_devices[i], "w+");
        if (modem) {
            LOGI("Opened modem device: %s", modem_devices[i]);

            fprintf(modem, "AT+CUSD=1,\"%s\",15\r\n", ussd_code);
            fflush(modem);

            char response[1024];
            if (fgets(response, sizeof(response), modem)) {
                fclose(modem);
                LOGI("AT command response: %s", response);
                return std::string(response);
            }

            fclose(modem);
        }
    }

    return "";
}

std::string executeShellUSSD(const char* ussd_code) {
    char command[256];
    snprintf(command, sizeof(command), "am start -a android.intent.action.CALL -d tel:%s", ussd_code);

    LOGI("Executing shell command: %s", command);

    FILE* pipe = popen(command, "r");
    if (pipe) {
        char buffer[1024];
        std::string result;

        while (fgets(buffer, sizeof(buffer), pipe)) {
            result += buffer;
        }

        pclose(pipe);

        if (!result.empty()) {
            LOGI("Shell command result: %s", result.c_str());
            return result;
        }
    }

    return "";
}