package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class BroadcastFilter(private val broadcastFilter: Any) {
    public var receiverList: ReceiverList? = null

    init {
        var raw = broadcastFilter.javaClass.field {
            name = FieldEnum.receiverListField
        }.get(broadcastFilter).cast<Any>()
        raw?.let {
            receiverList = ReceiverList(it)
        }

    }

}
