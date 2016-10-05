package com.example.batteryinformation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.widget.Toast;
import android.content.IntentFilter;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        String action = intent.getAction();
        boolean isCharging = false;
        float batteryPct = 0;
        if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
            isCharging = true;
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.example.batteryinformation");
            launchIntent.putExtra("some_data", "value");
            context.startActivity(launchIntent);
        }
        else if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            isCharging = false;
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float) scale;
        }

        Toast.makeText(context,  "percent : "+ batteryPct +"\nCharging : "+ isCharging + "\nusbCharge : " +usbCharge + "\nacCharge : " +acCharge , Toast.LENGTH_SHORT).show();
    }

}



//        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                status == BatteryManager.BATTERY_STATUS_FULL;
//

//