package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
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
        var TYPE_ACTIVITY = 0
        var TYPE_BROADCAST = 1
        var TYPE_SERVICE = 2
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
        val eventText: String
        when (event) {
            ACTIVITY_PAUSED -> {
                if (AppStateChangeExecutor.freezedApps.contains(packageName)) {
                    return
                }
                eventText = "paused"
            }
            ACTIVITY_RESUMED -> {
                if (!AppStateChangeExecutor.freezedApps.contains(packageName)) {
                    return
                }
                eventText = "resumed"
            }
            else -> {
                return
            }
        }
        atsLogD("[$packageName] event:$eventText")

        appStateChangeExecutor.execute(
            packageName,
            if (event == ACTIVITY_RESUMED) AppStateChangeExecutor.TYPE_RELEASE else AppStateChangeExecutor.TYPE_NONE
        )

    }

    override fun onHook() {
        ClassEnum.ActivityManagerServiceClass.hook {
            injectMember {
                method {
                    name = "systemReady"
                    param(Runnable::class.java, ClassEnum.TimingsTraceAndSlogClass)
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
                    hookStartIntent(appStateChangeExecutor)
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
                    param(ClassEnum.NotificationRecordClass)
                }
                afterHook {

                    val sbn = ClassEnum.NotificationRecordClass.clazz
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
                    param(ClassEnum.NotificationRecordClass)
                }
                afterHook {

                    val sbn = ClassEnum.NotificationRecordClass.clazz
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

    private fun hookStartIntent(appStateChangeExecutor: AppStateChangeExecutor) {
        TYPE_ACTIVITY = ClassEnum.IntentFirewallClass.clazz.field {
            name = "TYPE_ACTIVITY"
        }.get().int()
        TYPE_BROADCAST = ClassEnum.IntentFirewallClass.clazz.field {
            name = "TYPE_BROADCAST"
        }.get().int()
        TYPE_SERVICE = ClassEnum.IntentFirewallClass.clazz.field {
            name = "TYPE_SERVICE"
        }.get().int()

        ClassEnum.IntentFirewallClass.hook {
            injectMember {
                method {
                    name = "checkIntent"
                    param(
                        "${ClassEnum.IntentFirewallClass}\$FirewallIntentResolver",
                        ComponentNameClass,
                        IntType,
                        IntentClass, IntType, IntType, StringType, IntType
                    )
                }
                beforeHook {


                    when (args(2).int()) {
                        TYPE_ACTIVITY -> {

                            //ignored
                        }
                        TYPE_BROADCAST -> {

                            val pkgName = appStateChangeExecutor.getPackageNameByUId(args(7).int())
                                ?: // atsLogD("check broadcast ignored reason:pkg null")
                                return@beforeHook

                            if (appStateChangeExecutor.needPrevent(pkgName)) {
                                val intent = args(3).cast<Intent>()
                                atsLogD("[${pkgName}] block broadcast ${intent?.action}")
                                resultFalse()
                            }
                        }
                        TYPE_SERVICE -> {

                            val componentName = args(1).cast<ComponentName>()
                            val pkgName = componentName?.packageName
                            if (pkgName == null) {
                                atsLogD("check service ignored reason:pkg null")
                                return@beforeHook
                            }
                            if (appStateChangeExecutor.needPrevent(pkgName)) {
                                atsLogD("[${pkgName}] block service ${componentName?.className}")
                                resultFalse()
                            }
//                                atsLogI("IntentFirewall service ${args(1).cast<Any>()}")
                        }
                    }

                }
            }
        }.onHookClassNotFoundFailure {
            atsLogI("checkIntent class not found $packageName")
        }
        atsLogI("hooked firewall")
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

                        if (AppStateChangeExecutor.freezedApps.contains(packageName)) {
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