package com.CXXX

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PerverzijaPlugin : Plugin() {

    override fun load(context: Context) {
        Perverzija.pluginContext = context.applicationContext

        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(Perverzija())
        registerExtractorAPI(Xtremestream())

        openSettings = { ctx ->
            showSettingsDialog(ctx)
        }
    }

    private fun dp(ctx: Context, value: Int): Int {
        return (value * ctx.resources.displayMetrics.density).toInt()
    }

    private fun roundedDrawable(
        ctx: Context,
        bgColor: Int,
        radiusDp: Int = 12,
        strokeColor: Int = 0,
        strokeWidthDp: Int = 0
    ): GradientDrawable {
        val rPx = radiusDp * ctx.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = rPx
            if (strokeWidthDp > 0) {
                setStroke((strokeWidthDp * ctx.resources.displayMetrics.density).toInt(), strokeColor)
            }
        }
    }

    private fun showSettingsDialog(ctx: Context) {
        val currentSelected = Perverzija.getSelectedCatalogues(ctx).toMutableSet()
        val allItems = Perverzija.allCatalogues
        val checkBoxMap = mutableMapOf<String, CheckBox>()

        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(ctx, Color.parseColor("#0F172A"), radiusDp = 16)
            setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // --- 1. HEADER ---
        val headerLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(ctx, 10))
        }

        val titleView = TextView(ctx).apply {
            text = "Perverzija Home Catalogues"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F8FAFC"))
        }

        val subtitleView = TextView(ctx).apply {
            text = "Select catalogues to load. Fewer rows = faster home loading & no timeouts."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(ctx, 2), 0, dp(ctx, 6))
        }

        val counterView = TextView(ctx).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
        }

        fun updateCounter() {
            counterView.text = "Selected: ${currentSelected.size} of ${allItems.size} catalogues"
        }
        updateCounter()

        headerLayout.addView(titleView)
        headerLayout.addView(subtitleView)
        headerLayout.addView(counterView)
        rootLayout.addView(headerLayout)

        // --- 2. QUICK ACTION BUTTONS ---
        val actionsLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(ctx, 10))
        }

        fun createPillButton(text: String, onClick: () -> Unit): TextView {
            return TextView(ctx).apply {
                this.text = text
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#E2E8F0"))
                background = roundedDrawable(ctx, Color.parseColor("#1E293B"), radiusDp = 8, strokeColor = Color.parseColor("#334155"), strokeWidthDp = 1)
                setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
                isFocusable = true
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, dp(ctx, 8), 0)
                layoutParams = params
                setOnClickListener { onClick() }
            }
        }

        val fastDefaultBtn = createPillButton("⚡ Fast (5)") {
            currentSelected.clear()
            currentSelected.addAll(Perverzija.defaultSelectedCatalogues)
            checkBoxMap.forEach { (name, cb) ->
                cb.isChecked = currentSelected.contains(name)
            }
            updateCounter()
        }

        val selectAllBtn = createPillButton("Select All (48)") {
            currentSelected.clear()
            allItems.forEach { currentSelected.add(it.name) }
            checkBoxMap.forEach { (_, cb) ->
                cb.isChecked = true
            }
            updateCounter()
        }

        val clearBtn = createPillButton("Clear") {
            currentSelected.clear()
            currentSelected.add("Home") // Always keep at least Home
            checkBoxMap.forEach { (name, cb) ->
                cb.isChecked = currentSelected.contains(name)
            }
            updateCounter()
        }

        actionsLayout.addView(fastDefaultBtn)
        actionsLayout.addView(selectAllBtn)
        actionsLayout.addView(clearBtn)
        rootLayout.addView(actionsLayout)

        // --- 3. SCROLLABLE CATALOGUE CHECKBOXES ---
        val displayMetrics = ctx.resources.displayMetrics
        val maxScrollHeight = (displayMetrics.heightPixels * 0.50).toInt()

        val scrollView = ScrollView(ctx).apply {
            isFillViewport = true
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
            params.setMargins(0, 0, 0, dp(ctx, 12))
            layoutParams = params
            background = roundedDrawable(ctx, Color.parseColor("#0B0F19"), radiusDp = 10, strokeColor = Color.parseColor("#1E293B"), strokeWidthDp = 1)
            setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
        }

        val listLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(listLayout)

        val tintList = ColorStateList.valueOf(Color.parseColor("#38BDF8"))

        val categories = listOf("Essential", "Studios", "Tags")
        categories.forEach { cat ->
            val itemsInCat = allItems.filter { it.category == cat }
            if (itemsInCat.isNotEmpty()) {
                val catHeader = TextView(ctx).apply {
                    val label = when (cat) {
                        "Essential" -> "⚡ ESSENTIAL (RECOMMENDED FOR SPEED)"
                        "Studios" -> "🎬 STUDIOS (${itemsInCat.size})"
                        "Tags" -> "🏷️ TAGS & CATEGORIES (${itemsInCat.size})"
                        else -> cat.uppercase()
                    }
                    text = label
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#38BDF8"))
                    setPadding(dp(ctx, 4), dp(ctx, 10), dp(ctx, 4), dp(ctx, 4))
                }
                listLayout.addView(catHeader)

                itemsInCat.forEach { item ->
                    val cb = CheckBox(ctx).apply {
                        text = item.name
                        textSize = 14f
                        setTextColor(Color.parseColor("#E2E8F0"))
                        buttonTintList = tintList
                        isChecked = currentSelected.contains(item.name)
                        isFocusable = true
                        setPadding(dp(ctx, 6), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6))

                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                currentSelected.add(item.name)
                            } else {
                                currentSelected.remove(item.name)
                            }
                            updateCounter()
                        }
                    }
                    checkBoxMap[item.name] = cb
                    listLayout.addView(cb)
                }
            }
        }

        rootLayout.addView(scrollView)

        // --- 4. BOTTOM ACTION BUTTONS ---
        val bottomLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val alertDialog = AlertDialog.Builder(ctx)
            .setView(rootLayout)
            .create()

        val cancelBtn = TextView(ctx).apply {
            text = "Cancel"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
            background = roundedDrawable(ctx, Color.parseColor("#1E293B"), radiusDp = 8)
            setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 16), dp(ctx, 10))
            isFocusable = true
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, dp(ctx, 10), 0)
            layoutParams = params
            setOnClickListener {
                alertDialog.dismiss()
            }
        }

        val saveBtn = TextView(ctx).apply {
            text = "Save & Apply"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedDrawable(ctx, Color.parseColor("#0284C7"), radiusDp = 8)
            setPadding(dp(ctx, 20), dp(ctx, 10), dp(ctx, 20), dp(ctx, 10))
            isFocusable = true
            setOnClickListener {
                val toSave = if (currentSelected.isEmpty()) setOf("Home") else currentSelected
                Perverzija.setSelectedCatalogues(ctx, toSave)
                Toast.makeText(
                    ctx,
                    "✓ Saved! Home will show ${toSave.size} catalogues. Pull down to refresh.",
                    Toast.LENGTH_LONG
                ).show()
                alertDialog.dismiss()
            }
        }

        bottomLayout.addView(cancelBtn)
        bottomLayout.addView(saveBtn)
        rootLayout.addView(bottomLayout)

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
    }
}
