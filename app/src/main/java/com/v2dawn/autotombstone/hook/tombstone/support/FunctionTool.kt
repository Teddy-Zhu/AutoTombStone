package com.v2dawn.autotombstone.hook.tombstone.support;

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.IAtsConfigService
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.hook.core.YukiMemberHookCreater
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.log.loggerW
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookModulePrefs
import com.v2dawn.autotombstone.config.ConfigConst


fun YukiMemberHookCreater.MemberHookCreater.doNothing() {
    return replaceAny {
        resultNull()
    }
}

fun atsLogI(msg: String) {
    loggerI(msg = msg)
}

fun atsLogDApp(msg: String, atsConfigService: IAtsConfigService) {
    if (atsConfigService.getConfig(ConfigConst.COMMON_NAME, ConfigConst.ENABLE_MODULE_LOG.key)) {
        loggerD(msg = msg)
    }
}


fun atsLogD(msg: String) {
    if (YukiHookModulePrefs.InnerOpen.instance().name(ConfigConst.COMMON_NAME)
            .get(ConfigConst.ENABLE_MODULE_LOG)
    ) {
        loggerD(msg = msg)
    }
}

fun atsLogE(msg: String, e: Throwable) {
    loggerE(msg = msg, e = e)
}

fun atsLogW(msg: String) {
    loggerW(msg = msg)
}

fun ApplicationInfo.isSystem(): Boolean {
    return FunctionTool.isSystem(this)
}


fun ApplicationInfo.isImportantSystem(): Boolean {
    return FunctionTool.isImportantSystemApp(this)
}

object FunctionTool {

    val FLAG_SYSTEM: Int = android.content.pm.ApplicationInfo::class.java.field {
        name = "FLAG_SYSTEM"
    }.get().int();
    val FLAG_UPDATED_SYSTEM_APP: Int = android.content.pm.ApplicationInfo::class.java.field {
        name = "FLAG_UPDATED_SYSTEM_APP"
    }.get().int();


    fun isSystem(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and (FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP) != 0
    }

    fun isImportantSystemApp(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.uid < 10000
    }

    fun PackageParam.queryWhiteAppList(): Set<String> {
        return prefs.name(ConfigConst.WHITE_APPS_NAME)
            .getStringSet(ConfigConst.WHITE_APPS_NAME, setOf());
    }

    fun PackageParam.queryBlackSysAppsList(): Set<String> {
        return prefs.name(ConfigConst.BLACK_SYSTEM_APPS_NAME)
            .getStringSet(ConfigConst.BLACK_SYSTEM_APPS_NAME, setOf());
    }

    fun PackageParam.queryWhiteProcessesList(): Set<String> {
        return prefs.name(ConfigConst.WHITE_APP_PROCESSES_NAME)
            .getStringSet(ConfigConst.WHITE_APP_PROCESSES_NAME, setOf());
    }

    fun PackageParam.queryKillProcessesList(): Set<String> {
        return prefs.name(ConfigConst.KILL_APP_PROCESS_NAME)
            .getStringSet(ConfigConst.KILL_APP_PROCESS_NAME, setOf());
    }


}
