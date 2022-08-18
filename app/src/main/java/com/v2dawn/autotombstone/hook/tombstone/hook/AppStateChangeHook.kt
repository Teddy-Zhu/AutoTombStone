package com.v2dawn.autotombstone.hook.tombstone.hook

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.allFields
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ComponentNameClass
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.Event
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class AppStateChangeHook : YukiBaseHooker() {
    private val ACTIVITY_RESUMED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_RESUMED }.get(null).cast<Int>()!!
    private val ACTIVITY_PAUSED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_PAUSED }.get(null).cast<Int>()!!
    private lateinit var appStateChangeExecutor: AppStateChangeExecutor

    companion object {
        const val SIMPLE = 1
        const val DIFFICULT = 2
    }

    private fun stateBeforeHookMethod(param: HookParam, type: Int) {
        // 开启一个新线程防止避免阻塞主线程
        Thread(Runnable {
            // 获取切换事件
            val event = param.args(2).int()
            // AMS有两个方法，但参数不同
            val packageName =
                if (type == AppSwitchHook.SIMPLE) param.args(0).string() else ComponentName(
                    param.args(0).cast()
                ).packageName
            loggerD(msg = "event=" + event + " packageName=" + packageName)
            val userId = param.args(1) as Int
            if (userId != ActivityManagerService.MAIN_USER) {
                return@Runnable
            }
            if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
                // 不是进入前台或者后台就不处理
                return@Runnable
            }
            appStateChangeExecutor.execute(packageName, event == ACTIVITY_RESUMED)
        }).start()
    }

    override fun onHook() {
        ClassEnum.ActivityManagerServiceClass.hook {
            injectMember {
                method {
                    name = "systemReady"
                    param(Runnable::class.java, "com.android.server.utils.TimingsTraceAndSlog")
                }
                afterHook {
                    loggerD(msg = "ready ams")

                    appStateChangeExecutor =
                        AppStateChangeExecutor(instance)

                    hookOther(appStateChangeExecutor)
                }
            }
        }
    }

    private fun hookOther(appStateChangeExecutor: AppStateChangeExecutor) {
        // Hook 切换事件
        if (Build.MANUFACTURER == "samsung") {
            ClassEnum.ActivityManagerServiceClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.updateActivityUsageStats
                        param(
                            ComponentNameClass,
                            IntType, IntType, IBinderClass, ComponentNameClass, IntentClass
                        )
                    }
                    beforeHook {
                        stateBeforeHookMethod(this, DIFFICULT)
                    }
                }
            }

        } else {
            ClassEnum.ActivityManagerServiceClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.updateActivityUsageStats
                        param(
                            ComponentNameClass,
                            IntType, IntType, IBinderClass, ComponentNameClass
                        )
                    }
                    beforeHook {
                        stateBeforeHookMethod(this, DIFFICULT)
                    }
                }
            }
        }
        loggerI(msg = "hook commom app switch")

        ClassEnum.NotificationUsageStatsClass.hook {
            injectMember {
                method {
                    name = "registerRemovedByApp"
                    param("com.android.server.notification.NotificationRecord")
                }
                afterHook {
                    loggerD(msg = "app no or overlay closed")


                    val sbn = "com.android.server.notification.NotificationRecord".clazz
                        .field { name = "sbn" }.get(args(0).any())
                        .cast<StatusBarNotification>()!!
                    appStateChangeExecutor.execute(sbn.packageName)
                }
            }
        }
        // for overlay ui & notification
        loggerI(msg = "hook app notification remove & overlay closed")


    }


    inner class AppStateChangeExecutor(ams: Any) :
        Runnable {
        var thread: Thread? = null
        var queue: BlockingQueue<String> = ArrayBlockingQueue(20)
        var timerMap = Collections.synchronizedMap(HashMap<String, Timer?>())
        private val freezeUtils: FreezeUtils
        private val context: Context
        private val processList: ProcessList
        private val appOpsService: Any
        private val mUsageStatsService: Any
        private val activityManagerService: ActivityManagerService

        @JvmOverloads
        fun execute(packageName: String, release: Boolean = false): Boolean {
            prefs
            var timer = timerMap.getOrDefault(packageName, null)
            if (timer != null) {
                timer.cancel()
                timer = null
            }
            if (release) {
                timerMap.remove(packageName)
                check(packageName, true)
                return true
            }
            timer = Timer()
            timerMap[packageName] = timer
            timer.schedule(object : TimerTask() {
                override fun run() {
                    timerMap.remove(packageName)
                    queue.add(packageName)
                }
            }, DELAY_TIME)
            return true
        }

        override fun run() {
            while (true) {
                try {
                    val pkg = queue.take()
                    check(pkg)
                } catch (e: Exception) {
                    Log.e(e)
                }
            }
        }

        private fun check(packageName: String, release: Boolean = false) {
            if ("android" == packageName) {
                return
            }
            loggerD("check packageName=" + packageName.also { msg = it })
            var isForeground: Boolean? = null
            processList.reloadProcessRecord()
            val pid = getTargetProcessPid(packageName)
            if (pid == -1) {
                loggerD("app " + packageName + " not run, ignored".also { msg = it })
                return
            }
            loggerD("packageName=" + packageName + " pid=" + pid.also { msg = it })
            if (release) {
                isForeground = true
            } else {
                isForeground = isForeground(packageName, pid)
                if (isForeground == null) {
                    return
                }
            }
            loggerD(" pkg :$packageName isForeground :$isForeground forceRelease :" + release.also {
                msg = it
            })
            // 如果是进入前台
            if (isForeground) {
                // 后台APP移除
                memData.getAppBackgroundSet().remove(packageName)
            }

            // 重要系统APP
            val isImportantSystemApp = isImportantSystemApp(packageName)
            if (isImportantSystemApp) {
                loggerD(packageName + " is important system app".also { msg = it })
                return
            }
            // 系统APP
            val isSystem = isSystem(packageName)
            // 判断是否白名单系统APP
            if (isSystem && !memData.getBlackSystemApps().contains(packageName)) {
                loggerD(packageName + " is white system app".also { msg = it })
                return
            }
            if (isForeground) {
                memData.getAppBackgroundSet().remove(packageName)
                //继续事件
                onResume(packageName)
            } else {
                //暂停事件
                onPause(packageName, pid)
            }
            loggerD(packageName + " resolve end".also { msg = it })
        }

        fun getTargetProcessPid(packageName: String): Int {
            for (processRecord in processList.getProcessRecords()) {
                // 如果包名和事件的包名不同就不处理
                if (packageName == processRecord.getProcessName()) {
                    return processRecord.getPid()
                }
            }
            return -1
        }

        private fun isForeground(packageName: String, pid: Int): Boolean? {
            return try {
                if (SYS_SUPPORTS_SCHEDGROUPS) {
                    val cpuGroup: CpuGroup = CpuGroup.get(pid)
                    cpuGroup.isForeground()
                } else {
                    val stat: Stat = Stat.get(pid)
                    stat.policy() === 0
                }
            } catch (e: IOException) {
                loggerD("pkg: " + packageName + " not run, ignored".also { msg = it })
                null
            }
        }

        /**
         * APP切换至前台
         *
         * @param packageName 包名
         */
        private fun onResume(packageName: String) {
            val targetProcessRecords: kotlin.collections.List<ProcessRecord> =
                getTargetProcessRecords(packageName)
            // 如果目标进程为空就不处理
            if (targetProcessRecords.isEmpty()) {
                return
            }
            loggerD(packageName + " resumed process".also { msg = it })
            // 遍历目标进程列表
            for (targetProcessRecord in targetProcessRecords) {
                loggerD("process:" + targetProcessRecord.toString().also { msg = it })

                // 确保APP不在后台
                if (memData.getAppBackgroundSet().contains(packageName)) {
                    return
                }
                if (targetProcessRecord.getProcessName().equals(packageName)) {
                    XposedHelpers.callMethod(
                        appOpsService,
                        "setMode",
                        OP_WAKE_LOCK,
                        targetProcessRecord.getUserId(),
                        packageName,
                        AppOpsManager.MODE_DEFAULT
                    )
                    XposedHelpers.callMethod(
                        mUsageStatsService, "setAppInactive",
                        packageName, false
                    )
                    loggerD(
                        "set app:$packageName active, bucket:" + XposedHelpers.callMethod(
                            mUsageStatsService, "getAppStandbyBucket",
                            packageName
                        ).also { msg = it })
                    XposedHelpers.callMethod(
                        mUsageStatsService, "setAppStandbyBucket",
                        packageName, UsageStatsManager.STANDBY_BUCKET_ACTIVE
                    )
                }
                // 解冻进程
                freezeUtils.unFreezer(targetProcessRecord)
            }
        }

        /**
         * APP切换至后台
         *
         * @param packageName 包名
         */
        private fun onPause(packageName: String, mainPid: Int) {
            loggerD(packageName + " paused processing".also { msg = it })

            //double check 应用是否前台
            val isAppForeground = isForeground(packageName, mainPid)
            // 如果是前台应用就不处理
            if (isAppForeground == null || isAppForeground) {
                loggerD(packageName + " is in foreground".also { msg = it })
                return
            }

            // 后台应用添加包名
            memData.getAppBackgroundSet().add(packageName)
            val targetProcessRecords: kotlin.collections.List<ProcessRecord> =
                getTargetProcessRecords(packageName)
            // 如果目标进程为空就不处理
            if (targetProcessRecords.isEmpty()) {
                return
            }

            // 遍历目标进程
            for (targetProcessRecord in targetProcessRecords) {
                // 应用又进入前台了
                if (!memData.getAppBackgroundSet().contains(packageName)) {
                    // 为保证解冻顺利
                    return
                }

                // 目标进程名
                val processName: String = targetProcessRecord.getProcessName()
                if (processName == packageName) {
                    XposedHelpers.callMethod(
                        appOpsService, "setMode",
                        OP_WAKE_LOCK,
                        targetProcessRecord.getUserId(), packageName, AppOpsManager.MODE_IGNORED
                    )
                    XposedHelpers.callMethod(
                        mUsageStatsService, "setAppStandbyBucket",
                        packageName, STANDBY_BUCKET_NEVER
                    )
                    XposedHelpers.callMethod(
                        mUsageStatsService, "setAppInactive",
                        packageName, true
                    )
                    XposedHelpers.callMethod(
                        activityManagerService.getActivityManagerService(),
                        "makePackageIdle",
                        packageName,
                        targetProcessRecord.getUserId()
                    )
                    loggerD(
                        "set app:$packageName inactive,bucket:" + XposedHelpers.callMethod(
                            mUsageStatsService, "getAppStandbyBucket",
                            packageName
                        ).also { msg = it })
                }
                // 目标进程PID
                val pid: Int = targetProcessRecord.getPid()
                // 如果杀死进程列表包含进程名
                if (memData.getKillProcessList().contains(processName)) {
                    loggerD(processName + " kill".also { msg = it })
                    // 杀死进程
                    FreezeUtils.kill(pid)
                } else {
                    loggerD(processName + " freezer".also { msg = it })
                    loggerD("process:" + targetProcessRecord.toString().also { msg = it })
                    freezeUtils.freezer(targetProcessRecord)
                }
            }
        }

        /**
         * 获取目标进程
         *
         * @param packageName 包名
         * @return 目标进程列表
         */
        private fun getTargetProcessRecords(packageName: String): kotlin.collections.List<ProcessRecord> {
            // 从进程列表对象获取所有进程
            val processRecords: kotlin.collections.List<ProcessRecord> =
                processList.getProcessRecords()
            // 存放需要冻结/解冻的 processRecord
            val targetProcessRecords: MutableList<ProcessRecord> = ArrayList<ProcessRecord>()
            // 对进程列表加锁
            synchronized(processList.getProcessList()) {
                // 遍历进程列表
                for (processRecord in processRecords) {
                    if (processRecord.getUserId() !== ActivityManagerService.MAIN_USER) {
                        continue
                    }
                    val applicationInfo: ApplicationInfo = processRecord.getApplicationInfo()
                    // 如果包名和事件的包名不同就不处理
                    if (!applicationInfo.getPackageName().equals(packageName)) {
                        continue
                    }
                    // 获取进程名
                    val processName: String = processRecord.getProcessName()
                    // 如果进程名称不是包名开头就跳过 且非app启动
                    if (!processName.startsWith(packageName) && !processRecord.getApplicationInfo()
                            .getPackageName().equals(packageName)
                    ) {
                        continue
                    }
                    // 如果白名单进程包含进程则跳过
                    if (memData.getWhiteProcessList().contains(processName)) {
                        loggerD("white process " + processName.also { msg = it })
                        continue
                    }
                    // 如果白名单APP包含包名并且杀死进程不包含进程名就跳过
                    if (memData.getWhiteApps()
                            .contains(packageName) && !memData.getKillProcessList()
                            .contains(processName)
                    ) {
                        loggerD("white app process " + processName.also { msg = it })
                        continue
                    }
                    // 添加目标进程
                    targetProcessRecords.add(processRecord)
                }
            }
            return targetProcessRecords
        }

        private fun isSystem(packageName: String): Boolean {
            val applicationInfo = getApplicationInfo(packageName) ?: return true
            return applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }

        private fun isImportantSystemApp(packageName: String): Boolean {
            val applicationInfo = getApplicationInfo(packageName) ?: return true
            return applicationInfo.uid < 10000
        }

        private fun getApplicationInfo(packageName: String): ApplicationInfo? {
            try {
                val packageManager = context.packageManager
                return packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES
                )
            } catch (e: PackageManager.NameNotFoundException) {
                loggerD(packageName + " not found".also { msg = it })
            }
            return null
        }

        companion object {
            const val DELAY_TIME: Long = 5000
            private val SYS_SUPPORTS_SCHEDGROUPS = File("/dev/cpuctl/tasks").exists()
            private var OP_WAKE_LOCK = 40
            private var STANDBY_BUCKET_NEVER = 50
        }

        init {
            this.memData = memData
            thread = Thread(this)
            thread!!.isDaemon = true
            thread!!.start()
            freezeUtils = FreezeUtils(classLoader)
            activityManagerService = ActivityManagerService(ams)
            processList = activityManagerService.getProcessList()
            context = activityManagerService.getContext()
            appOpsService = XposedHelpers.getObjectField(ams, "mAppOpsService")
            mUsageStatsService = context.getSystemService(Context.USAGE_STATS_SERVICE)
            try {
                OP_WAKE_LOCK = XposedHelpers.getStaticIntField(
                    classLoader.loadClass("android.app.AppOpsManager"),
                    "OP_WAKE_LOCK"
                )
                STANDBY_BUCKET_NEVER = XposedHelpers.getStaticIntField(
                    classLoader.loadClass("android.app.usage.UsageStatsManager"),
                    "STANDBY_BUCKET_NEVER"
                )
            } catch (e: ClassNotFoundException) {
                Log.e(e)
            }
            loggerI(msg = "ams class:" + ams.javaClass)
            //        try {
//            loggerI(msg="aaaa" + IActivityManager.Stub.asInterface((IBinder) ams).getRunningAppProcesses().get(0).processName);
//        } catch (RemoteException e) {
//            Log.e(e);
//        }
        }
    }

}