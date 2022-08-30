package com.v2dawn.autotombstone.hook.tombstone.hook;

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.BroadcastFilter
import com.v2dawn.autotombstone.hook.tombstone.support.*

@Deprecated(message = "replaced by firewall")
class BroadcastDeliverHook : YukiBaseHooker() {

    private fun needClear(param: HookParam): Any? {
        val arg1 = param.args(1).any() ?: return null

        val broadcastFilter = BroadcastFilter(arg1)

        val receiverList = broadcastFilter.receiverList

        if (broadcastFilter.receiverList == null) {
            atsLogD("occur broadcast receiverList null")
            return null
        }
        // 如果广播为空就不处理
        val processRecord = receiverList?.processRecord
        // 如果进程或者应用信息为空就不处理
        if (processRecord?.applicationInfo == null) {
            atsLogD("occur broadcast app info null")
            return null
        }

        val currentPackageName: String = processRecord.applicationInfo.packageName ?: return null
        // 如果包名为空就不处理(猜测系统进程可能为空)
        processRecord.processName ?: return null
        // 如果是前台应用就不处理
        if (!AppStateChangeExecutor.freezedApps.contains(currentPackageName)) {
//            atsLogD("occur broadcast not freeze app ignored")
            return null
        }
        // 暂存


        //double check for firewall
        val app: Any = processRecord.processRecord

        atsLogD("[${processRecord.processName}|${currentPackageName}] clear broadcast")
        receiverList.clear()
        return app

    }

    private fun afterHookedMethod(param: HookParam, app: Any?) {

        if (app == null) {
            return
        }
        val arg1 = param.args(1).any() ?: return

        // 获取进程

        val broadcastFilter = BroadcastFilter(arg1)
        if (broadcastFilter.receiverList != null) {
//            atsLogD("${broadcastFilter.receiverList!!.processRecord?.processName} restore app:${app.hashCode()}")
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
                    app = needClear(this)
                }
                afterHook {
                    afterHookedMethod(this, app)
                }
            }
        }

        atsLogI("hooked broadcast")
    }
}
