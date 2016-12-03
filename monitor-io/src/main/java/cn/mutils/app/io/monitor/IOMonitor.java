package cn.mutils.app.io.monitor;

import libcore.io.Os;

import java.lang.reflect.Field;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class IOMonitor {

    private static final String LIB_CORE_CLASS = "libcore.io.Libcore";
    private static final String LIB_CORE_FIELD_OS = "os";

    private static volatile IOMonitor sInstance;

    private Os mLibCoreOs;

    private IOMonitor() {

    }

    public static IOMonitor getInstance() {
        if (sInstance == null) {
            synchronized (IOMonitor.class) {
                if (sInstance == null) {
                    sInstance = new IOMonitor();
                }
            }
        }
        return sInstance;
    }

    public boolean attachLibCoreIO(IOInterceptor interceptor) {
        try {
            Class<?> libCoreClass = Class.forName(LIB_CORE_CLASS);
            Field osField = libCoreClass.getDeclaredField(LIB_CORE_FIELD_OS);
            Os osObject = (libcore.io.Os) osField.get(null);
            if (osObject instanceof OsProxy) {
                return false;
            }
            Os osProxy = new BlockGuardOsProxy(osObject, interceptor);
            osField.set(null, osProxy);
            mLibCoreOs = osObject;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean detachLibCoreIO() {
        try {
            Class<?> libCoreClass = Class.forName(LIB_CORE_CLASS);
            Field osField = libCoreClass.getDeclaredField(LIB_CORE_FIELD_OS);
            Os osObject = (libcore.io.Os) osField.get(null);
            if (!(osObject instanceof OsProxy)) {
                return false;
            }
            if (mLibCoreOs != null) {
                osField.set(null, mLibCoreOs);
            }
            mLibCoreOs = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
