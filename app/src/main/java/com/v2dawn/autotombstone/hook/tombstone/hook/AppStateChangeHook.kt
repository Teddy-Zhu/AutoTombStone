package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
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

//    val ACTIVITY_STOPPED = 23
//    val ACTIVITY_DESTROYED = 24

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
        val componentName = param.args(0).cast<ComponentName>()!!
        val packageName = componentName.packageName
        val rootComponentName = param.args(4).cast<ComponentName>()
        val userId = param.args(1).int()
        if (userId != ActivityManagerService.MAIN_USER) {
            return
        }
        if ("android" == packageName) {
            return
        }
        atsLogD("[${packageName}] componentName:${componentName}, root:${rootComponentName} event:$event")
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
        atsLogD("[$packageName] $eventText")

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
        atsLogI("hook common app switch")

        ClassEnum.NotificationUsageStatsClass.hook {
            injectMember {
                method {
                    name = "registerDismissedByUser"
                    param("com.android.server.notification.NotificationRecord")
                }
                afterHook {

                    val sbn = "com.android.server.notification.NotificationRecord".clazz
                        .field { name = "sbn" }.get(args(0).any())
                        .cast<StatusBarNotification>()!!


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        atsLogD("[${sbn.packageName}|${sbn.opPkg}] notification or overlay closed bu")
                    } else {
                        atsLogD("[${sbn.packageName}] notification or overlay closed bu")
                    }
                    appStateChangeExecutor.execute(sbn.packageName)
                }
            }
        }

        ClassEnum.NotificationUsageStatsClass.hook {
            injectMember {
                method {
                    name = "registerRemovedByApp"
                    param("com.android.server.notification.NotificationRecord")
                }
                afterHook {

                    val sbn = "com.android.server.notification.NotificationRecord".clazz
                        .field { name = "sbn" }.get(args(0).any())
                        .cast<StatusBarNotification>()!!


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        atsLogD("[${sbn.packageName}|${sbn.opPkg}] notification or overlay closed ba")
                    } else {
                        atsLogD("[${sbn.packageName}] notification or overlay closed ba")
                    }
                    appStateChangeExecutor.execute(sbn.packageName)
                }
            }
        }
        // for overlay ui & notification
        atsLogI("hook app notification removed and overlay removed")
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
//                        atsLogD("set pid :$pid hasOverlayUi:$hasOverlayUi")
                        appStateChangeExecutor.executeByOverlayUi(pid, hasOverlayUi)
                    }
                }
            }

        ClassEnum.MediaFocusControlClass.hook {
            injectMember {
                allMethods("requestAudioFocus")
                afterHook {
                    val pkg = args(5).string()
                    atsLogD("[${pkg}] audio request result:${result}")

                    val retSuccess = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result
                    if (retSuccess) {
                        appStateChangeExecutor.executeByAudioFocus(pkg, true)
                    }
                }
            }
        }
        ClassEnum.MediaFocusControlClass.hook {
            injectMember {
                allMethods("abandonAudioFocus")
                afterHook {
                    val pkg = args(3).string()

                    atsLogD("[${pkg}] audio abandon result:${result}")
                    val retSuccess = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result
                    if (retSuccess) {
                        appStateChangeExecutor.executeByAudioFocus(pkg, false)
                    }
                }
            }
        }



        atsLogI("hook audio manager")

    }

    private fun registerAtsConfigService(appStateChangeExecutor: AppStateChangeExecutor) {
        atsLogI("register atsConfigReloadService")
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