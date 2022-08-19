package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import de.robv.android.xposed.XposedHelpers;


class ServiceInfo(private val serviceInfo: Any) {
    private val name: String = serviceInfo.javaClass.field {
        name = "name"
    }.get(serviceInfo).string()

    override fun toString(): String {
        return "ServiceInfo{" +
                "name='" + name + '\'' +
                '}'
    }

}
