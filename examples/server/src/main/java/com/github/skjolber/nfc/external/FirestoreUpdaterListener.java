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
import com.google.type.Date;

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

            String tagId = intent.getStringExtra("id");
            String kioskId = intent.getStringExtra("kioskId");
            if(tagId != null && !tagId.isEmpty() && !tagId.equals("null"))
                updateRealtimeData(tagId, kioskId);
            else
                ClearTheState(kioskId);
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

    public void updateRealtimeData(final String id, final String kioskId)
    {
        Log.d(TAG, "Let's Update");
        if (id != null && !id.isEmpty() && !id.equals("null"))
        {
            Log.d(TAG, "Let's Insert");
            db.collection("wristbands")
                .whereEqualTo("sn", id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                String participant_id = document.getString("participant_id");
                                Log.i(TAG,  "participant_id => " + participant_id);

                                // Update the state
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
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
        }
    }

    public void ClearTheState(String kioskId)
    {
        Log.d(TAG, "Let's Clear");
        // Update the state
        Map<String, Object> kiosk = new HashMap<>();
        kiosk.put("sn", "");
        kiosk.put("participant_id", "");

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

        public Wristband(String id, String sn, String url, String participant_id)
        {
            this.id = id;
            this.sn = sn;
            this.url = url;
            this.participant_id =  participant_id;
        }
    }
}
