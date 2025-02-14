package com.moutamid.messagesender;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.fxn.stash.Stash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class Constants {
    public static DatabaseReference databaseReference() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("messagesender");
        db.keepSynced(true);
        return db;
    }

    public static void checkApp(Activity activity) {
        String appName = "messagesender";

        new Thread(() -> {
            URL google = null;
            try {
                google = new URL("https://raw.githubusercontent.com/Moutamid/Moutamid/main/apps.txt");
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String input = null;
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                try {
                    if ((input = in != null ? in.readLine() : null) == null) break;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                stringBuffer.append(input);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            String htmlData = stringBuffer.toString();

            try {
                JSONObject myAppObject = new JSONObject(htmlData).getJSONObject(appName);

                boolean value = myAppObject.getBoolean("value");
                String msg = myAppObject.getString("msg");
                Stash.put("key", value);
                if (value) {
                    activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                            .setMessage(msg)
                            .setCancelable(false)
                            .show());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).start();
    }

}
