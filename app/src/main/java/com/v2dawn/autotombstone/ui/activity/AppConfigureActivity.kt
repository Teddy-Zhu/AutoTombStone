package com.v2dawn.autotombstone.ui.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IAtsConfigService
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import androidx.core.util.Pair
import androidx.core.view.isVisible
import com.android.server.AtsConfigService
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.classOf
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringType
import com.v2dawn.autotombstone.R
import com.v2dawn.autotombstone.databinding.ActivityAppConfigBinding
import com.v2dawn.autotombstone.databinding.AdapterItemAppBinding
import com.v2dawn.autotombstone.databinding.DiaAppFilterBinding
import com.v2dawn.autotombstone.hook.tombstone.support.ClassEnum
import com.v2dawn.autotombstone.hook.tombstone.support.atsLogE
import com.v2dawn.autotombstone.model.AppItemData
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.*
import com.v2dawn.autotombstone.utils.tool.SystemTool
import com.v2dawn.autotombstone.utils.tool.SystemTool.getApps
import com.v2dawn.autotombstone.utils.tool.SystemTool.loadApplicationInfos
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers


class AppConfigureActivity : BaseActivity<ActivityAppConfigBinding>() {
    companion object {

        const val TAG = "AppConfigureActivity"
    }

    /** 当前筛选条件 */
    private var filterText = ""
    private var showSystem = false

    /** 回调适配器改变 */
    private var onChanged: (() -> Unit)? = null

    /** 回调滚动事件改变 */
    private var onScrollEvent: ((Boolean) -> Unit)? = null

    /** 全部的App数据 */
    private var appConfigData = ArrayList<AppItemData>()
    var atsConfigService: IAtsConfigService? = null

    private var freezeApps: Set<String> = setOf()

    private var dataLoading: Boolean = true
    override fun onCreate() {
        /** 检查激活状态 */
        if (YukiHookAPI.Status.isXposedModuleActive.not()) {
            showDialog {
                title = "模块没有激活"
                msg = "模块没有激活，你的修改暂时无法生效，请先激活模块。"
                confirmButton(text = "我知道了") { cancel() }
                noCancelable()
            }
        }


        /** 返回按钮点击事件 */
        binding.titleBackIcon.setOnClickListener { onBackPressed() }
        /** 刷新适配器结果相关 */
        refreshAdapterResult()
        /** 设置上下按钮点击事件 */
        binding.configTitleUp.setOnClickListener {
            snake(msg = "滚动到顶部")
            onScrollEvent?.invoke(false)
        }
        binding.configTitleDown.setOnClickListener {
            snake(msg = "滚动到底部")
            onScrollEvent?.invoke(true)
        }

        binding.configTitleSync.setOnClickListener {
            refreshFreezeStatus()
        }
        /** 设置过滤按钮点击事件 */
        binding.configTitleFilter.setOnClickListener {
            showDialog<DiaAppFilterBinding> {
                title = "按条件过滤"
                binding.showSystemAppsSwitch.isChecked = showSystem
                binding.showSystemAppsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    showSystem = isChecked
                    onStartRefresh()
                }
                binding.iconFiltersEdit.apply {
                    requestFocus()
                    invalidate()
                    if (filterText.isNotBlank()) {
                        setText(filterText)
                        setSelection(filterText.length)
                    }
                    showKeyboard(this)
                }
                confirmButton {
                    filterText = binding.iconFiltersEdit.text.toString().trim()
                    refreshAdapterResult()
                }
                cancelButton {
                    hideKeyBoard(binding.iconFiltersEdit)
                }
                if (filterText.isNotBlank())
                    neutralButton(text = "清除条件") {
                        filterText = ""
                        refreshAdapterResult()
                    }
            }
        }

        /** 设置列表元素和 Adapter */
        binding.configListView.apply {
            bindAdapter {
                onBindDatas { appFilteredData }
                onBindViews<AdapterItemAppBinding> { binding, position ->
                    appFilteredData[position].also { bean ->
                        binding.appWhiteSwitch.setOnCheckedChangeListener(null)
                        binding.appWhiteSwitch.isChecked = bean.enable
                        binding.appWhiteSwitch.isEnabled = !bean.isImportantSystemApp
                        binding.adpAppIcon.setImageDrawable(bean.icon)
                        binding.adpAppName.text = bean.name
                        binding.adpAppName.setTextColor(
                            if (bean.inFreeze) getColor(R.color.red) else getColor(
                                R.color.colorTextDark
                            )
                        )
                        binding.adpAppPkgName.text = bean.packageName
                        binding.sysImpApp.tag = bean.name
                        binding.sysApp.tag = bean.name
                        binding.xpModule.tag = bean.name
                        binding.inFreeze.tag = bean.name
                        binding.sysImpApp.isVisible = bean.isImportantSystemApp
                        binding.sysApp.isVisible = bean.isSystem
                        binding.xpModule.isVisible = bean.isXposedModule
                        binding.inFreeze.isVisible = bean.inFreeze
                        binding.appWhiteSwitch.setOnCheckedChangeListener { _, b ->
//                            binding.appWhiteSwitch.isEnabled = b
                            //TODO notify adapter change enable
                            Log.d(TAG, "change app switch ${bean.name} ${position}")
                            if (bean.isImportantSystemApp) {
                                toast(getString(R.string.imp_sys_app_not_support));
                                return@setOnCheckedChangeListener
                            }

                            if (bean.isSystem) {
                                //black
                                if (updateBlackApps(bean.packageName, b)) {
                                    updateListData(position)
                                }
                            } else {
                                if (updateWhiteApps(bean.packageName, b)) {
                                    updateListData(position)
                                }
                            }
                        }

                        binding.sysApp.setOnClickListener(onClickAction)
                        binding.sysImpApp.setOnClickListener(onClickAction)
                        binding.xpModule.setOnClickListener(onClickAction)
                        binding.inFreeze.setOnClickListener {
                            toast("${bean.name} 在 ${it.tooltipText}")
                        }
                        binding.appContent.setOnLongClickListener {
                            showOperatePopup(it) { index, resId ->
                                if (!YukiHookAPI.Status.isModuleActive) {
                                    toast("模块未激活")
                                    return@showOperatePopup
                                }
                                when (resId) {
                                    R.string.control -> {
                                        getAtsService().control(bean.packageName)
                                        toast("压制成功")
                                    }
                                    R.string.uncontrol -> {
                                        getAtsService().unControl(bean.packageName)
                                        toast("恢复成功")
                                    }
                                    R.string.stop_services -> {
                                        getAtsService().stopService(bean.packageName)
                                        toast("停止服务成功")
                                    }
                                    R.string.active -> {
                                        getAtsService().makeIdle(bean.packageName, false)
                                        toast("解除App休眠成功")
                                    }
                                    R.string.inactive -> {
                                        getAtsService().makeIdle(bean.packageName, true)
                                        toast("强制App休眠成功")

                                    }
                                    R.string.kill_app -> {
                                        getAtsService().forceStop(bean.packageName)
                                        toast("App强制停止成功")
                                    }
                                    R.string.freeze_app -> {
                                        getAtsService().freezeApp(bean.packageName)
                                        toast("冻结App成功")
                                    }
                                    R.string.unfreeze_app -> {
                                        getAtsService().unFreezeApp(bean.packageName)
                                        toast("解冻App成功")
                                    }
                                }
                            }
                            true
                        }
                        binding.appContent.setOnClickListener {

                            var transitionViews = ArrayList<Pair<View, String>>().apply {
                                add(Pair(binding.adpAppIcon, "app_icon"))
                                add(Pair(binding.adpAppName, "app_name"))
                                add(Pair(binding.adpAppPkgName, "pkg_name"))
                                add(Pair(binding.appWhiteSwitch, "app_white_switch"))
                                if (bean.isImportantSystemApp) {
                                    add(Pair(binding.sysImpApp, "sys_imp_app"))
                                }
                                if (bean.isSystem) {
                                    add(Pair(binding.sysApp, "sys_app"))
                                }
                                if (bean.isXposedModule) {
                                    add(Pair(binding.xpModule, "xp_module"))
                                }
                                if (bean.inFreeze) {
                                    add(Pair(binding.inFreeze, "in_freeze"))
                                }
                            }

                            this@AppConfigureActivity.navigateWithTransition<AppConfigureDetailActivity>(
                                position,
                                bean,
                                *transitionViews.toTypedArray()
                            )
                        }
                    }
                }
            }.apply {
                setOnItemLongClickListener { _, _, p, _ ->
                    showDialog {
                        title = "复制${appFilteredData[p].name}包名"
                        msg = "是否复制包名\n${appFilteredData[p].packageName}\n到剪贴板？"
                        confirmButton { copyToClipboard(appFilteredData[p].packageName) }
                        cancelButton()
                    }
                    true
                }
                onChanged = {

                    Log.d(TAG, "onChanged")
                    notifyDataSetChanged()
                }
            }
            onScrollEvent = { post { setSelection(if (it) appFilteredData.lastIndex else 0) } }
        }

        runInSafe {
            freezeApps = hashSetOf<String>().apply {
                addAll(getAtsService().queryBackgroundApps())
            }
        }
        /** 装载数据 */
        mockLocalData(true)

//        setExitSharedElementCallback(object : SharedElementCallback() {
//
//            override fun onSharedElementEnd(
//                sharedElementNames: MutableList<String>?,
//                sharedElements: MutableList<View>?,
//                sharedElementSnapshots: MutableList<View>?
//            ) {
//                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
//                Log.d(TAG, "exitSharedElementCallback result:${transitionBack != null}")
//
//            }
//
//        })
    }

    private fun refreshFreezeStatus() =
        runInSafe {
            freezeApps = hashSetOf<String>().apply {
                addAll(getAtsService().queryBackgroundApps())
            }

            updateAppsFreezeStatus()
        }


    fun showKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    fun hideKeyBoard(view: View) {
        val inputMethodManager =
            this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    val onClickAction: View.OnClickListener = View.OnClickListener {
        toast("${it.tag} 是 ${it.tooltipText}")
    }

    private fun updateListData(position: Int) {


        buildAppItemData(
            packageManager,
            null,
            getBlackApps(),
            getWhiteApps(),
            appFilteredData[position],
            freezeApps,
            appFilteredData[position].packageName
        )
        (binding.configListView.adapter as BaseAdapter).notifyDataSetChanged()
    }

    fun getAtsService(): IAtsConfigService {
        if (atsConfigService == null) {
            try {
                val binder: IBinder = classOf(
                    ClassEnum.ServiceManagerClass,
                    Thread.currentThread().contextClassLoader
                )
                    .method {
                        name = "getService"
                        param(StringType)
                    }.get().invoke<IBinder>(AtsConfigService.serviceName)!!

                atsConfigService =
                    IAtsConfigService.Stub.asInterface(binder)
            } catch (e: Exception) {
                atsLogE("ats config service get error", e = e)
            }

        }

        return atsConfigService!!
    }

    private lateinit var transitionBack: Intent;
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (RESULT_OK == resultCode && data != null) {
            transitionBack = data
            Log.d(TAG, "reenter back")
            val position = transitionBack.getIntExtra("position", -1)
            if (position != -1) {
                // data
                updateListData(position)
            }
        }
        super.onActivityReenter(resultCode, data)
    }

    /** 开始手动同步 */
    private fun onStartRefresh(force: Boolean = false) {
        filterText = ""
        mockLocalData(force)
    }


    /** 装载或刷新本地数据 */
    private fun mockLocalData(force: Boolean) {
        refreshAppList(force)
    }


    private fun updateAppsFreezeStatus() {
        appConfigData.forEach {
            val newFreeze = freezeApps.contains(it.packageName)
            if (it.inFreeze != newFreeze) {
                it.inFreeze = newFreeze
                it.updatePriority()
            }
        }
        appConfigData.sortBy { it.priority }
    }

    private fun refreshAppList(force: Boolean) {
        dataLoading = true
        Observable.create { emitter: ObservableEmitter<List<AppItemData>> ->
            var starttime = System.currentTimeMillis();
            loadApplicationInfos(packageManager, force)
            Log.i(TAG, "loadApplicationInfos spend ${System.currentTimeMillis() - starttime}ms")
            starttime = System.currentTimeMillis();
            val blackApps = getBlackApps()
            val whiteApps = getWhiteApps()
            val data: List<AppItemData> = buildCache(blackApps, whiteApps)
            Log.i(TAG, "buildCache spend ${System.currentTimeMillis() - starttime}ms")
            emitter.onNext(data)
            emitter.onComplete()
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable: Throwable? ->
                Log.e(
                    TAG,
                    "refreshAppList: ",
                    throwable
                )
            }
            .subscribe(
                { data: List<AppItemData> ->
                    Log.d(TAG, "reload data")
                    appConfigData.clear()
                    appConfigData.addAll(data)
                    dataLoading = false
                    refreshAdapterResult()
                }
            ) { throwable: Throwable? ->
                Log.e(
                    TAG,
                    "refreshAppList sub: ",
                    throwable
                )
            }
    }

    private fun buildCache(
        sysBlackApps: Set<String>,
        whiteApps: Set<String>
    ): List<AppItemData> {
        val cache: MutableList<AppItemData> = ArrayList<AppItemData>()
        for (appInfo in getApps()!!) {
            val viewData =
                buildAppItemData(
                    packageManager,
                    appInfo,
                    sysBlackApps,
                    whiteApps,
                    null,
                    freezeApps,
                    appInfo.packageName
                )
            cache.add(viewData)
        }
        cache.sortBy { it.priority }
        return cache
    }

    /** 刷新适配器结果相关 */
    private fun refreshAdapterResult() {
        onChanged?.invoke()
        binding.configTitleCountText.text =
            if (filterText.isBlank()) "已找到 ${appFilteredData.size} 个 APP"
            else "“${filterText}” 匹配到 ${appFilteredData.size} 个结果"
        binding.configListNoDataView.apply {
            text =
                if (dataLoading) context.getString(R.string.wait_loading) else if (appConfigData.isEmpty()) context.getString(
                    R.string.found_nothing_refresh
                ) else context.getString(
                    R.string.found_nothing
                )
            isVisible = appFilteredData.isEmpty()
        }
    }


    /**
     * 当前结果下的图标数组
     * @return [Array]
     */
    private val appFilteredData
        get() = if (filterText.isBlank()) appConfigData.filter { if (it.isSystem) showSystem else true }
        else appConfigData.filter {
            (!it.isSystem || (showSystem && it.isSystem)) && (it.name.lowercase()
                .contains(filterText.lowercase()) || it.packageName.lowercase()
                .contains(filterText.lowercase()))
        }

    override fun onBackPressed() {
        if (filterText.isNotBlank()) {
            onStartRefresh(false)
            return
        }
        if (MainActivity.isActivityLive.not())
            showDialog {
                title = "提示"
                msg = "要返回模块主页吗？"
                confirmButton {
                    super.onBackPressed()
                    navigate<MainActivity>()
                }
                cancelButton { super.onBackPressed() }
            }
        else super.onBackPressed()
    }
}