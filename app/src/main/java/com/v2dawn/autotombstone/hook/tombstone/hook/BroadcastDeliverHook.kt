package com.v2dawn.autotombstone.hook.tombstone.hook;

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.BroadcastFilter
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteProcessesList
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD

class BroadcastDeliverHook : YukiBaseHooker() {

    fun myReplaceMethod(param: HookParam): Any? {
        val arg1 = param.args(1).any() ?: return null

        val broadcastFilter = BroadcastFilter(arg1)

        val receiverList = broadcastFilter.receiverList

        if (broadcastFilter.receiverList == null || broadcastFilter.receiverList!!.isNull()) {
            return null
        }
        // 如果广播为空就不处理
        val processRecord = receiverList?.processRecord
        // 如果进程或者应用信息为空就不处理
        if (processRecord?.applicationInfo == null) {
            return null
        }
        if (processRecord.userId != ActivityManagerService.MAIN_USER) {
            return null
        }
        val applicationInfo = processRecord.applicationInfo
        val packageName: String = processRecord.applicationInfo.packageName ?: return null
        // 如果包名为空就不处理(猜测系统进程可能为空)
        val processName: String = processRecord.processName ?: return null
        // 如果进程名称不是包名开头就跳过
        if (!processName.startsWith(packageName)) {
            return null
        }
        // 如果是系统应用并且不是系统黑名单就不处理
        if (applicationInfo.uid < 10000 || applicationInfo.isSystem && !queryBlackSysAppsList()
                .contains(packageName)
        ) {
            return null
        }
        // 如果是前台应用就不处理
        if (!AppStateChangeExecutor.backgroundApps.contains(packageName)) {
            return null
        }
        // 如果白名单应用或者进程就不处理
        if (queryWhiteAppList().contains(packageName) || queryWhiteProcessesList()
                .contains(processName)
        ) {
            return null
        }
        // 暂存
        val app: Any = processRecord.processRecord

        atsLogD("${processRecord.processName.toString()}  clear broadcast")
        // 清楚广播
        receiverList.clear()

        return app
    }

    fun afterHookedMethod(param: HookParam, app: Any?) {

        if (app == null) {
            return
        }
        val arg1 = param.args(1).any() ?: return

        // 获取进程

        val broadcastFilter = BroadcastFilter(arg1)
        if (broadcastFilter.receiverList != null) {
            broadcastFilter.receiverList!!.restore(app)
        } else {
            return
        }

    }


    override fun onHook() {
        // Hook 广播分发
        ClassEnum.BroadcastQueueClass.hook {
            injectMember {
                method {
                    name = MethodEnum.deliverToRegisteredReceiverLocked
                    param(
                        ClassEnum.BroadcastRecordClass, ClassEnum.BroadcastFilterClass,
                        Boolean::class.javaPrimitiveType!!,
                        IntType
                    )
                }
                var app: Any? = null
                beforeHook {
                    if (AppStateChangeExecutor.instance != null) {
                        atsLogD("app state change check not null")
                    } else {
                        atsLogD("app state change check null")
                    }
                    app = myReplaceMethod(this)
                }
                afterHook {
                    afterHookedMethod(this, app)
                }
            }
        }
    }
}
