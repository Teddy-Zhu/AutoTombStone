package com.v2dawn.autotombstone.hook.tombstone.hook;

import cn.myflv.android.noactive.entity.FieldEnum
import cn.myflv.android.noactive.entity.MemData
import cn.myflv.android.noactive.server.ActivityManagerService
import cn.myflv.android.noactive.server.ApplicationInfo
import cn.myflv.android.noactive.server.BroadcastFilter
import cn.myflv.android.noactive.server.ProcessRecord
import cn.myflv.android.noactive.server.ReceiverList
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BroadcastDeliverHook(memData: MemData) : XC_MethodHook() {
    private val memData: MemData
    @Throws(Throwable::class)
    public override fun beforeHookedMethod(param: MethodHookParam) {
        val args = param.args
        if (args[1] == null) {
            return
        }
        val broadcastFilter = BroadcastFilter(args[1])
        val receiverList: ReceiverList = broadcastFilter.getReceiverList() ?: return
        // 如果广播为空就不处理
        val processRecord: ProcessRecord = receiverList.getProcessRecord()
        // 如果进程或者应用信息为空就不处理
        if (processRecord == null || processRecord.getApplicationInfo() == null) {
            return
        }
        if (processRecord.getUserId() !== ActivityManagerService.MAIN_USER) {
            return
        }
        val applicationInfo: ApplicationInfo = processRecord.getApplicationInfo()
        val packageName: String = processRecord.getApplicationInfo().getPackageName() ?: return
        // 如果包名为空就不处理(猜测系统进程可能为空)
        val processName: String = processRecord.getProcessName()
        // 如果进程名称不是包名开头就跳过
        if (!processName.startsWith(packageName)) {
            return
        }
        // 如果是系统应用并且不是系统黑名单就不处理
        if (applicationInfo.getUid() < 10000 || applicationInfo.isSystem() && !memData.getBlackSystemApps()
                .contains(packageName)
        ) {
            return
        }
        // 如果是前台应用就不处理
        if (!memData.getAppBackgroundSet().contains(packageName)) {
            return
        }
        // 如果白名单应用或者进程就不处理
        if (memData.getWhiteApps().contains(packageName) || memData.getWhiteProcessList()
                .contains(processName)
        ) {
            return
        }
        // 暂存
        val app: Any = processRecord.getProcessRecord()
        param.setObjectExtra(FieldEnum.app, app)
        loggerD(processRecord.getProcessName().toString() + " clear broadcast".also { msg = it })
        // 清楚广播
        receiverList.clear()
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)

        // 获取进程
        val app = param.getObjectExtra(FieldEnum.app) ?: return
        val args = param.args
        if (args[1] == null) {
            return
        }
        val receiverList = XposedHelpers.getObjectField(args[1], FieldEnum.receiverList)
            ?: return
        // 还原修改
        XposedHelpers.setObjectField(receiverList, FieldEnum.app, app)
    }

    init {
        this.memData = memData
    }
}
