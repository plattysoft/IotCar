package com.plattysoft.iotcar;

import java.io.IOException;

/**
 * Created by Raul Portales on 14/01/18.
 */

interface CommandListener {
    void onCommandReceived(IotCarCommand enumCommand) throws IOException;
}
