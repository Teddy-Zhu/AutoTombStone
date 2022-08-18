package com.v2dawn.autotombstone.config

import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData

object ConfigConst {


    //file common
//    val ENABLE_MODULE = PrefsData("_enable_module", true)
    val ENABLE_MODULE_LOG = PrefsData("_enable_module_log", false)
    val ENABLE_HIDE_ICON = PrefsData("_hide_icon", false)

    //file white_apps
    val WHITE_APPS_NAME = "white_apps"
    val WHITE_APPS = PrefsData("_$WHITE_APPS_NAME", "[]")

    //file black_sys_apps
    val BLACK_SYSTEM_APPS_NAME = "black_system_apps"
    val BLACK_SYSTEM_APPS = PrefsData("_$BLACK_SYSTEM_APPS_NAME", "[]")

    //file white_app_processes
    val WHITE_APP_PROCESSES_NAME = "white_app_processes"
    val WHITE_APP_PROCESSES = PrefsData("_$WHITE_APP_PROCESSES_NAME", "[]")

    val KILL_APP_PROCESS_NAME = "kill_app_process"
    val KILL_APP_PROCESS = PrefsData("_$KILL_APP_PROCESS_NAME", false)
}