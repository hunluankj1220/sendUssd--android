package com.project.setussd.utils.chato;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UssdState {

    public static AtomicInteger inputCount = new AtomicInteger(0);
    public static AtomicBoolean handledFirst = new AtomicBoolean(false);
    public static AtomicBoolean handledSecond = new AtomicBoolean(false);

    public static void reset() {
        inputCount.set(0);
        handledFirst.set(false);
        handledSecond.set(false);
    }
}
