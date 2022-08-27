package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ApplicationInfoClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.*
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList

class ANRHook : YukiBaseHooker() {

    private fun needHookApplication(
        userId: Int?,
        processName: String?,
        application: ApplicationInfo?
    ): Boolean {
        if (application == null) {
            atsLogI("allow anr reason:empty app info")
            return false
        }
        // 是否系统进程
        val isSystem: Boolean = application.isSystem()
        // 进程对应包名
        val packageName: String = application.packageName
        val isNotBlackSystem: Boolean = queryBlackSysAppsList().contains(packageName)
        val isWhiteApp: Boolean = queryWhiteAppList().contains(packageName)
        val isImportSystemApp = application.isImportantSystem()
        if (isImportSystemApp) {
            atsLogI("[${packageName}] allow anr reason:important sys app")

            return false
        }
        if (userId != null) {
            if (userId == ActivityManagerService.MAIN_USER) {
                atsLogI("[${packageName}] allow anr reason:main user")
                return false
            }
        }

        // 系统应用并且不是系统黑名单
        if (isSystem && isNotBlackSystem) {
            atsLogI("[${packageName}] allow anr reason:system app")

            return false
        }
        if (isWhiteApp && !AppStateChangeExecutor.backgroundApps.contains(packageName)) {
            atsLogI("[${packageName}] allow anr reason:whiteApp")
            return false
        }

        atsLogD("${(processName ?: packageName)} keep no anr")
        // 不处理
        return true
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
                        atsLogD("trigger anr")
                        val applicationInfo = args(2).cast<ApplicationInfo>()
                        // ANR进程为空就不处理
                        val arg0 = args(0).cast<Any>()
                        var processName: String? = null
                        var userId: Int? = null
                        if (arg0 != null) {
                            // ANR进程
                            val processRecord = ProcessRecord(arg0)
                            processName = processRecord.processName
                            userId = processRecord.userId
                        }

                        if (needHookApplication(
                                userId,
                                processName,
                                applicationInfo
                            )
                        ) {
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
