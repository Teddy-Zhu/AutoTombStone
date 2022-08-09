package com.v2dawn.tdytombstone.utils.tool

import android.content.Context
import com.google.android.material.snackbar.Snackbar
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.dataChannel
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.channel.data.ChannelData
import com.v2dawn.tdytombstone.constants.HookConst.SYSTEMUI_PACKAGE_NAME
import com.v2dawn.tdytombstone.utils.factory.showDialog

/**
 * 系统界面工具
 */
object SystemUITool {

    private val CALL_HOST_REFRESH_CACHING = ChannelData("call_host_refresh_caching", false)
    private val CALL_MODULE_REFRESH_RESULT = ChannelData("call_module_refresh_result", false)

    /**
     * 宿主注册监听
     */
    object Host {

        /**
         * 监听系统界面刷新改变
         * @param param 实例
         * @param result 回调 - ([Boolean] 是否成功)
         */
        fun onRefreshSystemUI(param: PackageParam, result: (Boolean) -> Boolean) {
            param.dataChannel.with { wait(CALL_HOST_REFRESH_CACHING) { put(CALL_MODULE_REFRESH_RESULT, result(it)) } }
        }
    }

    /**
     * 检查模块是否激活
     * @param context 实例
     * @param result 成功后回调
     */
    fun checkingActivated(context: Context, result: (Boolean) -> Unit) = context.dataChannel(SYSTEMUI_PACKAGE_NAME).checkingVersionEquals(result)

    /**
     * 重启系统界面
     * @param context 实例
     */
    fun restartSystemUI(context: Context) =
        context.showDialog {
            title = "重启系统界面"
            msg = "你确定要立即重启系统界面吗？\n\n" +
                    "重启过程会黑屏并等待进入锁屏重新解锁。"
            confirmButton {
                execShell(cmd = "pgrep systemui").also { pid ->
                    if (pid.isNotBlank())
                        execShell(cmd = "kill -9 $pid")
                    else toast(msg = "ROOT 权限获取失败")
                }
            }
            cancelButton()
        }

    /**
     * 刷新系统界面状态栏与通知图标
     * @param context 实例
     * @param isRefreshCacheOnly 仅刷新缓存不刷新图标和通知改变 - 默认：否
     * @param callback 成功后回调
     */
    fun refreshSystemUI(context: Context? = null, isRefreshCacheOnly: Boolean = false, callback: () -> Unit = {}) = runInSafe {
        if (YukiHookAPI.Status.isXposedModuleActive)
            context?.showDialog {
                title = "请稍后"
                progressContent = "正在等待系统界面刷新"
                /** 是否等待成功 */
                var isWaited = false
                /** 设置等待延迟 */
                delayedRun(ms = 5000) {
                    if (isWaited) return@delayedRun
                    cancel()
                    context.snake(msg = "预计响应超时，建议重启系统界面", actionText = "立即重启") { restartSystemUI(context) }
                }
                checkingActivated(context) { isValied ->
                    when {
                        isValied.not() -> {
                            cancel()
                            isWaited = true
                            context.snake(msg = "请重启系统界面以生效模块更新", actionText = "立即重启") { restartSystemUI(context) }
                        }
                        else -> context.dataChannel(SYSTEMUI_PACKAGE_NAME).with {
                            wait(CALL_MODULE_REFRESH_RESULT) {
                                cancel()
                                isWaited = true
                                callback()
                                if (it.not()) context.snake(msg = "刷新失败，建议重启系统界面", actionText = "立即重启") { restartSystemUI(context) }
                            }
                            put(CALL_HOST_REFRESH_CACHING, isRefreshCacheOnly)
                        }
                    }
                }
                noCancelable()
            }
        else context?.snake(msg = "模块没有激活，更改不会生效")
    }

    /**
     * 显示需要重启系统界面的 [Snackbar]
     * @param context 实例
     */
    fun showNeedRestartSnake(context: Context) =
        if (YukiHookAPI.Status.isXposedModuleActive)
            context.snake(msg = "设置需要重启系统界面才能生效", actionText = "立即重启") { restartSystemUI(context) }
        else context.snake(msg = "模块没有激活，更改不会生效")
}