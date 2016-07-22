package com.tomerrosenfeld.tweaksforgo.Activities;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.vending.billing.IInAppBillingService;
import com.tomerrosenfeld.tweaksforgo.Prefs;
import com.tomerrosenfeld.tweaksforgo.R;
import com.tomerrosenfeld.tweaksforgo.SecretConstants;
import com.tomerrosenfeld.tweaksforgo.Services.MainService;
import com.tomerrosenfeld.tweaksforgo.SettingsFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Prefs prefs;
    private IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            setUpDonateButton();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (!prefs.getBoolean(Prefs.setup, false)) {
            //Start setup
            startActivity(new Intent(this, TeamPicker.class));
            finish();
        } else {
            //Actual oncreate
            applyTheme();
            setContentView(R.layout.activity_main);
            getFragmentManager().beginTransaction()
                    .replace(R.id.preferences_holder, new SettingsFragment())
                    .commit();
            startService(new Intent(getApplicationContext(), MainService.class));
            //Set up IAP
            Intent billingServiceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            billingServiceIntent.setPackage("com.android.vending");
            bindService(billingServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        }
    }

    private void setUpDonateButton() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        int color = typedValue.data;
        findViewById(R.id.donate).setEnabled(true);
        findViewById(R.id.donate).setBackgroundColor(color);
        findViewById(R.id.donate).setOnClickListener(this);
    }


    private void applyTheme() {
        try {
            setTheme(prefs.getInt(Prefs.theme, prefs.getInt(Prefs.theme, R.style.AppTheme)));
        } catch (Exception e) {
            e.printStackTrace();
            startActivity(new Intent(this, TeamPicker.class));
            finish();
        }
    }

    private boolean hasUsageAccess() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = getPackageManager();
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void askForPermission(Activity activity, DialogInterface.OnClickListener onClickListener, boolean app, String permissionName) {
        new AlertDialog.Builder(activity)
                .setTitle("A permission is required")
                .setMessage("This " + (app ? "app " : "feature ") + "requires a special permission to " + permissionName + ", click grant to allow it now")
                .setPositiveButton("Grant permission", onClickListener)
                .show();
    }

    private void handleUsageAccessPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (!hasUsageAccess()) {
                    askForPermission(this, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            startActivity(intent);
                        }
                    }, true, "access usage");
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                askForPermission(this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                    }
                }, true, "access usage");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_change_team:
                startActivity(new Intent(getApplicationContext(), TeamPicker.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleUsageAccessPermission();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.donate) {
            try {
                Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                        SecretConstants.getPropertyValue(getApplicationContext(), "IAPID"), "inapp", SecretConstants.getPropertyValue(getApplicationContext(), "googleIAPCode"));
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                assert pendingIntent != null;
                startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
            } catch (IntentSender.SendIntentException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(MainActivity.class.getSimpleName(), "Purchase");
    }
}
