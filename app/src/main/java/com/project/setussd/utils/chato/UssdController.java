package com.project.setussd.utils.chato;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.project.setussd.Contact;

/**
 * 发送ussd指令
 */
public class UssdController {

    public static boolean hasInput = false;

    public static void start(Context context, String ussd) {

        hasInput = false;
//        Log.i(Contact.TAG, "sendUssd: ."+ussd);
        String encode = Uri.encode(ussd);
        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + encode));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}