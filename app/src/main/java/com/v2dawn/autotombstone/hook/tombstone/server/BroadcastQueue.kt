package com.v2dawn.autotombstone.hook.tombstone.server;

import com.v2dawn.autotombstone.hook.tombstone.support.FieldEnum
import de.robv.android.xposed.XposedHelpers;


class BroadcastQueue(private val broadcastQueue: Any) {
    private val activityManagerService: ActivityManagerService = ActivityManagerService(
        XposedHelpers.getObjectField(
            broadcastQueue, FieldEnum.mService
        )
    )

}
