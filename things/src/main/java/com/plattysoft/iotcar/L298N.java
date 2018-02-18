package com.plattysoft.iotcar;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Created by Raul Portales on 16/12/17.
 */

public class L298N implements AutoCloseable {
    Motor mLeftMotor;
    Motor mRightMotor;

    private L298N(String gpioEngine1Forward, String gpioEngine1Backward, String gpioEngine2Forward, String gpioEngine2Backward) throws IOException {
        mLeftMotor = new Motor(gpioEngine1Forward, gpioEngine1Backward);
        mRightMotor = new Motor(gpioEngine2Forward, gpioEngine2Backward);
    }

    public static L298N open(String gpioEngine1Forward, String gpioEngine1Backward, String gpioEngine2Forward, String gpioEngine2Backward) throws IOException {
        return new L298N(gpioEngine1Forward, gpioEngine1Backward, gpioEngine2Forward, gpioEngine2Backward);
    }

    @Override
    public void close() throws Exception {
        mLeftMotor.close();
        mRightMotor.close();
    }

    public void setMode(MotorMode mode) throws IOException {
        switch(mode) {
            case FORWARD:
                mLeftMotor.forward();
                mRightMotor.forward();
                break;
            case BACKWARD:
                mLeftMotor.backward();
                mRightMotor.backward();
                break;
            case SPIN_RIGHT:
                mLeftMotor.backward();
                mRightMotor.forward();
                break;
            case SPIN_LEFT:
                mLeftMotor.forward();
                mRightMotor.backward();
                break;
            case TURN_RIGHT:
                mLeftMotor.forward();
                mRightMotor.stop();
                break;
            case TURN_LEFT:
                mLeftMotor.stop();
                mRightMotor.forward();
                break;
            case STOP:
                mLeftMotor.stop();
                mRightMotor.stop();
                break;
        }
    }
}
