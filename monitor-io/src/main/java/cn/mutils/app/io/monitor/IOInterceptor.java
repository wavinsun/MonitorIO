package cn.mutils.app.io.monitor;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public interface IOInterceptor {

    void onMkDir(String path, int mode);

    void onRemove(String path);

    void onRename(String oldPath, String newPath);

    void onOpen(String path, int flags, int mode);

}
