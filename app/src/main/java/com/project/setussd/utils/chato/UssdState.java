package com.project.setussd.utils.chato;

public class UssdState {

//    // ⭐ 当前输入弹窗计数
//    public static int inputDialogCount = 0;
//
//    // ⭐ 防重复输入
//    public static boolean hasInput = false;
//
//    // ⭐ 防重复截图
//    public static boolean hasCaptured = false;

    public static int inputCount = 0;

    public static boolean handledFirst = false;
    public static boolean handledSecond = false;

    public static void reset() {
        inputCount = 0;
        handledFirst = false;
        handledSecond = false;
    }
}