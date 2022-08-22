package com.v2dawn.autotombstone.hook.tombstone.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.BuildConfig
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.ui.activity.MainActivity
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

object ConfigReloadHook: YukiBaseHooker() {

    override fun onHook() {
        loadApp(BuildConfig.APPLICATION_ID) {
            MainActivity::class.java.hook {
                injectMember {
                    method {
                        name = "prefChange"
                        param(StringType, StringType)
                    }
                    afterHook {

                        loggerD(msg = "module change ${args(0).string()} , ${args(1).string()}")

                        if (AppStateChangeExecutor.instance != null){
                            AppStateChangeExecutor.instance!!.reloadConfig(args(0).string())
                        }
                    }
                }
            }

        }
    }


}