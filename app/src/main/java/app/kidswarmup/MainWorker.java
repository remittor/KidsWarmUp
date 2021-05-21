package app.kidswarmup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainWorker extends Worker {
 
    static final String TAG = "workmgr";
    public static final String workName = "app.kidswarmup.MainWorker";
    private Context m_context;

    public MainWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.m_context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "doWork: workerId = " + getId());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(m_context, MainActivity.class);
                intent.putExtra("after_worker", true);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                m_context.startActivity(intent);
            }
        });
        /* try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */
        return Result.success();
    }

    public boolean getDisplayActive() {
        DisplayManager dm = (DisplayManager) getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }

    public boolean getSystemInteractive() {
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        return pm.isInteractive();
    }

}
