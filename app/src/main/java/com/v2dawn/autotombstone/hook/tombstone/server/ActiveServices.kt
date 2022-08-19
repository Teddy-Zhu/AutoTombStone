package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XposedHelpers


class ActiveServices(val activeServices: Any) {
    private fun killServicesLocked(processRecord: ProcessRecord, restart: Boolean) {
        activeServices.javaClass.method {
            name = MethodEnum.killServicesLocked
            param(ClassEnum.ProcessRecordClass, Boolean::class.javaPrimitiveType!!)
        }.get(activeServices).call(processRecord, restart)
    }

    fun stopInBackgroundLocked(uid: Int) {
        activeServices.javaClass.method {
            name="stopInBackgroundLocked"
            param(IntType)
        }.get(activeServices).call(uid)
    }

    fun killServicesLocked(processRecord: ProcessRecord) {
        killServicesLocked(processRecord, false)
    }
}
