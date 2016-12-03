package cn.mutils.app.io.monitor.annotation;

import java.util.Comparator;

/**
 * Created by wenhua.ywh on 2016/11/30.
 */
public class AccessModeComparator implements Comparator<AccessMode> {

    @Override
    public int compare(AccessMode lhs, AccessMode rhs) {
        return lhs.ordinal() - rhs.ordinal();
    }

}
