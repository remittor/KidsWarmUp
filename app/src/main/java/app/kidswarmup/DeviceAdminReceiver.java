package app.kidswarmup;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/*
To activate this app as Device Owner Admin:
1. BACKUP YOUR DEVICE
2. Execute following commands:
adb shell
su
cat > /data/system/device_owner.xml <<"DEVICE_OWNER"
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<device-owner package="app.kidswarmup" name="KidsWarmUp" />
DEVICE_OWNER
chown system:system /data/system/device_owner.xml
reboot

Or following command (Android 7.1.2++):
adb shell
su
cat > /data/system/device_policy_2.xml <<"DEVICE_OWNER2"
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<root>
    <device-owner package="app.kidswarmup" name="ClickCounter" component="app.kidswarmup/app.kidswarmup.DeviceAdminReceiver" userRestrictionsMigrated="true" />
</root>
DEVICE_OWNER2
chown system:system /data/system/device_policy_2.xml
reboot

(с) https://stackoverflow.com/questions/21183328/how-to-make-my-app-a-device-owner/27909315#answer-26839548
 */

public class DeviceAdminReceiver extends BroadcastReceiver {
    /**
     * @param context The context of the application.
     * @return The component name of this component in the given context.
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case "android.app.action.DEVICE_ADMIN_ENABLED":
                return;
            case "android.app.action.PROFILE_PROVISIONING_COMPLETE":
                return;
            case "android.intent.action.BOOT_COMPLETED":
                return;
            default:
                break;
        }
    }
}
