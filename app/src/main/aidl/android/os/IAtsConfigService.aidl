package android.os;

import java.lang.String;
// Declare any non-default types here with import statements

interface IAtsConfigService {
    void configChange(String name,String key);

    boolean control(String packageName);

    boolean unControl(String packageName);

    boolean stopService(String packageName);

    boolean makeIdle(String packageName, boolean idle);

    boolean forceStop(String packageName);

    boolean restartSystem();
}