package com.v2dawn.autotombstone.hook.tombstone.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.hook.support.AppStateChangeExecutor
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogD
import com.v2dawn.autotombstone.utils.factory.runInSafe
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

@Deprecated(message = "do not need any more , replace with ats config service aidl")
class ConfigReloadHook(val reloadQueue: ArrayBlockingQueue<String>) : YukiBaseHooker() {

    override fun onHook() {

        "$packageName.ui.activity.MainActivity".hook {
            injectMember {
                method {
                    name = "prefChange"
                    param(StringType, StringType)
                }
                afterHook {

                    atsLogD("config queue obj: ${reloadQueue.hashCode()}")
                    val key = args(1).string();
                    val prefName = args(0).string()
                    atsLogD("module change name:$prefName ,key:$key")


                    Timer().schedule(object: TimerTask() {
                        override fun run() {
                            reloadQueue.put("$prefName#$key")
                            atsLogD("config change reload push end")
                        }

                    },5000)
                }
            }
        }

    }


}