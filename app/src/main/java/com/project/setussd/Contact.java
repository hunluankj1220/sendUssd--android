package com.project.setussd;

public class Contact {
    public static  String TAG = "SENDUSSD";
    public static final int REQ_CAPTURE = 1001;
    public static String AUTO_RECHARGE_PASSWORD = "333777";
    public static String AUTO_RECHARGE_USSDCODE = "*9*7*01080975195*100#";
    public static final String ACTION_USSD_RESULT =
            "com.project.setussd.USSSD_RESULT";
    public static final int REQUEST_CODE_CALL_PHONE = 1;
    public static final String EXTRA_IMG = "img";
    public static final String EXTRA_TEXT = "text";
    public static final String STATUS_TEXT = "status";
    // ⭐ 是否只处理第二个输入框
    public static boolean ONLY_SECOND_INPUT = true;
    public static int lastDialogHash = 0;
}
