package com.v2dawn.autotombstone.hook.tombstone.support

import android.content.Context
import android.os.IAtsConfigService
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor

class AtsConfigService() :
    IAtsConfigService.Stub() {

    companion object {
        public val serviceName = "user.ats.config.service"
    }

    override fun configChange(name: String?, key: String?) {
        AppStateChangeExecutor.instance?.reloadConfigQueue?.put("$name#$key")
    }


}