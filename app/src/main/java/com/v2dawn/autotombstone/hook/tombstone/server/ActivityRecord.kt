package com.v2dawn.autotombstone.hook.tombstone.server

import android.content.ComponentName
import com.highcapable.yukihookapi.hook.factory.field

class ActivityRecord(val activityRecord: Any) {

    val mActivityComponent: ComponentName

    val nowVisible: Boolean
    val mVisible: Boolean

    init {

        mActivityComponent = activityRecord.javaClass.field {
            name = "mActivityComponent"
        }.get(activityRecord).cast<ComponentName>()!!

        nowVisible = activityRecord.javaClass.field {
            name = "nowVisible"
        }.get(activityRecord).boolean()

        mVisible = activityRecord.javaClass.field {
            name = "mVisible"
        }.get(activityRecord).boolean()

    }

    override fun toString(): String {
        return "ActivityRecord(mActivityComponent=$mActivityComponent, nowVisible=$nowVisible, mVisible=$mVisible)"
    }


}