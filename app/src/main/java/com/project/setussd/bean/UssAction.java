package com.project.setussd.bean;

public class UssAction {
    public int step;
    public String password;
    public String action;     // INPUT_PASSWORD / FINISH / RETRY
    public boolean needRetry;
    public int retryCount;
}
