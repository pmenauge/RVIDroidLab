package com.pmenauge.rvi.rvidroidlib;

import android.util.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by pmenauge on 18/04/17.
 */

public class RviObjectMapper extends ObjectMapper {
    private static final String TAG = "RviObjectMapper";

    public RviObjectMapper() {
        super();
    }

    public <T> T readValue(InputStream src, Class<T> valueType)
            throws IOException, JsonParseException, JsonMappingException
    {
        Log.d(TAG, "readValue() isMarkSupported="+src.markSupported());
        // Extract RPC request from HTTP query and call parent
        if (src == null) {
            Log.e(TAG, "readValue() no input stream provided");
            throw new IOException("no input stream provided");
        }
        int nbInputBytes = src.available();
        byte [] readBytes = new byte[nbInputBytes];
        int i = 0;
        Log.d(TAG, "readValue() isMarkSupported="+src.markSupported()+", available="+nbInputBytes);
        for (i = 0 ; i < nbInputBytes; i++) {
            readBytes[i] = (byte) src.read();
            if (readBytes[i] == '\n') {
                if (i > 4) {
                    Log.d(TAG, "readValue() " + readBytes[i-3]+","+ readBytes[i-2]+","+readBytes[i-1]+","+readBytes[i]);
                }
                if (i > 4 && readBytes[i-3]=='\r'
                        && readBytes[i-2]=='\n'
                        && readBytes[i-1]=='\r'
                        && readBytes[i]=='\n') {
                    Log.d(TAG, "readValue() found empty line at position " + i);
                    src.mark(nbInputBytes - i + 1);
                    src.reset();
                    break;
                }
            }
        }

        Log.d(TAG, "readValue() Read utf-8 "+(i+1)+" bytes=\n"+new String(readBytes, 0, i+1, "UTF-8")+"\n");
        Log.d(TAG, "readValue() remaining nb of bytes in stream ="+src.available());
        return super.readValue(src, valueType);
    }

}
