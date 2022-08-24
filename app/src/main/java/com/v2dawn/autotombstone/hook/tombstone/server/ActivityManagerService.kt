package com.v2dawn.autotombstone.hook.tombstone.server;


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD


class ActivityManagerService(
    val activityManagerService: Any
) {
    companion object {
        const val MAIN_USER = 0
    }

    val processList: ProcessList;
    val context: Context;
    public val activeServices: ActiveServices;

    init {
        processList = ProcessList(
            activityManagerService.javaClass
                .field {
                    superClass(true)
                    name = FieldEnum.mProcessListField
                }.get(activityManagerService).cast<Any>()!!
        )

        activeServices = ActiveServices(
            activityManagerService.javaClass
                .field {
                    superClass(true)
                    name = FieldEnum.mServicesField
                }.get(activityManagerService).cast<Any>()!!
        )
        context = activityManagerService.javaClass.field {
            name = FieldEnum.mContextField
            superClass(true)
        }.get(activityManagerService).cast<Context>()!!

    }


    fun isAppForeground(uid: Int): Boolean {
        try {
            return activityManagerService.javaClass
                .method {
                    name = MethodEnum.isAppForeground
                    param(IntType)
                    superClass(true)
                }.get(activityManagerService).invoke<Boolean>(uid)!!
        } catch (e: Exception) {
            atsLogD("call isAppForeground method error")
        }
        return true
    }

    fun isAppForeground(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true

        val uid = applicationInfo.uid
        return isAppForeground(uid)
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
            atsLogD("$packageName not found")
        }
        return null
    }


}
