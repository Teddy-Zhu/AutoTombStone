package com.android.server

import android.os.IAtsConfigService
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import java.util.*

class AtsConfigService(val appStateChangeExecutor: AppStateChangeExecutor) :
    IAtsConfigService.Stub() {

    val timerMap = Collections.synchronizedMap(HashMap<String, Timer?>())

    companion object {
        public val serviceName = "tv_tuner_resource_mgr" //Context.TV_TUNER_RESOURCE_MGR_SERVICE
    }

    override fun configChange(name: String?, key: String?) {
        if (name == null || key == null) {
            return
        }
        val uk = "$name#$key"
        synchronized(uk.intern()) {
            var timer = timerMap[uk]

            timer?.cancel()
            atsLogI("reload config $name , key:$key")

            timer = Timer().apply {
                schedule(
                    object : TimerTask() {
                        override fun run() {
                            timerMap.remove(uk)
                            appStateChangeExecutor.reloadConfig(name, key)
                        }
                    }, 5000
                )
            }
            timerMap[uk] = timer
        }

    }


}