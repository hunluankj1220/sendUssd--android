package com.project.setussd.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWorker1 extends Worker {

    public MyWorker1(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        // 执行后台任务，例如上传图片
        boolean success = uploadImages();
        return success ? Result.success() : Result.failure();
    }

    private boolean uploadImages() {
        Log.i("USSD_LOG", "uploadImages ");
        // 模拟上传逻辑
        return true;
    }
}
