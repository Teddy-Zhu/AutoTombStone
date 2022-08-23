package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum


class ComponentName(private val componentName: Any) {


    val packageName = componentName.javaClass.field {
        name = FieldEnum.mPackageField
    }.get(componentName).string()

    override fun toString(): String {
        return "ComponentName{" +
                "packageName='" + packageName + '\'' +
                '}'
    }

}
