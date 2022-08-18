package com.v2dawn.autotombstone.hook.tombstone.server;


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.highcapable.yukihookapi.hook.log.loggerD
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.InvocationTargetException


class ActivityManagerService(private val activityManagerService: Any) {
    private val processList: ProcessList = ProcessList(
        XposedHelpers.getObjectField(
            activityManagerService,
            FieldEnum.mProcessList
        )
    )
    private val activeServices: ActiveServices = ActiveServices(
        XposedHelpers.getObjectField(
            activityManagerService,
            FieldEnum.mServices
        )
    )
    private val context: Context = XposedHelpers.getObjectField(activityManagerService, FieldEnum.mContext) as Context

    fun isAppForeground(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        val uid = applicationInfo.uid
        var clazz: Class<*>? = activityManagerService.javaClass
        while (clazz != null && clazz.name != Any::class.java.name && clazz.name != ClassEnum.ActivityManagerServiceClass) {
            clazz = clazz.superclass
        }
        if (clazz == null || clazz.name != ClassEnum.ActivityManagerServiceClass) {
            loggerD(msg="super activityManagerService is not found")
            return true
        }
        try {
            return XposedHelpers.findMethodBestMatch(clazz, MethodEnum.isAppForeground, uid).invoke(
                activityManagerService, uid
            ) as Boolean
        } catch (e: IllegalAccessException) {
            loggerD(msg="call isAppForeground method error")
        } catch (e: InvocationTargetException) {
            loggerD(msg="call isAppForeground method error")
        }
        return true
    }

    fun isSystem(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        return applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    fun isImportantSystemApp(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        return applicationInfo.uid < 10000
    }

    fun getApplicationInfo(packageName: String): ApplicationInfo? {
        try {
            val packageManager = context.packageManager
            return packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_UNINSTALLED_PACKAGES
            )
        } catch (e: NameNotFoundException) {
            loggerD(msg="$packageName not found")
        }
        return null
    }

    companion object {
        const val MAIN_USER = 0
    }

}
