package com.pmenauge.rvi.rvidroidlib;

import java.util.ArrayList;

/**
 * Created by pmenauge on 27/03/17.
 */

public interface RviManagerChangeListener {
    public void rviConnectionStateChanged(RviManager source, int newConnectionState);

    public void rviServicesListChanged(RviManager source, ArrayList newServicesList);
}
