package org.webrtc;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by deanwild on 16/06/2017.
 */

public class CustomPeerConnection {

    private final List<MediaStream> localStreams;
    private final long nativePeerConnection;
    private final long nativeObserver;
    private List<RtpSender> senders;
    private List<RtpReceiver> receivers;

    CustomPeerConnection(long nativePeerConnection, long nativeObserver) {
        this.nativePeerConnection = nativePeerConnection;
        this.nativeObserver = nativeObserver;
        this.localStreams = new LinkedList();
        this.senders = new LinkedList();
        this.receivers = new LinkedList();
    }

    public native SessionDescription getLocalDescription();

    public native SessionDescription getRemoteDescription();

    public native DataChannel createDataChannel(String var1, DataChannel.Init var2);

    public native void createOffer(SdpObserver var1, MediaConstraints var2);

    public native void createAnswer(SdpObserver var1, MediaConstraints var2);

    public native void setLocalDescription(SdpObserver var1, SessionDescription var2);

    public native void setRemoteDescription(SdpObserver var1, SessionDescription var2);

    public boolean setConfiguration(org.webrtc.PeerConnection.RTCConfiguration config) {
        return this.nativeSetConfiguration(config, this.nativeObserver);
    }

    public boolean addIceCandidate(IceCandidate candidate) {
        return this.nativeAddIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
    }

    public boolean removeIceCandidates(IceCandidate[] candidates) {
        return this.nativeRemoveIceCandidates(candidates);
    }

    public boolean addStream(MediaStream stream) {
        boolean ret = this.nativeAddLocalStream(stream.nativeStream);
        if (!ret) {
            return false;
        } else {
            this.localStreams.add(stream);
            return true;
        }
    }

    public void removeStream(MediaStream stream) {
        this.nativeRemoveLocalStream(stream.nativeStream);
        this.localStreams.remove(stream);
    }

    public RtpSender createSender(String kind, String stream_id) {
        RtpSender new_sender = this.nativeCreateSender(kind, stream_id);
        if (new_sender != null) {
            this.senders.add(new_sender);
        }

        return new_sender;
    }

    public List<RtpSender> getSenders() {
        Iterator var1 = this.senders.iterator();

        while (var1.hasNext()) {
            RtpSender sender = (RtpSender) var1.next();
            sender.dispose();
        }

        this.senders = this.nativeGetSenders();
        return Collections.unmodifiableList(this.senders);
    }

    public List<RtpReceiver> getReceivers() {
        Iterator var1 = this.receivers.iterator();

        while (var1.hasNext()) {
            RtpReceiver receiver = (RtpReceiver) var1.next();
            receiver.dispose();
        }

        this.receivers = this.nativeGetReceivers();
        return Collections.unmodifiableList(this.receivers);
    }

    public boolean getStats(StatsObserver observer, MediaStreamTrack track) {
        return this.nativeGetStats(observer, track == null ? 0L : track.nativeTrack);
    }

    public boolean startRtcEventLog(int file_descriptor, int max_size_bytes) {
        return this.nativeStartRtcEventLog(file_descriptor, max_size_bytes);
    }

    public void stopRtcEventLog() {
        this.nativeStopRtcEventLog();
    }

    public native org.webrtc.PeerConnection.SignalingState signalingState();

    public native org.webrtc.PeerConnection.IceConnectionState iceConnectionState();

    public native org.webrtc.PeerConnection.IceGatheringState iceGatheringState();

    public native void close();

    public void dispose() {
        this.close();
        Iterator var1 = this.localStreams.iterator();

        while (var1.hasNext()) {
            MediaStream receiver = (MediaStream) var1.next();
            this.nativeRemoveLocalStream(receiver.nativeStream);
            receiver.dispose();
        }

        this.localStreams.clear();
        var1 = this.senders.iterator();

        while (var1.hasNext()) {
            RtpSender receiver1 = (RtpSender) var1.next();
            receiver1.dispose();
        }

        this.senders.clear();
        var1 = this.receivers.iterator();

        while (var1.hasNext()) {
            RtpReceiver receiver2 = (RtpReceiver) var1.next();
            receiver2.dispose();
        }

        this.receivers.clear();
        freePeerConnection(this.nativePeerConnection);
        freeObserver(this.nativeObserver);
    }

    private static native void freePeerConnection(long var0);

    private static native void freeObserver(long var0);

    public native boolean nativeSetConfiguration(org.webrtc.PeerConnection.RTCConfiguration var1, long var2);

    private native boolean nativeAddIceCandidate(String var1, int var2, String var3);

    private native boolean nativeRemoveIceCandidates(IceCandidate[] var1);

    private native boolean nativeAddLocalStream(long var1);

    private native void nativeRemoveLocalStream(long var1);

    private native boolean nativeGetStats(StatsObserver var1, long var2);

    private native RtpSender nativeCreateSender(String var1, String var2);

    private native List<RtpSender> nativeGetSenders();

    private native List<RtpReceiver> nativeGetReceivers();

    private native boolean nativeStartRtcEventLog(int var1, int var2);

    private native void nativeStopRtcEventLog();

    static {
        System.loadLibrary("jingle_peerconnection_so");
    }

    public static class RTCConfiguration {
        public org.webrtc.PeerConnection.IceTransportsType iceTransportsType;
        public List<org.webrtc.PeerConnection.IceServer> iceServers;
        public org.webrtc.PeerConnection.BundlePolicy bundlePolicy;
        public org.webrtc.PeerConnection.RtcpMuxPolicy rtcpMuxPolicy;
        public org.webrtc.PeerConnection.TcpCandidatePolicy tcpCandidatePolicy;
        public org.webrtc.PeerConnection.CandidateNetworkPolicy candidateNetworkPolicy;
        public int audioJitterBufferMaxPackets;
        public boolean audioJitterBufferFastAccelerate;
        public int iceConnectionReceivingTimeout;
        public int iceBackupCandidatePairPingInterval;
        public org.webrtc.PeerConnection.KeyType keyType;
        public org.webrtc.PeerConnection.ContinualGatheringPolicy continualGatheringPolicy;
        public int iceCandidatePoolSize;
        public boolean pruneTurnPorts;
        public boolean presumeWritableWhenFullyRelayed;
        public Integer iceCheckMinInterval;
        public boolean disableIPv6OnWifi;

        public RTCConfiguration(List<org.webrtc.PeerConnection.IceServer> iceServers) {
            this.iceTransportsType = org.webrtc.PeerConnection.IceTransportsType.ALL;
            this.bundlePolicy = org.webrtc.PeerConnection.BundlePolicy.BALANCED;
            this.rtcpMuxPolicy = org.webrtc.PeerConnection.RtcpMuxPolicy.REQUIRE;
            this.tcpCandidatePolicy = org.webrtc.PeerConnection.TcpCandidatePolicy.ENABLED;
            org.webrtc.PeerConnection.CandidateNetworkPolicy var10001 = this.candidateNetworkPolicy;
            this.candidateNetworkPolicy = org.webrtc.PeerConnection.CandidateNetworkPolicy.ALL;
            this.iceServers = iceServers;
            this.audioJitterBufferMaxPackets = 50;
            this.audioJitterBufferFastAccelerate = false;
            this.iceConnectionReceivingTimeout = -1;
            this.iceBackupCandidatePairPingInterval = -1;
            this.keyType = org.webrtc.PeerConnection.KeyType.ECDSA;
            this.continualGatheringPolicy = org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_ONCE;
            this.iceCandidatePoolSize = 0;
            this.pruneTurnPorts = false;
            this.presumeWritableWhenFullyRelayed = false;
            this.iceCheckMinInterval = null;
            this.disableIPv6OnWifi = false;
        }
    }

    public static enum ContinualGatheringPolicy {
        GATHER_ONCE,
        GATHER_CONTINUALLY;

        private ContinualGatheringPolicy() {
        }
    }

    public static enum KeyType {
        RSA,
        ECDSA;

        private KeyType() {
        }
    }

    public static enum CandidateNetworkPolicy {
        ALL,
        LOW_COST;

        private CandidateNetworkPolicy() {
        }
    }

    public static enum TcpCandidatePolicy {
        ENABLED,
        DISABLED;

        private TcpCandidatePolicy() {
        }
    }

    public static enum RtcpMuxPolicy {
        NEGOTIATE,
        REQUIRE;

        private RtcpMuxPolicy() {
        }
    }

    public static enum BundlePolicy {
        BALANCED,
        MAXBUNDLE,
        MAXCOMPAT;

        private BundlePolicy() {
        }
    }

    public static enum IceTransportsType {
        NONE,
        RELAY,
        NOHOST,
        ALL;

        private IceTransportsType() {
        }
    }

    public static class IceServer {
        public final String uri;
        public final String username;
        public final String password;
        public final org.webrtc.PeerConnection.TlsCertPolicy tlsCertPolicy;

        public IceServer(String uri) {
            this(uri, "", "");
        }

        public IceServer(String uri, String username, String password) {
            this(uri, username, password, org.webrtc.PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE);
        }

        public IceServer(String uri, String username, String password, org.webrtc.PeerConnection.TlsCertPolicy tlsCertPolicy) {
            this.uri = uri;
            this.username = username;
            this.password = password;
            this.tlsCertPolicy = tlsCertPolicy;
        }

        public String toString() {
            return this.uri + " [" + this.username + ":" + this.password + "] [" + this.tlsCertPolicy + "]";
        }
    }

    public interface Observer {
        void onSignalingChange(org.webrtc.PeerConnection.SignalingState var1);

        void onIceConnectionChange(org.webrtc.PeerConnection.IceConnectionState var1);

        void onIceConnectionReceivingChange(boolean var1);

        void onIceGatheringChange(org.webrtc.PeerConnection.IceGatheringState var1);

        void onIceCandidate(IceCandidate var1);

        void onIceCandidatesRemoved(IceCandidate[] var1);

        void onAddStream(MediaStream var1);

        void onRemoveStream(MediaStream var1);

        void onDataChannel(DataChannel var1);

        void onRenegotiationNeeded();

        void onAddTrack(RtpReceiver var1, MediaStream[] var2);
    }

    public static enum SignalingState {
        STABLE,
        HAVE_LOCAL_OFFER,
        HAVE_LOCAL_PRANSWER,
        HAVE_REMOTE_OFFER,
        HAVE_REMOTE_PRANSWER,
        CLOSED;

        private SignalingState() {
        }
    }

    public static enum TlsCertPolicy {
        TLS_CERT_POLICY_SECURE,
        TLS_CERT_POLICY_INSECURE_NO_CHECK;

        private TlsCertPolicy() {
        }
    }

    public static enum IceConnectionState {
        NEW,
        CHECKING,
        CONNECTED,
        COMPLETED,
        FAILED,
        DISCONNECTED,
        CLOSED;

        private IceConnectionState() {
        }
    }

    public static enum IceGatheringState {
        NEW,
        GATHERING,
        COMPLETE;

        private IceGatheringState() {
        }
    }
}

