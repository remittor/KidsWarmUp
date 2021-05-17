package app.kidswarmup;

import android.app.Activity;
import android.content.Context;
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

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
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
        if (id == R.id.devadm_delete_button)
            onDevAdmDeleteButtonClick();
    }

    private void onAppCloseButtonClick() {
        Toast.makeText(this, "app_close_button", Toast.LENGTH_LONG).show();
        //disableKioskMode();
    }

    private void onDevAdmInstallButtonClick() {
        String pkgName = getApplicationContext().getPackageName();   // app.kidswarmup
        String appName = getEnglishString(R.string.app_name);        // KidsWarmUp
        String[] cmd = new String[]{
            "mount -o rw,remount /data",
            "cat > /data/system/device_owner.xml <<\"DEVICE_OWNER1\"",
            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>",
            "<root>",
            "    <device-owner",
            "        package=\"" + pkgName + "\"",
            "        name=\""+ appName + "\"",
            "    />",
            "</root>",
            "DEVICE_OWNER1",
            "chown system:system /data/system/device_owner.xml",
            "cat > /data/system/device_policy_2.xml <<\"DEVICE_POL2\"",
            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\" ?>",
            "<root>",
            "    <device-owner",
            "        package=\"" + pkgName + "\"",
            "        name=\"" + appName + "\"",
            "        component=\"" + pkgName + "/" + pkgName + ".DeviceAdminReceiver\"",
            "        userRestrictionsMigrated=\"true\"",
            "    />",
            "</root>",
            "DEVICE_POL2",
            "chown system:system /data/system/device_policy_2.xml"
        };
        try {
            doSuCmd(cmd);
            Toast.makeText(this, "Device-Owner policies installed!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Can't copy files to /data/system/* !!! Is root available?", e);
        }
    }

    private void onDevAdmDeleteButtonClick() {
        String[] cmd = new String[]{
            "mount -o rw,remount /data",
            "rm -f /data/system/device_owner.xml",
            "rm -f /data/system/device_owner_2.xml",
            "rm -f /data/system/device_policy_2.xml",
            "rm -f /data/system/device_policies.xml"
        };
        try {
            doSuCmd(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Can't remove files from /data/system/* !!! Is root available?", e);
        }
        Toast.makeText(this, "Please, REBOOT device!", Toast.LENGTH_LONG).show();
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