package com.v2dawn.autotombstone.utils.tool

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException


object SystemTool {

    private val applicationInfoList: MutableList<ApplicationInfo> = ArrayList()

    fun isXposedModule(applicationInfo: ApplicationInfo?): Boolean {
        return if (applicationInfo?.metaData == null) false else applicationInfo.metaData.containsKey(
            "xposedmodule"
        )
    }


    fun isImportantSystemApp(applicationInfo: ApplicationInfo?): Boolean {
        return if (applicationInfo == null) false else applicationInfo.uid < 10000
    }

    fun isSystem(applicationInfo: ApplicationInfo?): Boolean {
        return if (applicationInfo == null) false else (applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    }

    fun loadApplicationInfos(pm: PackageManager, forceRefresh: Boolean) {
        if (!forceRefresh && applicationInfoList.size > 0) {
            return
        }
        applicationInfoList.clear()
        val allApplicationInfoList =
            pm.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES)
        applicationInfoList.addAll(allApplicationInfoList);

    }

    fun getApps(): List<ApplicationInfo>? {
        return applicationInfoList
    }

}