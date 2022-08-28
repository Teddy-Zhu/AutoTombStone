package com.android.server

import android.content.Context
import android.os.IAtsConfigService
import android.os.PowerManager
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import java.util.*

class AtsConfigService(val context: Context, val appStateChangeExecutor: AppStateChangeExecutor) :
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

    override fun control(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.controlApp(packageName)
        return true
    }

    override fun unControl(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.unControlApp(packageName)
        return true
    }

    override fun freezeApp(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.freezeApp(packageName)
        return true
    }

    override fun unFreezeApp(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.unFreezeApp(packageName)
        return true
    }

    override fun stopService(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.stopServices(packageName)
        return true
    }

    override fun makeIdle(packageName: String?, idle: Boolean): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.makeAppIdle(packageName,idle)
        return true
    }

    override fun forceStop(packageName: String?): Boolean {
        if (packageName == null) return false
        appStateChangeExecutor.forceStopApp(packageName)
        return true
    }

    override fun restartSystem(): Boolean {
        appStateChangeExecutor.restartSystem()
        return true
    }

}