package com.plattysoft.iotcar;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.pio.PeripheralManagerService;
import com.leinardi.android.things.driver.hcsr04.Hcsr04SensorDriver;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements CommandListener, SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String PROX_1_TRIGGER_PIN = "GPIO6_IO13";
    private static final String PROX_1_ECHO_PIN = "GPIO6_IO12";

    private L298N mMotorController;
    private ApiServer mApiServer;

    private Hcsr04SensorDriver mProximitySensorDriver;

    private SensorManager mSensorManager;
    private CarMode mCarMode = CarMode.REMOTE_CONTROLLED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // PINS for the motor controller L298N
        // BCM22, BCM23, BCM24, BCM25
        try {
//            mMotorController = L298N.open("BCM22", "BCM23", "BCM24", "BCM25");
            mMotorController = L298N.open("GPIO2_IO00", "GPIO2_IO05",
                    "GPIO2_IO07", "GPIO6_IO15");
            mMotorController.setMode(MotorMode.STOP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Pins for the proximity sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
        try {
            mProximitySensorDriver = new Hcsr04SensorDriver(PROX_1_TRIGGER_PIN, PROX_1_ECHO_PIN);
            mProximitySensorDriver.registerProximitySensor();
        } catch (IOException e) {
            // couldn't configure the device...
            e.printStackTrace();
        }
        // Web server to read the API calls
        mApiServer = new ApiServer(this);
    }

    private SensorManager.DynamicSensorCallback mDynamicSensorCallback = new SensorManager
            .DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                mSensorManager.registerListener(MainActivity.this,
                        sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    };
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            try {
                mMotorController.setMode(MotorMode.FORWARD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            try {
                mMotorController.setMode(MotorMode.BACKWARD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            try {
                mMotorController.setMode(MotorMode.TURN_LEFT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            try {
                mMotorController.setMode(MotorMode.TURN_RIGHT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            mMotorController.setMode(MotorMode.STOP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mMotorController.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mProximitySensorDriver != null) {
            mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);
            mSensorManager.unregisterListener(this);
            mProximitySensorDriver.unregisterProximitySensor();
            try {
                mProximitySensorDriver.close();
            } catch (IOException e) {
                // error closing sensor
                e.printStackTrace();
            } finally {
                mProximitySensorDriver = null;
            }
        }
    }

    @Override
    public void onCommandReceived(IotCarCommand enumCommand) throws IOException {
        if (mCarMode != CarMode.REMOTE_CONTROLLED) {
            return;
        }
        switch (enumCommand) {
            case LEFT:
                mMotorController.setMode(MotorMode.TURN_LEFT);
                break;
            case RIGHT:
                mMotorController.setMode(MotorMode.TURN_RIGHT);
                break;
            case SPIN_LEFT:
                mMotorController.setMode(MotorMode.SPIN_LEFT);
                break;
            case SPIN_RIGHT:
                mMotorController.setMode(MotorMode.SPIN_RIGHT);
                break;
            case FORWARD:
                mMotorController.setMode(MotorMode.FORWARD);
                break;
            case BACKWARD:
                mMotorController.setMode(MotorMode.BACKWARD);
                break;
            case STOP:
                mMotorController.setMode(MotorMode.STOP);
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, String.format(Locale.getDefault(), "sensor changed: [%f]", sensorEvent.values[0]));

        if (sensorEvent.values[0] < 9) {
            try {
                mCarMode = CarMode.AVOIDING;
                mMotorController.setMode(MotorMode.BACKWARD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mCarMode == CarMode.AVOIDING && sensorEvent.values[0] > 14) {
            try {
                mMotorController.setMode(MotorMode.STOP);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCarMode = CarMode.REMOTE_CONTROLLED;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
