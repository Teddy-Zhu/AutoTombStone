package com.v2dawn.autotombstone.hook.tombstone.server;

import android.os.Binder
import android.os.UserHandle
import android.util.SparseArray
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD


class ActiveServices(val activeServices: Any) {

    private val mServiceMap: SparseArray<Any> = activeServices.javaClass.field {
        name = "mServiceMap"
    }.get(activeServices).cast<SparseArray<Any>>()!!

    private fun killServicesLocked(processRecord: ProcessRecord, restart: Boolean) {
        activeServices.javaClass.method {
            name = MethodEnum.killServicesLocked
            param(ClassEnum.ProcessRecordClass, Boolean::class.javaPrimitiveType!!)
        }.get(activeServices).call(processRecord, restart)
    }

    fun getServiceMap(uid: Int): Any {
        val uid = UserHandle::class.java.method {
            name = "getUserId"
            param(IntType)
        }.get().invoke<Int>(uid)!!

        return mServiceMap.get(uid)!!
    }

    public fun ensureNotStartingBackground(serviceMap: Any, serviceRecord: Any) {
        atsLogD("call ensureNotStartingBackground,class:${serviceMap.javaClass}")
        serviceMap.javaClass.method {
            name = "ensureNotStartingBackgroundLocked"
            param(ClassEnum.ServiceRecordClass)
        }.get(serviceMap).invoke<Any>(serviceRecord)
    }

    fun stopInBackgroundLocked(uid: Int) {
        activeServices.javaClass.method {
            name = "stopInBackgroundLocked"
            param(IntType)
        }.get(activeServices).call(uid)
    }

    fun killServicesLocked(processRecord: ProcessRecord) {
        killServicesLocked(processRecord, false)
    }
}
