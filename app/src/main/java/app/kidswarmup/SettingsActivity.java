package app.kidswarmup;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.DataOutputStream;
import java.util.Locale;

import app.kidswarmup.MainActivity;

public class SettingsActivity extends AppCompatActivity implements
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    View.OnClickListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private String appName;            // KidsWarmUp
    private String pkgName;            // app.kidswarmup
    private ComponentName devAdmName;  // app.kidswarmup/app.kidswarmup.DeviceAdminReceiver
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        appName = getEnglishString(R.string.app_name);
        pkgName = getApplicationContext().getPackageName();
        devAdmName = DeviceAdminReceiver.getComponentName(this);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_main, rootKey);
        }
    }

    public static class DeviceAdminFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_devadm, rootKey);
        }
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.app_close_button)
            onAppCloseButtonClick();
        if (id == R.id.devadm_install_button)
            onDevAdmInstallButtonClick();
        if (id == R.id.devadm_policies_button)
            onDevAdmUpdatePoliciesButtonClick();
        if (id == R.id.devadm_activate_button)
            onDevAdmActivateButtonClick();
        if (id == R.id.devadm_delete_button)
            onDevAdmDeleteButtonClick(false);
    }

    private void onAppCloseButtonClick() {
        Toast.makeText(this, "app_close_button", Toast.LENGTH_LONG).show();
        //disableKioskMode();
    }

    private boolean remountDataPart(boolean writeable, boolean silent) {
        try {
            String[] cmd = new String[] { "mount -o rw,remount /data" };
            doSuCmd(cmd);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed on remount /data as RW !!! Is root available?", e);
            if (!silent)
                Toast.makeText(this, "ERROR: Failed to remount /data partition! Required root permissions!", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void onDevAdmInstallButtonClick() {
        if (!remountDataPart(true, false))
            return;
        int x = 0;
        String fn = "";
        try {
            fn = "/data/system/device_owner.xml";
            String[] cmd = new String[]{
                    "cat > " + fn + " <<\"DEVICE_OWNER1\"",
                    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                    "<root>",
                    "    <device-owner",
                    "        package=\"" + pkgName + "\"",
                    "        name=\"" + appName + "\"",
                    "    />",
                    "</root>",
                    "DEVICE_OWNER1",
                    "chown system:system " + fn
            };
            doSuCmd(cmd);
            x++;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
        }
        try {
            fn = "/data/system/device_owner_2.xml";
            String[] cmd = new String[]{
                    "cat > " + fn + " <<\"DEVICE_OWNER2\"",
                    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                    "<root>",
                    "    <device-owner",
                    "        package=\"" + pkgName + "\"",
                    "        name=\"" + appName + "\"",
                    "        component=\"" + devAdmName.flattenToString() + "\"",
                    "        userRestrictionsMigrated=\"true\"",
                    "        canAccessDeviceIds=\"true\"",
                    "    />",
                    "    <device-owner-context userId=\"0\" />",
                    "</root>",
                    "DEVICE_OWNER2",
                    "chown system:system " + fn
            };
            doSuCmd(cmd);
            x++;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
        }
        try {
            fn = "/data/system/device_policy_2.xml";
            String[] cmd = new String[]{
                    "cat > " + fn + " <<\"DEVICE_POL2\"",
                    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                    "<root>",
                    "    <device-owner",
                    "        package=\"" + pkgName + "\"",
                    "        name=\"" + appName + "\"",
                    "        component=\"" + devAdmName.flattenToString() + "\"",
                    "        userRestrictionsMigrated=\"true\"",
                    "    />",
                    "</root>",
                    "DEVICE_POL2",
                    "chown system:system " + fn
            };
            doSuCmd(cmd);
            x++;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
        }
        if (x >= 3)
            Toast.makeText(this, "Device-Owner configs installed to /data/system !", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "ERROR: Device-Owner configs NOT installed! " + x, Toast.LENGTH_LONG).show();
    }

    private void onDevAdmUpdatePoliciesButtonClick() {
        if (!remountDataPart(true, false))
            return;
        String fn = "";
        try {
            fn = "/data/system/device_policies.xml";
            String[] cmd = new String[]{
                    "rm -f " + fn,
                    "cat > " + fn + " <<\"DEVICE_POLICIES\"",
                    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                    "<policies setup-complete=\"true\">",
                    "    <admin name=\"" + devAdmName.flattenToString() + "\"",
                    "        <policies flags=\"1023\" />",
                    "        <strong-auth-unlock-timeout value=\"0\" />",
                    "        <test-only-admin value=\"true\" />",
                    "        <user-restrictions no_add_managed_profile=\"true\" />",
                    "        <default-enabled-user-restrictions>",
                    "            <restriction value=\"no_add_managed_profile\" />",
                    "        </default-enabled-user-restrictions>",
                    "        <cross-profile-calendar-packages />",
                    "    </admin>",
                    "    <lock-task-features value=\"16\" />",
                    "</policies>",
                    "DEVICE_POLICIES",
                    "chown system:system " + fn
            };
            doSuCmd(cmd);
            Toast.makeText(this, "File " + fn + " replaced!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
            Toast.makeText(this, "ERROR: File " + fn + " NOT replaced!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean onDevAdmActivateButtonClick() {
        Log.i(TAG, "DevADM: isDeviceOwnerApp = " + dpm.isDeviceOwnerApp(pkgName) + ", isAdminActive = " + dpm.isAdminActive(devAdmName));
        if (dpm.isDeviceOwnerApp(pkgName) && dpm.isAdminActive(devAdmName)) {
            Log.i(TAG, "Calling setLockTaskPackages()");
            String[] packages = new String[]{pkgName};
            try {
                dpm.setLockTaskPackages(devAdmName, packages);
            } catch (SecurityException e) {
                Log.e(TAG, "setLockTaskPackages() failed", e);
                Toast.makeText(this, R.string.set_lock_packages_failed, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        Log.i(TAG, "DevAdm: isDeviceOwnerApp = " + dpm.isDeviceOwnerApp(pkgName) + ", isAdminActive = " + dpm.isAdminActive(devAdmName));

        DevicePolicyManager manager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!manager.isAdminActive(devAdmName)) {
            Log.i(TAG, "Device admin is not active");
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, devAdmName);
            startActivityForResult(intent, 0);
            Toast.makeText(this, "Device admin is not active", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!manager.isDeviceOwnerApp(getPackageName())) {
            Toast.makeText(this, R.string.app_is_not_device_admin, Toast.LENGTH_LONG).show();
            Log.i(TAG, "App is NOT device owner");
            return false;
        }
        Toast.makeText(this, R.string.app_is_device_admin, Toast.LENGTH_LONG).show();
        Log.i(TAG, "App is device owner");
        return true;
    }

    private void onDevAdmDeleteButtonClick(boolean silent) {
        if (!remountDataPart(true, false))
            return;
        String[] cmd = new String[]{
                "rm -f /data/system/device_owner.xml",
                "rm -f /data/system/device_owner_2.xml",
                "rm -f /data/system/device_policy_2.xml",
                "rm -f /data/system/device_policies.xml"
        };
        try {
            doSuCmd(cmd);
            if (!silent)
                Toast.makeText(this, "All XML files deleted! Please, REBOOT device!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Can't remove files from /data/system/* !!! Is root available?", e);
            if (!silent)
                Toast.makeText(this, "ERROR: XML files NOT deleted!", Toast.LENGTH_LONG).show();
        }
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

    @NonNull
    protected String getEnglishString(int id) {
        Context ctx = getApplicationContext();
        Configuration configuration = getEnglishConfiguration();
        return ctx.createConfigurationContext(configuration).getResources().getString(id);
    }

    @NonNull
    private Configuration getEnglishConfiguration() {
        Context ctx = getApplicationContext();
        Configuration configuration = new Configuration(ctx.getResources().getConfiguration());
        configuration.setLocale(new Locale("en"));
        return configuration;
    }

}