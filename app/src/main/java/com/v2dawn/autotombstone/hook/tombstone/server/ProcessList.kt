package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import java.util.*


class ProcessList(private val processList: Any) {
    private val mutex = Any()
    public val processRecords = Collections.synchronizedList(ArrayList<ProcessRecord>())
    public fun reloadProcessRecord() {
        try {
            processRecords.clear()
            val processRecordList = processList.javaClass.field {
                name = FieldEnum.mLruProcessesField
            }.get(processList).list<Any>()
            for (proc in processRecordList) {
                processRecords.add(ProcessRecord(proc))
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        fun setOomAdj(packageParam: PackageParam, pid: Int, uid: Int, oomAdj: Int) {
            packageParam.apply {
                ClassEnum.ProcessListClass.clazz.method {
                    name = MethodEnum.setOomAdj
                    param(IntType, IntType, IntType)
                }.get().call(pid, uid, oomAdj)
            }

        }

    }


    init {
        reloadProcessRecord()
    }
}
