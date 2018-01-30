package com.example.userverify;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String[] sms_phone_state = {Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE};
    private static final int RC_SMS_PHONE_STATE_PER = 101;
    private AppCompatEditText contactNumberField;
    private AppCompatEditText textMessageField;
    private AppCompatTextView primarySimLbl;
    private AppCompatTextView secondarySimLbl;
    private List<CharSequence> displayName;
    private Map<CharSequence, Integer> smsSubId;

    private BroadcastReceiver sentStatusReceiver, deliveredStatusReceiver;
    private AppCompatTextView progressStatusLbl;
    private RelativeLayout progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        this.secondarySimLbl = findViewById(R.id.secondary_sim_lbl);
        this.primarySimLbl = findViewById(R.id.primary_sim_lbl);
        this.textMessageField = findViewById(R.id.text_message_field);
        this.contactNumberField = findViewById(R.id.contact_number_field);
        primarySimLbl.setOnClickListener(this);
        secondarySimLbl.setOnClickListener(this);
        this.progressIndicator = findViewById(R.id.progress_indicator);
        this.progressStatusLbl = findViewById(R.id.progress_status_lbl);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        checkPermissions();
    }

    private boolean hasPermissions() {
        return EasyPermissions.hasPermissions(this, sms_phone_state);
    }

    @AfterPermissionGranted(value = RC_SMS_PHONE_STATE_PER)
    public void checkPermissions() {
        if (hasPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                doTask();
            }
        } else {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(MainActivity.this, RC_SMS_PHONE_STATE_PER, sms_phone_state)
                        .setRationale("Please provide us the permission")
                        .setPositiveButtonText("OK")
                        .setNegativeButtonText("Cancel")
                        .setTheme(android.R.style.Theme_Material_Dialog_Alert)
                        .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void doTask() {
        SubscriptionManager subManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subManager == null) return;

        List<SubscriptionInfo> subInfoList = subManager.getActiveSubscriptionInfoList();

        Log.d("Test", "Current list = " + subInfoList);
        displayName = new ArrayList<>();
        smsSubId = new HashMap<>();
        for (int i = 0; i < subInfoList.size(); i++) {
            SubscriptionInfo subscriptionInfo = subInfoList.get(i);
            int subID = subscriptionInfo.getSubscriptionId();
            CharSequence carrierName = subscriptionInfo.getCarrierName();
            displayName.add(carrierName);
            smsSubId.put(carrierName, subID);
        }

        if (displayName.size() == 1) {
            primarySimLbl.setText(displayName.get(0));
            secondarySimLbl.setVisibility(View.GONE);
        } else if (displayName.size() > 1){
            secondarySimLbl.setVisibility(View.VISIBLE);
            primarySimLbl.setText(displayName.get(0));
            secondarySimLbl.setText(displayName.get(1));
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            doTask();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        } else {
            requestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (hasPermissions()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    doTask();
                }
            } else {
                requestPermissions();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        String num = contactNumberField.getText().toString();
        String msg = textMessageField.getText().toString();
        if (TextUtils.isEmpty(num) || TextUtils.isEmpty(msg)) {
            Toast.makeText(this, "Please enter mobile number and message.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (view.getId()) {
            case R.id.primary_sim_lbl:
                sendSMS(num, msg, smsSubId.get(displayName.get(0)));
                break;
            case R.id.secondary_sim_lbl:
                sendSMS(num, msg, smsSubId.get(displayName.get(1)));
                break;
            default:
                break;
        }
    }

    private void sendSMS(String num, String textMsg, int subId) {
        SmsManager sms;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            sms = SmsManager.getSmsManagerForSubscriptionId(subId);
            Log.i(TAG, "SMS subscription ID: " + sms.getSubscriptionId());
        } else {
            sms = SmsManager.getDefault();
        }
        // if message length is too long messages are divided
        List<String> messages = sms.divideMessage(textMsg);
        for (String msg : messages) {
            PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);
            sms.sendTextMessage(num, null, msg, sentIntent, deliveredIntent);
            progressIndicator.setVisibility(View.VISIBLE);
            progressStatusLbl.setText("SMS sending..");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sentStatusReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String s = "Unknown Error";
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        s = "Sent Successfully..";
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        s = "Generic Failure Error";
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        s = "Error : No Service Available";
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        s = "Error : Null PDU";
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        s = "Error : Radio is off";
                        break;
                    default:
                        break;
                }
                progressStatusLbl.setText(s);
            }
        };
        deliveredStatusReceiver=new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String s = "Message Not Delivered";
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        s = "Delivered Successfully...";
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                progressStatusLbl.setText(s);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setVisibility(View.GONE);
                    }
                }, 1000);
            }
        };
        registerReceiver(sentStatusReceiver, new IntentFilter("SMS_SENT"));
        registerReceiver(deliveredStatusReceiver, new IntentFilter("SMS_DELIVERED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(sentStatusReceiver);
        unregisterReceiver(deliveredStatusReceiver);
    }
}