package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XposedHelpers
import java.util.*


class ProcessList(private val processList: Any) {
    private val mutex = Any()
    private val processRecords = Collections.synchronizedList(ArrayList<ProcessRecord>())
    fun reloadProcessRecord() {
        try {
            processRecords.clear()
            val processRecordList = XposedHelpers.getObjectField(
                processList, FieldEnum.mLruProcesses
            ) as kotlin.collections.List<*>
            for (proc in processRecordList) {
                val processRecord = ProcessRecord(proc!!)
                processRecords.add(processRecord)
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        fun setOomAdj(classLoader: ClassLoader?, pid: Int, uid: Int, oomAdj: Int) {
            val ProcessList = XposedHelpers.findClass(ClassEnum.ProcessList, classLoader)
            XposedHelpers.callStaticMethod(ProcessList, MethodEnum.setOomAdj, pid, uid, oomAdj)
        }
    }

    init {
        reloadProcessRecord()
    }
}
