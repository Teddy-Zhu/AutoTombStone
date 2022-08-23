package com.v2dawn.autotombstone.hook.tombstone.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.utils.factory.runInSafe
import java.util.*

class ConfigReloadHook : YukiBaseHooker() {

    override fun onHook() {

        "$packageName.ui.activity.MainActivity".hook {
            injectMember {
                method {
                    name = "prefChange"
                    param(StringType, StringType)
                }
                afterHook {


                    val key = args(1).string();
                    val prefName = args(0).string()
                    atsLogD("module change name:$prefName ,key:$key")

                    Thread {
                        Thread.sleep(5000)
                        prefs(prefName).clearCache(key)
                        atsLogD("debug log change reload end")
                    }.apply {
                        isDaemon = true
                    }.start()
                }
            }
        }

    }


}