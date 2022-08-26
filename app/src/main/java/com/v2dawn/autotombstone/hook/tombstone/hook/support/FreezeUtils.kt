package com.v2dawn.autotombstone.hook.tombstone.hook.support

import android.os.Process
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.server.ProcessRecord
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.PrintWriter

class FreezeUtils(
    private val packageParam: PackageParam,
    private val freezerConfigParam: FreezerConfig
) {

    companion object {

        private const val CONT = 18
        private const val FREEZE_ACTION = 1
        private const val UNFREEZE_ACTION = 0
        private const val V1_FREEZER_FROZEN_PORCS =
            "/sys/fs/cgroup/freezer/perf/frozen/cgroup.procs"
        private const val V1_FREEZER_THAWED_PORCS =
            "/sys/fs/cgroup/freezer/perf/thawed/cgroup.procs"
    }

    private var freezerApi = false
    private var freezerVersion = 0
    private var stopSignal: Int = 19
    private var useKill: Boolean = false

    fun freezer(processRecord: ProcessRecord) {
        if (useKill) {
            Process.sendSignal(processRecord.pid, stopSignal)
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord.pid, processRecord.uid, true)
                } else {
                    freezePid(processRecord.pid, processRecord.uid)
                }
            } else {
                freezePid(processRecord.pid)
            }
        }
    }

    fun unFreezer(processRecord: ProcessRecord) {
        if (useKill) {
            Process.sendSignal(processRecord.pid, CONT)
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord.pid, processRecord.uid, false)
                } else {
                    thawPid(processRecord.pid, processRecord.uid)
                }
            } else {
                thawPid(processRecord.pid)
            }
        }
    }

    fun freezeBinder(pid: Int, frozen: Boolean) {
        packageParam.apply {
            ClassEnum.CachedAppOptimizerClass.clazz
                .method {
                    name = "freezeBinder"
                    param(IntType, Boolean::class.javaPrimitiveType!!)
                }.get().int(pid, frozen)
        }
    }

    fun setProcessFrozen(pid: Int, uid: Int, frozen: Boolean) {
        packageParam.apply {
            ClassEnum.ProcessClass
                .clazz.method {
                    name = MethodEnum.setProcessFrozen
                    param(IntType, IntType, Boolean::class.javaPrimitiveType!!)
                }.get().call(pid, uid, frozen)

        }

    }


    val frozenPids: List<Int>
        get() {
            val pids: MutableList<Int> = ArrayList()
            try {
                val reader = BufferedReader(FileReader(V1_FREEZER_FROZEN_PORCS))
                while (true) {
                    val line = reader.readLine() ?: break
                    try {
                        pids.add(line.toInt())
                    } catch (ignored: NumberFormatException) {
                    }
                }
                reader.close()
            } catch (ignored: IOException) {
            }
            return pids
        }

    fun isFrozonPid(pid: Int): Boolean {
        return frozenPids.contains(pid)
    }

    fun freezePid(pid: Int) {
        writeNode(V1_FREEZER_FROZEN_PORCS, pid)
    }

    fun thawPid(pid: Int) {
        writeNode(V1_FREEZER_THAWED_PORCS, pid)
    }

    private fun writeNode(path: String, `val`: Int) {
        try {
            val writer = PrintWriter(path)
            writer.write(Integer.toString(`val`))
            writer.close()
        } catch (e: IOException) {
            atsLogE("Freezer V1 failed: ${e.message}", e = e)
        }
    }

    private fun setFreezeAction(pid: Int, uid: Int, action: Boolean) {
        val path = "/sys/fs/cgroup/uid_$uid/pid_$pid/cgroup.freeze"
        try {
            val writer = PrintWriter(path)
            if (action) {
                writer.write(Integer.toString(FREEZE_ACTION))
            } else {
                writer.write(Integer.toString(UNFREEZE_ACTION))
            }
            writer.close()
        } catch (e: IOException) {
            atsLogE("Freezer V2 failed: ${e.message}", e = e)
        }
    }

    fun thawPid(pid: Int, uid: Int) {
//        freezeBinder(pid, false)
        setFreezeAction(pid, uid, false)
    }

    fun freezePid(pid: Int, uid: Int) {
//        freezeBinder(pid, true)
        setFreezeAction(pid, uid, true)
    }

    fun kill(pid: Int) {
        Process.killProcess(pid)
    }


    fun loadFreezer(freezerConfig: FreezerConfig) {
        val defaultFreezerVersion: String = freezerConfig.getFreezerVersion()
        when (defaultFreezerVersion) {
            FreezerConfig.API -> {
                freezerApi = true
                this.freezerVersion = 2
            }
            FreezerConfig.V2 -> {
                freezerApi = false
                this.freezerVersion = 2
            }
            FreezerConfig.V1 -> {
                freezerApi = false
                this.freezerVersion = 1
            }
            else -> {
                freezerApi = false
                this.freezerVersion = 1
            }
        }
        stopSignal = freezerConfig.killSignal
        useKill = freezerConfig.isUseKill
        if (useKill) {
            atsLogI("Kill -$stopSignal")
        } else {
            atsLogI("Freezer $freezerVersion")
        }
    }

    init {
        loadFreezer(freezerConfigParam)
    }
}