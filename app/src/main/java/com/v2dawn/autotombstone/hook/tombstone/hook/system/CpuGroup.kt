package com.v2dawn.autotombstone.hook.tombstone.hook.system;

import java.io.IOException;

class CpuGroup protected constructor(path: String?) : ProcFile(path) {
    val isForeground: Boolean
        get() = !content!!.contains("background")

    companion object {
        @Throws(IOException::class)
        operator fun get(pid: Int): CpuGroup {
            return CpuGroup(String.format("/proc/%d/cgroup", pid))
        }
    }
}
