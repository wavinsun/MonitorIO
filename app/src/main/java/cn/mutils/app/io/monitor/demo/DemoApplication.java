package cn.mutils.app.io.monitor.demo;

import android.app.Application;

/**
 * Created by wenhua.ywh on 2016/11/30.
 */
public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            DemoMonitor guard = new DemoMonitor(this);
            guard.install();
        }
    }
}
