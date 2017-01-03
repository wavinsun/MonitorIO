package cn.mutils.app.io.monitor.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by wenhua.ywh on 2016/11/30.
 */
public class SDMonitorActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_sdmonitor);

        findViewById(R.id.mk_dir).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);
        findViewById(R.id.rename).setOnClickListener(this);
        findViewById(R.id.open_r).setOnClickListener(this);
        findViewById(R.id.open_w).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mk_dir: {
                File dir = new File(Environment.getExternalStorageDirectory() + "/demo/");
                dir.mkdirs();
                File dirError = new File(Environment.getExternalStorageDirectory() + "/demo0/");
                dirError.mkdirs();
            }
            break;
            case R.id.remove: {
                File dir = new File(Environment.getExternalStorageDirectory() + "/demo");
                dir.delete();
                File dirError = new File(Environment.getExternalStorageDirectory() + "/demo0/");
                dirError.mkdirs();
            }
            break;
            case R.id.rename: {
                File dir = new File(Environment.getExternalStorageDirectory() + "/demo");
                dir.renameTo(new File(Environment.getExternalStorageDirectory() + "/demo"));
                File dirError = new File(Environment.getExternalStorageDirectory() + "/demo0/");
                dirError.renameTo(new File(Environment.getExternalStorageDirectory() + "/demo"));
            }
            break;
            case R.id.open_r: {
                try {
                    FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/demo/0");
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/test.log");
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case R.id.open_w: {
                try {
                    FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/demo");
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/demo0");
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
        }
    }
}
