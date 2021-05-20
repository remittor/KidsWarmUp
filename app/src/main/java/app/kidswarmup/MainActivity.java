package app.kidswarmup;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private TextView stepsCurrentTextView;
    private TextView stepsTargetTextView;
    private ProgressBar progressBar;
    private EditText stepsTargetEditText;
    private Button buttonStart;
    private Button buttonStop;
    private FloatingActionButton buttonMenu;
    private Button buttonDevAdmInstall;
    private Button buttonDevAdmDelete;
    private int stepsCurrent = 0;
    private int stepsTarget = 0;
    private boolean configured = false;
    private int prevButton = 0;
    private boolean firstStepHalfDone = false;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPrefs();
        setContentView(R.layout.activity_main);
        mainFrame = findViewById(R.id.mainFrame);
        mainFrame.setOnGenericMotionListener(this);
        //mainFrame.setOnKeyListener(this);
        //mainFrame.getContext();
        stepsCurrentTextView = findViewById(R.id.steps_left);
        stepsTargetEditText = findViewById(R.id.steps_setup);
        progressBar = findViewById(R.id.progressBar);
        stepsTargetTextView = findViewById(R.id.steps_target);
        buttonStart = findViewById(R.id.button_start);
        buttonStart.setOnClickListener(this);
        buttonStop = findViewById(R.id.button_stop);
        buttonStop.setOnClickListener(this);
        buttonMenu = findViewById(R.id.menu_btn);
        buttonMenu.setOnClickListener(this);
        appContext = getApplicationContext();
        rootPresent = checkRootAvailability();
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
            if (configured) {
                setControlsVisibility(false);
                enableKioskMode();
                setupUI();
            }
        }
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
        Toast.makeText(this, "savePrefs", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "MainActivity onDestroy");
        super.onDestroy();
        savePrefs();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "MainActivity onResume");
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            boolean pref_changed = data.getBooleanExtra("pref_changed", false);
            Log.i(TAG, "pref_changed = " + pref_changed);
            if (pref_changed)
                initMainWorker(0, 0);
            if (resultCode == Activity.RESULT_OK){
                boolean app_close = data.getBooleanExtra("app_close", false);
                //Toast.makeText(this, "RESULT_OK: app_close = " + app_close, Toast.LENGTH_LONG).show();
                if (app_close) {
                    Log.i(TAG, "disableKioskMode()");
                    stopLockTask();
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

    private boolean initMainWorker(long repeatInterval, long flexInterval) {
        TimeUnit tu = TimeUnit.MINUTES;
        if (repeatInterval < 0)
            return stopMainWorker();
        boolean app_autorun = false;
        if (repeatInterval == 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            app_autorun = prefs.getBoolean("app_autorun", false);
            try {
                repeatInterval = Integer.parseInt(prefs.getString("timeout", "40"));
                flexInterval = Integer.parseInt(prefs.getString("timeout_first", "5"));
            } catch (Exception e) {
                app_autorun = false;
            }
            if (!app_autorun || repeatInterval <= 0)
                return stopMainWorker();
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(MainWorker.class, repeatInterval, tu, flexInterval, tu).build();
        WorkManager wrkmgr = WorkManager.getInstance(getApplicationContext());
        wrkmgr.enqueueUniquePeriodicWork(MainWorker.workName, ExistingPeriodicWorkPolicy.REPLACE, request);
        Log.i(TAG, "MainWorker started: timeout = " + repeatInterval + ", flex = " + flexInterval);
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
                Log.i(TAG, "worker id  = " + workInfo.getId());
            }
            //return running;
        } catch (ExecutionException e) {
            e.printStackTrace();
            //return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            //return false;
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
        progressBar.incrementProgressBy(1);
        stepsCurrentTextView.setText(String.valueOf(stepsCurrent));
        if (stepsCurrent >= stepsTarget) {
            configured = false;
            setControlsVisibility(true);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_start)
            onStartButtonClick();
        if (id == R.id.button_stop)
            onStopButtonClick();
        if (id == R.id.menu_btn)
            onMenuButtonClick();
    }

    private void onMenuButtonClick() {
        //PasswordDialog.show(mainFrame.getContext(), "123");
        //Intent i = new Intent(MainActivity.this, SettingsActivity.class);
        //startActivity(i);
        Intent i = new Intent(this, SettingsActivity.class);
        startActivityForResult(i, 1);
    }

    private void onStopButtonClick() {
        setupUI();
        disableKioskMode();
    }

    private void onStartButtonClick() {
        try {
            stepsTarget = Integer.parseInt(stepsTargetEditText.getText().toString());
        } catch (NumberFormatException ignored) {
            // May be thrown if stepsTargetEditText is empty
            return;
        }
        setControlsVisibility(false);
        stepsCurrent = 0;
        setupUI();
        enableKioskMode();
        configured = true;
    }

    private void setupUI() {
        stepsCurrentTextView.setText(String.valueOf(stepsCurrent));
        stepsTargetTextView.setText(String.valueOf(stepsTarget));
        progressBar.setMax(stepsTarget);
        progressBar.setProgress(stepsCurrent);
    }

    private void setControlsVisibility(boolean configuring) {
        int visibility = configuring ? View.VISIBLE : View.GONE;
        stepsTargetEditText.setVisibility(visibility);
        buttonStart.setVisibility(visibility);
        if (configuring) {
            buttonStop.setVisibility(View.GONE);
        } else {
            buttonStop.setVisibility(View.VISIBLE);
        }
    }

    private void enableKioskMode() {
        Log.i(TAG, "enableKioskMode()");
        ComponentName componentName = DeviceAdminReceiver.getComponentName(this);
        String packageName = getPackageName();
        if (dpm.isDeviceOwnerApp(packageName) && dpm.isAdminActive(componentName)) {
            Log.i(TAG, "Calling setLockTaskPackages()");
            String[] packages = new String[]{packageName};
            try {
                dpm.setLockTaskPackages(componentName, packages);
            } catch (SecurityException e) {
                Log.e(TAG, "setLockTaskPackages() failed", e);
                Toast.makeText(this, R.string.set_lock_packages_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.i(TAG, "isDeviceOwnerApp() == " + dpm.isDeviceOwnerApp(packageName) + " isAdminActive() == " + dpm.isAdminActive(componentName));
        }
        if (dpm.isLockTaskPermitted(packageName)) {
            Log.i(TAG, "isLockTaskPermitted == true");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        dpm.setLockTaskFeatures(componentName, LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK);
                    } else {
                        dpm.setLockTaskFeatures(componentName, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "setLockTaskFeatures() failed - is the app a Device Owner?", e);
                Toast.makeText(this, R.string.set_lock_features_failed, Toast.LENGTH_LONG).show();
            }
            startLockTask();
        } else {
            Log.i(TAG, "isLockTaskPermitted == false");
            requestDeviceAdmin();
        }
    }

    private void requestDeviceAdmin() {
        Toast.makeText(this, R.string.enable_device_admin, Toast.LENGTH_LONG).show();
        provisionOwner();
    }

    private void disableKioskMode() {
        Log.i(TAG, "disableKioskMode()");
        stopLockTask();
    }

    private void provisionOwner() {
        DevicePolicyManager manager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName componentName = DeviceAdminReceiver.getComponentName(this);

        if (!manager.isAdminActive(componentName)) {
            Log.i(TAG, "Device admin is not active");
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
            return;
        }

        if (manager.isDeviceOwnerApp(getPackageName())) {
            Toast.makeText(this, R.string.app_is_device_admin, Toast.LENGTH_LONG).show();
            Log.i(TAG, "App is device owner");
        } else {
            Toast.makeText(this, R.string.app_is_not_device_admin, Toast.LENGTH_LONG).show();
            Log.i(TAG, "App is NOT device owner");
        }
    }

}