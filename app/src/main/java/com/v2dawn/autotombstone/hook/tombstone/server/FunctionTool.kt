package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.core.YukiMemberHookCreater
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookModulePrefs
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.yuki.XposedModulePrefs

fun YukiMemberHookCreater.MemberHookCreater.doNothing() {
    return replaceAny {
        resultNull()
    }
}

fun PackageParam.iPrefs(name: String): XposedModulePrefs {
    return XposedModulePrefs.instance(name)
}

object FunctionTool {


    fun isSystem(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.flags and (applicationInfo.FLAG_SYSTEM or applicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }


    fun YukiBaseHooker.queryWhiteAppList(): Set<String> {
        return prefs.name(ConfigConst.WHITE_APPS_NAME)
            .getStringSet(ConfigConst.WHITE_APPS_NAME, setOf());
    }

    fun YukiBaseHooker.queryBlackSysAppsList(): Set<String> {
        return prefs.name(ConfigConst.BLACK_SYSTEM_APPS_NAME)
            .getStringSet(ConfigConst.BLACK_SYSTEM_APPS_NAME, setOf());
    }

    fun YukiBaseHooker.queryWhiteProcessesList(): Set<String> {
        return prefs.name(ConfigConst.WHITE_APP_PROCESSES_NAME)
            .getStringSet(ConfigConst.WHITE_APP_PROCESSES_NAME, setOf());
    }

    fun YukiBaseHooker.queryKillProcessesList(): Set<String> {
        return prefs.name(ConfigConst.KILL_APP_PROCESS_NAME)
            .getStringSet(ConfigConst.KILL_APP_PROCESS_NAME, setOf());
    }


}
