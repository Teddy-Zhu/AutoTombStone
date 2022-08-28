package com.v2dawn.autotombstone.hook.tombstone.hook.support

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.usage.IUsageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.*
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.system.CpuGroup
import com.v2dawn.autotombstone.hook.tombstone.hook.system.Stat
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.PowerManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.*
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryKillProcessesList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteProcessesList
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors


@SuppressLint("ServiceCast")
class AppStateChangeExecutor(
    private val packageParam: PackageParam,
    ams: Any,
) : Runnable {

    val queue: BlockingQueue<String> = ArrayBlockingQueue(20)
    val taskQueue: BlockingQueue<String> = ArrayBlockingQueue(5)

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
    private val acService: Any

    private val eventExecutor = Executors.newSingleThreadExecutor()

    private val operateExecutor = Executors.newSingleThreadExecutor()

    companion object {

        var instance: AppStateChangeExecutor? = null
        public val backgroundApps = hashSetOf<String>()

        val hasOverlayUiPackages = hashSetOf<String>()
        val hasAudioFocusPackages = hashSetOf<String>()

        const val DELAY_TIME: Long = 10000
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

    private fun findPackageName(pid: Int): String? {
        try {
            for (runningAppProcess in iActivityManager.getRunningAppProcesses()) {
                if (runningAppProcess.pid == pid) {
                    return runningAppProcess.processName
                }
            }
        } catch (e: RemoteException) {
            atsLogE("invoke error", e = e)
        }
        return null
    }

    fun executeByAudioFocus(packageName: String, hasFocus: Boolean): Boolean {

        atsLogD("[$packageName] ${if (hasFocus) "request" else "lost"} audio focus")
        if (hasFocus) {
            hasAudioFocusPackages.add(packageName)
            return true
        } else {
            hasAudioFocusPackages.remove(packageName)
            return execute(packageName)
        }
    }


    fun executeByOverlayUi(pid: Int, hasOverlayUi: Boolean): Boolean {

        val pkgName = findPackageName(pid);
        if (pkgName == null) {
            atsLogD("not found pid process:$pid")
            return false
        }
        atsLogD("[$pkgName] ${if (hasOverlayUi) "has" else "remove"} overlay ui")
        if (hasOverlayUi) {
            hasOverlayUiPackages.add(pkgName)
            return true
        } else {
            hasOverlayUiPackages.remove(pkgName)
            return execute(pkgName)
        }
    }

    @JvmOverloads
    fun execute(packageName: String, release: Boolean = false): Boolean {

        synchronized(packageName.intern()) {
            var timer = timerMap.getOrDefault(packageName, null)

            timer?.cancel()
            if (release) {
                timerMap.remove(packageName)
                queue.add(packageName)
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
        var isForeground: Boolean?
        processList.reloadProcessRecord()
        val targetProcessRecord = getTargetProcessPid(packageName)
        if (targetProcessRecord == null) {
            atsLogD("[$packageName] not run, ignored")
            return
        }
        val pid = targetProcessRecord.pid
        atsLogD("[$packageName] pid=$pid")
        if (release) {
            isForeground = true
        } else {
//            val cpuforeground = isForeground(
//                packageName,
//                pid
//            )
            val sysForeground = isAppForeground(packageName)
//            val hasOverlay = hasOverlayUiPackages.contains(packageName)
//            val hasActivity = targetProcessRecord.hasRunningActivity(packageName)
            isForeground = sysForeground
//                    || hasOverlayUiPackages.contains(packageName) || hasAudioFocusPackages.contains(packageName)

//            val importance = acService.javaClass.method {
//                name = "getPackageImportance"
//                param(StringType)
//            }.get(acService).int(packageName)
//            atsLogD("[$packageName] sys:${sysForeground},hasOverlay:${hasOverlay},hasActivity:${hasActivity},import:${importance}")
            atsLogD("[$packageName] sys status:${sysForeground}")

        }
//        atsLogD(" pkg :$packageName isForeground :$isForeground forceRelease :$release")

        val runInFreeze = backgroundApps.contains(packageName)
        // 重要系统APP
        val isImportantSystemApp = isImportantSystemApp(packageName)
        if (isImportantSystemApp) {
            atsLogD("[$packageName] is important system app")
            if (!runInFreeze) {
                return
            }
        }
        // 系统APP
        val isSystem = isSystem(packageName)
        // 判断是否白名单系统APP
        packageParam.apply {
            if (isSystem && !queryBlackSysAppsList().contains(packageName)) {
                atsLogD("[$packageName] is white system app")
                if (!runInFreeze) {
                    return
                }
                return
            }
        }

        if (isForeground) {
            //继续事件
            onResumeNew(packageName, true, false, runInFreeze)
        } else {

            //暂停事件
            onPauseNew(packageName, true, false)
        }
        atsLogD("[$packageName] resolve end")

    }

    ////////// open api

    public fun controlApp(packageName: String) {
        atsLogD("[$packageName] user operate onPause")
        operateExecutor.submit {
            onPauseNew(packageName, false, false)
        }
    }

    public fun unControlApp(packageName: String) {
        atsLogD("[$packageName] user operate onResume")
        operateExecutor.submit {
            onResumeNew(packageName, false, false, true)
        }
    }

    public fun freezeApp(packageName: String) {
        atsLogD("[$packageName] user operate freeze")
        operateExecutor.submit {
            onPauseNew(
                packageName, false, false,
                false, true, false, false, false
            )
        }
    }

    public fun unFreezeApp(packageName: String) {
        atsLogD("[$packageName] user operate unfreeze")
        operateExecutor.submit {
            onResumeNew(
                packageName, false, false,
                false, true, false
            )
        }
    }

    public fun stopServices(packageName: String) {
        atsLogD("[$packageName] user operate stopServices")

        operateExecutor.submit {
            onPauseNew(
                packageName, false, false,
                false, false, true, false, false
            )
        }
    }

    public fun makeAppIdle(packageName: String, idle: Boolean) {
        atsLogD("[$packageName] user operate setAppIdle")

        operateExecutor.submit {
            setAppIdle(packageName, idle)
            if (idle) {
                makePackageIdle(packageName)
            }
        }
    }

    public fun forceStopApp(packageName: String) {
        atsLogD("[$packageName] user operate stopPackage")
        operateExecutor.submit {
            stopPackage(packageName)
        }
    }

    public fun restartSystem() {
        operateExecutor.submit {
            restart()
        }
    }

    private fun restart() {
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager;
        manager.reboot("reboot")
    }


    //////////


    fun onPauseNew(
        packageName: String,
        doubleCheckStatus: Boolean,
        ignoreConfig: Boolean,
        killProcess: Boolean = true,
        freeze: Boolean = true,
        stopService: Boolean = true,
        makeIdle: Boolean = true,
        wakeLock: Boolean = true
    ) {

        atsLogD("[$packageName] onPause handle start")

        //double check 应用是否前台
        if (doubleCheckStatus) {
            val isAppForeground =
                isAppForeground(packageName)

            if (isAppForeground) {
                atsLogD("[$packageName] is foreground ignored")
                return
            }
        }
        val killProcessList: Set<String>
        val whiteProcessList: Set<String>
        val whiteApps: Set<String>
        packageParam.apply {
            killProcessList = queryKillProcessesList()
            whiteProcessList = queryWhiteProcessesList()
            whiteApps = queryWhiteAppList()
        }

        val targetProcessRecords: List<ProcessRecord> =
            getTargetProcessRecordsNew(packageName)
        // 如果目标进程为空就不处理
        if (targetProcessRecords.isEmpty()) {
            atsLogD("[$packageName] empty processes ignored")
            return
        }
        // 后台应用添加包名
        backgroundApps.add(packageName)
        val isWhiteApp = whiteApps.contains(packageName)

        if (makeIdle && !isWhiteApp) {
            makePackageIdle(packageName)
        }

        // 遍历目标进程
        for (targetProcessRecord in targetProcessRecords) {
            // 应用又进入前台了
            if (doubleCheckStatus) {
                if (!backgroundApps.contains(packageName)) {
                    // 为保证解冻顺利
                    return
                }
            }

            // 目标进程名
            val processName: String = targetProcessRecord.processName!!

            if (processName == packageName) {
                // 如果白名单进程包含进程则跳过
                if (!ignoreConfig && isWhiteApp) {
                    atsLogD("[$packageName] in white apps ignored")
                    continue
                } else {
                    if (wakeLock) {
                        setWakeLock(
                            packageName,
                            targetProcessRecord.userId,
                            AppOpsManager.MODE_IGNORED
                        )
                    }

                }
            }

            val isInkillProcess = killProcessList.contains(processName)
            // 如果白名单APP包含包名并且杀死进程不包含进程名就跳过
            if (!ignoreConfig && isWhiteApp && !isInkillProcess) {
                atsLogD("[$processName|$packageName] white app process ignored")
                continue
            } else if (!ignoreConfig && whiteProcessList.contains(processName)) {
                // 如果白名单进程包含进程则跳过
                atsLogD("[$processName] white process ignored");
                continue;
            }

            if (stopService) {
//                needStopProcesses.add(targetProcessRecord)
                stopServiceLocked(targetProcessRecord)
            }

            // 如果杀死进程列表包含进程名
            if (isInkillProcess) {
                // 杀死进程
                if (killProcess) {
                    atsLogD("[$processName] kill")
                    freezeUtils.kill(targetProcessRecord.pid)
                }
            } else {
                if (freeze) {
                    atsLogD("[$processName] freezer")
                    freezeUtils.freezer(targetProcessRecord)
//                    needFreezerProcesses.add(targetProcessRecord)
                }
            }

        }
        packageParam.apply {
            if (wakeLock && !isWhiteApp) {
                PowerManagerService.instance?.release(packageName)
            }
        }

//        if (needFreezerProcesses.isNotEmpty()) {
//            needFreezerProcesses.forEach {
//                atsLogD("[${it.processName}] freezer")
//                freezeUtils.freezer(it)
//            }
//        }

        if (makeIdle && !isWhiteApp) {
            setAppIdle(packageName, true)
            makePackageIdle(packageName)
        }
        atsLogD("[$packageName] onPause handle end")
    }


    private fun onResumeNew(
        packageName: String,
        doubleCheckStatus: Boolean,
        ignoreConfig: Boolean,
        lastStatusChange: Boolean = true,
        freeze: Boolean = true,
        wakeLock: Boolean = true
    ) {
        atsLogD("[$packageName] onResume handle start")
        backgroundApps.remove(packageName)

        if (!lastStatusChange) {
            atsLogD("[$packageName] status not change ignored")
            return
        }
        val targetProcessRecords: List<ProcessRecord> =
            getTargetProcessRecordsNew(packageName)
        // 如果目标进程为空就不处理
        if (targetProcessRecords.isEmpty()) {
            atsLogD("[$packageName] empty processes ignored")
            return
        }
        val whiteProcessList: Set<String>
        packageParam.apply {
            whiteProcessList = queryWhiteProcessesList()
        }
        // 遍历目标进程列表
        for (targetProcessRecord in targetProcessRecords) {
//            atsLogD("process: $targetProcessRecord")

            if (doubleCheckStatus) {
                // 确保APP不在后台
                if (backgroundApps.contains(packageName)) {
                    return
                }
            }
            val processName = targetProcessRecord.processName
            if (wakeLock && processName.equals(packageName)) {
                setWakeLock(packageName, targetProcessRecord.userId, AppOpsManager.MODE_DEFAULT)
            }

            if (!ignoreConfig && whiteProcessList.contains(processName)) {
                continue
            }
            // 解冻进程
            if (freeze) {
                atsLogD("[$processName] unfreezer")
                freezeUtils.unFreezer(targetProcessRecord)
            }
        }
//        setAppIdle(packageName, false)
        atsLogD("[$packageName] onResume handle end")
    }

    private fun getTargetProcessPid(packageName: String): ProcessRecord? {
        synchronized(processList.processRecords) {
            for (processRecord in processList.processRecords) {
                // 如果包名和事件的包名不同就不处理
                if (packageName == processRecord.processName) {
                    return processRecord
                }
            }
        }

        return null
    }

    fun stopPackage(packageName: String) {
        activityManagerService.forceStopPackage(packageName)
    }

    private fun setWakeLock(packageName: String, userId: Int, status: Int) {

        appOpsService.javaClass
            .method {
                name = MethodEnum.setMode
                param(
                    IntType, IntType,
                    StringType, IntType
                )
                superClass()
            }.get(appOpsService)
            .call(
                OP_WAKE_LOCK,
                userId,
                packageName,
                status
            )
    }

    /**
     * by system
     */
    private fun isAppForeground(packageName: String): Boolean {
        val applicationInfo = getApplicationInfo(packageName) ?: return true
        val uid = applicationInfo.uid
        return isAppForeground(uid)
    }

    private fun isAppForeground(uid: Int): Boolean {
        return activityManagerService.isAppForeground(uid)
    }

    /**
     * by cpuset
     */
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
            atsLogD("packageName=$packageName not run, ignored")
            false
        }
    }

    private fun makePackageIdle(packageName: String) {

        activityManagerService.activityManagerService.javaClass.method {
            name = "makePackageIdle"
            param(StringType, IntType)
            superClass()
        }.get(activityManagerService.activityManagerService)
            .call(packageName, ActivityManagerService.MAIN_USER)
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
                "[$pkgName] make ${if (idle) "inactive" else "active"}"
            )
        } catch (e: RemoteException) {
            atsLogE("call app idle error", e = e)
        }
    }


    private fun stopServiceLocked(processRecord: ProcessRecord) {
        atsLogD("[${processRecord.processName}] try to stop services")
        activityManagerService.activeServices.activeServices.javaClass.method {
            name = "stopInBackgroundLocked"
            param(IntType)
        }.get(activityManagerService.activeServices.activeServices).call(processRecord.uid)
    }

    private fun stopServiceLocked_dep(processRecord: ProcessRecord) {
        stopServiceLocked(processRecord, false)
    }

    private fun stopServiceLocked(
        processRecord: ProcessRecord,
        enqueueOomAdj: Boolean,
        setDelay: Boolean = false
    ) {
        for (processServiceRecord in processRecord.processServiceRecords) {
            for (mService in processServiceRecord.mServices) {
                if (setDelay) {
                    mService.setDelay(false)
                }
                atsLogD("[${mService.serviceInfo.name}] try to stop")
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

    private fun getTargetProcessRecordsNew(
        packageName: String
    ): List<ProcessRecord> {
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
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            )
        } catch (e: PackageManager.NameNotFoundException) {
            atsLogD("[$packageName] not found")
        }
        return null
    }

    public fun reloadConfig(name: String, key: String) {
        packageParam.apply {
            atsLogI("reload config:$name,key:$key")
            prefs.name(name).clearCache(key)
//                    prefs.name(kv[0]).clearCache()
        }
    }

    init {
        freezeUtils = FreezeUtils(packageParam, freezerConfig)


        activityManagerService = ActivityManagerService(ams)
        processList = activityManagerService.processList
        context = activityManagerService.context
        appOpsService = ams.javaClass.field {
            name = "mAppOpsService"
            type = "com.android.server.appop.AppOpsService"
            superClass()
        }.get(ams).cast<Any>()!!

        acService = context.getSystemService(Context.ACTIVITY_SERVICE)

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
        eventExecutor.submit(this)
    }
}