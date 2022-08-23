package com.v2dawn.autotombstone.hook.tombstone.hook;

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerI
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogI
import com.v2dawn.autotombstone.hook.tombstone.support.doNothing

class PowerKeeper : YukiBaseHooker() {

    fun hook() {
        try {
            ClassEnum.PowerStateMachineClass
                .hook {
                    injectMember {
                        method {
                            name = MethodEnum.clearAppWhenScreenOffTimeOut
                            emptyParam()
                        }
                        doNothing()
                    }
                    injectMember {
                        method {
                            name = MethodEnum.clearAppWhenScreenOffTimeOutInNight
                            emptyParam()
                        }
                        doNothing()
                    }
                    injectMember {
                        method {
                            name = MethodEnum.clearUnactiveApps
                            emptyParam()
                        }
                        doNothing()
                    }
                }

            atsLogI( "Disable MIUI clearApp")
        } catch (throwable: Throwable) {
            atsLogI( "Disable MIUI clearApp failed: ${throwable.message}")
        }
        try {

            ClassEnum.MilletConfigClass.hook {
                injectMember {
                    method {
                        name = MethodEnum.getEnable
                        emptyParam()
                    }
                    replaceToFalse()
                }
            }

            atsLogI( "Disable millet")
        } catch (throwable: Throwable) {
            atsLogI( "Disable millet failed: ${throwable.message}")
        }
    }

    override fun onHook() {
        // 禁用 millet
        hook()
    }
}
