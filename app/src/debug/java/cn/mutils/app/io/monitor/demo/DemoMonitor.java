package cn.mutils.app.io.monitor.demo;

import android.content.Context;

import cn.mutils.app.io.monitor.SDMonitor;
import cn.mutils.app.io.monitor.annotation.AccessManifest;
import cn.mutils.app.io.monitor.annotation.AccessPath;

/**
 * Created by wenhua.ywh on 2016/11/30.
 */
@AccessManifest({@AccessPath("demo")})
public class DemoMonitor extends SDMonitor {

    public DemoMonitor(Context context) {
        super(context);
        this.setMethod(SDMonitor.METHOD_SHOW_ALERT);
        this.add("test");
        this.addFile("test.log");
    }

}
