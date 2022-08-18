package com.v2dawn.autotombstone.hook.tombstone.hook;


import com.highcapable.yukihookapi.hook.log.loggerD
import com.v2dawn.autotombstone.hook.tombstone.server.*
import de.robv.android.xposed.XC_MethodHook


class OomAdjHook(private val classLoader: ClassLoader, memData: MemData, type: Int) :
    XC_MethodHook() {
    private val memData: MemData
    private val type: Int
    fun computeOomAdj(param: MethodHookParam) {
        val processRecord: ProcessRecord = when (type) {
            Android_S -> ProcessStateRecord(param.thisObject).processRecord
            Android_Q_R, Color -> ProcessRecord(
                param.args[0]
            )
            else -> return
        }
        // 如果进程或者应用信息为空就不处理
        if (processRecord.applicationInfo == null) {
            return
        }
        if (processRecord.userId !== ActivityManagerService.MAIN_USER) {
            return
        }
        val applicationInfo: ApplicationInfo = processRecord.applicationInfo
        val packageName: String = processRecord.applicationInfo.packageName ?: return
        // 如果包名为空就不处理(猜测系统进程可能为空)
        val processName: String = processRecord.processName
        // 如果进程名称等于包名就跳过
        if (!processName.startsWith(packageName)) {
            return
        }
        // 如果是系统应用并且不是系统黑名单就不处理
        if (applicationInfo.uid < 10000 || applicationInfo.isSystem && !memData.getBlackSystemApps()
                .contains(packageName)
        ) {
            return
        }
        // 如果是前台应用就不处理
        if (!memData.getAppBackgroundSet().contains(packageName)) {
            return
        }
        val finalCurlAdj: Int

        // 如果白名单应用或者进程就不处理
        if (memData.getWhiteApps().contains(packageName) || memData.getWhiteProcessList()
                .contains(processName)
        ) {
            finalCurlAdj = if (processName == packageName) 500 else 700
        } else {
            val curAdj = if (processName == packageName) 700 else 900
            finalCurlAdj = curAdj + memData.getBackgroundIndex(packageName)
        }
        loggerD(msg = "$processName -> $finalCurlAdj")
        when (type) {
            Android_S -> param.args[0] = finalCurlAdj
            Android_Q_R -> processRecord.setCurAdj(finalCurlAdj)
            Color -> ProcessList.setOomAdj(
                classLoader,
                processRecord.pid,
                processRecord.uid,
                finalCurlAdj
            )
            else -> {}
        }
    }

    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        if (type != Color) {
            computeOomAdj(param)
        }
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        if (type == Color) {
            computeOomAdj(param)
        }
    }

    companion object {
        const val Android_S = 1
        const val Android_Q_R = 2
        const val Color = 3
    }

    init {
        this.memData = memData
        this.type = type
    }
}
