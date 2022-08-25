package com.v2dawn.autotombstone.hook.tombstone.server

import android.os.IBinder
import com.highcapable.yukihookapi.hook.factory.field


class WakeLock(raw: Any) : ForkOrigin(raw) {
    val flags: Int = getRawData().javaClass.field {
        name = "flags"
    }.get(getRawData()).int()
    val lock: IBinder = getRawData().javaClass.field {
        name = "lock"
    }.get(getRawData()).cast()!!
    val packageName: String = getRawData().javaClass.field {
        name = "packageName"
    }.get(getRawData()).string()
    val tag: String = getRawData().javaClass.field {
        name = "tag"
    }.get(getRawData()).string()

}