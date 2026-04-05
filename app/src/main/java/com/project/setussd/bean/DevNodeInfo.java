package com.project.setussd.bean;

import java.util.List;

// 自己记录node的内容
public class DevNodeInfo {
    public String viewIdResourceName;
    public String packageName;
    public String className;
    public String text;
    public Boolean clickable;
    public List<DevNodeInfo> subs;
}
