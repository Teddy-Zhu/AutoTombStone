package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class ProcessStateRecord(private val processStateRecord: Any) {
    public val processRecord: ProcessRecord =
        ProcessRecord(XposedHelpers.getObjectField(processStateRecord, FieldEnum.mApp))

}
