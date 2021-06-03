package app.kidswarmup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalTime;
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
        boolean sleep_active = false;
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
            Log.i(TAG, "doWork");
            LocalTime sleep_time_start  = LocalTime.parse(prefs.getString("sleep_time_start", ""));
            LocalTime sleep_time_finish = LocalTime.parse(prefs.getString("sleep_time_finish", ""));
            LocalTime ctime = LocalTime.now();
            //Log.i(TAG, "doWork: s = " + sleep_time_start + ", f = " + sleep_time_finish + ", ct = " + ctime);
            if (sleep_time_finish.isAfter(sleep_time_start)) {
                if (ctime.isAfter(sleep_time_start) && ctime.isBefore(sleep_time_finish))
                    sleep_active = true;
            } else {
                if (ctime.isAfter(sleep_time_start) || ctime.isBefore(sleep_time_finish))
                    sleep_active = true;
            }
        } catch (Exception e) {
            sleep_active = false;
            e.printStackTrace();
        }
        int zen_mode = 0; // getZenModeState();
        int air_mode = getAirplaneMode();
        Log.i(TAG, "doWork: workerId = " + getId() + ", sleep = " + sleep_active + ", air_mode = " + air_mode);
        if (sleep_active || zen_mode > 0)
            return Result.success();  // kids sleep
        if (air_mode > 0)
            return Result.success();  // device sleep

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

    public int getZenModeState() {
        return Settings.Global.getInt(m_context.getContentResolver(), "zen_mode", -1);
        // 0 = DnD : OFF
        // 1 = DnD : ON - Priority Only
        // 2 = DnD : ON - Total Silence
        // 3 = DnD : ON - Alarms Only
    }

    public int getAirplaneMode() {
        return Settings.Global.getInt(m_context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, -1);
    }
}
