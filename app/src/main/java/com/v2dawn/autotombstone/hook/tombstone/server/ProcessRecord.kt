package com.v2dawn.autotombstone.hook.tombstone.server;

import android.content.pm.ApplicationInfo
import android.os.Build
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum

class ProcessRecord(val processRecord: Any) {
    val uid: Int
    var pid = 0
    val processName: String?
    val userId: Int
    val applicationInfo: ApplicationInfo?
    public val processServiceRecords: MutableList<ProcessServiceRecord>

    val mWindowProcessController: WindowProcessController

    fun setCurAdj(curAdj: Int) {
        processRecord.javaClass
            .field {
                name = FieldEnum.curAdjField
            }.get(processRecord).set(curAdj)
    }

    fun hasRunningActivity(packageName: String): Boolean {
        return mWindowProcessController.hasRunningActivity(packageName)
    }

    override fun toString(): String {
        return "ProcessRecord{" +
                "uid=" + uid +
                ", pid=" + pid +
                ", processName='" + processName + '\'' +
                ", userId=" + userId +
                ", applicationInfo=" + applicationInfo +
                ", processServiceRecords=" + processServiceRecords +
                '}'
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            pid = processRecord.javaClass.field { name = FieldEnum.mPidField }
                .get(processRecord).int()
        } else {
            pid = processRecord.javaClass.field { name = FieldEnum.pidField }
                .get(processRecord).int()
        }
        uid = processRecord.javaClass.field { name = FieldEnum.uidField }
            .get(processRecord).int()
        processName = processRecord.javaClass.field { name = FieldEnum.processNameField }
            .get(processRecord).string()

        userId = processRecord.javaClass.field { name = FieldEnum.userIdField }
            .get(processRecord).int()
        applicationInfo = processRecord.javaClass.field { name = FieldEnum.infoField }
            .get(processRecord).cast<ApplicationInfo>()

        mWindowProcessController = WindowProcessController(
            processRecord.javaClass.method {
                name = "getWindowProcessController"
                emptyParam()
            }.get(processRecord).invoke<Any>()!!
        )

        val ms = processRecord.javaClass.field { name = FieldEnum.mServicesField }
            .get(processRecord).cast<Any>()!!

        processServiceRecords = ArrayList()
        if (ms is Collection<*>) {
            for (o in ms) {
                processServiceRecords.add(ProcessServiceRecord(o!!))
            }
        } else {
            processServiceRecords.add(ProcessServiceRecord(ms))
        }
    }
}
