package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum


class ActiveServices(private val activeServices: Any) {
    fun killServicesLocked(processRecord: ProcessRecord) {
        activeServices.javaClass.method {
            name = MethodEnum.killServicesLocked
            param(ClassEnum.ProcessRecordClass, Boolean::class.javaPrimitiveType!!)
        }.get(activeServices).call(processRecord, true)
    }

}
