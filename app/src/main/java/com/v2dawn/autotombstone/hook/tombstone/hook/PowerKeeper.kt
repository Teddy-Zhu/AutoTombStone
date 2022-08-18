package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.content.Context
import com.highcapable.yukihookapi.hook.log.loggerI
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class PowerKeeper : IAppHook {
    override fun hook(packageParam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                ClassEnum.PowerStateMachine,
                packageParam.classLoader,
                MethodEnum.clearAppWhenScreenOffTimeOut,
                XC_MethodReplacement.DO_NOTHING
            )
            XposedHelpers.findAndHookMethod(
                ClassEnum.PowerStateMachine,
                packageParam.classLoader,
                MethodEnum.clearAppWhenScreenOffTimeOutInNight,
                XC_MethodReplacement.DO_NOTHING
            )
            XposedHelpers.findAndHookMethod(
                ClassEnum.PowerStateMachine, packageParam.classLoader, MethodEnum.clearUnactiveApps,
                Context::class.java, XC_MethodReplacement.DO_NOTHING
            )
            loggerI(msg = "NoActive(info) -> Disable MIUI clearApp")
        } catch (throwable: Throwable) {
            loggerI(msg = "NoActive(error) -> Disable MIUI clearApp failed: " + throwable.message)
        }
        try {
            XposedHelpers.findAndHookMethod(
                ClassEnum.MilletConfig, packageParam.classLoader, MethodEnum.getEnable,
                Context::class.java, MilletHook()
            )
            loggerI(msg = "NoActive(info) -> Disable millet")
        } catch (throwable: Throwable) {
            loggerI(msg = "NoActive(error) -> Disable millet failed: " + throwable.message)
        }
    }
}
