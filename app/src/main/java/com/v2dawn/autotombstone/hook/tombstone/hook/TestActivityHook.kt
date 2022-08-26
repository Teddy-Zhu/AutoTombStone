package com.v2dawn.autotombstone.hook.tombstone.hook

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.IBinder
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.type.android.*
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class TestActivityHook : YukiBaseHooker() {
    override fun onHook() {
        val a = arrayOf<Intent>()
        var b = arrayOf<String>()


        ClassEnum.ActivityManagerServiceClass.hook {
            injectMember {
                method {
                    name = "startProcessLocked"
                    param(
                        StringType,
                        ApplicationInfo::class.java,
                        Boolean::class.javaPrimitiveType!!,
                        IntType,
                        "com.android.server.am.HostingRecord",
                        IntType,
                        Boolean::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!
                    )
                }
                afterHook {
                    val pkg = args(1).cast<ApplicationInfo>()
                    val process = args(0).string()
                    atsLogI("startProcessLocked pkg:$pkg,process:$process")
                }

            }
            injectMember {
                method {
                    name = "broadcastIntentWithFeature"
                    param(
                        "android.app.IApplicationThread",
                        StringType,
                        IntentClass,
                        StringType,
                        "android.content.IIntentReceiver",
                        IntType,
                        StringType,
                        BundleClass,
                        b.javaClass,
                        b.javaClass,
                        b.javaClass,
                        IntType,
                        BundleClass,
                        Boolean::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!,
                        IntType
                    )
                }
                afterHook {

                }
            }
            injectMember {
                method {
                    name = "startService"
                    param(
                        "android.app.IApplicationThread",
                        IntentClass,
                        StringType,
                        Boolean::class.javaPrimitiveType!!,
                        StringType,
                        StringType,
                        IntType
                    )
                }
                afterHook {

                }
            }

            injectMember {
                method {
                    name = "bindServiceInstance"
                    param(
                        "android.app.IApplicationThread",
                        IBinderClass, IntentClass, StringType, "android.app.IServiceConnection",
                        IntType, StringType, Boolean::class.javaPrimitiveType!!,
                        IntType, StringType, StringType, IntType
                    )
                }
                beforeHook {

                }
            }

            injectMember {
                method {
                    name = "startActivityWithFeature"
                    param(
                        "android.app.IApplicationThread",
                        StringType, StringType, IntentClass, StringType, IBinderClass,
                        StringType, IntType, IntType, "android.app.ProfilerInfo", BundleClass
                    )
                }
                afterHook {

                }
            }
            injectMember {
                method {
                    name = "startActivityAsUserWithFeature"
                    param(
                        "android.app.IApplicationThread",
                        StringType, StringType, IntentClass, StringType, IBinderClass,
                        StringType, IntType, IntType, "android.app.ProfilerInfo", BundleClass,
                        IntType
                    )
                }
                afterHook { }
            }


            injectMember {
                method {
                    name = "moveActivityTaskToBack"
                    param(IBinderClass, Boolean::class.javaPrimitiveType!!)
                }

                afterHook {

                }
            }

        }
    }
}