package app.kidswarmup;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.DataOutputStream;

import static android.app.admin.DevicePolicyManager.LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK;
import static android.view.InputDevice.SOURCE_KEYBOARD;
import static android.view.InputDevice.SOURCE_MOUSE;
import static android.view.InputDevice.SOURCE_MOUSE_RELATIVE;
import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.BUTTON_PRIMARY;
import static android.view.MotionEvent.BUTTON_SECONDARY;

public class MainActivity extends Activity implements View.OnGenericMotionListener, View.OnClickListener, View.OnKeyListener {

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
    private int stepsCurrent = 0;
    private int stepsTarget = 0;
    private boolean configured = false;
    private int prevButton = 0;
    private boolean firstStepHalfDone = false;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainFrame = findViewById(R.id.mainFrame);
        mainFrame.setOnGenericMotionListener(this);
        mainFrame.setOnKeyListener(this);
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
            stepsCurrent = savedInstanceState.getInt("stepsCurrent");
            stepsTarget = savedInstanceState.getInt("stepsTarget");
            configured = savedInstanceState.getBoolean("configured");
            prevButton = savedInstanceState.getInt("prevButton");
            firstStepHalfDone = savedInstanceState.getBoolean("firstStepHalfDone");
            if (configured) {
                setControlsVisibility(false);
                enableKioskMode();
                setupUI();
            }
        }
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
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        int source = event.getSource();
        Log.i(TAG, "key = " + keyCode);
        if (source != SOURCE_KEYBOARD)
            return false;
        if (event.getAction() != KeyEvent.ACTION_UP)
            return false;
        if ((prevButton == 0 || prevButton == KeyEvent.KEYCODE_X) && keyCode == KeyEvent.KEYCODE_Z) {
            countStep();
            prevButton = keyCode;
            return false;
        }
        if ((prevButton == 0 || prevButton == KeyEvent.KEYCODE_Z) && keyCode == KeyEvent.KEYCODE_X) {
            countStep();
            prevButton = keyCode;
            return false;
        }
        return false;
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent motionEvent) {
        int source = motionEvent.getSource();
        if (source != SOURCE_MOUSE && source != SOURCE_MOUSE_RELATIVE)
            return false;
        if (motionEvent.getActionMasked() != ACTION_BUTTON_PRESS)
            return false;
        int buttonState = motionEvent.getButtonState();
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
        //PasswordDialog.show(appContext);
        //PasswordDialog.show(mainFrame.getContext());
        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(i);
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