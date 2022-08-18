package com.v2dawn.autotombstone.hook.tombstone.hook.system;

import java.io.IOException;

class CpuSet protected constructor(path: String?) : ProcFile(path) {
    //foreground ||  top-app
    val isForeground: Boolean
        get() = !content!!.contains("background")

    companion object {
        @Throws(IOException::class)
        operator fun get(pid: Int): CpuSet {
            return CpuSet(String.format("/proc/%d/cpuset", pid))
        }
    }
}
