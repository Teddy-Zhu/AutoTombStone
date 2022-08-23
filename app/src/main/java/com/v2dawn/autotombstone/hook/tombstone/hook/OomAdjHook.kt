package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerW
import com.highcapable.yukihookapi.hook.param.HookParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.LongType
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.server.*
import com.v2dawn.autotombstone.hook.tombstone.support.*
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryBlackSysAppsList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteAppList
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.queryWhiteProcessesList


class OomAdjHook :
    YukiBaseHooker() {


    companion object {
        const val Android_S = 1
        const val Android_Q_R = 2
        const val Color = 3
    }

    fun computeOomAdj(param: HookParam, type: Int) {
        val processRecord: ProcessRecord = when (type) {
            Android_S -> ProcessStateRecord(param.instance).processRecord
            Android_Q_R, Color -> ProcessRecord(
                param.args(0).any()!!
            )
            else -> return
        }
        // 如果进程或者应用信息为空就不处理
        if (processRecord.applicationInfo == null) {
            return
        }
        if (processRecord.userId != ActivityManagerService.MAIN_USER) {
            return
        }
        val applicationInfo: ApplicationInfo = processRecord.applicationInfo
        val packageName: String = processRecord.applicationInfo.packageName ?: return
        // 如果包名为空就不处理(猜测系统进程可能为空)
        val processName: String = processRecord.processName ?: return
        // 如果进程名称等于包名就跳过
        if (!processName.startsWith(packageName)) {
            return
        }
        // 如果是系统应用并且不是系统黑名单就不处理
        if (applicationInfo.uid < 10000 || applicationInfo.isSystem && !queryBlackSysAppsList()
                .contains(packageName)
        ) {
            return
        }
        // 如果是前台应用就不处理
        if (!AppStateChangeExecutor.backgroundApps.contains(packageName)) {
            return
        }
        val finalCurlAdj: Int

        // 如果白名单应用或者进程就不处理
        if (queryWhiteAppList().contains(packageName) || queryWhiteProcessesList()
                .contains(processName)
        ) {
            finalCurlAdj = if (processName == packageName) 500 else 700
        } else {
            val curAdj = if (processName == packageName) 700 else 900
            finalCurlAdj = curAdj + AppStateChangeExecutor.getBackgroundIndex(packageName)
        }
        atsLogD("$processName -> $finalCurlAdj")
        when (type) {
            Android_S -> {
                param.args(0).set(finalCurlAdj)
            }
            Android_Q_R -> processRecord.setCurAdj(finalCurlAdj)
            Color -> ProcessList.setOomAdj(
                this,
                processRecord.pid,
                processRecord.uid,
                finalCurlAdj
            )
            else -> {}
        }
    }

    override fun onHook() {
        // Hook oom_adj
        if (!prefs(ConfigConst.COMMON_NAME)
                .get(ConfigConst.DISABLE_OOM)
        ) {
            val colorOs = prefs(ConfigConst.COMMON_NAME)
                .get(ConfigConst.ENABLE_COLOROS_OOM)
            if (!colorOs && (Build.MANUFACTURER.equals("OPPO") || Build.MANUFACTURER.equals("OnePlus"))) {
                atsLogW("If you are using ColorOS");
                atsLogW("You can create file color.os");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (colorOs) {
                    atsLogI("Hello ColorOS");
                    ClassEnum.OomAdjusterClass.hook {
                        injectMember {
                            method {
                                name = MethodEnum.computeOomAdjLSP
                                param(
                                    ClassEnum.ProcessRecordClass,
                                    IntType,
                                    ClassEnum.ProcessRecordClass,
                                    Boolean::class.javaPrimitiveType!!,
                                    LongType, Boolean::class.javaPrimitiveType!!,
                                    Boolean::class.javaPrimitiveType!!
                                )
                            }
                            afterHook {
                                computeOomAdj(this, Color)
                            }
                        }
                    }

                } else {
                    ClassEnum.ProcessStateRecordClass.hook {
                        injectMember {
                            method {
                                name = MethodEnum.setCurAdj
                                param(IntType)
                            }
                            beforeHook {
                                computeOomAdj(this, Android_S)
                            }

                        }
                    }
                }
                atsLogI("Auto lmk");
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                ClassEnum.OomAdjusterClass.hook {
                    injectMember {
                        method {
                            name = MethodEnum.applyOomAdjLocked
                            param(
                                ClassEnum.ProcessRecordClass,
                                Boolean::class.javaPrimitiveType!!,
                                LongType, LongType
                            )
                        }
                        beforeHook {
                            computeOomAdj(this, Android_Q_R)
                        }
                    }
                }
                atsLogI("Auto lmk");
            }
        }


    }
}
