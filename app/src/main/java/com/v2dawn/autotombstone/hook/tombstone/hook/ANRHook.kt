package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ApplicationInfoClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.server.WindowProcessController
import com.v2dawn.autotombstone.hook.tombstone.support.*
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList

class ANRHook : YukiBaseHooker() {
    private fun needHookApplicationNew(
        processName: String?,
        application: ApplicationInfo?
    ): Boolean {
        if (application == null) {
            atsLogI("allow anr reason:empty app info")
            return false
        }

        if (AppStateChangeExecutor.backgroundApps.contains(application.packageName)) {
            atsLogD("[${processName}|${application.packageName}] keep no anr ,start refreeze app")

            AppStateChangeExecutor.instance?.controlApp(packageName)
            return true
        }
        atsLogI("[${application.packageName}] allow anr reason:not freeze app")
        // 不处理
        return false
    }

    private fun resolveAnr(
        processRecordRaw: Any?,
        applicationInfo: ApplicationInfo?,
        referer: String
    ): Boolean {
        atsLogD("trigger anr $referer")
        // ANR进程为空就不处理
        var processName: String? = null

        var currentApplication = applicationInfo
        atsLogD("trigger anr $referer, app:${currentApplication?.packageName}")
        if (processRecordRaw != null) {
            // ANR进程
            val processRecord = ProcessRecord(processRecordRaw)
            processName = processRecord.processName
            atsLogD("trigger anr $referer, pr app:${processRecord.applicationInfo?.packageName}")

            if (currentApplication == null) {
                currentApplication = processRecord.applicationInfo
            }
        }

        return needHookApplicationNew(processName, currentApplication)

    }

    override fun onHook() {
        // Hook ANR
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            ClassEnum.AnrHelperClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.appNotResponding
                        param(
                            ClassEnum.ProcessRecordClass, StringType,
                            ApplicationInfoClass,
                            StringType, ClassEnum.WindowProcessControllerClass,
                            Boolean::class.javaPrimitiveType!!,
                            StringType
                        )
                    }
                    beforeHook {
                        var applicationInfo = args(2).cast<ApplicationInfo>()
                        val arg0 = args(0).cast<Any>()

                        if (resolveAnr(
                                arg0,
                                applicationInfo,
                                "AnrHelperClass"
                            )
                        ) {
                            resultNull()
                        }
                    }

                }
            }

            ClassEnum.AnrRecordClass.hook {
                injectMember {
                    method {
                        name = "appNotResponding"
                        param(Boolean::class.javaPrimitiveType!!)
                    }
                    beforeHook {
                        var applicationInfo = instance.javaClass.field {
                            name = "mAppInfo"
                        }.get(instance).cast<ApplicationInfo>()
                        // ANR进程为空就不处理
                        val arg0 = instance.javaClass.field {
                            name = "mApp"
                        }.get(instance).any()

                        if (resolveAnr(
                                arg0,
                                applicationInfo,
                                "AnrRecordClass"
                            )
                        ) {
                            resultNull()
                        }
                    }
                }
            }
            ClassEnum.ProcessErrorStateRecordClass.hook {
                injectMember {
                    method {
                        name = "appNotResponding"
                        param(
                            StringType,
                            ApplicationInfoClass,
                            StringType,
                            ClassEnum.WindowProcessControllerClass,
                            Boolean::class.javaPrimitiveType!!,
                            StringType,
                            Boolean::class.javaPrimitiveType!!
                        )
                    }
                    beforeHook {
                        val processRecordRaw = instance.javaClass.field {
                            name = "mApp"
                        }.get(instance).any()
                        var applicationInfo = args(1).cast<ApplicationInfo>()

                        if (resolveAnr(processRecordRaw,  applicationInfo,"ProcessErrorStateRecordClass")) {
                            resultNull()
                        }
                    }
                }
            }
            atsLogI("Auto keep process")
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            ClassEnum.ProcessRecordClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.appNotResponding
                        param(
                            StringType, ApplicationInfoClass,
                            StringType, ClassEnum.WindowProcessControllerClass,
                            Boolean::class.javaPrimitiveType!!,
                            StringType
                        )
                        doNothing()
                    }
                }
            }
            atsLogI("Android Q")
            atsLogI("Force keep process")
        } else {
            ClassEnum.AppErrorsClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.appNotResponding
                        param(
                            ClassEnum.ProcessRecordClass,
                            ClassEnum.ActivityRecordClass,
                            ClassEnum.ActivityRecordClass,
                            Boolean::class.javaPrimitiveType!!,
                            StringType
                        )
                        doNothing()
                    }
                }
            }
            atsLogI("Android N-P")
            atsLogI("Force keep process")
        }

    }

}
