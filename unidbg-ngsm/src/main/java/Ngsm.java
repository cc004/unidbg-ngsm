import android.Constants;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.*;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.thread.Task;
import com.github.unidbg.thread.UniThreadDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Ngsm{
    private final AndroidEmulator emulator;

    private final DvmClass Ngsm;
    private final UniThreadDispatcher dispatcher;
    private final VM vm;

    public Ngsm() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(Constants.PACKAGE_NAME)
                .build();
        emulator.getSyscallHandler().setVerbose(false);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        // emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        AndroidResolver resolver = new AndroidResolver(23);
        resolver.paths.add("/android/sdk23/");
        resolver.paths.add("/android/sdk23/data/app/" + Constants.PACKAGE_NAME + "/lib/");
        memory.setLibraryResolver(resolver);
        vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(true);

        dispatcher = (UniThreadDispatcher) emulator.getThreadDispatcher();
        dispatcher.canReturn = true;

        DalvikModule dm = vm.loadLibrary(new File(
                "unidbg-ngsm/src/main/resources/libngsm.so"), false);
        Ngsm = vm.resolveClass("com/nexon/ngsm/Ngsm");
        dm.callJNI_OnLoad(emulator);
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public void ngsmNativeInit() {
        dispatcher.canReturn = false;
        String methodSign = "ngsmNativeInit(Landroid/app/Activity;Landroid/content/Context;Lcom/nexon/ngsm/Ngsm;I)V";
        Ngsm.callStaticJniMethodObject(emulator, methodSign,
                ProxyDvmObject.createObject(vm, this),
                ProxyDvmObject.createObject(vm, new Context()),
                ProxyDvmObject.createObject(vm, this), 2079);
    }

    public void ngsmNativeRun(String npaCode, String npaSN) {
        dispatcher.canReturn = false;
        String methodSign = "ngsmNativeRun(Ljava/lang/String;Ljava/lang/String;)V";
        Ngsm.callStaticJniMethodObject(emulator, methodSign, npaCode, npaSN);
    }

    public String ngsmNativeGetNgsmToken(String npaCode, String npaSN) {
        String methodSign = "ngsmNativeGetNgsmToken(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        StringObject value = Ngsm.callStaticJniMethodObject(emulator, methodSign, npaCode, npaSN);
        return value.getValue();
    }

    // apis

    public void onInitComplete(int var1) {
        System.out.println("ngsm init completed " + var1);
        dispatcher.canReturn = true;
    }
    public void onRunComplete(int var1) {
        System.out.println("ngsm run completed " + var1);
        dispatcher.canReturn = true;
    }
    public void onAbuseDetect(int var1, String var2, boolean var3) {
        System.err.println("ngsm abuse detected " + var1 + ", " + var2 + ", " + var3);
    }
    public WindowManager getWindowManager() {
        return new WindowManager();
    }
    public ApplicationInfo getApplicationInfo() {
        return new ApplicationInfo(Constants.PACKAGE_NAME);
    }
    public File getFilesDir() {
        return new File("/data/data/" + Constants.PACKAGE_NAME + "/files/");
    }
    public String getPackageCodePath() {
        return "/data/app/" + Constants.PACKAGE_NAME + "/base.apk";
    }
}
