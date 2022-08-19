package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import de.robv.android.xposed.XposedHelpers;


class ConnectionRecord(private val connectionRecord: Any) {
    private val clientProcessName: String = connectionRecord.javaClass.field {
        name = "clientProcessName"
    }.get(connectionRecord).string()

    private val clientPackageName: String = connectionRecord.javaClass.field {
        name = "clientPackageName"
    }.get(connectionRecord).string()

    override fun toString(): String {
        return "ConnectionRecord{" +
                "clientProcessName='" + clientProcessName + '\'' +
                ", clientPackageName='" + clientPackageName + '\'' +
                '}'
    }

}
