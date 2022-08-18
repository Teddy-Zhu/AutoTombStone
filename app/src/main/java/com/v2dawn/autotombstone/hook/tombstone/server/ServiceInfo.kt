package com.v2dawn.autotombstone.hook.tombstone.server;

import de.robv.android.xposed.XposedHelpers;


class ServiceInfo(private val serviceInfo: Any) {
    private val name: String = XposedHelpers.getObjectField(serviceInfo, "name") as String
    override fun toString(): String {
        return "ServiceInfo{" +
                "name='" + name + '\'' +
                '}'
    }

}
