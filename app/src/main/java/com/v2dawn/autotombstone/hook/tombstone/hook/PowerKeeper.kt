package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.content.Context
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerI
import com.v2dawn.autotombstone.hook.tombstone.server.doNothing
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object PowerKeeper : YukiBaseHooker() {

    fun hook() {
        try {
            ClassEnum.PowerStateMachineClass
                .hook {
                    injectMember {
                        method {
                            name = MethodEnum.clearAppWhenScreenOffTimeOut
                            emptyParam()
                        }
                        doNothing()
                    }
                    injectMember {
                        method {
                            name = MethodEnum.clearAppWhenScreenOffTimeOutInNight
                            emptyParam()
                        }
                        doNothing()
                    }
                    injectMember {
                        method {
                            name = MethodEnum.clearUnactiveApps
                            emptyParam()
                        }
                        doNothing()
                    }
                }

            loggerI(msg = "NoActive(info) -> Disable MIUI clearApp")
        } catch (throwable: Throwable) {
            loggerI(msg = "NoActive(error) -> Disable MIUI clearApp failed: ${throwable.message}")
        }
        try {

            ClassEnum.MilletConfigClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.getEnable
                        emptyParam()
                    }
                    replaceToFalse()
                }
            }

            loggerI(msg = "NoActive(info) -> Disable millet")
        } catch (throwable: Throwable) {
            loggerI(msg = "NoActive(error) -> Disable millet failed: ${throwable.message}")
        }
    }

    override fun onHook() {
        // 禁用 millet
        loadApp("com.miui.powerkeeper") {
            hook()
        }
    }
}
