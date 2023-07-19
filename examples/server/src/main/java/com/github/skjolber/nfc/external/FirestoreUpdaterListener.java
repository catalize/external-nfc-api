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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

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
        String participant_id = "";
        // Get data first of the participants who hold this Wristband
        db.collection("wristbands")
                .whereEqualTo("sn", id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                //participant_id = document.getData
                                //Log.d(TAG,  " => " + document.getData().);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

        Map<String, Object> kiosk = new HashMap<>();
        kiosk.put("sn", id);
        kiosk.put("participant_id", participant_id);

        db.collection("states").document("jti-conference-" + kioskId)
                .set(kiosk, SetOptions.merge())
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

    class Wristband {
        public String id;
        public String participant_id;
        public String sn;
        public String url;
        public Timestamp timestamp;

        public Wristband(String id, String sn, String url, String participant_id)
        {
            this.id = id;
            this.sn = sn;
            this.url = url;
            this.participant_id =  participant_id;
        }
    }
}
