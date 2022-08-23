package com.v2dawn.autotombstone.hook.tombstone.support;

import com.highcapable.yukihookapi.hook.core.YukiMemberHookCreater
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.log.loggerW
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.server.ApplicationInfo


fun YukiMemberHookCreater.MemberHookCreater.doNothing() {
    return replaceAny {
        resultNull()
    }
}

fun atsLogI(msg: String) {
    loggerI(msg = msg)
}

fun atsLogD(msg: String) {
//    if (ConfigConst.isDebug) {
        loggerD(msg = msg)
//    }
}

fun atsLogE(msg: String, e: Throwable) {
    loggerE(msg = msg, e = e)
}

fun atsLogW(msg: String) {
    loggerW(msg = msg)
}


object FunctionTool {

    fun isSystem(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and (applicationInfo.FLAG_SYSTEM or applicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
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
