package com.v2dawn.autotombstone.hook.tombstone.server;


import android.content.ComponentName
import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum


class ServiceRecord(val serviceRecord: Any) {
    val isForeground: Boolean =
        serviceRecord.javaClass.field {
            name = "isForeground"
        }.get(serviceRecord).boolean()
    val processName: String =
        serviceRecord.javaClass.field {
            name = FieldEnum.processNameField
        }.get(serviceRecord).string()
    private val name: ComponentName? = serviceRecord.javaClass.field {
        name = "name"
    }.get(serviceRecord).cast<ComponentName>()
    val serviceInfo: ServiceInfo = ServiceInfo(
        serviceRecord.javaClass.field {
            name = "serviceInfo"
        }.get(serviceRecord).cast()!!
    )

    public fun setDelay(delay: Boolean) {
        serviceRecord.javaClass.field {
            name = "delayed"
        }.get(serviceRecord).set(delay)
    }

    override fun toString(): String {
        return "ServiceRecord{" +
                "isForeground=" + isForeground +
                ", processName='" + processName + '\'' +
                ", name=" + name +
                ", serviceInfo=" + serviceInfo +
                '}'
    }

}
