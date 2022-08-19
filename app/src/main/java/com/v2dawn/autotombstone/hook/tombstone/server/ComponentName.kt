package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers


class ComponentName(private val componentName: Any) {


    private val packageName = componentName.javaClass.field {
        name = FieldEnum.mPackageField
    }.get(componentName).string()

    override fun toString(): String {
        return "ComponentName{" +
                "packageName='" + packageName + '\'' +
                '}'
    }

}
