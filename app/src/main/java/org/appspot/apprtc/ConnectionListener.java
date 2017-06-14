package org.appspot.apprtc;

/**
 * Created by deanwild on 14/06/2017.
 */

public interface ConnectionListener {
    void onConnectionEstablished();
    void onChannelClose();
    void onChannelError();
    void onIceDisconnected();
    void onPeerConnectionError();
}
