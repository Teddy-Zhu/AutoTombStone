package com.v2dawn.autotombstone.hook.tombstone.server;


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum


class ActivityManagerService(
    val activityManagerService: Any
) {
    companion object {
        const val MAIN_USER = 0
    }

    val processList: ProcessList;
    val context: Context;
    private val activeServices: ActiveServices;

    init {
        processList = ProcessList(
            activityManagerService.javaClass
                .field {
                    name = FieldEnum.mProcessListField
                }.get(activityManagerService).cast<Any>()!!
        )

        activeServices = ActiveServices(
            activityManagerService.javaClass
                .field {
                    name = FieldEnum.mServicesField
                }.get(activityManagerService).cast<Any>()!!
        )
        context = activityManagerService.javaClass.field {
            name = FieldEnum.mContextField
        }.get(activityManagerService).cast<Context>()!!
    }


    fun isAppForeground(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        val uid = applicationInfo.uid
        var clazz: Class<*>? = activityManagerService.javaClass
        while (clazz != null && clazz.name != Any::class.java.name && clazz.name != ClassEnum.ActivityManagerServiceClass) {
            clazz = clazz.superclass
        }
        if (clazz == null || clazz.name != ClassEnum.ActivityManagerServiceClass) {
            loggerD(msg = "super activityManagerService is not found")
            return true
        }
        try {
            return activityManagerService.javaClass
                .method {
                    name = MethodEnum.isAppForeground
                    param(IntType)
                }.get(activityManagerService).invoke<Boolean>(activityManagerService, uid)!!
        } catch (e: Exception) {
            loggerD(msg = "call isAppForeground method error")
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
            loggerD(msg = "$packageName not found")
        }
        return null
    }


}
