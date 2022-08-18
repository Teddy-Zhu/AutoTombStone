package com.v2dawn.autotombstone.hook.tombstone.server;

import de.robv.android.xposed.XposedHelpers;


class ConnectionRecord(private val connectionRecord: Any) {
    private val clientProcessName: String = XposedHelpers.getObjectField(connectionRecord, "clientProcessName") as String
    private val clientPackageName: String = XposedHelpers.getObjectField(connectionRecord, "clientPackageName") as String
    override fun toString(): String {
        return "ConnectionRecord{" +
                "clientProcessName='" + clientProcessName + '\'' +
                ", clientPackageName='" + clientPackageName + '\'' +
                '}'
    }

}
