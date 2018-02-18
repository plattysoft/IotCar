package com.plattysoft.iotcar;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public class MainActivity extends Activity {

    public static final String IOT_CAR_URL = "http://192.168.42.115:8080";
    private IoTCarRemote mApiEndpoint;

    public interface IoTCarRemote {
        @POST("command")
        Call<Void> sendCommand(@Body IotCarCommand command);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        mApiEndpoint = new Retrofit.Builder()
                .baseUrl(IOT_CAR_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(IoTCarRemote.class);
    }



    private void sendCommand(IotCarCommand value) {
        mApiEndpoint.sendCommand(value).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Mark the buttos as selected
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Notify the user TODO
            }
        });
    }

    public void forwardButtonClick(View view){
        sendCommand(IotCarCommand.FORWARD);
    }

    public void leftButtonClick(View view){
        sendCommand(IotCarCommand.LEFT);
    }

    public void rightSpinButtonClick(View view){
        sendCommand(IotCarCommand.SPIN_RIGHT);
    }

    public void leftSpinButtonClick(View view){
        sendCommand(IotCarCommand.SPIN_LEFT);
    }

    public void stopButtonClick(View view){
        sendCommand(IotCarCommand.STOP);
    }

    public void rightButtonClick(View view){
        sendCommand(IotCarCommand.RIGHT);
    }

    public void backwardButtonClick(View view){
        sendCommand(IotCarCommand.BACKWARD);
    }
}