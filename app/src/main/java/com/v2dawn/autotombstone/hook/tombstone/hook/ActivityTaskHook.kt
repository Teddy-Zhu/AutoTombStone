package com.v2dawn.autotombstone.hook.tombstone.hook

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD

class ActivityTaskHook : YukiBaseHooker() {
    private fun needHook(param: HookParam): Boolean {

        val isKill = param.args(1).boolean()
        val removeFromRecent = param.args(2).boolean()
        val reason = param.args(3).string()
        val task = param.args(0).any()
        if (isKill) {
            return false
        }

        if ("removeTaskLocked" == reason) {
            return false
        }

        atsLogD("remove task:$task,reason:$reason,removeFromRecent:$removeFromRecent,kill:$isKill")

        return true
    }

    override fun onHook() {


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            ClassEnum.ActivityTaskSupervisorClass.hook {
                injectMember {
                    method {
                        name = "removeTask"
                        param(
                            "com.android.server.wm.Task",
                            Boolean::class.javaPrimitiveType!!,
                            Boolean::class.javaPrimitiveType!!,
                            StringType
                        )
                        beforeHook {
                            if (needHook(this)) {
                                resultNull()
                            }
                        }
                    }
                }
            }
        }
    }
}