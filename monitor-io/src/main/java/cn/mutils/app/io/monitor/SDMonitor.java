package cn.mutils.app.io.monitor;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.mutils.app.io.monitor.annotation.AccessManifest;
import cn.mutils.app.io.monitor.annotation.AccessMode;
import cn.mutils.app.io.monitor.annotation.AccessModeComparator;
import cn.mutils.app.io.monitor.annotation.AccessPath;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class SDMonitor implements IOInterceptor {

    public static final int METHOD_THROW_ERROR = 0;
    public static final int METHOD_SHOW_ALERT = 1;

    public static final int MODE_DEFAULT = 0x00000000;
    public static final int MODE_MAKE_DIR = 0x00000001;
    public static final int MODE_REMOVE = 0x00000002;
    public static final int MODE_RENAME = 0x00000004;
    public static final int MODE_READ = 0x00000008;
    public static final int MODE_WRITE = 0x00000010;

    private static final String TAG = SDMonitor.class.getSimpleName();

    // https://android.googlesource.com/platform/libcore/+/jb-mr2-release/luni/src/main/java/libcore/io/IoBridge.java
    private static final int IO_MODE_READ = 0;
    private static final int IO_MODE_WRITE = 0600;

    private Context mContext;

    private boolean mEnabled;

    private Map<AccessMode, List<String>> mAccessMap = new ConcurrentHashMap<AccessMode, List<String>>();

    private int mMethod = METHOD_THROW_ERROR;
    private AppAlert mAppAlert;
    private long mAlertGapTime = 5000;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public SDMonitor(Context context) {
        mContext = context;
        initAccessManifest();
        mAppAlert = new AppAlert(mContext);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    protected void finalize() throws Throwable {
        if (mHandlerThread != null) {
            mHandlerThread.getLooper().quit();
        }
        super.finalize();
    }

    private void initAccessManifest() {
        AccessManifest manifest = this.getClass().getAnnotation(AccessManifest.class);
        if (manifest == null) {
            return;
        }
        for (AccessPath accessPath : manifest.value()) {
            String path = accessPath.value();
            if (path.isEmpty()) {
                continue;
            }
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            AccessMode[] modes = accessPath.mode();
            if (modes.length == 0) {
                modes = AccessMode.defaultModes();
            } else {
                //将OPEN转变为OPEN_R和OPEN_W拷贝到数组
                int openIndex = Arrays.binarySearch(modes, AccessMode.OPEN);
                if (openIndex != -1) {
                    AccessMode[] newModes = new AccessMode[modes.length + 1];
                    System.arraycopy(modes, 0, newModes, 0, modes.length);
                    newModes[openIndex] = AccessMode.OPEN_R;
                    newModes[modes.length] = AccessMode.OPEN_W;
                }
            }
            for (AccessMode accessMode : modes) {
                List<String> paths = null;
                if (!mAccessMap.containsKey(accessMode)) {
                    paths = new CopyOnWriteArrayList<String>();
                    mAccessMap.put(accessMode, paths);
                } else {
                    paths = mAccessMap.get(accessMode);
                }
                paths.add(path);
            }
        }
        mEnabled = !mAccessMap.isEmpty();
    }

    public void setAlertGapTime(long gapTime) {
        mAlertGapTime = gapTime;
    }

    public void setMethod(int method) {
        mMethod = method;
    }

    public void add(String path) {
        add(path, MODE_DEFAULT);
    }

    public void add(String path, int modes) {
        if (modes == MODE_DEFAULT) {
            modes = (MODE_MAKE_DIR | MODE_REMOVE | MODE_RENAME | MODE_READ | MODE_WRITE);
        }
        if ((modes & MODE_MAKE_DIR) == MODE_MAKE_DIR) {
            add(path, AccessMode.MK_DIR);
        }
        if ((modes & MODE_REMOVE) == MODE_REMOVE) {
            add(path, AccessMode.REMOVE);
        }
        if ((modes & MODE_RENAME) == MODE_RENAME) {
            add(path, AccessMode.RENAME);
        }
        if ((modes & MODE_READ) == MODE_READ) {
            add(path, AccessMode.OPEN_R);
        }
        if ((modes & MODE_WRITE) == MODE_WRITE) {
            add(path, AccessMode.OPEN_W);
        }
    }

    private synchronized void add(String path, AccessMode accessMode) {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        List<String> paths = null;
        if (!mAccessMap.containsKey(accessMode)) {
            paths = new CopyOnWriteArrayList<String>();
            mAccessMap.put(accessMode, paths);
        } else {
            paths = mAccessMap.get(accessMode);
            if (paths.contains(path)) {
                return;
            }
        }
        paths.add(path);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void install() {
        IOMonitor.getInstance().attachLibCoreIO(this);
    }

    public void uninstall() {
        IOMonitor.getInstance().detachLibCoreIO();
    }

    @Override
    public void onMkDir(final String path, final int mode) {
        final AccessError e = new AccessError(path, AccessMode.MK_DIR, null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "MkDir: [" + mode + "] " + path);
                authorize(path, AccessMode.MK_DIR, e);
            }
        });
    }

    @Override
    public void onRemove(final String path) {
        final AccessError e = new AccessError(path, AccessMode.REMOVE, null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Remove: " + path);
                authorize(path, AccessMode.REMOVE, e);
            }
        });
    }

    @Override
    public void onRename(final String oldPath, final String newPath) {
        final AccessError eOld = new AccessError(oldPath, AccessMode.RENAME, null);
        final AccessError eNew = new AccessError(newPath, AccessMode.RENAME, null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Rename: " + oldPath + " -> " + newPath);
                authorize(oldPath, AccessMode.RENAME, eOld);
                authorize(newPath, AccessMode.RENAME, eNew);
            }
        });
    }

    @Override
    public void onOpen(final String path, final int flags, final int mode) {
        final AccessError e = new AccessError(path, null, null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Open: " + "[" + flags + "][" + mode + "] " + path);
                if (mode == IO_MODE_READ) {
                    e.setMode(AccessMode.OPEN_R);
                    authorize(path, AccessMode.OPEN_R, e);
                }
                if (mode == IO_MODE_WRITE) {
                    e.setMode(AccessMode.OPEN_W);
                    authorize(path, AccessMode.OPEN_W, e);
                }
            }
        });
    }

    private void authorize(String path, AccessMode mode, AccessError e) {
        if (!mEnabled) {
            return;
        }
        if (isSystemIO(path)) {
            return;
        }
        List<String> paths = mAccessMap.get(mode);
        if (paths == null || path.isEmpty()) {
            return;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return;
        }
        File sd = Environment.getExternalStorageDirectory();
        String sdRoot = sd.getAbsolutePath() + "/";
        if (!path.startsWith(sdRoot)) {
            return;
        }
        boolean authorized = false;
        for (String accessPath : paths) {
            String accessDir = sdRoot + accessPath;
            if (mode == AccessMode.MK_DIR) {
                if (accessDir.length() == path.length() + 1) {
                    if (accessDir.startsWith(path)) {
                        authorized = true;
                        break;
                    }
                } else {
                    if (path.length() > accessDir.length() && path.startsWith(accessDir)) {
                        authorized = true;
                        break;
                    }
                }
            } else {
                if (path.length() > accessDir.length() && path.startsWith(accessDir)) {
                    authorized = true;
                    break;
                }
            }
        }
        if (!authorized) {
            if (mMethod == METHOD_THROW_ERROR) {
                e.setMessage(printAccessMap());
                throw e;
            } else {
                if (!mAppAlert.isShow() && (System.currentTimeMillis() - mAppAlert.getManualCloseTime()) > mAlertGapTime) {
                    final String eStr = printStackTrace(e);
                    mAppAlert.post(new Runnable() {
                        @Override
                        public void run() {
                            mAppAlert.setClipboardText(eStr);
                            mAppAlert.setText(eStr);
                            mAppAlert.show();
                        }
                    });
                }
            }
            return;
        }
        Log.d(TAG, "Authorize: [" + mode + "] " + path + "");
    }

    /**
     * 是否是系统IO操作
     */
    private boolean isSystemIO(String path) {
        if (!path.startsWith("/")) {
            return true;
        }
        if (path.startsWith("/proc/")) {
            return true;
        }
        if (path.startsWith("/data/")) {
            return true;
        }
        return false;
    }

    private String printAccessMap() {
        if (mAccessMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int sizeOfPlus = 87;
        for (int i = 0; i < sizeOfPlus; i++) {
            sb.append("*");
        }
        String sdRoot = null;
        try {
            sdRoot = Environment.getExternalStorageDirectory() + "/";
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<AccessMode, List<String>> sortedMap = new TreeMap<AccessMode, List<String>>(new AccessModeComparator());
        sortedMap.putAll(mAccessMap);
        for (Map.Entry<AccessMode, List<String>> entry : sortedMap.entrySet()) {
            AccessMode mode = entry.getKey();
            List<String> paths = entry.getValue();
            for (String path : paths) {
                sb.append("\n* [");
                sb.append(mode);
                sb.append("] ");
                if (sdRoot != null) {
                    sb.append(sdRoot);
                }
                sb.append(path);
            }
        }
        sb.append("\n");
        for (int i = 0; i < sizeOfPlus; i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public static String printStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }
}
