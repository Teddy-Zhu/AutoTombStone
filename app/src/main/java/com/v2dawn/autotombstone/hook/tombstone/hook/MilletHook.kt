package com.v2dawn.autotombstone.hook.tombstone.hook;

import de.robv.android.xposed.XC_MethodReplacement

class MilletHook : XC_MethodReplacement() {
    @Throws(Throwable::class)
    override fun replaceHookedMethod(param: MethodHookParam): Any {
        return false
    }
}
