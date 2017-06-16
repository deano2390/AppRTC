package org.appspot.apprtc;

import android.net.Uri;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;

/**
 * Created by deanwild on 14/06/2017.
 */

public class Connection {

    static final String TAG = "CallRTCClient";

    private final AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    private final ConnectionListener connectionListener;
    private final CallActivity activity;
    private final PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private final EglBase rootEglBase;
    private final CallActivity.ProxyRenderer localProxyRenderer;
    private final CallActivity.ProxyRenderer remoteProxyRenderer;
    private AppRTCClient appRtcClient;
    private PeerConnectionClient peerConnectionClient;
    private final boolean screencaptureEnabled;
    private boolean iceConnected;
    private boolean disconnected;

    public Connection(
            CallActivity activity,
            PeerConnectionClient.PeerConnectionParameters peerConnectionParameters,
            boolean loopback,
            boolean screencaptureEnabled,
            String room1,
            Uri roomUri,
            ConnectionListener connectionListener,
            CallActivity.ProxyRenderer localProxyRenderer,
            CallActivity.ProxyRenderer remoteProxyRenderer,
            EglBase rootEglBase) {

        this.activity = activity;
        this.peerConnectionParameters = peerConnectionParameters;
        this.screencaptureEnabled = screencaptureEnabled;
        this.connectionListener = connectionListener;
        this.localProxyRenderer = localProxyRenderer;
        this.remoteProxyRenderer = remoteProxyRenderer;
        this.rootEglBase = rootEglBase;

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
        if (loopback || !DirectRTCClient.IP_PATTERN.matcher(room1).matches()) {
            appRtcClient = new WebSocketRTCClient(signalingEvents);
        } else {
            Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
            appRtcClient = new DirectRTCClient(signalingEvents);
        }

        // Create connection parameters.
        roomConnectionParameters = new AppRTCClient.RoomConnectionParameters(roomUri.toString(), room1, loopback);

        peerConnectionClient = new PeerConnectionClient();

        if (loopback) {
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;
            peerConnectionClient.setPeerConnectionFactoryOptions(options);
        }

        peerConnectionClient.createPeerConnectionFactory(
                activity.getApplicationContext(), peerConnectionParameters, peerConnectionEvents, rootEglBase.getEglBaseContext());
    }

    void runOnUiThread(Runnable runnable){
        if(activity != null && !activity.isFinishing()){
            activity.runOnUiThread(runnable);
        }
    }

    void logAndToast(String message){
        if(activity != null && !activity.isFinishing()){
            activity.logAndToast(message);
        }
    }

    AppRTCClient.SignalingParameters signalingParameters;
    AppRTCClient.SignalingEvents signalingEvents = new AppRTCClient.SignalingEvents() {
        @Override
        public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onConnectedToRoomInternal(params);
                }
            });
        }

        // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
        // All callbacks are invoked from websocket signaling looper thread and
        // are routed to UI thread.
        private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
            //final long delta = System.currentTimeMillis() - callStartedTimeMs;

            signalingParameters = params;
            //logAndToast("Creating peer connection, delay=" + delta + "ms");
            VideoCapturer videoCapturer = null;
            if (peerConnectionParameters.videoCallEnabled) {
                videoCapturer = activity.createVideoCapturer();
            }

            peerConnectionClient.createPeerConnection(
                    localProxyRenderer,
                    remoteProxyRenderer,
                    videoCapturer,
                    signalingParameters);

            if (signalingParameters.initiator) {
                logAndToast("Creating OFFER...");
                // Create offer. Offer SDP will be sent to answering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createOffer();
            } else {
                if (params.offerSdp != null) {
                    peerConnectionClient.setRemoteDescription(params.offerSdp);
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
                if (params.iceCandidates != null) {
                    // Add remote ICE candidates from room.
                    for (IceCandidate iceCandidate : params.iceCandidates) {
                        peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                    }
                }
            }
        }

        @Override
        public void onRemoteDescription(final SessionDescription sdp) {
           // final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                        return;
                    }
                    //logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                    peerConnectionClient.setRemoteDescription(sdp);
                    if (!signalingParameters.initiator) {
                        logAndToast("Creating ANSWER...");
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                        peerConnectionClient.createAnswer();
                    }
                }
            });
        }

        @Override
        public void onRemoteIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                        return;
                    }
                    peerConnectionClient.addRemoteIceCandidate(candidate);
                }
            });
        }

        @Override
        public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                        return;
                    }
                    peerConnectionClient.removeRemoteIceCandidates(candidates);
                }
            });
        }

        @Override
        public void onChannelClose() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    logAndToast("Remote end hung up; dropping PeerConnection");

                    disconnect();

                    if(connectionListener != null){
                        connectionListener.onChannelClose();
                    }
                }
            });
        }

        @Override
        public void onChannelError(String description) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    logAndToast("onChannelError; dropping PeerConnection");

                    disconnect();

                    if(connectionListener != null){
                        connectionListener.onChannelError();
                    }
                }
            });
        }
    };

    PeerConnectionClient.PeerConnectionEvents peerConnectionEvents = new PeerConnectionClient.PeerConnectionEvents() {
        // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
        // Send local peer connection SDP and ICE candidates to remote party.
        // All callbacks are invoked from peer connection client looper thread and
        // are routed to UI thread.
        @Override
        public void onLocalDescription(final SessionDescription sdp) {
            //final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appRtcClient != null) {
                        //logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                        if (signalingParameters.initiator) {
                            appRtcClient.sendOfferSdp(sdp);
                        } else {
                            appRtcClient.sendAnswerSdp(sdp);
                        }
                    }
                    if (peerConnectionParameters.videoMaxBitrate > 0) {
                        Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                        peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                    }
                }
            });
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appRtcClient != null) {
                        appRtcClient.sendLocalIceCandidate(candidate);
                    }
                }
            });
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appRtcClient != null) {
                        appRtcClient.sendLocalIceCandidateRemovals(candidates);
                    }
                }
            });
        }

        @Override
        public void onIceConnected() {
            //final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // logAndToast("ICE connected, delay=" + delta + "ms");
                    iceConnected = true;

                    if(connectionListener != null){
                        connectionListener.onConnectionEstablished();
                    }
                }
            });
        }

        @Override
        public void onIceDisconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAndToast("ICE disconnected");
                    iceConnected = false;
                    disconnect();

                    if(connectionListener != null){
                        connectionListener.onIceDisconnected();
                    }
                }
            });
        }

        @Override
        public void onPeerConnectionClosed() {

        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {
          /*  runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isError && iceConnected) {
                        hudFragment.updateEncoderStatistics(reports);
                    }
                }
            });*/
        }

        @Override
        public void onPeerConnectionError(String description) {
            logAndToast("onPeerConnectionError");
            disconnect();

            if(connectionListener != null){
                connectionListener.onPeerConnectionError();
            }
        }
    };

    public void onStop() {
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.stopVideoSource();
        }
    }

    public void onStart() {
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.startVideoSource();
        }
    }

    public void changeCaptureFormat(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    public void setAudioEnabled(boolean micEnabled) {
        if (peerConnectionClient != null) {
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
    }

    public void connectToRoom() {
        // Start room connection.
        appRtcClient.connectToRoom(roomConnectionParameters);
    }

    public void disconnect() {

        disconnected = true;

        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }

        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
    }

    public boolean isDisconnected() {
        return disconnected;
    }
}
