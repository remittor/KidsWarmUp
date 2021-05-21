package app.kidswarmup;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.InputDeviceCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK;
import static android.view.InputDevice.SOURCE_KEYBOARD;
import static android.view.InputDevice.SOURCE_MOUSE;
import static android.view.InputDevice.SOURCE_MOUSE_RELATIVE;
import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.BUTTON_PRIMARY;
import static android.view.MotionEvent.BUTTON_SECONDARY;

public class MainActivity extends Activity implements View.OnGenericMotionListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context appContext;
    private boolean rootPresent = false;
    private FrameLayout mainFrame;
    private FrameLayout canvasFrame;
    private FloatingActionButton buttonMenu;
    private int stepsCurrent = 0;
    private int stepsTarget = 0;
    private boolean configured = false;
    private int prevButton = 0;
    private boolean firstStepHalfDone = false;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: after_worker = " + getIntent().getBooleanExtra("after_worker", false));
        loadPrefs();
        setContentView(R.layout.activity_main);
        mainFrame = findViewById(R.id.mainFrame);
        mainFrame.setOnGenericMotionListener(this);
        //mainFrame.setOnKeyListener(this);
        //mainFrame.getContext();
        canvasFrame = findViewById(R.id.canvas_frame);
        buttonMenu = findViewById(R.id.menu_btn);
        buttonMenu.setOnClickListener(this);
        appContext = getApplicationContext();
        rootPresent = checkRootAvailability();
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        enableKioskMode();
        setupUI();
        //setControlsVisibility(false);
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
            if (configured) {
                //
            }
        }
        getMainWorkerInfo();
        boolean after_worker = getIntent().getBooleanExtra("after_worker", false);
        initMainWorker(false, after_worker);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        stepsCurrent = savedInstanceState.getInt("stepsCurrent");
        stepsTarget = savedInstanceState.getInt("stepsTarget");
        configured = savedInstanceState.getBoolean("configured");
        prevButton = savedInstanceState.getInt("prevButton");
        firstStepHalfDone = savedInstanceState.getBoolean("firstStepHalfDone");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("stepsCurrent", stepsCurrent);
        outState.putInt("stepsTarget", stepsTarget);
        outState.putBoolean("configured", configured);
        outState.putInt("prevButton", prevButton);
        outState.putBoolean("firstStepHalfDone", firstStepHalfDone);
    }

    public void loadPrefs(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //workRequestId = UUID.fromString(prefs.getString("workRequestId", "0-0-0-0-0"));
        //setupUI();
    }

    public void savePrefs(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = prefs.edit();
        //ed.putString("workRequestId", workRequestId.toString());
        ed.commit();
        Log.i(TAG, "savePrefs");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        savePrefs();
    }

    @Override
    protected void onResume() {
        //Log.i(TAG, "onResume");
        Log.i(TAG, "onResume: after_worker = " + getIntent().getBooleanExtra("after_worker", false));
        getIntent().putExtra("after_worker", false);
        super.onResume();
        setControlsVisibility(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            boolean pref_changed = data.getBooleanExtra("pref_changed", false);
            Log.i(TAG, "pref_changed = " + pref_changed);
            if (pref_changed)
                initMainWorker(pref_changed, false);
            if (resultCode == Activity.RESULT_OK){
                boolean app_close = data.getBooleanExtra("app_close", false);
                //Toast.makeText(this, "RESULT_OK: app_close = " + app_close, Toast.LENGTH_LONG).show();
                if (app_close) {
                    disableKioskMode();
                    finish();
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Toast.makeText(this, "RESULT_CANCELED", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean stopMainWorker() {
        try {
            WorkManager wrkmgr = WorkManager.getInstance(getApplicationContext());
            wrkmgr.cancelUniqueWork(MainWorker.workName);
            Log.i(TAG, "MainWorker canceled!");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean initMainWorker(boolean pref_changed, boolean after_worker) {
        if (!pref_changed && !after_worker)
            return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean app_autorun = prefs.getBoolean("app_autorun", false);
        long timeout = 0;
        try {
            timeout = Integer.parseInt(prefs.getString("timeout", "empty"));
        } catch (Exception e) {
            app_autorun = false;
        }
        boolean timeout_init_use = prefs.getBoolean("timeout_init_use", false);
        long timeout_init = 0;
        try {
            timeout_init = Integer.parseInt(prefs.getString("timeout_init", "empty"));
        } catch (Exception e) {
            timeout_init_use = false;
        }
        if (!app_autorun && !timeout_init_use)
            return stopMainWorker();

        if (after_worker) {
            if (!timeout_init_use)
                return false;
            timeout_init_use = false;  // disable initial timeout
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("timeout_init_use", timeout_init_use).commit();
            stopMainWorker();
            if (!app_autorun)
                return true;
        }
        timeout = (timeout < 15) ? 15 : timeout;
        timeout_init = (timeout_init < 1) ? 1 : timeout_init;

        long repeatInterval = timeout;
        long flexInterval = 5;

        if (timeout_init_use) {
            if (timeout_init >= 10) {
                repeatInterval = timeout_init + 5;
                flexInterval = 5;
            } else {
                repeatInterval = 15;
                flexInterval = (timeout_init > 10) ? 5 : 15 - timeout_init;
            }
            Log.i(TAG, "MainWorker: timeout_init = " + timeout_init);
        } else {
            Log.i(TAG, "MainWorker: timeout = " + timeout);
        }
        TimeUnit tu = TimeUnit.MINUTES;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(MainWorker.class, repeatInterval, tu, flexInterval, tu).build();
        WorkManager wrkmgr = WorkManager.getInstance(getApplicationContext());
        wrkmgr.enqueueUniquePeriodicWork(MainWorker.workName, ExistingPeriodicWorkPolicy.REPLACE, request);
        Log.i(TAG, "MainWorker started: repeatInterval = " + repeatInterval + ", flexInterval = " + flexInterval + ", id = " + request.getId());
        return true;
    }

    private void getMainWorkerInfo() {
        WorkManager wrkmgr = WorkManager.getInstance(getApplicationContext());
        ListenableFuture<List<WorkInfo>> wilst = wrkmgr.getWorkInfosForUniqueWork(MainWorker.workName);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = wilst.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = (state == WorkInfo.State.RUNNING) | (state == WorkInfo.State.ENQUEUED);
                Log.i(TAG, "worker: " + workInfo);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checkRootAvailability() {
        //commands.add("mount -o rw,remount /system");
        try {
            String[] cmd = new String[]{"true"};
            doSuCmd(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to run true as root - is root available?", e);
            return false;
        }
        return true;
    }

    public void doSuCmd(String[] commands) throws Exception {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        for (String tmpCmd : commands) {
            os.writeBytes(tmpCmd + "\n");
        }
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        process.waitFor();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int source = event.getSource();
        while (source == SOURCE_KEYBOARD) {
            if ((prevButton == 0 || prevButton == KeyEvent.KEYCODE_X) && keyCode == KeyEvent.KEYCODE_Z) {
                countStep();
                prevButton = keyCode;
                break;
            }
            if ((prevButton == 0 || prevButton == KeyEvent.KEYCODE_Z) && keyCode == KeyEvent.KEYCODE_X) {
                countStep();
                prevButton = keyCode;
                break;
            }
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        int source = event.getSource();
        if (source != SOURCE_MOUSE && source != SOURCE_MOUSE_RELATIVE)
            return false;
        if (event.getActionMasked() != ACTION_BUTTON_PRESS)
            return false;
        int buttonState = event.getButtonState();
        if ((prevButton == 0 || prevButton == BUTTON_SECONDARY) && buttonState == BUTTON_PRIMARY) {
            countStep();
            prevButton = buttonState;
            return false;
        }
        if ((prevButton == 0 || prevButton == BUTTON_PRIMARY) && buttonState == BUTTON_SECONDARY) {
            countStep();
            prevButton = buttonState;
            return false;
        }
        return false;
    }

    private void countStep() {
        Log.i(TAG, "stepsCurrent = " + stepsCurrent + ", stepsTarget = " + stepsTarget);
        stepsCurrent += 1;
        if (stepsCurrent >= stepsTarget) {
            configured = false;
            setControlsVisibility(true);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.menu_btn)
            onMenuButtonClick();
    }

    private void onMenuButtonClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String psw = prefs.getString("menu_password", "1234");
        if (psw.isEmpty()) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 1);
        } else {
            PasswordDialog.show(mainFrame.getContext(), psw, 1);
        }
    }

    private void setupUI() {
        //Display display = getWindowManager().getDefaultDisplay();
        //DisplayMetrics dispMetrics = new DisplayMetrics();
        //display.getMetrics(dispMetrics);
        //float dpHeight = dispMetrics.heightPixels / dispMetrics.scaledDensity;
        //float dpWidth  = dispMetrics.widthPixels / dispMetrics.scaledDensity;
        //Log.i(TAG, "display size: " + dpWidth + " x " + dpHeight + " dp (" + dispMetrics.scaledDensity + ")");
        //Log.i(TAG, "canvas size: " + canvasFrame.getLayoutParams().width + " x " + canvasFrame.getLayoutParams().height);
        ViewGroup.LayoutParams clp = canvasFrame.getLayoutParams();
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(clp.width, clp.height, 1);
        canvasFrame.setLayoutParams(nlp);
    }

    private void setControlsVisibility(boolean configuring) {
        int visibility = configuring ? View.VISIBLE : View.GONE;
    }

    private void enableKioskMode() {
        Log.i(TAG, "enableKioskMode()");
        ComponentName componentName = DeviceAdminReceiver.getComponentName(this);
        String packageName = getPackageName();
        if (dpm.isDeviceOwnerApp(packageName) && dpm.isAdminActive(componentName)) {
            //Log.i(TAG, "Calling setLockTaskPackages()");
            try {
                dpm.setLockTaskPackages(componentName, new String[]{ packageName });
            } catch (SecurityException e) {
                Log.e(TAG, "FAIL: setLockTaskPackages()", e);
                Toast.makeText(this, R.string.set_lock_packages_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.i(TAG, "isDeviceOwnerApp() == " + dpm.isDeviceOwnerApp(packageName) + " isAdminActive() == " + dpm.isAdminActive(componentName));
        }
        if (dpm.isLockTaskPermitted(packageName) == false) {
            Log.i(TAG, "isLockTaskPermitted() returned FALSE");
            //Toast.makeText(this, R.string.set_lock_permitted_failed, Toast.LENGTH_LONG).show();
            Toast.makeText(this, R.string.enable_device_admin, Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    dpm.setLockTaskFeatures(componentName, LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK);
                } else {
                    dpm.setLockTaskFeatures(componentName, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "FAIL: setLockTaskFeatures()", e);
                Toast.makeText(this, R.string.set_lock_features_failed, Toast.LENGTH_LONG).show();
            }
        }
        startLockTask();
    }

    private void disableKioskMode() {
        Log.i(TAG, "disableKioskMode()");
        stopLockTask();
    }
}