package com.v2dawn.autotombstone.hook.tombstone.server;


import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class ServiceRecord(private val serviceRecord: Any) {
    var isForeground: Boolean = XposedHelpers.getBooleanField(serviceRecord, "isForeground")
    private val processName: String = XposedHelpers.getObjectField(serviceRecord, FieldEnum.processName) as String
    private val name: ComponentName = ComponentName(
        XposedHelpers.getObjectField(
            serviceRecord, "name"
        )
    )
    private val serviceInfo: ServiceInfo = ServiceInfo(
        XposedHelpers.getObjectField(
            serviceRecord, "serviceInfo"
        )
    )

    override fun toString(): String {
        return "ServiceRecord{" +
                "isForeground=" + isForeground +
                ", processName='" + processName + '\'' +
                ", name=" + name +
                ", serviceInfo=" + serviceInfo +
                '}'
    }

}
