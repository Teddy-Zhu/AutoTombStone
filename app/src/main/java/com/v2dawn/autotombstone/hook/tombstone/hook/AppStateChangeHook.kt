package com.v2dawn.autotombstone.hook.tombstone.hook

import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ComponentNameClass
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.ComponentName
import com.v2dawn.autotombstone.hook.tombstone.server.Event
import com.v2dawn.autotombstone.hook.tombstone.support.*
import java.util.concurrent.ArrayBlockingQueue

class AppStateChangeHook() : YukiBaseHooker() {
    private val ACTIVITY_RESUMED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_RESUMED }.get(null).cast<Int>()!!
    private val ACTIVITY_PAUSED: Int =
        Event.EventClass.clazz.field { name = Event.ACTIVITY_PAUSED }.get(null).cast<Int>()!!

    private val SIMPLE = 1
    private val DIFFICULT = 2

    companion object {
    }

    private fun stateBeforeHookMethod(
        param: HookParam,
        type: Int,
        appStateChangeExecutor: AppStateChangeExecutor
    ) {
        // 开启一个新线程防止避免阻塞主线程
        Thread(Runnable {
            // 获取切换事件
            val event = param.args(2).int()
            // AMS有两个方法，但参数不同
            val packageName =
                if (type == SIMPLE) param.args(0).string() else ComponentName(
                    param.args(0).cast<Any>()!!
                ).packageName
            atsLogD("event=$event packageName=$packageName")
            val userId = param.args(1).int()
            if (userId != ActivityManagerService.MAIN_USER) {
                return@Runnable
            }
            if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
                // 不是进入前台或者后台就不处理
                return@Runnable
            }
            appStateChangeExecutor.execute(packageName, event == ACTIVITY_RESUMED)
        }).start()
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
                        stateBeforeHookMethod(this, DIFFICULT, appStateChangeExecutor)
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
                        stateBeforeHookMethod(this, DIFFICULT, appStateChangeExecutor)
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

}