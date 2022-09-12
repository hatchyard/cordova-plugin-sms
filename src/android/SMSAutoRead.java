package com.ishara11rathnayake.smsautoread;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.Context;
import android.annotation.SuppressLint;
import android.os.Build;
import androidx.annotation.NonNull;

/**
 * This class echoes a string called from JavaScript.
 */
public class SMSAutoRead extends CordovaPlugin {

    BroadcastReceiver smsBroadcastReceiver;
    private static final int REQ_USER_CONSENT = 200;
    private CallbackContext callback = null;
    private CordovaPlugin plugin = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callback = callbackContext;
        this.plugin = this;
        switch (action) {
            case "start":
                return this.start(callbackContext);
            case "startWatching":
                this.startWatching();
                return true;
            case "stop":
                this.stop();
                return true;
        }
        return false;
    }

    private boolean start(CallbackContext callback) {
        if ("start".isEmpty()) {
            smsBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SmsRetriever.SMS_RETRIEVED_ACTION)) {
                        Bundle extras = intent.getExtras();

                        Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                        switch (smsRetrieverStatus.getStatusCode()) {
                            case CommonStatusCodes.SUCCESS:
                                Intent messageIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                                cordova.startActivityForResult(plugin, messageIntent, REQ_USER_CONSENT);
                                break;
                            case CommonStatusCodes.TIMEOUT:
                                break;
                        }
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
            cordova.getActivity().getApplicationContext().registerReceiver(smsBroadcastReceiver, intentFilter);
            return true;
        } else {
            return this.verifyAppSignature(SMSAutoRead.this.cordova.getActivity(), callback);
        }
    }

    private void startWatching() {
        SmsRetrieverClient client = SmsRetriever.getClient(cordova.getActivity().getApplicationContext());
        client.startSmsUserConsent(null);
    }

    private void stop() {
        cordova.getActivity().getApplicationContext().unregisterReceiver(smsBroadcastReceiver);
        this.smsBroadcastReceiver = null;
    }

    private String getOtpFromMessage(String message) {
        String mask = "[0-9]+";
        Pattern otpPattern = Pattern.compile(mask);
        Matcher matcher = otpPattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(0);
        }

        return "Not Found";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQ_USER_CONSENT) {
            if ((resultCode == Activity.RESULT_OK) && (intent != null)) {
                String message = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                String otpMsg = getOtpFromMessage(message);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, otpMsg);
                callback.sendPluginResult(pluginResult);
            } else {
                this.startWatching();
            }
        }
    }

    // Signature integrity check
    private static List<String> getSignatures(@NonNull PackageManager pm, @NonNull String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo == null
                        || packageInfo.signingInfo == null) {
                    return null;
                }
                if (packageInfo.signingInfo.hasMultipleSigners()) {
                    return signatureDigest(packageInfo.signingInfo.getApkContentsSigners());
                } else {
                    return signatureDigest(packageInfo.signingInfo.getSigningCertificateHistory());
                }
            } else {
                @SuppressLint("PackageManagerGetSignatures")
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                if (packageInfo == null
                        || packageInfo.signatures == null
                        || packageInfo.signatures.length == 0
                        || packageInfo.signatures[0] == null) {
                    return null;
                }
                return signatureDigest(packageInfo.signatures);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static String signatureDigest(Signature sig) {
        byte[] signature = sig.toByteArray();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] digest = md.digest(signature);
            return Base64.encodeToString(digest, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static List<String> signatureDigest(Signature[] sigList) {
        List<String> signaturesList = new ArrayList<>();
        for (Signature signature : sigList) {
            if (signature != null) {
                signaturesList.add(signatureDigest(signature));
            }
        }
        return signaturesList;
    }

    private boolean verifyAppSignature(Context context, CallbackContext callback) {
        //you should load approvedSignatures from a secure place not plain text
        List<String> approvedSignatures = new ArrayList<>();
        approvedSignatures.add(context.getString(context.getResources().getIdentifier( "sig_val", "string", context.getPackageName())));
        System.out.println("SIGNATURE sotred ------- "+approvedSignatures);
        List<String> currentSignatures = getSignatures(context.getPackageManager(), context.getPackageName());
        if (currentSignatures != null && currentSignatures.size() > 0) {
            //first checking if no unapproved signatures exist
            for (String signatureHex : currentSignatures) {
                System.out.println("SIGNATURE --------------------- "+signatureHex);
                if (!signatureHex.isEmpty() && !approvedSignatures.contains(signatureHex.trim())) {
                    System.out.println("NOOOOOOOOOOOOOSSSSS");
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, false);
                    callback.sendPluginResult(pluginResult);
                    return false;

                }
            }
            //now checking if any of approved signatures exist
            for (String signatureHex : currentSignatures) {
                if (!signatureHex.isEmpty() && approvedSignatures.contains(signatureHex.trim())) {
                    System.out.println("YESSSSSSSSSSSSSSSSSSSSSSSSSSS");
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, true);
                    callback.sendPluginResult(pluginResult);
                    return true;
                }
            }
        }
        System.out.println("NOOOOOOOSSSSSSSS WEND");
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, true);
        callback.sendPluginResult(pluginResult);
        return false;
    }
}