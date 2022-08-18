package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.Context
import android.content.pm.PackageManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.v2dawn.autotombstone.hook.tombstone.hook.system.ContextProxy
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum

object UsageContextHook : YukiBaseHooker() {
    override fun onHook() {
        ClassEnum.UsageStatsServiceClass.hook {
            injectMember {
                constructor {
                    param(ContextClass)
                }
                beforeHook {
                    val context = args().first().cast<Context>() ?: return@beforeHook
                    if (PackageManager.PERMISSION_GRANTED == context.checkCallingPermission("android.permission.CHANGE_APP_IDLE_STATE")) {
                        loggerD(msg = "has permission ignored")

                    } else {
                        loggerI(msg = "proxy usage context")
                        args(0).set(
                            ContextProxy(
                                context,
                                ClassEnum.UsageStatsServiceClass + "_Proxy"
                            )
                        )
                    }
                }
            }
        }
    }
}