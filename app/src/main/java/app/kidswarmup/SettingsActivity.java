package app.kidswarmup;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import app.kidswarmup.MainActivity;

public class SettingsActivity extends AppCompatActivity implements
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    View.OnClickListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private String appName;            // KidsWarmUp
    private String pkgName;            // app.kidswarmup
    private ComponentName devAdmName;  // app.kidswarmup/app.kidswarmup.DeviceAdminReceiver
    private boolean prefChanged = false;
    private static String app_version;
    private int menu_depth = 0;
    private CharSequence main_title;
    private CharSequence prev_title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        menu_depth = 1;
        appName = getEnglishString(R.string.app_name);
        pkgName = getApplicationContext().getPackageName();
        devAdmName = DeviceAdminReceiver.getComponentName(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(pkgName, 0);
            app_version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        main_title = getTitle();
        prev_title = main_title;
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
        prev_title = getTitle();
        menu_depth++;
        setTitle(pref.getTitle());
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_main, rootKey);
            int t_num = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            setEditTextType("timeout", t_num);
            setEditTextType("timeout_init", t_num);
            setEditTextType("step_target", t_num);
            setEditTextType("menu_password", t_num);
            androidx.preference.EditTextPreference edit = getPreferenceManager().findPreference("app_version");
            edit.setTitle(app_version);
            edit = getPreferenceManager().findPreference("menu_password");
            edit.setSummaryProvider(new Preference.SummaryProvider() {
                @Override
                public CharSequence provideSummary(Preference preference) {
                    String psw = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("menu_password", "");
                    if (psw.isEmpty())
                        return getResources().getString(R.string.not_set);
                    return new String(new char[psw.length()]).replace('\0', '*');
                }
            });
        }

        public void setEditTextType(String key, int type) {
            androidx.preference.EditTextPreference edit = getPreferenceManager().findPreference(key);
            edit.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(type);
                    editText.setSelectAllOnFocus(true);
                    editText.setSelection(0, editText.getText().toString().length());
                }
            });
        }
    }

    public static class AuxFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_aux, rootKey);
            int t_time = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME;
            setEditTextType("sleep_time_start", t_time);
            setEditTextType("sleep_time_finish", t_time);
        }

        public void setEditTextType(String key, int type) {
            androidx.preference.EditTextPreference edit = getPreferenceManager().findPreference(key);
            edit.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(type);
                    editText.setSelectAllOnFocus(true);
                    editText.setSelection(0, editText.getText().toString().length());
                }
            });
        }
    }

    public static class DeviceAdminFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_devadm, rootKey);
        }
    }

    //@Override
    //protected void onResume() {
    //    super.onResume();
    //}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "change key = " + key);
        if (key.length() > 2)
            prefChanged = true;
    }

    private void menuReturn() {
        menu_depth--;
        if (menu_depth <= 1)
            setTitle(main_title);
        else
            setTitle(prev_title);
    }

    @Override
    public void onBackPressed() {
        setSettingsResult(Activity.RESULT_CANCELED, false);
        super.onBackPressed();
        menuReturn();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setSettingsResult(Activity.RESULT_OK, false);
            if (menu_depth >= 2) {
                super.onBackPressed();
            } else {
                NavUtils.navigateUpFromSameTask(this);
            }
            menuReturn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.app_close_button)
            onAppCloseButtonClick();
        if (id == R.id.devadm_activate_button)
            onDevAdmActivateButtonClick();
        if (id == R.id.devadm_install_button)
            onDevAdmInstallButtonClick();
        if (id == R.id.devadm_policies_button)
            onDevAdmUpdatePoliciesButtonClick();
        if (id == R.id.devadm_reboot_button)
            onDevAdmRebootButtonClick();
        if (id == R.id.devadm_delete_button)
            onDevAdmDeleteButtonClick();
    }

    private void setSettingsResult(int resultCode, boolean app_close) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("pref_changed", prefChanged);
        returnIntent.putExtra("app_close", app_close);
        setResult(resultCode, returnIntent);
    }

    private void onAppCloseButtonClick() {
        setSettingsResult(Activity.RESULT_OK, true);
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "DESTROY settings activity");
        super.onDestroy();
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

    private boolean devAdmDeleteFiles(String flist, boolean silent) {
        if (!remountDataPart(true, silent))
            return false;
        ArrayList<String> cmd = new ArrayList<String>();
        if (flist.contains("device_owner.xml") || flist.contains("dev_owner"))
            cmd.add("rm -f /data/system/device_owner.xml");
        if (flist.contains("device_owner_2.xml") || flist.contains("dev_owner_2"))
            cmd.add("rm -f /data/system/device_owner_2.xml");
        if (flist.contains("device_policy_2.xml") || flist.contains("dev_pol_2"))
            cmd.add("rm -f /data/system/device_policy_2.xml");
        if (flist.contains("device_policies.xml") || flist.contains("dev_pol"))
            cmd.add("rm -f /data/system/device_policies.xml");
        try {
            doSuCmd(cmd.toArray(new String[0]));
        } catch (Exception e) {
            Log.e(TAG, "Can't remove files from /data/system/* !!! Is root available?", e);
            return false;
        }
        return true;
    }

    private boolean devAdmCreateDeviceOwner(boolean silent) {
        if (!remountDataPart(true, silent))
            return false;
        String fn = "/data/system/device_owner.xml";
        String[] cmd = new String[]{
                "cat > " + fn + " <<\"DEVICE_OWNER1\"",
                "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                "<root>",
                "<device-owner package=\"" + pkgName + "\" name=\"" + appName + "\" />",
                "</root>",
                "DEVICE_OWNER1",
                "chown system:system " + fn
        };
        try {
            doSuCmd(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
            return false;
        }
        return true;
    }

    private void onDevAdmInstallButtonClick() {
        if (!remountDataPart(true, false))
            return;
        int x = 0;
        String fn = "";
        try {
            fn = "/data/system/device_owner_2.xml";
            String[] cmd = new String[]{
                    "cat > " + fn + " <<\"DEVICE_OWNER2\"",
                    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
                    "<root>",
                    "<device-owner" +
                    " package=\"" + pkgName + "\"" +
                    " name=\"" + appName + "\"" +
                    " component=\"" + devAdmName.flattenToString() + "\"" +
                    " userRestrictionsMigrated=\"true\"" +
                    " canAccessDeviceIds=\"true\"" +
                    " />",
                    "<device-owner-context userId=\"0\" />",
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
                    "<device-owner" +
                    " package=\"" + pkgName + "\"" +
                    " name=\"" + appName + "\"" +
                    " component=\"" + devAdmName.flattenToString() + "\"" +
                    " userRestrictionsMigrated=\"true\"" +
                    " />",
                    "</root>",
                    "DEVICE_POL2",
                    "chown system:system " + fn
            };
            doSuCmd(cmd);
            x++;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file " + fn, e);
        }
        if (x >= 2)
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
                    //"<admin name=\"com.google.android.gms/com.google.android.gms.mdm.receivers.MdmDeviceAdminReceiver\">",
                    //"<policies flags=\"540\" />",
                    //"<strong-auth-unlock-timeout value=\"0\" />",
                    //"</admin>",
                    "<admin name=\"" + devAdmName.flattenToString() + "\">",
                    "<policies flags=\"1023\" />",
                    "<strong-auth-unlock-timeout value=\"0\" />",
                    "<test-only-admin value=\"true\" />",
                    "<user-restrictions no_add_managed_profile=\"true\" />",
                    "<default-enabled-user-restrictions>",
                    "<restriction value=\"no_add_managed_profile\" />",
                    "</default-enabled-user-restrictions>",
                    "<cross-profile-calendar-packages />",
                    "</admin>",
                    "<password-validity value=\"true\" />",
                    "<lock-task-features value=\"16\" />",
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

    private void onDevAdmActivateButtonClick() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(devAdmName)) {
            Log.i(TAG, "Device admin is not active");
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, devAdmName);
            startActivityForResult(intent, 0);
            return;
        }
        Toast.makeText(this, "Device Admin already activated", Toast.LENGTH_LONG).show();
    }

    private void onDevAdmDeleteButtonClick() {
        if (!remountDataPart(true, false))
            return;
        String flist = "dev_owner + dev_owner_2 + dev_pol_2 + dev_pol";
        boolean x = devAdmDeleteFiles(flist, false);
        if (x)
            Toast.makeText(this, "All XML files deleted!\n" + "Please, REBOOT device!", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "ERROR: XML files NOT deleted!", Toast.LENGTH_LONG).show();
    }

    private void onDevAdmRebootButtonClick() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm.isAdminActive(devAdmName) && dpm.isDeviceOwnerApp(pkgName)) {
            dpm.reboot(devAdmName);
            return;
        }
        try {
            Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot" });
            proc.waitFor();
        } catch (Exception ex) {
            Log.i(TAG, "Could not reboot", ex);
            Toast.makeText(this, "ERROR: Could not reboot", Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, "Device rebooting...", Toast.LENGTH_LONG).show();
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