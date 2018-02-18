package com.plattysoft.iotcar;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Raul Portales on 14/01/18.
 */

class ApiServer extends NanoHTTPD {
    private CommandListener mListener;

    public ApiServer(CommandListener listener) {
        super(8080);
        mListener = listener;
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Handle "command PATH with a POST
        String path = session.getUri();
        Log.e("NanoHttp", path);

        String msg = "\"Unknown command\"";
        if (path.endsWith("command")) {
            msg = "\"Command Received\"";

            Map<String, String> files = new HashMap<String, String>();
            try {
                session.parseBody(files);
                String command = files.get("postData");
                Log.e("NanoHttp", command);
                command = stripQuotes(command);
                IotCarCommand enumCommand = IotCarCommand.valueOf(command);
                Log.e("NanoHttp", enumCommand.toString());
                mListener.onCommandReceived(enumCommand);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }
        }
        return newFixedLengthResponse(msg);
    }

    private String stripQuotes(String command) {
        while (command.startsWith("\"")) {
            command = command.substring(1);
        }
        while (command.endsWith("\"")) {
            command = command.substring(0, command.length()-1);
        }
        return command;
    }
}
