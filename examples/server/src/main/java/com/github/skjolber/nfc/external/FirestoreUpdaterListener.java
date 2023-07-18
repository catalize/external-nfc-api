package com.github.skjolber.nfc.external;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.skjolber.nfc.NfcReader;
import com.github.skjolber.nfc.NfcService;
import com.github.skjolber.nfc.service.AbstractService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class FirestoreUpdaterListener extends Service {

    private FirebaseFirestore db;
    public static final String TAG = "FirestoreUpdaterListener";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public BroadcastReceiver FirestoreReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Tag is scanned --" + action);
            updateRealtimeData(intent.getStringExtra("id"), intent.getStringExtra("kioskId"));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        this.db = FirebaseFirestore.getInstance();
        Log.i(TAG, "Hiyaa I am starting!");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FirestoreService.ACTION_UPDATE_DATA);

        registerReceiver(FirestoreReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(FirestoreReceiver);
    }

    public void updateRealtimeData(String id, String kioskId)
    {
        Map<String, Object> kiosk = new HashMap<>();
        kiosk.put(kioskId, id);

        db.collection("states").document("jti-conference")
                .set(kiosk)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Written Successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }
}
