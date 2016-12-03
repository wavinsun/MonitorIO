package cn.mutils.app.io.monitor;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import cn.mutils.app.io.monitor.annotation.AccessManifest;
import cn.mutils.app.io.monitor.annotation.AccessMode;
import cn.mutils.app.io.monitor.annotation.AccessModeComparator;
import cn.mutils.app.io.monitor.annotation.AccessPath;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class SDMonitor implements IOInterceptor {

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

    public SDMonitor(Context context) {
        mContext = context;
        initAccessManifest();
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
    public void onMkDir(String path, int mode) {
        Log.d(TAG, "MkDir: [" + mode + "] " + path);
        authorize(path, AccessMode.MK_DIR);
    }

    @Override
    public void onRemove(String path) {
        Log.d(TAG, "Remove: " + path);
        authorize(path, AccessMode.REMOVE);
    }

    @Override
    public void onRename(String oldPath, String newPath) {
        Log.d(TAG, "Rename: " + oldPath + " -> " + newPath);
        authorize(oldPath, AccessMode.RENAME);
        authorize(newPath, AccessMode.RENAME);
    }

    @Override
    public void onOpen(String path, int flags, int mode) {
        Log.d(TAG, "Open: " + "[" + flags + "][" + mode + "] " + path);
        if (mode == IO_MODE_READ) {
            authorize(path, AccessMode.OPEN_R);
        }
        if (mode == IO_MODE_WRITE) {
            authorize(path, AccessMode.OPEN_W);
        }
    }

    private void authorize(String path, AccessMode mode) {
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
            throw new AccessError(path, mode, printAccessMap());
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
}
