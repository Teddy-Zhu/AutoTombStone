package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import de.robv.android.xposed.XposedHelpers


class ActiveServices(private val activeServices: Any) {
    fun killServicesLocked(processRecord: ProcessRecord) {
        XposedHelpers.callMethod(
            activeServices,
            MethodEnum.killServicesLocked,
            processRecord.processRecord,
            false
        )
    }
}
