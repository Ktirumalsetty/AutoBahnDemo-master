package autobahn.demo.com.autobahndemo;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;

import static autobahn.demo.com.autobahndemo.EchoClientActivity.mStatusline;

/**
 * Created by KTirumalsetty on 5/22/2017.
 */

public class ApplicationController extends Application {

    public static final String TAG = "ApplicationController";
    public final static String WS_URI = "ws://52.26.113.63:4510/api/values";
    public static  final long PING_DURATION = 30000L;

    private static ApplicationController sApplicationController;
    public static WebSocket mConnection ;
    final String wsuri = "ws://52.26.113.63:4510/api/values";
    private static final String PING ="HI";
    private Handler mHandler = new Handler();

    public static ApplicationController getInstance() {
        return sApplicationController;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplicationController = this;
    }

    public static WebSocket getWebSocketInstance(){
        if (mConnection == null){
            mConnection = new WebSocketConnection();
        }
        return mConnection;
    }

    public void connect() {

//        final String wsuri = "http://" + mHostname.getText() + ":" + mPort.getText();
        try {

            mConnection.connect(WS_URI, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    sendConnectionOn();
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG,"onTextMessage "+payload);
                    alert("Got echo: " + payload);
//                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                    sendMessage(payload);
                }

                @Override
                public void onClose(int code, String reason) {
                    alert("Connection lost.  "+reason);
                    mStatusline.setText("Status: "+getTime()+"  "+code+" "+reason);
                    sendConnectionLost(reason,code);
                }

                @Override
                public void onPongMessage(byte[] payload) {
                    Log.d(TAG,"onPongMessage "+new String(payload));
                    super.onPongMessage(payload);
//                    alert("Got onPongMessage: " + payload);
                    alert("Got Pong .  "+new String(payload));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mConnection.isConnected())
                                mConnection.sendPingMessage(PING.getBytes());
                            else
                                Log.i(TAG," onPongMessage Connection Lost");
                        }
                    }, PING_DURATION);

                }
            });

        } catch (WebSocketException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void sendConnectionOn() {
        Log.d(TAG, "sendConnectionOn ");
        Intent intent = new Intent("MESSAGE");
        // You can also include some extra data.
        intent.putExtra("IsConnectionOpen", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendConnectionLost(String reason,int code) {
        Log.d(TAG, "sendConnectionLost");
        Intent intent = new Intent("MESSAGE");
        // You can also include some extra data.
        intent.putExtra("IsConnectionLost", true);
        intent.putExtra("Reason", reason);
        intent.putExtra("CODE", code);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private String getTime(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss aa");
        String currentTime = sdf.format(cal.getTime());
        Log.d("TIME", currentTime);
        return  currentTime;
    }

    private void alert(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    private void sendMessage(String message) {
        Log.d(TAG, "sendMessage");
        Intent intent = new Intent("MESSAGE");
        // You can also include some extra data.
        intent.putExtra("IsMessageReceived", true);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
