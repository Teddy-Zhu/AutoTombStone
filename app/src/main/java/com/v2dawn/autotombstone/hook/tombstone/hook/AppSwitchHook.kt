package com.v2dawn.autotombstone.hook.tombstone.hook;

import cn.myflv.android.noactive.server.ActivityManagerService
import cn.myflv.android.noactive.server.ComponentName
import cn.myflv.android.noactive.server.Event
import de.robv.android.xposed.XC_MethodHook

class AppSwitchHook(
    classLoader: ClassLoader?,
    appStateChangeExecutor: AppStateChangeExecutor,
    type: Int
) :
    XC_MethodHook() {
    private val ACTIVITY_RESUMED: Int
    private val ACTIVITY_PAUSED: Int
    private val type: Int
    var appStateChangeExecutor: AppStateChangeExecutor

    /**
     * Hook APP 切换事件
     *
     * @param param 方法参数
     * @throws Throwable 异常
     */
    @Throws(Throwable::class)
    public override fun beforeHookedMethod(param: MethodHookParam) {

    }

    companion object {
        const val SIMPLE = 1
        const val DIFFICULT = 2
    }

    init {
        ACTIVITY_RESUMED = Event.ACTIVITY_RESUMED(classLoader)
        ACTIVITY_PAUSED = Event.ACTIVITY_PAUSED(classLoader)
        this.type = type
        this.appStateChangeExecutor = appStateChangeExecutor
    }
}
