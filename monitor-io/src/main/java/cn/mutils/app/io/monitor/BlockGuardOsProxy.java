package cn.mutils.app.io.monitor;

import libcore.io.BlockGuardOs;
import libcore.io.ErrnoException;
import libcore.io.Os;

import proguard.annotation.Keep;
import proguard.annotation.KeepClassMembers;

import java.io.FileDescriptor;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
@Keep
@KeepClassMembers
public class BlockGuardOsProxy extends BlockGuardOs implements OsProxy {

    private CopyOnWriteArrayList<IOInterceptor> mInterceptors = new CopyOnWriteArrayList<IOInterceptor>();

    public BlockGuardOsProxy(Os os) {
        super(os);
    }

    public BlockGuardOsProxy(Os os, IOInterceptor interceptor) {
        this(os);
        addInterceptor(interceptor);
    }

    public void addInterceptor(IOInterceptor interceptor) {
        if (mInterceptors.contains(interceptor)) {
            return;
        }
        mInterceptors.add(interceptor);
    }

    public void removeInterceptor(IOInterceptor interceptor) {
        mInterceptors.remove(interceptor);
    }

    public void removeAllInterceptors() {
        mInterceptors.clear();
    }

    @Override
    public void mkdir(String path, int mode) throws ErrnoException {
        for (IOInterceptor interceptor : mInterceptors) {
            interceptor.onMkDir(path, mode);
        }
        super.mkdir(path, mode);
    }

    @Override
    public void remove(String path) throws ErrnoException {
        for (IOInterceptor interceptor : mInterceptors) {
            interceptor.onRemove(path);
        }
        super.remove(path);
    }

    @Override
    public void rename(String oldPath, String newPath) throws ErrnoException {
        for (IOInterceptor interceptor : mInterceptors) {
            interceptor.onRename(oldPath, newPath);
        }
        super.rename(oldPath, newPath);
    }

    @Override
    public FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        for (IOInterceptor interceptor : mInterceptors) {
            interceptor.onOpen(path, flags, mode);
        }
        return super.open(path, flags, mode);
    }

}
