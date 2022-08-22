package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.ui.activity.MainActivity
import java.util.*

object Android : YukiBaseHooker() {

    override fun onHook() {

        loggerI(msg = Build.MANUFACTURER + " device")
        loadSystem {

            loadHooker(CacheFreezerHook)
            loadHooker(UsageContextHook)
            loadHooker(AppStateChangeHook)
            loadHooker(OomAdjHook)
            loadHooker(ANRHook)

        }

        loadHooker(ConfigReloadHook)
    }


}
