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

    fun reloadDebug() {
        ConfigConst.isDebug =
            prefs.name(ConfigConst.COMMON_NAME).get(ConfigConst.ENABLE_MODULE_LOG);
    }

    override fun onHook() {
        if (AppStateChangeExecutor.instance != null) {
            atsLogD("test app state instance not null")
        } else {
            atsLogD("test app state instance null")
        }

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

                    if (AppStateChangeExecutor.instance != null) {
                        atsLogD("push to queue execute")
                        AppStateChangeExecutor.instance!!.reloadConfig(
                            "$prefName#$key"
                        )
                    } else {
                        atsLogD("app state change instance is null")
                    }
                    if (ConfigConst.ENABLE_MODULE_LOG.key == key) {
                        Thread {
                            atsLogD("debug log change reload")
                            Thread.sleep(5000)
                            reloadDebug()
                            atsLogD("debug log change reload end")
                        }.apply {
                            isDaemon = true
                        }.start()
                        atsLogD("reload task push end")
                    }
                }
            }
        }

    }


}