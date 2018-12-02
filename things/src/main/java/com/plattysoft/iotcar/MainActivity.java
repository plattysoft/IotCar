package com.plattysoft.iotcar;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.leinardi.android.things.driver.hcsr04.Hcsr04;
import com.leinardi.android.things.driver.hcsr04.Hcsr04SensorDriver;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity implements CommandListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String PROX_1_TRIGGER_PIN = "GPIO6_IO13";
    private static final String PROX_1_ECHO_PIN = "GPIO6_IO12";

    private static final String LINE_DETECTOR_LEFT_PIN = "GPIO2_IO03";
    private static final String LINE_DETECTOR_RIGHT_PIN = "GPIO1_IO10";

    public static final int MIN_AVOIDING_THRESSHOLD = 7;

    private L298N mMotorController;
    private ApiServer mApiServer;

    private Hcsr04 mProximitySensor;

    private SensorManager mSensorManager;
    private CarMode mCarMode = CarMode.SELF_DRIVING;
    private ReadingThread mReadingThread;

    private boolean mFinishing = false;
    private int mUnderThresshold = 0;
    private int mOverThresshold = 0;
    private CarMode mPreviousCarMode;
    private Random mRandom = new Random();
    private Gpio mLineDetectorLeft;
    private Gpio mLineDetectorRight;

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
        try {
            mProximitySensor = new Hcsr04(PROX_1_TRIGGER_PIN, PROX_1_ECHO_PIN);
            mReadingThread = new ReadingThread();
            mReadingThread.start();
        } catch (IOException e) {
            // couldn't configure the device...
            e.printStackTrace();
        }
        // Initialize the line detection
        try {
            PeripheralManager peripheralManager = PeripheralManager.getInstance();
            mLineDetectorLeft = peripheralManager.openGpio(LINE_DETECTOR_LEFT_PIN);
            mLineDetectorLeft.setDirection(Gpio.DIRECTION_IN);
            mLineDetectorLeft.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mLineDetectorLeft.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    try {
                        Log.e("Sensor changed", gpio.getName()+" (Left): "+gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            mLineDetectorRight = peripheralManager.openGpio(LINE_DETECTOR_RIGHT_PIN);
            mLineDetectorRight.setDirection(Gpio.DIRECTION_IN);
            mLineDetectorRight.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mLineDetectorRight.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    try {
                        Log.e("Sensor changed", gpio.getName()+" (Right): "+gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
        } catch (IOException e) {
            // couldn't configure the device...
            e.printStackTrace();
        }

        // Web server to read the API calls
        mApiServer = new ApiServer(this);
    }

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
        if (mProximitySensor != null) {
            mProximitySensor.close();
        }
        mFinishing = true;
    }

    @Override
    public void onCommandReceived(IotCarCommand enumCommand) throws IOException {
        if (mCarMode == CarMode.AVOIDING) {
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

    private void onDistanceRead(float value) {
        Log.i(TAG, String.format(Locale.getDefault(), "sensor changed: [%f]", value));
        // TODO: Use some filtering or average, at the moment is stopping too much with false positives
        // Maybe 3 consecutive readings under 9. Each read takes less than 10ms
        // Same for exiting, 3 consecutive reads over 14
        if (value < MIN_AVOIDING_THRESSHOLD) {
            mUnderThresshold++;
        }
        else {
            mUnderThresshold = 0;
        }
        if (mUnderThresshold >= 3 && mCarMode != CarMode.AVOIDING) {
            try {
                mPreviousCarMode = mCarMode;
                mCarMode = CarMode.AVOIDING;
                mMotorController.setMode(MotorMode.BACKWARD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (value > 14) {
            mOverThresshold++;
        }
        else {
            mOverThresshold = 0;
        }
        if (mCarMode == CarMode.AVOIDING && mOverThresshold >= 3) {
            try {
                mMotorController.setMode(MotorMode.STOP);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCarMode = mPreviousCarMode;
            if (mCarMode != CarMode.REMOTE_CONTROLLED) {
                try {
                    spinRandomlyAndMove();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void spinRandomlyAndMove() throws IOException, InterruptedException {
        if (mRandom.nextBoolean()) {
            mMotorController.setMode(MotorMode.SPIN_LEFT);
        }
        else {
            mMotorController.setMode(MotorMode.SPIN_RIGHT);
        }
        long turningTime = 200 + mRandom.nextInt(300);
        Thread.sleep(turningTime);
        mMotorController.setMode(MotorMode.FORWARD);
    }


    private class ReadingThread extends Thread {
        @Override
        public void run() {
            while (!mFinishing) {
                onDistanceRead(mProximitySensor.readDistance());
            }
        }
    }
}
