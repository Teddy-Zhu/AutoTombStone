package com.v2dawn.autotombstone.hook.tombstone.hook;


import com.highcapable.yukihookapi.hook.log.loggerD
import de.robv.android.xposed.XC_MethodHook


class TestHook(private val TAG: String) : XC_MethodHook() {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
        loggerD(msg = TAG)
    }
}
