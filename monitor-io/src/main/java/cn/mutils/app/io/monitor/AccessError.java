package cn.mutils.app.io.monitor;

import cn.mutils.app.io.monitor.annotation.AccessMode;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class AccessError extends Error {

    private String mPath;
    private AccessMode mMode;

    public AccessError() {
        super();
    }

    public AccessError(String path, AccessMode mode, String message) {
        super(message);
        mPath = path;
        mMode = mode;
    }

    public AccessError(String detailMessage) {
        super(detailMessage);
    }

    public AccessError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AccessError(Throwable throwable) {
        super(throwable);
    }

    @Override
    public String toString() {
        String msg = getLocalizedMessage();
        String name = getClass().getName();
        if (msg == null && mPath == null) {
            return name;
        }
        if (mPath == null) {
            return name + ":\n" + msg;
        }
        return name + ": Access Denied -> [" + mMode + "] " + mPath + (msg != null ? ("\n" + msg) : "");
    }
}
