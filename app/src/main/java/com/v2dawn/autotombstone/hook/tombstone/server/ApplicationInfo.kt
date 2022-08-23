package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FunctionTool.isSystem
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum


class ApplicationInfo(val applicationInfo: Any) {
    val FLAG_SYSTEM: Int;
    val FLAG_UPDATED_SYSTEM_APP: Int
    val flags: Int
    val uid: Int
    val processName: String?
    val packageName: String?
    val isSystem: Boolean
        get() = isSystem(this)

    init {
        FLAG_SYSTEM = applicationInfo.javaClass.field {
            name = "FLAG_SYSTEM"
        }.get(applicationInfo).int()
        FLAG_UPDATED_SYSTEM_APP = applicationInfo.javaClass.field {
            name = "FLAG_UPDATED_SYSTEM_APP"
        }.get(applicationInfo).int()

        flags = applicationInfo.javaClass.field {
            name = FieldEnum.flagsField
        }.get(applicationInfo).int()
        uid = applicationInfo.javaClass.field {
            name = FieldEnum.uidField
        }.get(applicationInfo).int()
        processName = applicationInfo.javaClass.field {
            name = FieldEnum.processNameField
        }.get(applicationInfo).cast()
        packageName = applicationInfo.javaClass.field {
            name = FieldEnum.packageNameField
            superClass(true)
        }.get(applicationInfo).cast()
    }

    override fun toString(): String {
        return "ApplicationInfo{" +
                "processName='" + processName + '\'' +
                ", packageName='" + packageName + '\'' +
                '}'
    }

}
