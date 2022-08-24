package com.android.server

import android.content.Context
import android.os.IAtsConfigService
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import java.util.*

class AtsConfigService(val appStateChangeExecutor: AppStateChangeExecutor) :
    IAtsConfigService.Stub() {

    companion object {
        public val serviceName = "tv_tuner_resource_mgr" //Context.TV_TUNER_RESOURCE_MGR_SERVICE
    }

    override fun configChange(name: String?, key: String?) {
        atsLogI("reload config $name , key:$key")
        Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    appStateChangeExecutor.reloadConfigQueue.put("$name#$key")
                }
            }, 5000
        )
    }


}