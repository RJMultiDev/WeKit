package moe.ouom.wekit.ui.creator.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.material.materialswitch.MaterialSwitch
import moe.ouom.wekit.config.ConfigManager
import moe.ouom.wekit.constants.Constants
import moe.ouom.wekit.util.common.ModuleRes
import moe.ouom.wekit.util.log.Logger

abstract class BaseRikkaDialog(
    context: Context,
    private val title: String
) : AppCompatDialog(context, getThemeId()) {

    companion object {
        private fun getThemeId(): Int {
            val themeId = ModuleRes.getId("Theme.WeKit", "style")
            return if (themeId != 0) themeId else android.R.style.Theme_DeviceDefault_Light_NoActionBar
        }
    }

    protected lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val layoutId = ModuleRes.getId("module_dialog_frame", "layout")
        if (layoutId == 0) return

        val rootView = layoutInflater.inflate(layoutId, null)
        setContentView(rootView)

        val idAppBar = ModuleRes.getId("topAppBar", "id")
        val toolbar = rootView.findViewById<Toolbar>(idAppBar)
        toolbar.title = title
        toolbar.setNavigationIcon(ModuleRes.getId("ic_outline_arrow_back_ios_new_24", "drawable"))
        toolbar.setNavigationOnClickListener { dismiss() }

        val idSettingsFrame = ModuleRes.getId("settings", "id")
        val settingsFrame = rootView.findViewById<FrameLayout>(idSettingsFrame)

        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
        }

        contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 100)
        }

        scrollView.addView(contentContainer)
        settingsFrame.addView(scrollView)

        initPreferences()
    }

    abstract fun initPreferences()

    // ----------------------------------------------------------------
    //  UI 构建辅助方法
    // ----------------------------------------------------------------

    protected fun addCategory(title: String) {
        val layoutId = ModuleRes.getId("module_item_entry_category", "layout")
        if (layoutId == 0) return

        val view = layoutInflater.inflate(layoutId, contentContainer, false)
        val idTitle = ModuleRes.getId("title", "id")
        val tvTitle = view.findViewById<TextView>(idTitle)

        // 隐藏不需要的元素
        val idIcon = ModuleRes.getId("icon", "id")
        val idArrow = ModuleRes.getId("arrow", "id")
        view.findViewById<View>(idIcon)?.visibility = View.GONE
        view.findViewById<View>(idArrow)?.visibility = View.GONE

        // 样式调整
        view.minimumHeight = 0
        view.background = null

        val density = context.resources.displayMetrics.density
        val paddingHorizontal = (16 * density).toInt()
        val paddingBottom = (8 * density).toInt()
        val isFirstItem = contentContainer.childCount == 0
        val paddingTop = if (isFirstItem) (10 * density).toInt() else (24 * density).toInt()

        view.setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)

        tvTitle.text = title
        tvTitle.textSize = 14f
        val accentColor = ModuleRes.getColor("colorAccent") ?: -16776961
        tvTitle.setTextColor(accentColor)

        contentContainer.addView(view)
    }

    /**
     * 添加开关选项
     * @param fullKey 如果为 true，则 key 不会自动拼接 PrekXXX 前缀 (用于 Config/Log 等全局设置)
     */
    protected fun addSwitchPreference(
        key: String,
        title: String,
        summary: String,
        iconName: String? = null,
        useFullKey: Boolean = false
    ) {
        val layoutId = ModuleRes.getId("module_item_switch", "layout")
        if (layoutId == 0) return

        val view = layoutInflater.inflate(layoutId, contentContainer, false)

        val tvTitle = view.findViewById<TextView>(ModuleRes.getId("title", "id"))
        val tvSummary = view.findViewById<TextView>(ModuleRes.getId("summary", "id"))
        val switchWidget = view.findViewById<MaterialSwitch>(ModuleRes.getId("widget_switch", "id"))
        val root = view.findViewById<View>(ModuleRes.getId("item_root", "id"))

        tvTitle.text = title
        tvSummary.text = summary

        // 图标处理
        if (iconName != null) {
            val ivIcon = view.findViewById<ImageView>(ModuleRes.getId("icon", "id"))
            val drawable = ModuleRes.getDrawable(iconName)
            if (drawable != null) {
                ivIcon.setImageDrawable(drawable)
                ivIcon.visibility = View.VISIBLE
            }
        }

        // Key 处理逻辑
        val configKey = if (useFullKey) key else "${Constants.PrekXXX}$key"
        val isChecked = ConfigManager.getDefaultConfig().getBooleanOrFalse(configKey)
        switchWidget.isChecked = isChecked

        val listener = CompoundButton.OnCheckedChangeListener { _, checked ->
            ConfigManager.getDefaultConfig().edit().putBoolean(configKey, checked).apply()
            Logger.d("BaseRikkaDialog: Config changed [$configKey] -> $checked")
        }

        switchWidget.setOnCheckedChangeListener(listener)
        root.setOnClickListener { switchWidget.toggle() }

        contentContainer.addView(view)
    }

    /**
     * 添加普通点击项 (类似 Preference)
     * 用于打开二级菜单、显示版本信息、链接跳转等
     */
    protected fun addPreference(
        title: String,
        summary: String? = null,
        iconName: String? = null,
        onClick: ((View, TextView?) -> Unit)? = null
    ): TextView? { // 返回 Summary TextView
        val layoutId = ModuleRes.getId("module_item", "layout")
        if (layoutId == 0) return null

        val view = layoutInflater.inflate(layoutId, contentContainer, false)

        val tvTitle = view.findViewById<TextView>(ModuleRes.getId("title", "id"))
        val tvSummary = view.findViewById<TextView>(ModuleRes.getId("summary", "id"))
        val ivIcon = view.findViewById<ImageView>(ModuleRes.getId("icon", "id"))
        val ivArrow = view.findViewById<View>(ModuleRes.getId("arrow", "id"))
        val root = view.findViewById<View>(ModuleRes.getId("item_root", "id"))

        tvTitle.text = title

        if (!summary.isNullOrEmpty()) {
            tvSummary.text = summary
            tvSummary.visibility = View.VISIBLE
        } else {
            tvSummary.visibility = if (summary == null) View.GONE else View.VISIBLE
        }

        if (iconName != null) {
            val drawable = ModuleRes.getDrawable(iconName)
            if (drawable != null) {
                ivIcon.setImageDrawable(drawable)
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_menu_agenda)
            }
            ivIcon.visibility = View.VISIBLE
        } else {
            ivIcon.visibility = View.GONE
        }

        if (onClick != null) {
            root.setOnClickListener { onClick.invoke(root, tvSummary) }
            ivArrow?.visibility = View.VISIBLE
        } else {
            root.isClickable = false
            root.background = null
            ivArrow?.visibility = View.GONE
        }

        contentContainer.addView(view)

        // 返回 summary view
        return tvSummary
    }
}