package com.v2dawn.autotombstone.ui.activity

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.v2dawn.autotombstone.R
import com.v2dawn.autotombstone.databinding.ActivityAppConfigDetailBinding
import com.v2dawn.autotombstone.databinding.AdapterAppServerAppBinding
import com.v2dawn.autotombstone.model.AppItemData
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.*


class AppConfigureDetailActivity : BaseActivity<ActivityAppConfigDetailBinding>() {
    companion object {
        const val TAG = "AppConfigureDetailActivity"
    }

    data class ServiceDateItem(val serviceName: String, var label: String, var status: Int)

    private lateinit var appItemData: AppItemData;

    private val servicesData = ArrayList<ServiceDateItem>()
    private var onChanged: (() -> Unit)? = null

    private var position: Int = -1

    override fun onCreateWrapper(savedInstanceState: Bundle?) {

        val pkgName = intent.getStringExtra("pkgName");

        position = intent.getIntExtra("position", -1);

        if (position == -1) {
            Log.d(TAG, "position is -1, back to app configure activity");
            onBackPressed()
            return
        }
        val freeApps = hashSetOf<String>()

        getAtsService()?.queryBackgroundApps()?.forEach {
            freeApps.add(it)
        }

        appItemData = buildAppItemData(
            packageManager,
            packageManager.getApplicationInfo(
                pkgName!!,
                PackageManager.GET_META_DATA
            ),
            getBlackApps(),
            getWhiteApps(),
            null,
            freeApps,
            pkgName,
        )

        binding.titleBackIcon.setOnClickListener { onBackPressed() }

        binding.adpAppIcon.setImageDrawable(appItemData.icon)

        binding.adpAppName.text = appItemData.name
        binding.adpAppName.setTextColor(if (appItemData.inFreeze) getColor(R.color.red) else getColor(R.color.colorTextDark))
        binding.adpAppPkgName.text = appItemData.packageName
        binding.appWhiteSwitch.setOnCheckedChangeListener(null)
        binding.appWhiteSwitch.isChecked = appItemData.enable
        binding.sysImpApp.isVisible = appItemData.isImportantSystemApp
        binding.sysApp.isVisible = appItemData.isSystem
        binding.xpModule.isVisible = appItemData.isXposedModule
        binding.inFreeze.isVisible = appItemData.inFreeze

        binding.appWhiteSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (appItemData.isImportantSystemApp) {
                toast(getString(R.string.imp_sys_app_not_support));
                return@setOnCheckedChangeListener
            }

            if (appItemData.isSystem) {
                //black
                if (updateBlackApps(appItemData.packageName, isChecked)) {
                    //todo notify app list adapter?
                }
            } else {
                if (updateWhiteApps(appItemData.packageName, isChecked)) {
                    //todo notify app list adapter?
                }
            }
        }
        binding.iconContent.isVisible =
            appItemData.isImportantSystemApp || appItemData.isSystem || appItemData.isXposedModule
                    || appItemData.inFreeze
        binding.appWhiteSwitch.isEnabled = !appItemData.isImportantSystemApp


        binding.sysApp.setOnClickListener(onClickAction)
        binding.sysImpApp.setOnClickListener(onClickAction)
        binding.xpModule.setOnClickListener(onClickAction)
        binding.inFreeze.setOnClickListener{
            toast("${appItemData.name} 在 ${it.tooltipText}")
        }


        val services = HashSet<String>()


        var packageInfo: PackageInfo =
            packageManager.getPackageInfo(appItemData.packageName, PackageManager.GET_SERVICES)
        if (packageInfo.services != null) {
            for (service in packageInfo.services) {
                services.add(service.processName)
            }
        }

        val whiteProcesses = getWhiteProcesses();
        val killProcesses = getKillProcesses();

        for (service in services) {
            val status: Int = getStatus(whiteProcesses, killProcesses, service)
            servicesData.add(
                ServiceDateItem(
                    service,
                    status = status,
                    label = getStatusText(status)
                )
            )
        }

        binding.serviceListView.apply {
            bindAdapter {
                onBindDatas { servicesData }
                onBindViews<AdapterAppServerAppBinding> { binding, position ->
                    servicesData[position].also {
                        binding.appServiceName.text = it.serviceName
                        binding.appStatusText.text = it.label

                        binding.appServiceContent.setOnClickListener { view ->
                            showPopup(view, it.status) { index ->
                                val iwhiteProcesses = getWhiteProcesses()
                                val ikillProcesses = getKillProcesses()

                                when (index) {
                                    0 -> {
                                        if (iwhiteProcesses.contains(it.serviceName)) {
                                            setWhiteProcesses(iwhiteProcesses.minus(it.serviceName))
                                        } else if (killProcesses.contains(it.serviceName)) {
                                            setKillProcesses(ikillProcesses.minus(it.serviceName))
                                        }
                                    }
                                    1 -> {
                                        if (iwhiteProcesses.contains(it.serviceName)) {
                                            setWhiteProcesses(iwhiteProcesses.minus(it.serviceName))
                                        }
                                        if (!ikillProcesses.contains(it.serviceName)) {
                                            setKillProcesses(ikillProcesses.plus(it.serviceName))
                                        }
                                    }
                                    2 -> {
                                        if (ikillProcesses.contains(it.serviceName)) {
                                            setKillProcesses(ikillProcesses.minus(it.serviceName))
                                        }
                                        if (!iwhiteProcesses.contains(it.serviceName)) {
                                            setWhiteProcesses(iwhiteProcesses.plus(it.serviceName))
                                        }
                                    }
                                }

                                reBuildDataService(it)
                                if (onChanged != null) {
                                    onChanged!!()
                                }
                            }
                        }
                    }
                }
            }.apply {
                onChanged = {
                    notifyDataSetChanged()
                }
            }
        }
        onCreate()
    }

    private fun reBuildDataService(serviceDataItem: ServiceDateItem) {
        val whiteProcesses = getWhiteProcesses();
        val killProcesses = getKillProcesses();
        serviceDataItem.status =
            getStatus(whiteProcesses, killProcesses, serviceDataItem.serviceName)
        serviceDataItem.label =
            getStatusText(serviceDataItem.status)
    }

    private fun getStatusText(status: Int): String {
        when (status) {
            0 -> return getString(R.string.follow_process)
            1 -> return getString(R.string.force_process)
            else -> return getString(R.string.ignore_process)
        }
    }

    private fun getStatus(
        whiteProcesses: Set<String>,
        killProcesses: Set<String>,
        process: String
    ): Int {
        if (whiteProcesses.contains(process)) {
            return 2
//            return getString(R.string.ignore_process)
        }
        if (killProcesses.contains(process)) {
//            return getString(R.string.force_process)
            return 1
        }
//        return getString(R.string.follow_process)
        return 0
    }

    private val onClickAction: View.OnClickListener = View.OnClickListener {
        toast("${appItemData.name} 是 ${it.tooltipText}")
    }

    override fun onCreate() {

    }


    override fun finishAfterTransition() {
        val data = Intent()
        data.putExtra("position", position)
        setResult(RESULT_OK, data)
        super.finishAfterTransition()
    }

    override fun onBackPressed() {
        ActivityCompat.finishAfterTransition(this);
        super.onBackPressed()
    }
}