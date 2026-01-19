package moe.ouom.wekit.ui.creator.dialog

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import moe.ouom.wekit.BuildConfig
import moe.ouom.wekit.config.ConfigManager
import moe.ouom.wekit.constants.Constants
import moe.ouom.wekit.util.common.Utils.jumpUrl
import moe.ouom.wekit.util.log.Logger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainSettingsDialog(context: Context) : BaseRikkaDialog(context, "WeKit") {

    // 定义优先级 映射关系 (值 -> 显示文本)
    private val priorityMap = mapOf(
        10000 to "高优先级",
        50 to "智能",
        -10000 to "低优先级"
    )

    override fun initPreferences() {
        addCategory("设定")
        val categories = listOf(
            "聊天与消息" to "ic_twotone_message_24",
            "资料卡" to "ic_profile",
            "优化与修复" to "ic_baseline_auto_fix_high_24",
            "开发者选项" to "ic_baseline_developer_mode_24",
            "娱乐功能" to "ic_baseline_free_breakfast_24"
        )
        categories.forEach { (name, iconName) ->
            addPreference(title = name, iconName = iconName,
                onClick = { anchor, summaryView ->
                    CategorySettingsDialog(context, name).show()
                })
        }

        addCategory("调试")
        addSwitchPreference(
            key = Constants.PrekEnableLog,
            title = "日志记录",
            summary = "反馈问题前必须开启日志记录",
            iconName = "ic_baseline_border_color_24",
            useFullKey = true
        )

        // ==========================================
        // 兼容 (Compatibility)
        // ==========================================
        addCategory("兼容")

        val priorityKey = "${Constants.PrekCfgXXX}wekit_hook_priority"
        // 获取当前 int 值
        val currentPriorityVal = ConfigManager.getDefaultConfig().getInt(priorityKey, 50)
        // 转换为文字，如果找不到对应的值，则显示"自定义: [自定义的值]"
        val displaySummary = priorityMap[currentPriorityVal] ?: "自定义: $currentPriorityVal"

        // 调用 addPreference 并接收返回的 summary TextView
        addPreference(
            title = "XC_MethodHook 优先级",
            summary = displaySummary,
            iconName = "ic_outline_alt_route_24",
            onClick = { anchor, summaryView ->
                showPriorityPopup(anchor, priorityKey, summaryView)
            }
        )

        // ==========================================
        // 关于 (About)
        // ==========================================
        addCategory("关于")
        addPreference(title = "版本", summary = BuildConfig.VERSION_NAME)

        val buildTimeStr = try {
            val date = Date(BuildConfig.BUILD_TIMESTAMP)
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        } catch (_: Exception) { "N/A" }
        addPreference(title = "编译时间", summary = buildTimeStr)
        addPreference("Build UUID", BuildConfig.BUILD_UUID)

        addPreference(
            title = "GitHub",
            summary = "cwuom/wekit",
            iconName = "ic_github",
            onClick = { anchor, summaryView -> jumpUrl(context, "https://github.com/cwuom/wekit") }
        )
        addPreference(
            title = "Telegram",
            summary = "@ouom_pub",
            iconName = "ic_telegram",
            onClick = { anchor, summaryView -> jumpUrl(context, "https://t.me/ouom_pub") }
        )
    }

    /**
     * 使用 PopupMenu 实现依附窗口的菜单
     */
    private fun showPriorityPopup(anchor: View, key: String, summaryTextView: TextView?) {
        val popup = PopupMenu(context, anchor)
        val menu = popup.menu

        priorityMap.forEach { (value, title) ->
            val menuItem = menu.add(title)
            menuItem.setOnMenuItemClickListener {
                ConfigManager.getDefaultConfig().edit().putInt(key, value).apply()
                Logger.d("MainSettings: Priority changed to $value ($title)")
                summaryTextView?.text = title
                true
            }
        }

        popup.show()
    }
}