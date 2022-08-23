package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.Context
import android.content.pm.PackageManager
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.v2dawn.autotombstone.hook.tombstone.hook.system.ContextProxy
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class UsageContextHook : YukiBaseHooker() {
    override fun onHook() {
        ClassEnum.UsageStatsServiceClass.hook {
            injectMember {
                constructor {
                    param(ContextClass)
                }
                beforeHook {
                    val context = args().first().cast<Context>() ?: return@beforeHook
                    if (PackageManager.PERMISSION_GRANTED == context.checkCallingPermission("android.permission.CHANGE_APP_IDLE_STATE")) {
                        atsLogD( "has permission ignored")

                    } else {
                        atsLogI( "proxy usage context")
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