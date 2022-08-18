package com.v2dawn.autotombstone.hook.tombstone.server;

import de.robv.android.xposed.XposedHelpers;

object Event {
    const val EventClass = "android.app.usage.UsageEvents.Event"
    const val ACTIVITY_RESUMED = "ACTIVITY_RESUMED"
    const val ACTIVITY_PAUSED = "ACTIVITY_PAUSED"
    const val ACTIVITY_STOPPED = "ACTIVITY_STOPPED"
    const val ACTIVITY_DESTROYED = "ACTIVITY_DESTROYED"

}
