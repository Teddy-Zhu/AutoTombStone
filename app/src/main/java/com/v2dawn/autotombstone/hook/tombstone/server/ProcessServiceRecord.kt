package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import de.robv.android.xposed.XposedHelpers


class ProcessServiceRecord(val processServiceRecord: Any) {
    val mExecutingServices: MutableList<ServiceRecord> = ArrayList()
    public val mServices: MutableList<ServiceRecord> = ArrayList()
    val mConnections: MutableList<ConnectionRecord> = ArrayList()
    private var mHasForegroundServices = false

    private fun loadConnections(serviceRecords: MutableList<ConnectionRecord>, fieldName: String) {
        serviceRecords.clear()
        val services = processServiceRecord.javaClass.field {
            name = fieldName
        }.get(processServiceRecord).cast<Collection<*>>()!!
        for (service in services) {
            serviceRecords.add(ConnectionRecord(service!!))
        }
    }

    private fun loadServices(serviceRecords: MutableList<ServiceRecord>, fieldName: String) {
        serviceRecords.clear()
        val services = processServiceRecord.javaClass.field {
            name = fieldName
        }.get(processServiceRecord).cast<Collection<*>>()!!
        for (service in services) {
            serviceRecords.add(ServiceRecord(service!!))
        }
    }

    override fun toString(): String {
        return "ProcessServiceRecord{" +
                "mExecutingServices=" + mExecutingServices +
                ", mServices=" + mServices +
                ", mConnections=" + mConnections +
                ", mHasForegroundServices=" + mHasForegroundServices +
                '}'
    }

    init {
        if (ClassEnum.ServiceRecordClass == processServiceRecord.javaClass.name) {
            mServices.add(ServiceRecord(processServiceRecord))
        } else {
            mHasForegroundServices = processServiceRecord.javaClass.field {
                name = "mHasForegroundServices"
            }.get(processServiceRecord).boolean()

            loadServices(mServices, "mServices")
            loadServices(mExecutingServices, "mExecutingServices")
            loadConnections(mConnections, "mConnections")
        }
    }
}
