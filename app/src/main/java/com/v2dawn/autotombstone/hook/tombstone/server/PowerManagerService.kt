package com.v2dawn.autotombstone.hook.tombstone.server

import android.os.PowerManager
import com.highcapable.yukihookapi.hook.factory.field


class PowerManagerService(val raw: Any) : ForkOrigin(raw) {

    private val wakeLockMap: HashMap<String, List<WakeLock>>


    init {

        val wls = getRawData().javaClass.field {
            name = "mWakeLocks"
        }.get(getRawData()).list<PowerManager.WakeLock>()

        wakeLockMap = HashMap<String, List<WakeLock>>()
        for (wl in wls) {
            val wk = WakeLock(wl)
            wakeLockMap.computeIfAbsent(wk.packageName,ArrayList<WakeLock>())

        }
    }

}