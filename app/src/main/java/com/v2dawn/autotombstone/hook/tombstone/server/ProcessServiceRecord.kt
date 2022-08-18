package com.v2dawn.autotombstone.hook.tombstone.server;

import de.robv.android.xposed.XposedHelpers


class ProcessServiceRecord(processServiceRecord: Any) {
    private var processServiceRecord: Any? = null
    val mExecutingServices: MutableList<ServiceRecord> = ArrayList()
    val mServices: MutableList<ServiceRecord> = ArrayList()
    val mConnections: MutableList<ConnectionRecord> = ArrayList()
    private var mHasForegroundServices = false
    private fun loadConnections(serviceRecords: MutableList<ConnectionRecord>, fieldName: String) {
        serviceRecords.clear()
        val services = XposedHelpers.getObjectField(
            processServiceRecord,
            fieldName
        ) as Collection<*>
        for (service in services) {
            serviceRecords.add(ConnectionRecord(service!!))
        }
    }

    private fun loadServices(serviceRecords: MutableList<ServiceRecord>, fieldName: String) {
        serviceRecords.clear()
        val services = XposedHelpers.getObjectField(
            processServiceRecord,
            fieldName
        ) as Collection<*>
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
        if ("com.android.server.am.ServiceRecord" == processServiceRecord.javaClass.name) {
            this.processServiceRecord = null
            mServices.add(ServiceRecord(processServiceRecord))
        } else {
            this.processServiceRecord = processServiceRecord
            mHasForegroundServices =
                XposedHelpers.getBooleanField(processServiceRecord, "mHasForegroundServices")
            loadServices(mServices, "mServices")
            loadServices(mExecutingServices, "mExecutingServices")
            loadConnections(mConnections, "mConnections")
        }
    }
}
