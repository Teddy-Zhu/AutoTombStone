package com.v2dawn.autotombstone.hook.tombstone.hook.support

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.usage.IUsageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.system.CpuGroup
import com.v2dawn.autotombstone.hook.tombstone.hook.system.Stat
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryKillProcessesList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteProcessesList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

@SuppressLint("ServiceCast")
class AppStateChangeExecutor(
    private val packageParam: PackageParam,
    ams: Any,
) : Runnable {

    private val thread: Thread = Thread(this)
    public val reloadConfigQueue = ArrayBlockingQueue<String>(10)

    private val reloadThread: Thread = Thread {
        try {
            atsLogD("task queue obj: ${reloadConfigQueue.hashCode()}")

            while (true) {
                val params = reloadConfigQueue.take()
                val kv = params.split("#")
                if (kv.size != 2) {
                    atsLogW("reload kv exception :$params")
                    continue
                }
                packageParam.apply {
                    atsLogI("reload config:${kv[0]},key:${kv[1]}")
                    prefs.name(kv[0]).clearCache(kv[1])
//                    prefs.name(kv[0]).clearCache()
                }
            }
        } catch (e: Exception) {
            //ignored
        }
    }
    val queue: BlockingQueue<String> = ArrayBlockingQueue(20)
    val timerMap = Collections.synchronizedMap(HashMap<String, Timer?>())
    private val freezeUtils: FreezeUtils
    private val freezerConfig = FreezerConfig(packageParam);

    private val context: Context
    private val processList: ProcessList
    private val appOpsService: Any

    //    private val mUsageStatsService: Any
    private val activityManagerService: ActivityManagerService

    private var usm: IUsageStatsManager

    private val iActivityManager: IActivityManager

    private var useOriginMethod = true

    companion object {

        var instance: AppStateChangeExecutor? = null
        public val backgroundApps = hashSetOf<String>()

        const val DELAY_TIME: Long = 5000
        private val SYS_SUPPORTS_SCHEDGROUPS = File("/dev/cpuctl/tasks").exists()
        private var OP_WAKE_LOCK = 40
        private var STANDBY_BUCKET_NEVER = 50


        fun getBackgroundIndex(packageName: String): Int {
            var total: Int = backgroundApps.size
            for (pkg in backgroundApps) {
                if (backgroundApps.contains(pkg)) {
                    continue
                }
                total -= if (packageName == pkg) {
                    return total
                } else {
                    1
                }
            }
            return total
        }

    }

    public fun execute(pid: Int, hasOverlayUi: Boolean): Boolean {
        if (hasOverlayUi) {
            return false
        }
        try {
            for (runningAppProcess in iActivityManager.getRunningAppProcesses()) {
                if (runningAppProcess.pid == pid) {
                    return execute(runningAppProcess.processName)
                }
            }
        } catch (e: RemoteException) {
            atsLogE("invoke error", e = e)
        }
        atsLogD("not found pid process:$pid")
        return false
    }

    @JvmOverloads
    public fun execute(packageName: String, release: Boolean = false): Boolean {

        synchronized(packageName.intern()) {
            var timer = timerMap.getOrDefault(packageName, null)

            timer?.cancel()
            if (release) {
                timerMap.remove(packageName)
                try {
                    check(packageName, true)
                } catch (e: Exception) {
                    atsLogE("call check failed", e = e)
                }
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
        }

        return true
    }

    override fun run() {
        while (true) {
            try {
                val pkg = queue.take()

                check(pkg)

            } catch (eex: Exception) {
                atsLogE("task exe error", e = eex)
            }
        }
    }

    private fun check(packageName: String) {
        check(packageName, false)
    }

    private fun check(packageName: String, release: Boolean = false) {


        if ("android" == packageName) {
            return
        }
        atsLogD("check packageName=$packageName")
        var isForeground: Boolean?
        processList.reloadProcessRecord()
        val pid = getTargetProcessPid(packageName)
        if (pid == -1) {
            atsLogD("app $packageName not run, ignored")
            return
        }
        atsLogD("packageName=$packageName, pid=$pid")
        if (release) {
            isForeground = true
        } else {
            isForeground =
                isForeground(packageName, pid) && isAppForeground(packageName)

        }
        atsLogD(" pkg :$packageName isForeground :$isForeground forceRelease :$release")
        // 如果是进入前台
        if (isForeground) {
            // 后台APP移除
            backgroundApps.remove(packageName)
        }

        // 重要系统APP
        val isImportantSystemApp = isImportantSystemApp(packageName)
        if (isImportantSystemApp) {
            atsLogD("$packageName is important system app")
            return
        }
        // 系统APP
        val isSystem = isSystem(packageName)
        // 判断是否白名单系统APP
        packageParam.apply {
            if (isSystem && !queryBlackSysAppsList().contains(packageName)) {
                atsLogD("$packageName is white system app")
                return
            }
        }
        if (isForeground) {
            backgroundApps.remove(packageName)
            //继续事件
            onResume(packageName)
        } else {
            //暂停事件
            onPause(packageName, pid)
        }
        atsLogD("$packageName resolve end")

    }

    private fun getTargetProcessPid(packageName: String): Int {
        synchronized(processList.processRecords) {
            for (processRecord in processList.processRecords) {
                // 如果包名和事件的包名不同就不处理
                if (packageName == processRecord.processName) {
                    return processRecord.pid
                }
            }
        }

        return -1
    }

    private fun isAppForeground(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        val uid = applicationInfo.uid
        return isAppForeground(uid)
    }

    private fun isAppForeground(pid: Int): Boolean {
        return try {
            iActivityManager.isAppForeground(pid)
        } catch (e: Exception) {
            activityManagerService.isAppForeground(pid)
        }
    }

    private fun isForeground(packageName: String, pid: Int): Boolean {
        return try {
            if (SYS_SUPPORTS_SCHEDGROUPS) {
                val cpuGroup: CpuGroup = CpuGroup[pid]
                cpuGroup.isForeground
            } else {
                val stat: Stat = Stat[pid]
                stat.policy() == 0
            }
        } catch (e: IOException) {
            atsLogD("pkg: $packageName not run, ignored")
            false
        }
    }

    @SuppressLint("NewApi")
    private fun setAppIdle(pkgName: String, idle: Boolean) {

        val uid = UserHandle::class.java.method {
            name = "getUserId"
            param(IntType)
        }.get().invoke<Int>(Binder.getCallingUid())!!
        try {
            usm.setAppInactive(pkgName, idle, uid)
            atsLogD(
                " set package $pkgName idle: $idle"
            )
        } catch (e: RemoteException) {
            atsLogE("call app idle error", e = e)
        }
    }

    /**
     * APP切换至前台
     *
     * @param packageName 包名
     */
    private fun onResume(packageName: String) {
        val targetProcessRecords: List<ProcessRecord> =
            getTargetProcessRecords(packageName)
        // 如果目标进程为空就不处理
        if (targetProcessRecords.isEmpty()) {
            return
        }
        atsLogD("$packageName resumed process")
        // 遍历目标进程列表
        for (targetProcessRecord in targetProcessRecords) {
//            atsLogD("process: $targetProcessRecord")

            // 确保APP不在后台
            if (backgroundApps.contains(packageName)) {
                return
            }
            if (targetProcessRecord.processName.equals(packageName)) {

                appOpsService.javaClass.method {
                    name = MethodEnum.setMode
                    param(
                        IntType, IntType,
                        StringType, IntType
                    )
                    superClass(true)
                }.get(appOpsService)
                    .call(
                        OP_WAKE_LOCK,
                        targetProcessRecord.userId,
                        packageName,
                        AppOpsManager.MODE_DEFAULT
                    )

//                    ClassEnum.UsageStatsServiceClass.clazz.method {
//                        name = MethodEnum.setAppInactive
//                        param(StringType, Boolean.javaClass)
//                    }.get(mUsageStatsService).call(packageName, false)
//
//                    loggerD(
//                        msg = "set app:$packageName active, bucket:" + ClassEnum.UsageStatsServiceClass.clazz.method {
//                            name = MethodEnum.getAppStandbyBucket
//                            emptyParam()
//                        }.get(mUsageStatsService).invoke<Boolean>()
//                    )
//
//                    ClassEnum.UsageStatsServiceClass.clazz.method {
//                        name = MethodEnum.setAppStandbyBucket
//                        param(StringType, IntType)
//                    }.get(mUsageStatsService)
//                        .call(packageName, UsageStatsManager.STANDBY_BUCKET_ACTIVE)


            }
            // 解冻进程
            freezeUtils.unFreezer(targetProcessRecord)
        }
        setAppIdle(packageName, false)

    }

    private fun stopServiceLocked(processRecord: ProcessRecord) {
        stopServiceLocked(processRecord, false)
    }

    private fun stopServiceLocked(processRecord: ProcessRecord, enqueueOomAdj: Boolean) {
        for (processServiceRecord in processRecord.processServiceRecords) {
            for (mService in processServiceRecord.mServices) {
                atsLogD("try stop service ${mService.processName}")
                if (useOriginMethod) {
                    activityManagerService.activeServices.activeServices.javaClass
                        .method {
                            name = "stopServiceLocked"
                            param(ClassEnum.ServiceRecordClass, Boolean::class.javaPrimitiveType!!)
                        }.get(activityManagerService.activeServices.activeServices)
                        .call(mService.serviceRecord, enqueueOomAdj)
                } else {
                    activityManagerService.activeServices.activeServices.javaClass.method {
                        name = "stopServiceLocked"
                        param(ClassEnum.ServiceRecordClass)
                    }.get(activityManagerService.activeServices.activeServices)
                        .call(mService.serviceRecord)
                }
            }
        }
    }

    /**
     * APP切换至后台
     *
     * @param packageName 包名
     */
    private fun onPause(packageName: String, mainPid: Int) {
        atsLogD("$packageName paused processing")

        //double check 应用是否前台
        val isAppForeground = isForeground(packageName, mainPid)
        // 如果是前台应用就不处理
        if (isAppForeground) {
            atsLogD("$packageName is in foreground ignored")
            return
        }

        // 后台应用添加包名
        backgroundApps.add(packageName)
        val targetProcessRecords: List<ProcessRecord> =
            getTargetProcessRecords(packageName)
        // 如果目标进程为空就不处理
        if (targetProcessRecords.isEmpty()) {
            return
        }
        setAppIdle(packageName, true)
        // 遍历目标进程
        for (targetProcessRecord in targetProcessRecords) {
            // 应用又进入前台了
            if (!backgroundApps.contains(packageName)) {
                // 为保证解冻顺利
                return
            }

            // 目标进程名
            val processName: String = targetProcessRecord.processName!!
            if (processName == packageName) {

                appOpsService.javaClass
                    .method {
                        name = MethodEnum.setMode
                        param(
                            IntType, IntType,
                            StringType, IntType
                        )
                        superClass(true)
                    }.get(appOpsService)
                    .call(
                        OP_WAKE_LOCK,
                        targetProcessRecord.userId,
                        packageName,
                        AppOpsManager.MODE_IGNORED
                    )

//                    ClassEnum.UsageStatsServiceClass.clazz.method {
//                        name = MethodEnum.setAppStandbyBucket
//                        param(StringType, IntType)
//                    }.get(mUsageStatsService)
//                        .call(packageName, STANDBY_BUCKET_NEVER)
//                    ClassEnum.UsageStatsServiceClass.clazz.method {
//                        name = MethodEnum.setAppInactive
//                        param(StringType, Boolean.javaClass)
//                    }.get(mUsageStatsService).call(packageName, true)
//
//                    ClassEnum.ActivityManagerServiceClass.clazz
//                        .method {
//                            name = MethodEnum.makePackageIdle
//                            param(
//                                StringType, IntType
//                            )
//                        }.get(activityManagerService.activityManagerService)
//                        .call(
//                            packageName,
//                            targetProcessRecord.userId
//                        )
//
//                    loggerD(
//                        msg = "set app:$packageName inactive,bucket:" + ClassEnum.UsageStatsServiceClass.clazz.method {
//                            name = MethodEnum.getAppStandbyBucket
//                            emptyParam()
//                        }.get(mUsageStatsService).invoke<Boolean>()
//                    )


            }

            stopServiceLocked(targetProcessRecord)

            // 目标进程PID
            val pid: Int = targetProcessRecord.pid
            // 如果杀死进程列表包含进程名
            packageParam.apply {
                if (queryKillProcessesList().contains(processName)) {
                    atsLogD("$processName kill")
                    // 杀死进程
                    freezeUtils.kill(pid)
                } else {
                    atsLogD("$processName freezer")
                    freezeUtils.freezer(targetProcessRecord)
                }
            }

        }
        setAppIdle(packageName, true)
    }

    /**
     * 获取目标进程
     *
     * @param packageName 包名
     * @return 目标进程列表
     */
    private fun getTargetProcessRecords(packageName: String): List<ProcessRecord> {
        // 从进程列表对象获取所有进程
        val processRecords: List<ProcessRecord> =
            processList.processRecords
        // 存放需要冻结/解冻的 processRecord
        val targetProcessRecords: MutableList<ProcessRecord> = ArrayList<ProcessRecord>()
        // 对进程列表加锁
        synchronized(processRecords) {
            // 遍历进程列表
            for (processRecord in processRecords) {
                if (processRecord.userId != ActivityManagerService.MAIN_USER) {
                    continue
                }
                val applicationInfo: ApplicationInfo = processRecord.applicationInfo ?: continue
                // 如果包名和事件的包名不同就不处理
                if (applicationInfo.packageName != packageName) {
                    continue
                }
                // 获取进程名
                val processName: String = processRecord.processName!!
                // 如果进程名称不是包名开头就跳过 且非app启动
                if (!processName.startsWith(packageName) && processRecord.applicationInfo
                        .packageName != packageName
                ) {
                    continue
                }


                // 如果白名单进程包含进程则跳过
                var skip = false
                packageParam.apply {

                    if (queryWhiteProcessesList().contains(processName)) {
                        atsLogD("white process $processName")
                        skip = true
                        return@apply
                    }
                    // 如果白名单APP包含包名并且杀死进程不包含进程名就跳过
                    if (queryWhiteAppList()
                            .contains(packageName) && !queryKillProcessesList()
                            .contains(processName)
                    ) {
                        atsLogD("white app process $processName")
                        skip = true
                        return@apply
                    }
                }

                if (skip) {
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
        return applicationInfo.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun isImportantSystemApp(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        return applicationInfo.uid < 10000
    }

    private fun getApplicationInfo(packageName: String): android.content.pm.ApplicationInfo? {
        try {
            val packageManager = context.packageManager
            return packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_UNINSTALLED_PACKAGES
            )
        } catch (e: PackageManager.NameNotFoundException) {
            atsLogD("$packageName not found")
        }
        return null
    }

    init {
        freezeUtils = FreezeUtils(packageParam, freezerConfig)


        activityManagerService = ActivityManagerService(ams)
        processList = activityManagerService.processList
        context = activityManagerService.context
        appOpsService = ams.javaClass.field {
            name = "mAppOpsService"
            type = "com.android.server.appop.AppOpsService"
            superClass(true)
        }.get(ams).cast<Any>()!!

        atsLogD("appOpsService class: ${appOpsService.javaClass}")
//        mUsageStatsService = context.getSystemService(Context.USAGE_STATS_SERVICE)

        //        this.mUsageStatsService = context.getSystemService(context.USAGE_STATS_SERVICE);


//        IUsageStatsManager
        try {

            packageParam.apply {
                usm = IUsageStatsManager.Stub.asInterface(
                    ClassEnum.ServiceManagerClass.clazz.method {
                        name = "getService"
                        param(StringType)
                    }.get().invoke<IBinder>(Context.USAGE_STATS_SERVICE)
                )
            }

        } catch (e: ClassNotFoundException) {
            atsLogE("", e = e)
            usm =
                IUsageStatsManager.Stub.asInterface(context.getSystemService(Context.USAGE_STATS_SERVICE) as IBinder?)
        }
//        XposedHelpers.callStaticMethod("android.os.ServiceManager","getService",new Class[]{String.class}, Context.USAGE_STATS_SERVICE);
        //        XposedHelpers.callStaticMethod("android.os.ServiceManager","getService",new Class[]{String.class}, Context.USAGE_STATS_SERVICE);
        iActivityManager = IActivityManager.Stub.asInterface(ams as IBinder)

        try {

            OP_WAKE_LOCK = AppOpsManager::class.java.field {
                name = "OP_WAKE_LOCK"
            }.get().cast<Int>()!!
            STANDBY_BUCKET_NEVER = UsageStatsManager::class.java.field {
                name = "STANDBY_BUCKET_NEVER"
            }.get().cast<Int>()!!

        } catch (e: Exception) {
            atsLogE("app state change executor start error", e = e)
        }
        atsLogI("ams class:" + ams.javaClass)
        for (declaredMethod in activityManagerService.activeServices.activeServices.javaClass.declaredMethods) {
            if ("stopServiceLocked" == declaredMethod.name && declaredMethod.parameterTypes.size == 1) {
                useOriginMethod = false
            }
        }
        thread.isDaemon = true
        reloadThread.isDaemon = true
        reloadThread.start()
        thread.start()
    }
}