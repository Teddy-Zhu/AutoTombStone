package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.os.Build
import android.service.notification.StatusBarNotification
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ComponentNameClass
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.Event
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI

class AppStateChangeHook() : YukiBaseHooker() {
    private val ACTIVITY_RESUMED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_RESUMED }.get(null).cast<Int>()!!
    private val ACTIVITY_PAUSED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_PAUSED }.get(null).cast<Int>()!!

    companion object {
        var serviceRegistered = false
    }

    private fun stateBeforeHookMethod(
        param: HookParam,
        appStateChangeExecutor: AppStateChangeExecutor
    ) {

        // 获取切换事件
        val event = param.args(2).int()
        // AMS有两个方法，但参数不同
        val packageName = param.args(0).cast<ComponentName>()!!.packageName
        val userId = param.args(1).int()
        if (userId != ActivityManagerService.MAIN_USER) {
            return
        }
        if ("android" == packageName) {
            return
        }

        val eventText: String
        when (event) {
            ACTIVITY_PAUSED -> {
                if (AppStateChangeExecutor.backgroundApps.contains(packageName)) {
                    return
                }
                eventText = "paused"
            }
            ACTIVITY_RESUMED -> {
                if (!AppStateChangeExecutor.backgroundApps.contains(packageName)) {
                    return
                }
                eventText = "resumed"
            }
            else -> {
                return
            }
        }
        atsLogD("packageName=$packageName $eventText")

        appStateChangeExecutor.execute(packageName, event == ACTIVITY_RESUMED)

    }

    override fun onHook() {
        ClassEnum.ActivityManagerServiceClass.hook {
            injectMember {
                method {
                    name = "systemReady"
                    param(Runnable::class.java, "com.android.server.utils.TimingsTraceAndSlog")
                }
                afterHook {
                    atsLogD("ready ams")

                    val appStateChangeExecutor =
                        AppStateChangeExecutor(this@AppStateChangeHook, instance)
                    AppStateChangeExecutor.instance = appStateChangeExecutor
                    registerAtsConfigService(appStateChangeExecutor)
//                    registerAtsConfigService(appStateChangeExecutor);
                    hookOther(appStateChangeExecutor)
                }
            }
        }
    }

    private fun hookOther(appStateChangeExecutor: AppStateChangeExecutor) {
        // Hook 切换事件
        if (Build.MANUFACTURER == "samsung") {
            ClassEnum.ActivityManagerServiceClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.updateActivityUsageStats
                        param(
                            ComponentNameClass,
                            IntType, IntType, IBinderClass, ComponentNameClass, IntentClass
                        )
                    }
                    beforeHook {
                        stateBeforeHookMethod(this, appStateChangeExecutor)
                    }
                }
            }

        } else {
            ClassEnum.ActivityManagerServiceClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.updateActivityUsageStats
                        param(
                            ComponentNameClass,
                            IntType, IntType, IBinderClass, ComponentNameClass
                        )
                    }
                    beforeHook {
                        stateBeforeHookMethod(this, appStateChangeExecutor)
                    }
                }
            }
        }
        atsLogI("hook commom app switch")

        ClassEnum.NotificationUsageStatsClass.hook {
            injectMember {
                method {
                    name = "registerRemovedByApp"
                    param("com.android.server.notification.NotificationRecord")
                }
                afterHook {
                    atsLogD("app no or overlay closed")


                    val sbn = "com.android.server.notification.NotificationRecord".clazz
                        .field { name = "sbn" }.get(args(0).any())
                        .cast<StatusBarNotification>()!!
                    appStateChangeExecutor.execute(sbn.packageName)
                }
            }
        }
        // for overlay ui & notification
        atsLogI("hook app notification remove & overlay closed")
        "${ClassEnum.ActivityManagerServiceClass}\$LocalService"
            .hook {
                injectMember {
                    method {
                        name = "setHasOverlayUi"
                        param(IntType, Boolean::class.javaPrimitiveType!!)
                    }
                    afterHook {
                        val hasOverlayUi = args(1).boolean()
                        val pid = args(0).int()
                        atsLogD("set pid :$pid hasOverlayUi:$hasOverlayUi")
                        appStateChangeExecutor.execute(pid, hasOverlayUi)
                    }
                }
            }


    }

    private fun registerAtsConfigService(appStateChangeExecutor: AppStateChangeExecutor) {
        atsLogI("register atsConfigService")
        if (serviceRegistered) return


        val atsConfigService = AtsConfigService(appStateChangeExecutor)

        ClassEnum.ServiceManagerClass.clazz
            .method {
                name = "addService"
                param(StringType, IBinderClass, Boolean::class.javaPrimitiveType!!)
            }.get().call(AtsConfigService.serviceName, atsConfigService, true)

        serviceRegistered = true
    }

}