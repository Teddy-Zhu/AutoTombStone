package com.v2dawn.autotombstone.hook.tombstone.hook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.type.android.*
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class ActivityHook : YukiBaseHooker() {
    override fun onHook() {
        val a = arrayOf<Intent>()
        var b = arrayOf<String>()

        ClassEnum.ActivityManagerClass.hook {
            injectMember {
                method {
                    name = "broadcastStickyIntent"
                    param(
                        IntentClass,
                        IntType,
                        IntType
                    )
                }
                afterHook {
                    atsLogI(
                        "broadcastStickyIntent ${
                            com.v2dawn.autotombstone.hook.tombstone.server.Intent(
                                args(0).any()!!
                            )
                        }"
                    )
                }
            }
            injectMember {
                method {
                    name = "moveTaskToFront"
                    param(IntType, IntType, BundleClass)
                }
                afterHook {

                    val ctx = instance.javaClass.field {
                        name = "mContext"
                    }.get(instance).cast<Context>()
                    val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ctx?.opPackageName
                    } else {
                        ctx?.packageName
                    }

                    atsLogI("moveTaskToFront pkg: $pkg,bundle:${args(2).any()}")
                }
            }
        }

    }
}