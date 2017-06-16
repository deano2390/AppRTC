/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;

import org.appspot.apprtc.AppRTCAudioManager.AudioDevice;
import org.appspot.apprtc.AppRTCAudioManager.AudioManagerEvents;
import org.appspot.apprtc.PeerConnectionClient.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionClient.PeerConnectionParameters;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoRenderer;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends Activity implements
        CallFragment.OnCallEvents {
    private static final String TAG = "CallRTCClient";

    // Fix for devices running old Android versions not finding the libraries.
    // https://bugs.chromium.org/p/webrtc/issues/detail?id=6751
    static {
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("boringssl.cr");
            System.loadLibrary("protobuf_lite.cr");
        } catch (UnsatisfiedLinkError e) {
            Logging.w(TAG, "Failed to load native dependencies: ", e);
        }
    }

    public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
    public static final String EXTRA_USERID = "org.appspot.apprtc.USERID";
    public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
    public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
    public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
            "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
    public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
    public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
            "org.appspot.apprtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
    public static final String EXTRA_ENABLE_LEVEL_CONTROL = "org.appspot.apprtc.ENABLE_LEVEL_CONTROL";
    public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
    public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
    public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
            "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
    public static final String EXTRA_USE_VALUES_FROM_INTENT =
            "org.appspot.apprtc.USE_VALUES_FROM_INTENT";
    public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
    public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
    public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
    public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
    public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
    public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
    public static final String EXTRA_ID = "org.appspot.apprtc.ID";

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;

    Connections connectionList = new Connections();

    boolean callConnected;

    public class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }

    private final ProxyRenderer locProxyRenderer = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer1 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer2 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer3 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer4 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer5 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer6 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer7 = new ProxyRenderer();
    private final ProxyRenderer remoteProxyRenderer8 = new ProxyRenderer();

    private SurfaceViewRenderer localRenderer, remoteRenderer1, remoteRenderer2, remoteRenderer3, remoteRenderer4, remoteRenderer5, remoteRenderer6, remoteRenderer7, remoteRenderer8;

    private AppRTCAudioManager audioManager = null;
    private EglBase rootEglBase;
    private VideoFileRenderer videoFileRenderer;
    private Toast logToast;
    private boolean commandLineRun;
    private int runTimeMs;
    private boolean activityRunning;

    private PeerConnectionParameters peerConnectionParameters;

    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled = false;
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds;

    // Controls
    private CallFragment callFragment;
    private HudFragment hudFragment;
    private CpuMonitor cpuMonitor;

    ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnectionEstablished() {
            callConnected();
        }

        @Override
        public void onChannelClose() {
            checkIfCallIsDead();
        }

        @Override
        public void onChannelError() {
            checkIfCallIsDead();
        }

        @Override
        public void onIceDisconnected() {
            checkIfCallIsDead();
        }

        @Override
        public void onPeerConnectionError() {
            checkIfCallIsDead();
        }

        void checkIfCallIsDead() {
            if (connectionList.areAllDisconnected()) {
                disconnect();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_call);

        // Create UI controls.
        localRenderer = (SurfaceViewRenderer) findViewById(R.id.box_local);
        remoteRenderer1 = (SurfaceViewRenderer) findViewById(R.id.box1);
        remoteRenderer2 = (SurfaceViewRenderer) findViewById(R.id.box2);
        remoteRenderer3 = (SurfaceViewRenderer) findViewById(R.id.box3);
        remoteRenderer4 = (SurfaceViewRenderer) findViewById(R.id.box4);
        remoteRenderer5 = (SurfaceViewRenderer) findViewById(R.id.box5);
        remoteRenderer6 = (SurfaceViewRenderer) findViewById(R.id.box6);
        remoteRenderer7 = (SurfaceViewRenderer) findViewById(R.id.box7);
        remoteRenderer8 = (SurfaceViewRenderer) findViewById(R.id.box8);

        callFragment = new CallFragment();
        hudFragment = new HudFragment();

        // Show/hide call control fragment on view click.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCallControlFragmentVisibility();
            }
        };

    /*    // Swap feeds on pip view click.
        pipRenderer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSwappedFeeds(!isSwappedFeeds);
            }
        });

        fullscreenRenderer.setOnClickListener(listener);*/
        //remoteRenderers.add(remoteProxyRenderer1);
        //remoteRenderers.add(remoteProxyRenderer2);
        //remoteRenderers.add(remoteProxyRenderer3);

        final Intent intent = getIntent();

        // Create video renderers.
        rootEglBase = EglBase.create();

        localRenderer.init(rootEglBase.getEglBaseContext(), null);

        remoteRenderer1.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer2.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer3.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer4.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer5.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer6.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer7.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer8.init(rootEglBase.getEglBaseContext(), null);


        localRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);

        remoteRenderer1.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer2.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer3.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer4.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer5.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer6.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer7.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer8.setScalingType(ScalingType.SCALE_ASPECT_FIT);

        localRenderer.setEnableHardwareScaler(false);
        remoteRenderer1.setEnableHardwareScaler(false);
        remoteRenderer2.setEnableHardwareScaler(false);
        remoteRenderer3.setEnableHardwareScaler(false);
        remoteRenderer4.setEnableHardwareScaler(false);
        remoteRenderer5.setEnableHardwareScaler(false);
        remoteRenderer6.setEnableHardwareScaler(false);
        remoteRenderer7.setEnableHardwareScaler(false);
        remoteRenderer8.setEnableHardwareScaler(false);


        //pipRenderer.init(rootEglBase.getEglBaseContext(), null);

//        pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        //String saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);

        // When saveRemoteVideoToFile is set we save the video from the remote to a file.
       /* if (saveRemoteVideoToFile != null) {
            int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
            int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
            try {
                videoFileRenderer = new VideoFileRenderer(
                        saveRemoteVideoToFile, videoOutWidth, videoOutHeight, rootEglBase.getEglBaseContext());
                remoteRenderers.add(videoFileRenderer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open video file for output: " + saveRemoteVideoToFile, e);
            }
        }*/
      /*  fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true *//* enabled *//*);
        fullscreenRenderer.setEnableHardwareScaler(true *//* enabled *//*);*/
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        //setSwappedFeeds(true /* isSwappedFeeds */);


        locProxyRenderer.setTarget(localRenderer);
        remoteProxyRenderer1.setTarget(remoteRenderer1);
        remoteProxyRenderer2.setTarget(remoteRenderer2);
        remoteProxyRenderer3.setTarget(remoteRenderer3);
        remoteProxyRenderer4.setTarget(remoteRenderer4);
        remoteProxyRenderer5.setTarget(remoteRenderer5);
        remoteProxyRenderer6.setTarget(remoteRenderer6);
        remoteProxyRenderer7.setTarget(remoteRenderer7);
        remoteProxyRenderer8.setTarget(remoteRenderer8);

        localRenderer.setMirror(true);
        remoteRenderer1.setMirror(false);
        remoteRenderer2.setMirror(false);
        remoteRenderer3.setMirror(false);
        remoteRenderer4.setMirror(false);
        remoteRenderer5.setMirror(false);
        remoteRenderer6.setMirror(false);
        remoteRenderer7.setMirror(false);
        remoteRenderer8.setMirror(false);


        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        Uri roomUri = intent.getData();
        if (roomUri == null) {
            logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Didn't get any URL in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Get Intent parameters.
        String roomID = intent.getStringExtra(EXTRA_ROOMID);
        Log.d(TAG, "Room ID: " + roomID);
        if (roomID == null || roomID.length() == 0) {
            logAndToast(getString(R.string.missing_url));
            Log.e(TAG, "Incorrect roomID in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        int userID = intent.getIntExtra(EXTRA_USERID, 0);

        boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

        int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
        int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);

        screencaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            videoWidth = displayMetrics.widthPixels;
            videoHeight = displayMetrics.heightPixels;
        }
        DataChannelParameters dataChannelParameters = null;
        if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
            dataChannelParameters = new DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
                    intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
        }
        peerConnectionParameters =
                new PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true), loopback,
                        tracing, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                        intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
                        intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
                        intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(EXTRA_ENABLE_LEVEL_CONTROL, false), dataChannelParameters);
        commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
        runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

        Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'");


        String room1 = null;
        String room2 = null;
        String room3 = null;
        String room4 = null;
        String room5 = null;
        String room6 = null;
        String room7 = null;
        String room8 = null;


        switch (userID) {
            case 1:
                room1 = roomID + "12";
                room2 = roomID + "13";
                room3 = roomID + "14";
                room4 = roomID + "15";
                room5 = roomID + "16";
                room6 = roomID + "17";
                room7 = roomID + "18";
                room8 = roomID + "19";
                break;
            case 2:
                room1 = roomID + "12";
                room2 = roomID + "23";
                room3 = roomID + "24";
                room4 = roomID + "25";
                room5 = roomID + "26";
                room6 = roomID + "27";
                room7 = roomID + "28";
                room8 = roomID + "29";
                break;
            case 3:
                room1 = roomID + "13";
                room2 = roomID + "23";
                room3 = roomID + "34";
                room4 = roomID + "35";
                room5 = roomID + "36";
                room6 = roomID + "37";
                room7 = roomID + "38";
                room8 = roomID + "39";
                break;
            case 4:
                room1 = roomID + "14";
                room2 = roomID + "24";
                room3 = roomID + "34";
                room4 = roomID + "45";
                room5 = roomID + "46";
                room6 = roomID + "47";
                room7 = roomID + "48";
                room8 = roomID + "49";
                break;
            case 5:
                room1 = roomID + "15";
                room2 = roomID + "25";
                room3 = roomID + "35";
                room4 = roomID + "45";
                room5 = roomID + "56";
                room6 = roomID + "57";
                room7 = roomID + "58";
                room8 = roomID + "59";
                break;
            case 6:
                room1 = roomID + "16";
                room2 = roomID + "26";
                room3 = roomID + "36";
                room4 = roomID + "46";
                room5 = roomID + "56";
                room6 = roomID + "67";
                room7 = roomID + "68";
                room8 = roomID + "69";
                break;

            case 7:
                room1 = roomID + "17";
                room2 = roomID + "27";
                room3 = roomID + "37";
                room4 = roomID + "47";
                room5 = roomID + "57";
                room6 = roomID + "67";
                room7 = roomID + "78";
                room8 = roomID + "79";
                break;

            case 8:
                room1 = roomID + "18";
                room2 = roomID + "28";
                room3 = roomID + "38";
                room4 = roomID + "48";
                room5 = roomID + "58";
                room6 = roomID + "68";
                room7 = roomID + "78";
                room8 = roomID + "89";
                break;

            case 9:
                room1 = roomID + "19";
                room2 = roomID + "29";
                room3 = roomID + "39";
                room4 = roomID + "49";
                room5 = roomID + "59";
                room6 = roomID + "69";
                room7 = roomID + "79";
                room8 = roomID + "89";
                break;
        }


        Connection connection1 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room1, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer1, rootEglBase);
        Connection connection2 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room2, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer2, rootEglBase);
        Connection connection3 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room3, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer3, rootEglBase);
        Connection connection4 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room4, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer4, rootEglBase);
        Connection connection5 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room5, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer5, rootEglBase);

        Connection connection6 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room6, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer6, rootEglBase);
        Connection connection7 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room7, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer7, rootEglBase);
        Connection connection8 = new Connection(this, peerConnectionParameters, loopback, screencaptureEnabled, room8, roomUri, connectionListener, locProxyRenderer, remoteProxyRenderer8, rootEglBase);

        connectionList.add(connection1);
        connectionList.add(connection2);
        connectionList.add(connection3);
        connectionList.add(connection4);
        connectionList.add(connection5);
        connectionList.add(connection6);
        connectionList.add(connection7);
        connectionList.add(connection8);

        // Create CPU monitor
        cpuMonitor = new CpuMonitor(this);
        hudFragment.setCpuMonitor(cpuMonitor);

        // Send intent arguments to fragments.
        callFragment.setArguments(intent.getExtras());
        hudFragment.setArguments(intent.getExtras());
        // Activate call and HUD fragments and onStart the call.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.add(R.id.hud_fragment_container, hudFragment);
        ft.commit();

        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }

        if (screencaptureEnabled) {
            startScreenCapture();
        } else {
            startCall();
        }
    }

    @TargetApi(17)
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;
        startCall();
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
    }

    private boolean captureToTexture() {
        return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    @TargetApi(21)
    private VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError("User revoked permission to capture the screen.");
            }
        });
    }

    // Activity interfaces
    @Override
    public void onStop() {
        super.onStop();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.

        connectionList.onStop();
        cpuMonitor.pause();
    }

    @Override
    public void onStart() {
        super.onStart();
        activityRunning = true;

        connectionList.onStart();

        cpuMonitor.resume();
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        rootEglBase.release();
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        /*if (peerConnectionClient1 != null) {
            peerConnectionClient1.switchCamera();
        }*/
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        //fullscreenRenderer.setScalingType(scalingType);
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        connectionList.changeCaptureFormat(width, height, framerate);
    }

    @Override
    public boolean onToggleMic() {
        micEnabled = !micEnabled;
        connectionList.setAudioEnabled(micEnabled);
        return micEnabled;
    }

    // Helper functions.
    private void toggleCallControlFragmentVisibility() {
        if (!callConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void startCall() {

        callStartedTimeMs = System.currentTimeMillis();

        connectionList.connectToRooms();

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AudioDevice audioDevice, Set<AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        /*if (peerConnectionClient1 == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }*/
        // Enable statistics callback.

        //peerConnectionClient1.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        //  setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AudioDevice device, final Set<AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;

        remoteProxyRenderer1.setTarget(null);
        remoteProxyRenderer2.setTarget(null);
        remoteProxyRenderer3.setTarget(null);

        locProxyRenderer.setTarget(null);

        connectionList.disconnect();

        if (localRenderer != null) {
            localRenderer.release();
            localRenderer = null;
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            videoFileRenderer = null;
        }
        if (remoteRenderer1 != null) {
            remoteRenderer1.release();
            remoteRenderer1 = null;
        }

        if (remoteRenderer2 != null) {
            remoteRenderer2.release();
            remoteRenderer2 = null;
        }

        if (remoteRenderer3 != null) {
            remoteRenderer3.release();
            remoteRenderer3 = null;
        }


        PeerConnectionClient.closeMediaSources();
        PeerConnectionClient.closeFactory();

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (callConnected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    public void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    VideoCapturer videoCapturer = null;

    synchronized VideoCapturer createVideoCapturer() {


        if (videoCapturer != null) {
            return videoCapturer;
        }

        String videoFileAsCamera = getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
        if (videoFileAsCamera != null) {
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                reportError("Failed to open video file for emulated camera");
                return null;
            }
        } else if (screencaptureEnabled) {
            return createScreenCapturer();
        } else if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(getString(R.string.camera2_texture_only_error));
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }

        return videoCapturer;
    }


}
