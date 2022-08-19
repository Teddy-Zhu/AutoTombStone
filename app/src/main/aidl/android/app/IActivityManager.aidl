package android.app;
// Declare any non-default types here with import statements
import android.app.IProcessObserver;
import android.app.ActivityManager;

interface IActivityManager {

    void registerProcessObserver(in IProcessObserver observer);
    void unregisterProcessObserver(in IProcessObserver observer);
    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses();
    List<ActivityManager.RunningServiceInfo> getServices(int maxNum, int flags) ;
    boolean isAppForeground(int uid);

    void setHasOverlayUi(int pid, boolean hasOverlayUi);


}