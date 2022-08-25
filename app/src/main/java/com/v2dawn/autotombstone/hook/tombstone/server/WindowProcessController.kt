package com.v2dawn.autotombstone.hook.tombstone.server

import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringType

class WindowProcessController(val mWindowProcessController: Any) {


    val mActivities: ArrayList<ActivityRecord>;

    fun hasRunningActivity(packageName: String): Boolean {
        return mWindowProcessController.javaClass.method {
            name = "hasRunningActivity"
            param(StringType)
        }.get(mWindowProcessController).invoke<Boolean>(packageName)!!

    }

    override fun toString(): String {
        return "WindowProcessController(mActivities=$mActivities)"
    }

    init {
        val ac = mWindowProcessController.javaClass.field {
            name = "mActivities"
        }.get(mWindowProcessController).list<Any>()
        mActivities = ArrayList()
        for (any in ac) {
            mActivities.add(ActivityRecord(any))
        }
    }


}