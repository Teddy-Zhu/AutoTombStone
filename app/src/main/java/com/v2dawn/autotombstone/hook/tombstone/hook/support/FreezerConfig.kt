package com.v2dawn.autotombstone.hook.tombstone.hook.support

import android.os.Build
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum

class FreezerConfig(private val packageParam: PackageParam) {

    fun getSupportedFreezeType(): IntArray {
        var t = intArrayOf(0, 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            t = t.plus(2)
            packageParam.apply {
                if (ClassEnum.CachedAppOptimizerClass
                        .clazz.method {
                            name = MethodEnum.isFreezerSupported
                            emptyParam()
                        }.get().invoke<Boolean>()!!
                ) {
                    t = t.plus(3)
                }
            }
            t = t.plus(4)
        }
        return t
    }

    // 0 kill 19 ,1 kill 20 , 2 freezeapi ,3 freeze v2, 4 freezev1

    fun getFreezeType(): Int {
        packageParam.apply {
            return prefs(ConfigConst.COMMON_NAME)
                .get(ConfigConst.FREEZE_TYPE)
        }
    }

}