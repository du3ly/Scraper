package com.example.duely.scraper;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.text.Text;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    TextView mNameTextView;
    TextView mStockName;
    TextView mTodayClose;
    TextView mPrevClose;
    TextView mOpen;
    TextView mLow;
    TextView mHigh;
    EditText inputTicker;

    String tickerName;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mNameTextView = (TextView) findViewById(R.id.mNameTextView);
        mStockName = (TextView) findViewById(R.id.mStockName);
        mTodayClose = (TextView) findViewById(R.id.mTodayClose);
        mPrevClose = (TextView) findViewById(R.id.mPrevClose);
        mOpen = (TextView) findViewById(R.id.mOpen);
        mLow = (TextView) findViewById(R.id.mLow);
        mHigh = (TextView) findViewById(R.id.mHigh);
        inputTicker = (EditText) findViewById(R.id.editTextTicker);

        findViewById(R.id.buttonSignOut).setOnClickListener(this);
        findViewById(R.id.buttonSearch).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                String urlYQL = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22GOOGL%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Top left corner: display the email address of the user
                    mNameTextView.setText(getString(R.string.emailpassword_status_fmt, user.getEmail()));

                    // Always query the GOOGL ticker when the user signs in
                    new getStockData().execute(urlYQL);

                } else {
                    // User is signed out, take them to the Home screen
                    startActivity(new Intent(HomeActivity.this, MainActivity.class));
                }

            }
        };
    }

    // [START on_start_add_listener]
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    // [END on_start_add_listener]

    // [START on_stop_remove_listener]
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    // [END on_stop_remove_listener]

    private void signOut() {
        mAuth.signOut();
    }

    // Handles the operation of opening up an URL connection, then retrieve the JSON data
    protected class getStockData extends AsyncTask<String, String, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {

            URLConnection connection = null;
            BufferedReader bufferedReader = null;
            try {
                URL url = new URL(params[0]);
                connection = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }

                return new JSONObject(stringBuffer.toString());
            } catch (Exception ex) {
                Log.e("App", "getStockData", ex);
                return null;
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            if (response != null) {
                try {
                    Log.e("App", "Success: " + response.getJSONObject("query").getString("results"));

                    String change = response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("Change");
                    String PrevClose = response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("PreviousClose");
                    Float todayValue = Float.parseFloat(change) + Float.parseFloat(PrevClose);
                    Double todayValue_2d = Math.round(todayValue*100.0)/100.0;

                    mStockName.setText(response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("symbol"));
                    mTodayClose.setText(todayValue_2d.toString());
                    mPrevClose.setText(response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("PreviousClose"));
                    mOpen.setText(response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("Open"));
                    mLow.setText(response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("DaysLow"));
                    mHigh.setText(response.getJSONObject("query").getJSONObject("results").getJSONObject("quote").getString("DaysHigh"));
                } catch (JSONException ex) {
                    Log.e("App", "Failure", ex);
                } catch (NumberFormatException ex) {
                    Log.e("App", "NumberFormatException", ex);
                    Toast.makeText(HomeActivity.this,"Invalid Ticker",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (i == R.id.buttonSignOut) {
            signOut();
        }

        /** This handles the stock ticker search function. It will grab the text that the user inputs into the EditText, then it will
         * be appended and concatenated to the Yahoo URL. Afterwards, the Yahoo API will take care of retrieving information for that ticker. **/
        if (i == R.id.buttonSearch) {
            tickerName = inputTicker.getText().toString().toUpperCase();
            new getStockData().execute("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22" + tickerName + "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
        }
    }
}