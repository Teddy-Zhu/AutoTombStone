package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.android.ApplicationInfoClass
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.hook.tombstone.server.ActivityManagerService
import com.v2dawn.autotombstone.hook.tombstone.server.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.server.doNothing
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum

object ANRHook : YukiBaseHooker() {

    private fun needHook(param: HookParam): Boolean {
        // ANR进程为空就不处理
        val arg0 = param.args().first().cast<Any>() ?: return false
        // ANR进程
        val processRecord = ProcessRecord(arg0)
        // 是否系统进程
        val isSystem: Boolean = processRecord.applicationInfo?.isSystem ?: false
        // 进程对应包名
        val packageName: String = processRecord.applicationInfo!!.packageName
        val isNotBlackSystem: Boolean = queryBlackSysAppsList().contains(packageName)

        // 系统应用并且不是系统黑名单
        if (isSystem && isNotBlackSystem || processRecord.userId != ActivityManagerService.MAIN_USER) {
            return false
        }
        loggerD(msg = "Keep ${(processRecord.processName ?: packageName)}")
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

            loggerI(msg = "Auto keep process")
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
            loggerI(msg = "Android Q")
            loggerI(msg = "Force keep process")
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
            loggerI(msg = "Android N-P")
            loggerI(msg = "Force keep process")
        }

    }

}
