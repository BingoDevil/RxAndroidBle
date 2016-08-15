package cn.bingo.myapp;

import android.app.Application;
import android.content.Context;

import com.polidea.rxandroidble.RxBleClient;

public class SampleApplication extends Application {
    private RxBleClient rxBleClient;

    /**
     * In practise you will use some kind of dependency injection pattern.
     */
    public static RxBleClient getRxBleClient(Context context) {
        SampleApplication application = (SampleApplication) context.getApplicationContext();
        return application.rxBleClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rxBleClient = RxBleClient.create(this);
    }
}
