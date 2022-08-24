package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field


class ServiceInfo(private val serviceInfo: Any) {
    val name: String = serviceInfo.javaClass.field {
        name = "name"
        superClass(true)
    }.get(serviceInfo).string()

    override fun toString(): String {
        return "ServiceInfo{" +
                "name='" + name + '\'' +
                '}'
    }

}
