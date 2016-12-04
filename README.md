# MonitorIO #
Android IO Monitor - SDMonitor

# SDMonitor #
## 组件简介 ##
SDMonitor对SD卡的基本的文件操作进行拦截进行授权操作。
## 组件使用 ##
自定义SDMonitor配置路径权限清单注解
<pre><code>
@AccessManifest({
        @AccessPath(value = "demo", mode = {
                AccessMode.MK_DIR, //创建文件夹
                AccessMode.REMOVE, //删除文件
                AccessMode.RENAME, //重命名文件
                AccessMode.OPEN_R, //打开文件读
                AccessMode.OPEN_W //打开文件写
        })})
public class DemoMonitor extends SDMonitor {

    public DemoMonitor(Context context) {
        super(context);
    }

}
</code></pre>

以上为详细配置，如果不配置mode详细权限，则该路径默认拥有以上操作权限
<pre><code>
@AccessManifest(@AccessPath("demo"))
public class DemoMonitor extends SDMonitor {

    public DemoMonitor(Context context){
        super(context);
    }

}
</code></pre>

同时也支持开放式的add接口
<pre><code>
public class DemoMonitor extends SDMonitor {

    public DemoMonitor(Context context){
        super(context);
        this.add("demo", SDMonitor.MODE_READ | SDMonitor.MODE_WRITE);
    }

}
</code></pre>

实例化SDMonitor并附加到应用
<pre><code>
public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            DemoMonitor monitor = new DemoMonitor(this);
            monitor.install();
        }
    }
}
</code></pre>

## Maven脚本 ##
使用maven.bat命令即可自动打包上传到本地maven仓库，Maven本地仓库服务下载：https://www.sonatype.com/download-oss-sonatype
<pre><code>
apply plugin: 'maven'

import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact

artifacts{
    archives new DefaultPublishArtifact("monitor-io", "jar", "jar", null, new Date(),
            new File("build/intermediates/bundles/release/classes.jar"))
}

uploadArchives {
    repositories.mavenDeployer {
        name = 'mavenCentralReleaseDeployer'
        repository(url: "http://127.0.0.1:8081/nexus/content/repositories/wavinsun") {
            authentication(userName: "wavinsun", password: "wavinsun")
        }
        addFilter("monitor-io") {artifact, file ->
            artifact.name == "monitor-io"
        }
        pom("monitor-io").packaging = 'jar'
        pom("monitor-io").groupId = "cn.mutils.app"
        pom("monitor-io").artifactId = "monitor-io"
        pom("monitor-io").version = "1.0.0"
    }
}
</code></pre>

## 组件接入 ##
<pre><code>
repositories {
   maven { url 'http://127.0.0.1:8081/nexus/content/repositories/wavinsun/'}
}
dependencies {
   compile 'cn.mutils.app:monitor-io:1.0.0@jar'
}
</code></pre>
## 更新日志 ##
 * 1.0.0 初始化版本
 
## 错误崩溃 ##
在SDMonitor运行期间，检测到不符合配置的SD操作将会抛出AccessError
<pre><code>
cn.mutils.app.io.monitor.AccessError: Access Denied -> [MK_DIR] /storage/emulated/0/demo0
***************************************************************************************
* [MK_DIR] /storage/emulated/0/demo/
* [REMOVE] /storage/emulated/0/demo/
* [RENAME] /storage/emulated/0/demo/
* [OPEN_R] /storage/emulated/0/demo/
* [OPEN_W] /storage/emulated/0/demo/
***************************************************************************************
</code></pre>

如果不采用错误崩溃方式处理AccessError，可以配置显示Alert提示框，点击任意地方即可影藏Alert并且拷贝信息到系统剪切板
<pre><code>
DemoMonitor monitor = new DemoMonitor(this);
monitor.setMethod(SDMonitor.METHOD_SHOW_ALERT);
</code></pre>
## 技术原理 ##
通过反射拿出系统打开io流的对象，进行hook监控。  
Android FileOutStream：
<pre><code>
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        this.mode = O_WRONLY | O_CREAT | (append ? O_APPEND : O_TRUNC);
        this.fd = IoBridge.open(file.getPath(), mode);
        this.shouldClose = true;
        this.guard.open("close");
    }

</code></pre>
通过以上代码可以知道IOBridge进行具体的逻辑,通过Google提供的Android源码无法查看到IOBridge，详情请参考https://android.googlesource.com/platform/libcore/+/jb-mr2-release/luni/src/main/java/libcore/io
<pre><code>
    public static FileDescriptor open(String path, int flags) throws FileNotFoundException {
        FileDescriptor fd = null;
        try {
            // On Android, we don't want default permissions to allow global access.
            int mode = ((flags & O_ACCMODE) == O_RDONLY) ? 0 : 0600;
            fd = Libcore.os.open(path, flags, mode);
            if (fd.valid()) {
                // Posix open(2) fails with EISDIR only if you ask for write permission.
                // Java disallows reading directories too.
                if (S_ISDIR(Libcore.os.fstat(fd).st_mode)) {
                    throw new ErrnoException("open", EISDIR);
                }
            }
            return fd;
        } catch (ErrnoException errnoException) {
            try {
                if (fd != null) {
                    IoUtils.close(fd);
                }
            } catch (IOException ignored) {
            }
            FileNotFoundException ex = new FileNotFoundException(path + ": " + errnoException.getMessage());
            ex.initCause(errnoException);
            throw ex;
        }
    }
</code></pre>
LibCore.os.open()：
<pre><code>
package libcore.io;
public final class Libcore {
    private Libcore() { }
    public static Os os = new BlockGuardOs(new Posix());
}
</code></pre>
通过反射将这个os属性改变增加自定义逻辑
<pre><code>
Class<?> libCoreClass = Class.forName(LIB_CORE_CLASS);
Field osField = libCoreClass.getDeclaredField(LIB_CORE_FIELD_OS);
Os osObject = (libcore.io.Os) osField.get(null);
if (osObject instanceof OsProxy) {
    return false;
}
Os osProxy = new BlockGuardOsProxy(osObject, interceptor);
osField.set(null, osProxy);
</code></pre>