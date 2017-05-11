package com.pmenauge.rvi.rvidroidcar;

import com.googlecode.jsonrpc4j.JsonRpcParam;

/**
 * Created by pmenauge on 20/04/17.
 */

public interface VehicleRemoteControl {

    public static final String ACTION_LOCK = "lock";
    public static final String ACTION_UNLOCK = "unlock";
    public static final String [] ALL_ACTIONS = {ACTION_LOCK, ACTION_UNLOCK};

    public static final String LOCK_ID_FRONT_LEFT = "r1_lt";
    public static final String LOCK_ID_FRONT_RIGHT = "r1_rt";
    public static final String LOCK_ID_BACK_LEFT = "r2_lt";
    public static final String LOCK_ID_BACK_RIGHT = "r2_rt";
    public static final String LOCK_ID_TRUNK = "trunk";
    public static final String LOCK_ID_HOOD = "hood";
    public static final String LOCK_ID_DOORS = "doors";
    public static final String [] ALL_LOCK_IDS = {LOCK_ID_FRONT_LEFT, LOCK_ID_FRONT_RIGHT, LOCK_ID_BACK_LEFT, LOCK_ID_BACK_RIGHT, LOCK_ID_TRUNK, LOCK_ID_HOOD, LOCK_ID_DOORS};

    public void lock(@JsonRpcParam(value="action") String action, @JsonRpcParam(value="locks") String [] locks);
}
