package com.v2dawn.autotombstone.hook.tombstone.hook;

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerI

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

    }

}
