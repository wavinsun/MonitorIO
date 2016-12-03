package libcore.io;

import java.io.FileDescriptor;

/**
 * Created by wenhua.ywh on 2016/11/29.
 */
public class BlockGuardOs implements Os{

    public BlockGuardOs(Os os) {

    }

    public void mkdir(String path, int mode) throws ErrnoException{

    }

    public void remove(String path) throws ErrnoException{

    }

    public void rename(String oldPath, String newPath) throws ErrnoException{

    }

    public FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        return null;
    }

}
