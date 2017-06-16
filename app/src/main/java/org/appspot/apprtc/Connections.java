package org.appspot.apprtc;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by deanwild on 14/06/2017.
 */
class Connections extends ArrayList<Connection> {

    public void onStop() {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.onStop();
        }
    }

    public void onStart() {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.onStart();
        }
    }

    public void changeCaptureFormat(int width, int height, int framerate) {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.changeCaptureFormat(width, height, framerate);
        }
    }

    public void setAudioEnabled(boolean micEnabled) {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.setAudioEnabled(micEnabled);
        }
    }

    public void connectToRooms() {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.connectToRoom();
        }
    }

    public void disconnect() {
        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            next.disconnect();
        }
    }

    public boolean areAllDisconnected() {

        Iterator<Connection> it = iterator();
        while (it.hasNext()) {
            Connection next = it.next();
            if (!next.isDisconnected()) {
                return false;
            }
        }

        return true;
    }
}
