package cn.mutils.app.io.monitor.annotation;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public enum AccessMode {

    /**
     * 创建文件夹
     */
    MK_DIR,

    /**
     * 删除文件
     */
    REMOVE,

    /**
     * 重命名文件
     */
    RENAME,

    /**
     * 打开文件
     *
     * 等价于{@link #OPEN_R}+{@link #OPEN_W}
     */
    OPEN,

    /**
     * 打开文件进行读
     */
    OPEN_R,

    /**
     * 打开文件进行写
     */
    OPEN_W;

    public static AccessMode[] defaultModes() {
        return new AccessMode[]{MK_DIR, REMOVE, RENAME, OPEN_R, OPEN_W};
    }

    public static AccessMode[] defaultFileModes() {
        return new AccessMode[]{REMOVE, RENAME, OPEN_R, OPEN_W};
    }

}
