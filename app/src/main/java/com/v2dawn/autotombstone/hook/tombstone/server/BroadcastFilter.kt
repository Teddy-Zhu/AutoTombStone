package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class BroadcastFilter(private val broadcastFilter: Any) {
    private val receiverList: ReceiverList =
        ReceiverList(XposedHelpers.getObjectField(broadcastFilter, FieldEnum.receiverList))

}
