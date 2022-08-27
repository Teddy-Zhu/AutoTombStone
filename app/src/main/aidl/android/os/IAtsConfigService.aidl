package android.os;

import java.lang.String;
// Declare any non-default types here with import statements

interface IAtsConfigService {
    void configChange(String name,String key);

    void restartSystem();
}