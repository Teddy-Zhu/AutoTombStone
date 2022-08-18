package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.server.FunctionTool.isSystem
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class ApplicationInfo(private val applicationInfo: Any) {
    val FLAG_SYSTEM: Int = XposedHelpers.getStaticIntField(applicationInfo.javaClass, "FLAG_SYSTEM")
    val FLAG_UPDATED_SYSTEM_APP: Int =
        XposedHelpers.getStaticIntField(applicationInfo.javaClass, "FLAG_UPDATED_SYSTEM_APP")
    val flags: Int = XposedHelpers.getIntField(applicationInfo, FieldEnum.flags)
    val uid: Int = XposedHelpers.getIntField(applicationInfo, FieldEnum.uid)
    val processName: String =
        XposedHelpers.getObjectField(applicationInfo, FieldEnum.processName) as String
    val packageName: String =
        XposedHelpers.getObjectField(applicationInfo, FieldEnum.packageName) as String
    val isSystem: Boolean
        get() = isSystem(this)


    override fun toString(): String {
        return "ApplicationInfo{" +
                "processName='" + processName + '\'' +
                ", packageName='" + packageName + '\'' +
                '}'
    }

}
