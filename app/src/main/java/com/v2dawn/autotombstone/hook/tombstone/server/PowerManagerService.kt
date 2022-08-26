package com.v2dawn.autotombstone.hook.tombstone.server

import android.os.IBinder
import android.os.PowerManager
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IBinderClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD


class PowerManagerService(raw: Any) : ForkOrigin(raw) {

//    private val wakeLockMap: HashMap<String, ArrayList<WakeLock>>


    companion object {
        var instance: PowerManagerService? = null
    }

    init {

//        val wls = getRawData().javaClass.field {
//            name = "mWakeLocks"
//        }.get(getRawData()).list<PowerManager.WakeLock>()
//
//        wakeLockMap = HashMap()
//        for (wl in wls) {
//            val wk = WakeLock(wl)
//            wakeLockMap.computeIfAbsent(wk.packageName) {
//                ArrayList<WakeLock>()
//            }.add(wk)
//        }
        instance = this
    }


    private fun reloadWakeLock(packageName: String): ArrayList<WakeLock> {
        val wls = getRawData().javaClass.field {
            name = "mWakeLocks"
        }.get(getRawData()).list<Any>()

        val cpy = ArrayList<Any>(wls)
        val list = ArrayList<WakeLock>()
        for (wl in cpy) {
            val wk = WakeLock(wl)
            if (packageName == wk.packageName) {
                list.add(wk)
            }
        }
        return list
    }


    fun release(packageName: String) {
        atsLogD("[$packageName] lock release")

        val list = reloadWakeLock(packageName)

        list.forEach {
            getRawData().javaClass.method {
                name = "releaseWakeLockInternal"
                param(IBinderClass, IntType)
            }.get(getRawData()).call(it.lock, it.flags)

        }
    }
}