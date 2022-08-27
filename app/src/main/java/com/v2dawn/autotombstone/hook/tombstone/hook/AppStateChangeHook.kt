package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Message
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
import com.highcapable.yukihookapi.hook.type.android.MessageClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.Event
import com.v2dawn.autotombstone.hook.tombstone.server.PowerManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.*

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
                    val context = instance.javaClass.field {
                        name = "mContext"
                        superClass()
                    }.get(instance).cast<Context>()
                    if (context != null) {
                        registerAtsConfigService(context, appStateChangeExecutor)
                    } else {
                        atsLogI("not found context reg ats failed")
                    }
//                    registerAtsConfigService(appStateChangeExecutor);
                    hookOther(appStateChangeExecutor)
                    hookProcessKill(appStateChangeExecutor)
                    hookTileClick(appStateChangeExecutor)
                }
            }
        }
    }

    private fun hookTileClick(appStateChangeExecutor: AppStateChangeExecutor) {

        val MSG_TILE_CLICKED = "${ClassEnum.TileServiceClass}\$H".clazz.field {
            name = "MSG_TILE_CLICKED"
        }.get().int()!!
        "${ClassEnum.TileServiceClass}\$H".hook {
            injectMember {
                method {
                    name = "handleMessage"
                    param(MessageClass)
                }
                beforeHook {
                    val msg = args(0).cast<Message>() ?: return@beforeHook
                    val context = instance as Context

                    if (msg.what == MSG_TILE_CLICKED) {
                        atsLogD("[${context.packageName}] tile click")
                        appStateChangeExecutor.execute(context.packageName, true)
                    }
                }
            }.onAllFailure {
                atsLogE("handle tile msg error", e = it)
            }
        }
        atsLogI("hook tile click")
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
        atsLogI("hooked audio focus")
    }

    private fun hookProcessKill(appStateChangeExecutor: AppStateChangeExecutor) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            ClassEnum.ProcessRecordClass.hook {
                injectMember {
                    method {
                        name = "setKilled"
                        param(Boolean::class.javaPrimitiveType!!)
                    }
                    beforeHook {
                        val processRecord = ProcessRecord(instance)
                        if (processRecord.isSandboxProcess()) {
                            return@beforeHook
                        }

                        val packageName: String =
                            processRecord.applicationInfo?.packageName ?: return@beforeHook

                        if (AppStateChangeExecutor.backgroundApps.contains(packageName)) {
                            appStateChangeExecutor.stopPackage(packageName)
                        }
                    }
                }
            }
            atsLogI("hook process record to kill")
        }

    }

    private fun registerAtsConfigService(
        context: Context,
        appStateChangeExecutor: AppStateChangeExecutor
    ) {
        atsLogI("register atsConfigReloadService")
        if (serviceRegistered) return


        val atsConfigService = AtsConfigService(context, appStateChangeExecutor)

        ClassEnum.ServiceManagerClass.clazz
            .method {
                name = "addService"
                param(StringType, IBinderClass, Boolean::class.javaPrimitiveType!!)
            }.get().call(AtsConfigService.serviceName, atsConfigService, true)

        serviceRegistered = true
    }

}