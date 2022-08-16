package com.v2dawn.autotombstone.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.v2dawn.autotombstone.databinding.ActivityAppConfigDetailBinding
import com.v2dawn.autotombstone.model.AppItemData
import com.v2dawn.autotombstone.ui.activity.base.BaseActivity
import com.v2dawn.autotombstone.utils.factory.buildAppItemData
import com.v2dawn.autotombstone.utils.factory.toast
import java.text.FieldPosition


class AppConfigureDetailActivity : BaseActivity<ActivityAppConfigDetailBinding>() {
    companion object {
        const val TAG = "AppConfigureDetailActivity"
    }

    private lateinit var appItemData: AppItemData;

    private var position: Int = -1;
    override fun onCreateWrapper(savedInstanceState: Bundle?) {

        val pkgName = intent.getStringExtra("pkgName");

        position = intent.getIntExtra("position", -1);

        appItemData = buildAppItemData(
            packageManager,
            packageManager.getApplicationInfo(pkgName!!, PackageManager.GET_META_DATA),
            ArrayList(),
            ArrayList()
        )

        binding.titleBackIcon.setOnClickListener { onBackPressed() }

        binding.adpAppIcon.setImageDrawable(appItemData.icon)

        binding.adpAppName.text = appItemData.name
        binding.adpAppPkgName.text = appItemData.packageName
        binding.appWhiteSwitch.isChecked = appItemData.enable
        binding.sysImpApp.isVisible = appItemData.isImportantSystemApp
        binding.sysApp.isVisible = appItemData.isSystem
        binding.xpModule.isVisible = appItemData.isXposedModule

        binding.iconContent.isVisible =
            appItemData.isImportantSystemApp || appItemData.isSystem || appItemData.isXposedModule
        binding.appWhiteSwitch.isEnabled = !appItemData.isImportantSystemApp
        binding.appWhiteSwitch.setOnCheckedChangeListener { btn, b ->
//                            binding.appWhiteSwitch.isEnabled = b
            //TODO notify adapter change enable
        }
        val onClickAction: View.OnClickListener = View.OnClickListener {
            toast("${appItemData.name} 是 ${it.tooltipText}")
        }
        binding.sysApp.setOnClickListener(onClickAction)
        binding.sysImpApp.setOnClickListener(onClickAction)
        binding.xpModule.setOnClickListener(onClickAction)

        onCreate()
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