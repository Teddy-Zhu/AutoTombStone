package com.v2dawn.autotombstone.hook.tombstone.yuki

import android.content.Context
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookModulePrefs
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData
import com.v2dawn.autotombstone.BuildConfig
import de.robv.android.xposed.XSharedPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap


class XposedModulePrefs(private val fileName: String?) {

    companion object {

        /** 使用 ConcurrentHashMap 容器，装载 RegisterSingletonFromMap 类对象  */
        private val map: MutableMap<String, XposedModulePrefs> =
            ConcurrentHashMap<String, XposedModulePrefs>()

        /** 提供公共静态的获取该私有类对象的方法  */
        fun instance(name: String): XposedModulePrefs {
            synchronized(XposedModulePrefs::class.java) {
                if (!map.containsKey(name)) {
                    map[name] = XposedModulePrefs(name)
                }
            }
            return map[name]!!
        }
    }

    /** 存储名称 - 默认包名 + _preferences */
    private var prefsName = if (fileName == null)
        "${BuildConfig.APPLICATION_ID}_preferences" else "${fileName}_preferences"

    /** [XSharedPreferences] 缓存的 [String] 键值数据 */
    private var xPrefCacheKeyValueStrings = HashMap<String, String>()

    /** [XSharedPreferences] 缓存的 [Set]<[String]> 键值数据 */
    private var xPrefCacheKeyValueStringSets = HashMap<String, Set<String>>()

    /** [XSharedPreferences] 缓存的 [Boolean] 键值数据 */
    private var xPrefCacheKeyValueBooleans = HashMap<String, Boolean>()

    /** [XSharedPreferences] 缓存的 [Int] 键值数据 */
    private var xPrefCacheKeyValueInts = HashMap<String, Int>()

    /** [XSharedPreferences] 缓存的 [Long] 键值数据 */
    private var xPrefCacheKeyValueLongs = HashMap<String, Long>()

    /** [XSharedPreferences] 缓存的 [Float] 键值数据 */
    private var xPrefCacheKeyValueFloats = HashMap<String, Float>()

    /** 是否使用键值缓存 */
    private var isUsingKeyValueCache = YukiHookAPI.Configs.isEnableModulePrefsCache

    /** 是否使用新版存储方式 EdXposed/LSPosed */
    private var isUsingNewXSharePrefs = true


    /**
     * 获得 [XSharedPreferences] 对象
     * @return [XSharedPreferences]
     */
    private val xPref
        get() = XSharedPreferences(BuildConfig.APPLICATION_ID, prefsName).apply {
            makeWorldReadable()
            reload()
        }

    /**
     * 获取 [XSharedPreferences] 是否可读
     *
     * - ❗只能在 [isXposedEnvironment] 中使用 - 模块环境中始终返回 false
     * @return [Boolean] 是否可读
     */
    val isXSharePrefsReadable
        get() = runCatching { xPref.let { it.file.exists() && it.file.canRead() } }.getOrNull()
            ?: false

    /**
     * 自定义 Sp 存储名称
     * @param name 自定义的 Sp 存储名称
     * @return [YukiHookModulePrefs]
     */
    fun name(name: String): XposedModulePrefs {
        isUsingKeyValueCache = YukiHookAPI.Configs.isEnableModulePrefsCache
        prefsName = name
        return this
    }

    /**
     * 忽略缓存直接读取键值
     *
     * 无论是否开启 [YukiHookAPI.Configs.isEnableModulePrefsCache]
     *
     * - 仅在 [XSharedPreferences] 下生效
     * @return [YukiHookModulePrefs]
     */
    fun direct(): XposedModulePrefs {
        isUsingKeyValueCache = false
        return this
    }

    /**
     * 获取 [String] 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据 - ""
     * @return [String]
     */
    fun getString(key: String, value: String = "") =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueStrings[key].let {
                (it ?: xPref.getString(key, value) ?: value).let { value ->
                    xPrefCacheKeyValueStrings[key] = value
                    value
                }
            }
        else resetCacheSet { xPref.getString(key, value) ?: value }

    /**
     * 获取 [Set]<[String]> 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据
     * @return [Set]<[String]>
     */
    fun getStringSet(key: String, value: Set<String>) =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueStrings[key].let {
                (it ?: xPref.getStringSet(key, value) ?: value).let { value ->
                    xPrefCacheKeyValueStringSets[key] = value as Set<String>
                    value
                }
            }
        else resetCacheSet { xPref.getStringSet(key, value) ?: value }

    /**
     * 获取 [Boolean] 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据 - false
     * @return [Boolean]
     */
    fun getBoolean(key: String, value: Boolean = false) =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueBooleans[key].let {
                it ?: xPref.getBoolean(key, value).let { value ->
                    xPrefCacheKeyValueBooleans[key] = value
                    value
                }
            }
        else resetCacheSet { xPref.getBoolean(key, value) }

    /**
     * 获取 [Int] 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据 - 0
     * @return [Int]
     */
    fun getInt(key: String, value: Int = 0) =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueInts[key].let {
                it ?: xPref.getInt(key, value).let { value ->
                    xPrefCacheKeyValueInts[key] = value
                    value
                }
            }
        else resetCacheSet { xPref.getInt(key, value) }

    /**
     * 获取 [Float] 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据 - 0f
     * @return [Float]
     */
    fun getFloat(key: String, value: Float = 0f) =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueFloats[key].let {
                it ?: xPref.getFloat(key, value).let { value ->
                    xPrefCacheKeyValueFloats[key] = value
                    value
                }
            }
        else resetCacheSet { xPref.getFloat(key, value) }

    /**
     * 获取 [Long] 键值
     *
     * - 智能识别对应环境读取键值数据
     *
     * - 建议使用 [PrefsData] 创建模板并使用 [get] 获取数据
     * @param key 键值名称
     * @param value 默认数据 - 0L
     * @return [Long]
     */
    fun getLong(key: String, value: Long = 0L) =
        if (isUsingKeyValueCache)
            xPrefCacheKeyValueLongs[key].let {
                it ?: xPref.getLong(key, value).let { value ->
                    xPrefCacheKeyValueLongs[key] = value
                    value
                }
            }
        else resetCacheSet { xPref.getLong(key, value) }

    /**
     *  获取全部存储的键值数据
     *
     * - 智能识别对应环境读取键值数据
     *
     * - ❗每次调用都会获取实时的数据 - 不受缓存控制 - 请勿在高并发场景中使用
     * @return [HashMap] 全部类型的键值数组
     */
    fun all() = HashMap<String, Any?>().apply {
        xPref.all.forEach { (k, v) -> this[k] = v }
    }


    /**
     * 智能获取指定类型的键值
     * @param prefs 键值实例
     * @param value 默认值 - 未指定默认为 [prefs] 中的 [PrefsData.value]
     * @return [T] 只能是 [String]、[Int]、[Float]、[Long]、[Boolean]
     */
    inline fun <reified T> get(prefs: PrefsData<T>, value: T = prefs.value): T =
        getPrefsData(prefs.key, value) as T

    /**
     * 智能获取指定类型的键值
     *
     * 封装方法以调用内联方法
     * @param key 键值
     * @param value 默认值
     * @return [Any]
     */
    @PublishedApi
    internal fun getPrefsData(key: String, value: Any?): Any = when (value) {
        is String -> getString(key, value)
        is Set<*> -> getStringSet(
            key,
            value as? Set<String> ?: error("Key-Value type ${value.javaClass.name} is not allowed")
        )
        is Int -> getInt(key, value)
        is Float -> getFloat(key, value)
        is Long -> getLong(key, value)
        is Boolean -> getBoolean(key, value)
        else -> error("Key-Value type ${value?.javaClass?.name} is not allowed")
    }

    /**
     * 清除 [XSharedPreferences] 中缓存的键值数据
     *
     * 无论是否开启 [YukiHookAPI.Configs.isEnableModulePrefsCache]
     *
     * 调用此方法将清除当前存储的全部键值缓存
     *
     * 下次将从 [XSharedPreferences] 重新读取
     */
    fun clearCache() {
        xPrefCacheKeyValueStrings.clear()
        xPrefCacheKeyValueStringSets.clear()
        xPrefCacheKeyValueBooleans.clear()
        xPrefCacheKeyValueInts.clear()
        xPrefCacheKeyValueLongs.clear()
        xPrefCacheKeyValueFloats.clear()
    }

    /**
     * 恢复 [isUsingKeyValueCache] 为默认状态
     * @param result 回调方法体的结果
     * @return [T]
     */
    private inline fun <T> resetCacheSet(result: () -> T): T {
        isUsingKeyValueCache = YukiHookAPI.Configs.isEnableModulePrefsCache
        return result()
    }

}