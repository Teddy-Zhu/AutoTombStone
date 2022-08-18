package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class ReceiverList(private val receiverList: Any) {
    private var processRecord: ProcessRecord? = null
    fun clear() {
        XposedHelpers.setObjectField(receiverList, FieldEnum.app, null)
    }

    init {
        try {
            processRecord = ProcessRecord(XposedHelpers.getObjectField(receiverList, FieldEnum.app))
        } catch (ignored: Exception) {
        }
    }
}
