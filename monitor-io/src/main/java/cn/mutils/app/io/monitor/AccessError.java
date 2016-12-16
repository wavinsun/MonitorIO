package cn.mutils.app.io.monitor;

import cn.mutils.app.io.monitor.annotation.AccessMode;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class AccessError extends Error {

    private String mPath;
    private AccessMode mMode;
    private String mMessage;

    public AccessError() {
        super();
    }

    public AccessError(String path, AccessMode mode, String message) {
        super();
        mPath = path;
        mMode = mode;
        mMessage = message;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public AccessMode getAccessMode() {
        return mMode;
    }

    public void setMode(AccessMode mode) {
        mMode = mode;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }


    @Override
    public String toString() {
        String name = getClass().getName();
        if (mMessage == null && mPath == null) {
            return name;
        }
        if (mPath == null) {
            return name + ":\n" + mMessage;
        }
        return name + ": Access Denied -> [" + mMode + "] " + mPath + (mMessage != null ? ("\n" + mMessage) : "");
    }
}
