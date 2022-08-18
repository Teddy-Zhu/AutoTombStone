package com.v2dawn.autotombstone.hook.tombstone.server;

import android.os.Build
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers

class ProcessRecord(val processRecord: Any) {
    val uid: Int
    var pid = 0
    val processName: String?
    val userId: Int
    val applicationInfo: ApplicationInfo?
    private val processServiceRecords: MutableList<ProcessServiceRecord> = ArrayList()


    fun setCurAdj(curAdj: Int) {
        XposedHelpers.setIntField(processRecord, FieldEnum.curAdj, curAdj)
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
            pid = XposedHelpers.getIntField(processRecord, FieldEnum.mPidField)
        } else {
            pid = XposedHelpers.getIntField(processRecord, FieldEnum.pidField)
        }
        uid = XposedHelpers.getIntField(processRecord, FieldEnum.uidField)
        processName = XposedHelpers.getObjectField(processRecord, FieldEnum.processNameField) as String
        userId = XposedHelpers.getIntField(processRecord, FieldEnum.userIdField)
        applicationInfo =
            ApplicationInfo(XposedHelpers.getObjectField(processRecord, FieldEnum.infoField))
        val ms = XposedHelpers.getObjectField(processRecord, FieldEnum.mServicesField)
        processServiceRecords.clear()
        if (ms is Collection<*>) {
            for (o in ms) {
                processServiceRecords.add(ProcessServiceRecord(o!!))
            }
        } else {
            processServiceRecords.add(ProcessServiceRecord(ms))
        }
    }
}
