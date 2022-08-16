package com.v2dawn.autotombstone.ui.activity

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.BaseAdapter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.util.Pair
import androidx.core.view.isVisible
import com.highcapable.yukihookapi.YukiHookAPI
import com.v2dawn.autotombstone.databinding.ActivityAppConfigBinding
import com.v2dawn.autotombstone.databinding.AdapterItemAppBinding
import com.v2dawn.autotombstone.databinding.DiaAppFilterBinding
import com.v2dawn.autotombstone.model.AppItemData
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.*
import com.v2dawn.autotombstone.utils.tool.SystemTool.getApps
import com.v2dawn.autotombstone.utils.tool.SystemTool.isImportantSystemApp
import com.v2dawn.autotombstone.utils.tool.SystemTool.isSystem
import com.v2dawn.autotombstone.utils.tool.SystemTool.isXposedModule
import com.v2dawn.autotombstone.utils.tool.SystemTool.loadApplicationInfos
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers


class AppConfigureActivity : BaseActivity<ActivityAppConfigBinding>() {
    companion object {
        fun build() {
            TODO("Not yet implemented")
        }

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

    override fun onCreate() {
        /** 检查激活状态 */
        if (YukiHookAPI.Status.isXposedModuleActive.not()) {
            showDialog {
                title = "模块没有激活"
                msg = "模块没有激活，你的修改暂时无法生效，请先激活模块。"
                confirmButton(text = "我知道了") { finish() }
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
                }
                confirmButton {
                    filterText = binding.iconFiltersEdit.text.toString().trim()
                    refreshAdapterResult()
                }
                cancelButton()
                if (filterText.isNotBlank())
                    neutralButton(text = "清除条件") {
                        filterText = ""
                        refreshAdapterResult()
                    }
            }
        }

        /** 设置同步列表按钮点击事件 */
        binding.configTitleSync.setOnClickListener { onStartRefresh() }
        /** 设置列表元素和 Adapter */
        binding.configListView.apply {
            bindAdapter {
                onBindDatas { appFilteredData }
                onBindViews<AdapterItemAppBinding> { binding, position ->
                    appFilteredData[position].also { bean ->
                        binding.adpAppIcon.setImageDrawable(bean.icon)

                        binding.adpAppName.text = bean.name
                        binding.adpAppPkgName.text = bean.packageName
                        binding.appWhiteSwitch.isChecked = bean.enable
                        binding.sysImpApp.isVisible = bean.isImportantSystemApp
                        binding.sysApp.isVisible = bean.isSystem
                        binding.xpModule.isVisible = bean.isXposedModule
                        binding.appWhiteSwitch.isEnabled = !bean.isImportantSystemApp
                        binding.appWhiteSwitch.setOnCheckedChangeListener { btn, b ->
//                            binding.appWhiteSwitch.isEnabled = b
                            //TODO notify adapter change enable
                        }
                        val onClickAction: View.OnClickListener = View.OnClickListener {
                            toast("${bean.name} 是 ${it.tooltipText}")
                        }
                        binding.sysApp.setOnClickListener(onClickAction)
                        binding.sysImpApp.setOnClickListener(onClickAction)
                        binding.xpModule.setOnClickListener(onClickAction)
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
                onChanged = { notifyDataSetChanged() }
            }
            onScrollEvent = { post { setSelection(if (it) appFilteredData.lastIndex else 0) } }
        }
        /** 装载数据 */
        mockLocalData(true)

        setExitSharedElementCallback(object : SharedElementCallback() {

            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
                if (transitionBack != null) {
                    val position = transitionBack.getIntExtra("position", -1)
                    if (position != -1) {
                        // data
                        updateListData(position)
                    }
                }
                Log.i(TAG, "ddd")
            }

        })
    }

    private fun updateListData(position: Int) {


        buildAppItemData(
            packageManager,
            appFilteredData[position].applicationInfo,
            ArrayList(),
            ArrayList(),
            appFilteredData[position]
        )
//        (binding.configListView.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private lateinit var transitionBack: Intent;
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (RESULT_OK == resultCode && data != null) {
            transitionBack = data
        }
        super.onActivityReenter(resultCode, data)
    }

    /** 开始手动同步 */
    private fun onStartRefresh(force: Boolean = false) = {
        filterText = ""
        mockLocalData(force)
    }
//        IconRuleManagerTool.syncByHand(context = this) {
//            filterText = ""
//            mockLocalData()
//        }

    /** 装载或刷新本地数据 */
    private fun mockLocalData(force: Boolean) {
        refreshAppList(showSystem, force)
    }

    private fun refreshAppList(showSystem: Boolean, force: Boolean) {

        Observable.create(
            ObservableOnSubscribe { emitter: ObservableEmitter<List<AppItemData>> ->
                loadApplicationInfos(packageManager, force)
                val data: List<AppItemData> = buildCache()
                emitter.onNext(data)
                emitter.onComplete()
            } as ObservableOnSubscribe<List<AppItemData>>)
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
                    appConfigData.clear()
                    appConfigData.addAll(data)
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

    private fun buildCache(): List<AppItemData> {
        val cache: MutableList<AppItemData> = ArrayList<AppItemData>()
        for (appInfo in getApps()!!) {
            val viewData = buildAppItemData(packageManager, appInfo, ArrayList(), ArrayList())
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
            text = if (appConfigData.isEmpty()) "噫，竟然什么都没有~\n请点击右上角同步按钮获取App数据" else "噫，竟然什么都没找到~"
            isVisible = appFilteredData.isEmpty()
        }
    }

    private fun loadProcessNames(appItemData: AppItemData) {

        if (appItemData.packageInfo == null) {
            val pkgInfo =
                packageManager.getPackageInfo(appItemData.packageName, PackageManager.GET_SERVICES);
            appItemData.packageInfo = pkgInfo;
            appItemData.processes.clear()
            for (service in pkgInfo.services) {
                appItemData.processes.add(service.processName);
            }
        }
    }

    /**
     * 当前结果下的图标数组
     * @return [Array]
     */
    private val appFilteredData
        get() = if (filterText.isBlank()) appConfigData.filter { if (it.isSystem) showSystem else true }
        else appConfigData.filter {
            (if (it.isSystem) showSystem else true) and
                    it.name.lowercase()
                        .contains(filterText.lowercase()) || it.packageName.lowercase()
                .contains(filterText.lowercase())
        }

    override fun onBackPressed() {
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