package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ApplicationInfoClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.*
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList

class ANRHook : YukiBaseHooker() {

    private fun needHook(param: HookParam): Boolean {
        // ANR进程为空就不处理
        val arg0 = param.args(0).cast<Any>() ?: return false
        // ANR进程
        val processRecord = ProcessRecord(arg0)
        if (processRecord.applicationInfo == null) {
            return false
        }
        // 是否系统进程
        val isSystem: Boolean = processRecord.applicationInfo.isSystem()
        // 进程对应包名
        val packageName: String = processRecord.applicationInfo.packageName
        val isNotBlackSystem: Boolean = queryBlackSysAppsList().contains(packageName)
        val isWhiteApp: Boolean = queryWhiteAppList().contains(packageName)
        val isImportSystemApp = processRecord.applicationInfo.isImportantSystem()
        if (isImportSystemApp) {
            return false
        }
        // 系统应用并且不是系统黑名单
        if (isSystem && isNotBlackSystem) {
            return false
        }
        if (isWhiteApp) {
            return false
        }
        atsLogD("Keep ${(processRecord.processName ?: packageName)}")
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
                        if (needHook(this@beforeHook)) {
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
