/******************************************************************************
 *
 * The MIT License (MIT)
 *
 * Copyright (c) Crossbar.io Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 ******************************************************************************/

package autobahn.demo.com.autobahndemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import de.tavendo.autobahn.WebSocket;


public class EchoClientActivity extends Activity {

    static final String TAG = "EchoClientActivity";
    private static final String PREFS_NAME = "AutobahnAndroidEcho";

    static EditText mHostname;
    static EditText mPort;
    static TextView mStatusline;
    static Button mStart;
    static EditText mMessage;
    static Button mSendMessage;
    static TextView mTvEchoMessage;
    static ListView mListView;
    private ArrayList<String> messages =  new ArrayList<String>();

    private SharedPreferences mSettings;
    private String PING ="HI";
    private Handler mHandler = new Handler();
    private BroadcastReceiver mReceiver;
    private WebSocket mConnection ;

    private void alert(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    private void loadPrefs() {

        mHostname.setText(mSettings.getString("hostname", "52.26.113.63:4510/api/values"));
        mPort.setText(mSettings.getString("port", "4510"));
    }

    private void savePrefs() {

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("hostname", mHostname.getText().toString());
        editor.putString("port", mPort.getText().toString());
        editor.commit();
    }

    private void setButtonConnect() {
        mHostname.setEnabled(true);
        mPort.setEnabled(true);
        mStart.setText("Connect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mConnection = ApplicationController.getWebSocketInstance();
                ((ApplicationController)getApplication()).connect();
                mStatusline.setText("Status: Connecting to " + ApplicationController.WS_URI + " ..");

                setButtonDisconnect();
            }
        });
    }

    private void setButtonDisconnect() {
        mHostname.setEnabled(false);
        mPort.setEnabled(false);
        mStart.setText("Disconnect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG,"setButtonDisconnect");
                mConnection.disconnect();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHostname = (EditText) findViewById(R.id.hostname);
        mPort = (EditText) findViewById(R.id.port);
        mStatusline = (TextView) findViewById(R.id.statusline);
        mStart = (Button) findViewById(R.id.start);
        mMessage = (EditText) findViewById(R.id.msg);
        mSendMessage = (Button) findViewById(R.id.sendMsg);
        mTvEchoMessage = (TextView) findViewById(R.id.tv_echo_reply);
        mListView = (ListView) findViewById(R.id.lv_messages);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        setButtonConnect();
        mSendMessage.setEnabled(false);
        mMessage.setEnabled(false);

        mSendMessage.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mConnection.sendTextMessage(mMessage.getText().toString());

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isConnectionLost = intent.getBooleanExtra("IsConnectionLost",false);
                boolean isConnectionOpen = intent.getBooleanExtra("IsConnectionOpen",false);
                boolean isMessageReceived = intent.getBooleanExtra("IsMessageReceived",false);
                if (isConnectionOpen){
                    mStatusline.setText("Status: Connected to " + ApplicationController.WS_URI);
                    savePrefs();
                    mSendMessage.setEnabled(true);
                    mMessage.setEnabled(true);
                    mConnection.sendPingMessage(PING.getBytes());
                }
                if (isConnectionLost){
                    String reason = intent.getStringExtra("Reason");
                    int code = intent.getIntExtra("CODE",0);

                    alert("Connection lost.  "+reason);
                    mStatusline.setText("Status: "+getTime()+"  "+code+" "+reason);
                    setButtonConnect();
                }
                if (isMessageReceived)
                {
                    String message = intent.getStringExtra("message");
                    Log.d(TAG,message);

                    mTvEchoMessage.setText(message);
                    messages.add(getTime() +"    "+message);
                    String[] messsagesArray = new String[messages.size()];
                    messsagesArray = messages.toArray(messsagesArray);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(EchoClientActivity.this,
                            android.R.layout.simple_list_item_1, android.R.id.text1, messsagesArray);
//                    ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, messages);

                    mListView.setAdapter(adapter);
                }


            }
        };
        IntentFilter intentFilter= new IntentFilter("MESSAGE");
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection.isConnected()) {
            mConnection.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.quit:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private String getTime(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss aa");
        String currentTime = sdf.format(cal.getTime());
        Log.d("TIME", currentTime);
        return  currentTime;
    }
}
