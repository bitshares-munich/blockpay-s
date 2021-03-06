package de.bitsharesmunich.blockpos;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketListener;

import java.io.IOException;

import de.bitsharesmunich.graphenej.api.BaseGrapheneHandler;
import de.bitsharesmunich.utils.BlockpayApplication;

/**
 * Created by nelson on 11/17/16.
 */
public class WebsocketWorkerThread extends Thread {
    private final String TAG = this.getClass().getName();
    private WebSocket mWebSocket;
    private BaseGrapheneHandler mListener;

    public WebsocketWorkerThread(WebSocketListener webSocketListener){
        this(webSocketListener, 0);
    }

    public WebsocketWorkerThread(WebSocketListener webSocketListener, int socketIndex){
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
        try {
            mWebSocket = factory.createSocket(BlockpayApplication.urlsSocketConnection[socketIndex]);
            mWebSocket.addListener(webSocketListener);
            mListener = (BaseGrapheneHandler) webSocketListener;
        } catch (IOException e) {
            Log.e(TAG, "IOException. Msg: "+e.getMessage());
        } catch(NullPointerException e){
            Log.e(TAG, "NullPointerException at WebsocketWorkerThreas. Msg: "+e.getMessage());
        }
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        try {
            mWebSocket.connect();
        } catch (WebSocketException e) {
            Log.e(TAG,"WebSocketException. Msg: "+e.getMessage());
            try {
                mListener.onError(mWebSocket, e);
            } catch (Exception e1) {
                Log.e(TAG,"Calling onError caused an exception. Msg: "+e.getMessage());
            }
        }
    }

    public boolean isConnected(){
        return mWebSocket.isOpen();
    }

    public void disconnect(){
        mWebSocket.disconnect();
    }
}