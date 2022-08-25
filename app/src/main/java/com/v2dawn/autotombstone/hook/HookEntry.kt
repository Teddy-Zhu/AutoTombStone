package com.v2dawn.autotombstone.hook

import android.content.Context
import android.media.AudioFocusRequest
import android.os.Build
import android.util.Log
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerI
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.hook.tombstone.hook.*
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

@InjectYukiHookWithXposed(isUsingResourcesHook = false)
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugTag = "autotombstone"
        isDebug = false
        isAllowPrintingLogs = true
        isEnableModulePrefsCache = true
        isEnableModuleAppResourcesCache = false
        isEnableHookModuleStatus = true
        isEnableDataChannel = false
        isEnableMemberCache = true
    }

    override fun onHook() = encase {

//        loadZygote {
//            loadHooker(ActivityThreadHook())
//        }
        loadSystem {
            atsLogI("${Build.MANUFACTURER} device:$packageName")


            loadHooker(CacheFreezerHook())
            loadHooker(UsageContextHook())
            loadHooker(AppStateChangeHook())
            loadHooker(BroadcastDeliverHook())
//            loadHooker(OomAdjHook())
            loadHooker(ANRHook())

        }
//        loadApp(BuildConfig.APPLICATION_ID) {
//            loadHooker(ConfigReloadHook())
//            atsLogI("load config reload hook")
//        }
        loadApp("com.miui.powerkeeper") {
            loadHooker(PowerKeeper())
            atsLogI("load miui power")
        }
    }
}