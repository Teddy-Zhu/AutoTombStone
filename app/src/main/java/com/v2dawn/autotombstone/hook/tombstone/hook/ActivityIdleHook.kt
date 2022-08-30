package com.v2dawn.autotombstone.hook.tombstone.hook

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.HookParam
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityRecord
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD

class ActivityIdleHook : YukiBaseHooker() {
    private fun needHook(param: HookParam): Boolean {

        val acRaw = param.args(0).any() ?: return false
        val activityRecord = ActivityRecord(acRaw)

        if (activityRecord.intent != null) {
            val pkg = activityRecord.intent.component?.packageName
            if (AppStateChangeExecutor.freezedApps.contains(pkg)) {
                atsLogD("[$pkg] activity idle ignored")
                return true
            }
        }
        return false
    }

    override fun onHook() {

        val sdkVersion = Build.VERSION.SDK_INT

        if (sdkVersion != Build.VERSION_CODES.Q) {
            if (sdkVersion != Build.VERSION_CODES.R) {

                ClassEnum.ActivityTaskSupervisorClass.hook {
                    injectMember {
                        method {
                            name = "activityIdleInternal"
                            param(
                                "com.android.server.wm.ActivityRecord",
                                Boolean::class.javaPrimitiveType!!,
                                Boolean::class.javaPrimitiveType!!,
                                "android.content.res.Configuration"
                            )
                        }
                        beforeHook {
                            if (needHook(this)) {
                                resultNull()
                            }
                        }
                    }
                }
            } else {
                ClassEnum.ActivityStackSupervisorClass.hook {
                    injectMember {
                        method {
                            name = "activityIdleInternal"
                            param(
                                "com.android.server.wm.ActivityRecord",
                                Boolean::class.javaPrimitiveType!!,
                                Boolean::class.javaPrimitiveType!!,
                                "android.content.res.Configuration"
                            )
                        }
                        beforeHook {
                            if (needHook(this)) {
                                resultNull()
                            }
                        }
                    }
                }
            }
        } else {
            ClassEnum.ActivityStackSupervisorHandlerClas.hook {
                injectMember {
                    method {
                        name = "activityIdleInternal"
                        param(
                            "com.android.server.wm.ActivityRecord",
                            Boolean::class.javaPrimitiveType!!
                        )
                    }
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