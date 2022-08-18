package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers


class ComponentName(private val componentName: Any) {
    private val packageName: String = XposedHelpers.getObjectField(componentName, FieldEnum.mPackage) as String
    override fun toString(): String {
        return "ComponentName{" +
                "packageName='" + packageName + '\'' +
                '}'
    }

}
