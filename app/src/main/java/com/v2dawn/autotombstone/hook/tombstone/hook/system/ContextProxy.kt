package com.v2dawn.autotombstone.hook.tombstone.hook.system;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Binder;
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers

class ContextProxy(base: Context?, private val tag: String) : ContextWrapper(base) {
    private val PER_USER_RANGE =
        XposedHelpers.getStaticIntField(UserHandle::class.java, "PER_USER_RANGE")

    override fun enforceCallingPermission(permission: String, message: String?) {
        val callingUid = Binder.getCallingUid().toLong()
        if (permission == "android.permission.CHANGE_APP_IDLE_STATE" && isSystemCall(callingUid)) {
            return
        }
        super.enforceCallingPermission(permission, message)
    }

    private fun isSystemCall(uid: Long): Boolean {
        return uid == 1000L || uid > PER_USER_RANGE && uid % PER_USER_RANGE == 1000L
    }

}
