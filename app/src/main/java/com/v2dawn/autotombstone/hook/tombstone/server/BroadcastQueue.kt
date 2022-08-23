package com.v2dawn.autotombstone.hook.tombstone.server;

import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum


class BroadcastQueue(packageParam: PackageParam, private val broadcastQueue: Any) {

    private val activityManagerService: ActivityManagerService = ActivityManagerService(
        broadcastQueue.javaClass.field {
            name = FieldEnum.mServiceField
        }.get(broadcastQueue).cast<Any>()!!
    )

}
