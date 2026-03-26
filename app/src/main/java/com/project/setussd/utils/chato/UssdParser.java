package com.project.setussd.utils.chato;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UssdParser {

    public static class Result {
        public String type = "未知";
        public String status = "未知";
        public String balance = "";
        public String message = "";
    }

    public static Result parse(String text) {

        Result r = new Result();
        r.message = text;

        if (text == null) return r;

        String lower = text.toLowerCase();

        // ===== 状态判断 =====
        if (lower.contains("success") || lower.contains("successful") || lower.contains("done") || lower.contains("completed")) {
            r.status = "成功";
        } else if (lower.contains("fail") || lower.contains("error")) {
            r.status = "失败";
        }

        // ===== 余额识别（重点：埃及EGP）=====
        Pattern p = Pattern.compile("(\\d+\\.?\\d*)\\s?(egp|£|le)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);

        if (m.find()) {
            r.balance = m.group(1) + " " + m.group(2);
            r.type = "余额查询";
            r.status = "成功";
        }

        // ===== 其他类型 =====
        if (lower.contains("recharge")) {
            r.type = "充值";
        }
        if (lower.contains("رصيد")) {
            r.type = "余额查询";
        }
        if (lower.contains("transfer")) {
            r.type = "转账";
        }

        return r;
    }
}