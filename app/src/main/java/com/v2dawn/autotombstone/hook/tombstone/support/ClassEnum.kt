package com.v2dawn.autotombstone.hook.tombstone.support;

object ClassEnum {

    const val ProcessErrorStateRecordClass ="com.android.server.am.ProcessErrorStateRecord"
    const val ActivityTaskSupervisorClass = "com.android.server.wm.ActivityTaskSupervisor"
    const val ActivityStackSupervisorClass = "com.android.server.wm.ActivityStackSupervisor"
    const val ActivityStackSupervisorHandlerClas =
        "com.android.server.wm.ActivityStackSupervisor\$ActivityStackSupervisorHandler"

    const val TileServiceClass = "android.service.quicksettings.TileService"

    const val ActivityTaskManagerServiceClass = "com.android.server.wm.ActivityTaskManagerService"
    const val ActivityManagerClass = "android.app.ActivityManager"
    const val RecentTasksClass = "com.android.server.wm.RecentTasks"
    const val PowerManagerServiceClass = "com.android.server.power.PowerManagerService"
    const val MediaFocusControlClass = "com.android.server.audio.MediaFocusControl"
    const val AudioManagerClass = "android.media.AudioManager"
    const val ActivityThreadClass = "android.app.ActivityThread"
    const val ServiceRecordClass = "com.android.server.am.ServiceRecord"
    const val ServiceManagerClass = "android.os.ServiceManager"
    const val AppOpsServiceClass = "com.android.server.appop.AppOpsService"
    const val NotificationUsageStatsClass = "com.android.server.notification.NotificationUsageStats"
    const val ActivityManagerServiceClass = "com.android.server.am.ActivityManagerService"
    const val UsageStatsServiceClass = "com.android.server.usage.UsageStatsService"
    const val BroadcastQueueClass = "com.android.server.am.BroadcastQueue"
    const val BroadcastRecordClass = "com.android.server.am.BroadcastRecord"
    const val BroadcastFilterClass = "com.android.server.am.BroadcastFilter"
    const val AnrHelperClass = "com.android.server.am.AnrHelper"
    const val ProcessRecordClass = "com.android.server.am.ProcessRecord"
    const val WindowProcessControllerClass = "com.android.server.wm.WindowProcessController"
    const val AnrRecordClass = "$AnrHelperClass\$AnrRecord"
    const val AppErrorsClass = "com.android.server.am.AppErrors"
    const val ActivityRecordClass = "com.android.server.am.ActivityRecord"
    const val ProcessStateRecordClass = "com.android.server.am.ProcessStateRecord"
    const val OomAdjusterClass = "com.android.server.am.OomAdjuster"
    const val MilletConfigClass = "com.miui.powerkeeper.millet.MilletConfig"
    const val CachedAppOptimizerClass = "com.android.server.am.CachedAppOptimizer"
    const val ProcessListClass = "com.android.server.am.ProcessList"
    const val PowerStateMachineClass = "com.miui.powerkeeper.statemachine.PowerStateMachine"
    const val ProcessClass = "android.os.Process"

    const val ActiveServicesClass = "com.android.server.am.ActiveServices"
}
