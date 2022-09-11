package android.app.usage;
// Declare any non-default types here with import statements
import java.lang.String;

interface IUsageStatsManager  {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    boolean isAppInactive(String packageName, int userId, String callingPackage);
    void setAppInactive(String packageName, boolean inactive, int userId);
    int getAppStandbyBucket(String packageName, String callingPackage, int userId);
    void setAppStandbyBucket(String packageName, int bucket, int userId);
}