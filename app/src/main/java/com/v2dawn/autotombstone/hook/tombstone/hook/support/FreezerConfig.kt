package com.v2dawn.autotombstone.hook.tombstone.hook.support

import android.os.Build
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import com.v2dawn.autotombstone.config.ConfigConst
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.MethodEnum

class FreezerConfig(private val packageParam: PackageParam) {

    companion object {

        public const val API = "Api"
        public const val V2 = "V2"
        public const val V1 = "V1"
    }

    private var isSupportV2 = false;


    private fun isConfigOn(configName: PrefsData<Boolean>): Boolean {
        packageParam.apply {
            return prefs(ConfigConst.COMMON_NAME)
                .get(configName)
        }
    }

    public val killSignal: Int
        get() {
            if (isConfigOn(ConfigConst.ENABLE_FORCE_KILL_19)) {
                return 19
            }
            return if (isConfigOn(ConfigConst.ENABLE_FORCE_KILL_20)) {
                20
            } else 19
        }

    fun getFreezerVersion(): String {
        if (isConfigOn(ConfigConst.ENABLE_FREEEZER_V2)) {
            return V2
        }
        if (isConfigOn(ConfigConst.ENABLE_FREEEZER_V1)) {
            return V1
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (isConfigOn(ConfigConst.ENABLE_FREEEZER_API)) {
                return API
            }

            if (isSupportV2) {
                return V2
            }
        }
        return V1
    }

    public val isUseKill: Boolean
        get() = isConfigOn(ConfigConst.ENABLE_FORCE_KILL_19) || isConfigOn(ConfigConst.ENABLE_FORCE_KILL_20)

    fun isColorOs(): Boolean {
        return isConfigOn(ConfigConst.ENABLE_COLOROS_OOM)
    }

    init {
        packageParam.apply {
            this@FreezerConfig.isSupportV2 = ClassEnum.CachedAppOptimizerClass
                .clazz.method {
                    name = MethodEnum.isFreezerSupported
                    emptyParam()
                }.get().invoke<Boolean>()!!
        }
    }

}