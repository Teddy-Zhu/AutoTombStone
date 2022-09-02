package com.v2dawn.autotombstone.config

import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData

object ConfigConst {

    //file common
//    val ENABLE_MODULE = PrefsData("_enable_module", true)
    val ENABLE_MODULE_LOG = PrefsData("enable_module_log", false)

    val COMMON_NAME = "common"
    val FREEZE_TYPE = PrefsData("freezeType", 1)
    val STOP_SERVICE = PrefsData("stopService", false)

    //file white_apps
    val WHITE_APPS_NAME = "white_apps"
    val WHITE_APPS = PrefsData(WHITE_APPS_NAME, setOf<String>())

    //file black_sys_apps
    val BLACK_SYSTEM_APPS_NAME = "black_system_apps"
    val BLACK_SYSTEM_APPS = PrefsData(BLACK_SYSTEM_APPS_NAME, setOf<String>())

    //file white_app_processes
    val WHITE_APP_PROCESSES_NAME = "white_app_processes"
    val WHITE_APP_PROCESSES = PrefsData(WHITE_APP_PROCESSES_NAME, setOf<String>())

    val KILL_APP_PROCESS_NAME = "kill_app_process"
    val KILL_APP_PROCESS = PrefsData(KILL_APP_PROCESS_NAME, setOf<String>())
}