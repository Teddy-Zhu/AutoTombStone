package com.v2dawn.autotombstone.hook.tombstone.hook

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.android.ComponentNameClass
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class TestActivityHook : YukiBaseHooker() {
    override fun onHook() {
        val a = arrayOf<Intent>()
        var b = arrayOf<String>()

        "com.android.server.statusbar.StatusBarManagerService".hook {
            injectMember {
                method {
                    name = "clickTile"
                    param(ComponentNameClass)
                }
                beforeHook {
                    atsLogI("StatusBarManagerService clickTile ${args(0).cast<ComponentName>()}")
                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("StatusBarManagerService class not found $packageName")
        }

        "com.android.systemui.qs.QSPanelControllerBase".hook {
            injectMember {
                method {
                    name = "clickTile"
                    param(ComponentNameClass)
                    superClass()
                }
                beforeHook {
                    atsLogI("QSPanelControllerBase clickTile ${args(0).cast<ComponentName>()}")
                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("QSPanelControllerBase class not fund $packageName")
        }
        "com.android.systemui.QSPanelController".hook {
            injectMember {
                method {
                    name = "clickTile"
                    param(ComponentNameClass)
                    superClass()
                }
                beforeHook {
                    atsLogI("QSPanelController clickTile ${args(0).cast<ComponentName>()}")
                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("QSPanelController class not fund $packageName")
        }

        "com.android.systemui.QuickQSPanelController".hook {
            injectMember {
                method {
                    name = "clickTile"
                    param(ComponentNameClass)
                    superClass()
                }
                beforeHook {
                    atsLogI("QuickQSPanelController clickTile ${args(0).cast<ComponentName>()}")
                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("QuickQSPanelController class not fund $packageName")
        }

        TileService::class.java.hook {
            injectMember {
                method {
                    name="onClick"
                    emptyParam()
                }
                beforeHook {
                    atsLogI("TileService click settings")
                }
            }
            injectMember {
                method {
                    name="startActivityAndCollapse"
                    param(IntentClass)
                }
                beforeHook {
                    atsLogI("startActivityAndCollapse ${com.v2dawn.autotombstone.hook.tombstone.server.Intent(args(0).any()!!)}")
                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("TileService class not fund $packageName")

        }

        atsLogI("loading finish")
//        ClassEnum.ActivityManagerServiceClass.hook {
//            injectMember {
//                method {
//                    name = "startProcessLocked"
//                    param(
//                        StringType,
//                        ApplicationInfo::class.java,
//                        Boolean::class.javaPrimitiveType!!,
//                        IntType,
//                        "com.android.server.am.HostingRecord",
//                        IntType,
//                        Boolean::class.javaPrimitiveType!!,
//                        Boolean::class.javaPrimitiveType!!
//                    )
//                }
//                afterHook {
//                    val pkg = args(1).cast<ApplicationInfo>()
//                    val process = args(0).string()
//                    atsLogI("startProcessLocked pkg:$pkg,process:$process")
//                }
//
//            }
//            injectMember {
//                method {
//                    name = "broadcastIntentWithFeature"
//                    param(
//                        "android.app.IApplicationThread",
//                        StringType,
//                        IntentClass,
//                        StringType,
//                        "android.content.IIntentReceiver",
//                        IntType,
//                        StringType,
//                        BundleClass,
//                        b.javaClass,
//                        b.javaClass,
//                        b.javaClass,
//                        IntType,
//                        BundleClass,
//                        Boolean::class.javaPrimitiveType!!,
//                        Boolean::class.javaPrimitiveType!!,
//                        IntType
//                    )
//                }
//                afterHook {
//                    val pkg = args(1).string()
//                    val intent =
//                        com.v2dawn.autotombstone.hook.tombstone.server.Intent(args(2).any()!!)
//                    atsLogI("broadcastIntentWithFeature from $pkg intent:$intent")
//                }
//            }
//            injectMember {
//                method {
//                    name = "startService"
//                    param(
//                        "android.app.IApplicationThread",
//                        IntentClass,
//                        StringType,
//                        Boolean::class.javaPrimitiveType!!,
//                        StringType,
//                        StringType,
//                        IntType
//                    )
//                }
//                afterHook {
//
//                    val pkg = args(4).string()
//                    val intent =
//                        com.v2dawn.autotombstone.hook.tombstone.server.Intent(args(1).any()!!)
//                    val requetFore = args(3).boolean()
//                    val result = result<ComponentName>()
//                    atsLogI("startService from:$pkg ,intent:$intent ,requestFore:$requetFore,cn:$result")
//                    if (pkg == "com.github.kr328.clash") {
//                        atsLogI("trace:${Log.getStackTraceString(Throwable())}")
//                    }
//                }
//            }
//
//            injectMember {
//                method {
//                    name = "bindServiceInstance"
//                    param(
//                        "android.app.IApplicationThread",
//                        IBinderClass, IntentClass, StringType, "android.app.IServiceConnection",
//                        IntType, StringType, Boolean::class.javaPrimitiveType!!,
//                        IntType, StringType, StringType, IntType
//                    )
//                }
//                beforeHook {
//
//
//                    val processRecord = ProcessRecord(instance.javaClass.method {
//                        name = "getRecordForAppLOSP"
//                        param("android.app.IApplicationThread")
//                    }.get(instance).invoke<Any>()!!)
//                    val callpkg = args(10).string()
//                    atsLogI("bindServiceInstance from:$callpkg,pkg:${processRecord.applicationInfo?.packageName},")
//
//                }
//            }
//
//            injectMember {
//                method {
//                    name = "startActivityWithFeature"
//                    param(
//                        "android.app.IApplicationThread",
//                        StringType, StringType, IntentClass, StringType, IBinderClass,
//                        StringType, IntType, IntType, "android.app.ProfilerInfo", BundleClass
//                    )
//                }
//                afterHook {
//                    val processRecord = ProcessRecord(instance.javaClass.method {
//                        name = "getRecordForAppLOSP"
//                        param("android.app.IApplicationThread")
//                    }.get(instance).invoke<Any>()!!)
//                    val intent =
//                        com.v2dawn.autotombstone.hook.tombstone.server.Intent(args(3).any()!!)
//                    val pkg = args(1).string()
//                    atsLogI("startActivityWithFeature from:$pkg pkg:${processRecord.applicationInfo?.packageName},intent:$intent")
//                }
//            }
//            injectMember {
//                method {
//                    name = "startActivityAsUserWithFeature"
//                    param(
//                        "android.app.IApplicationThread",
//                        StringType, StringType, IntentClass, StringType, IBinderClass,
//                        StringType, IntType, IntType, "android.app.ProfilerInfo", BundleClass,
//                        IntType
//                    )
//                }
//                afterHook {
//                    val processRecord = ProcessRecord(instance.javaClass.method {
//                        name = "getRecordForAppLOSP"
//                        param("android.app.IApplicationThread")
//                    }.get(instance).invoke<Any>()!!)
//                    val intent =
//                        com.v2dawn.autotombstone.hook.tombstone.server.Intent(args(3).any()!!)
//                    val pkg = args(1).string()
//                    atsLogI("startActivityAsUserWithFeature from:$pkg pkg:${processRecord.applicationInfo?.packageName},intent:$intent")
//                }
//            }
//
//
//            injectMember {
//                method {
//                    name = "moveActivityTaskToBack"
//                    param(IBinderClass, Boolean::class.javaPrimitiveType!!)
//                }
//
//                afterHook {
//                    val rd = args(0).any()!!
//                    atsLogI("moveActivityTaskToBack class:${rd.javaClass.name}")
//
//                }
//            }
//
//        }
    }
}