package com.CXXX

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AllPornStreamPlugin: Plugin() {

    override fun load(context: Context) {
        AllPornStream.pluginContext = context.applicationContext

        registerMainAPI(AllPornStream())
        registerExtractorAPI(StreamTapeto())
        registerExtractorAPI(BigwarpIO())

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
        val currentSelected = AllPornStream.getSelectedCatalogues(ctx).toMutableSet()
        val allItems = AllPornStream.allCatalogues
        val checkBoxMap = mutableMapOf<String, CheckBox>()
        val categoryHeaderMap = mutableMapOf<String, TextView>()

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
            setPadding(0, 0, 0, dp(ctx, 8))
        }

        val titleView = TextView(ctx).apply {
            text = "AllPornStream Catalogues"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F8FAFC"))
        }

        val subtitleView = TextView(ctx).apply {
            text = "Choose which rows appear on your Home page. Fewer rows = faster load."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(ctx, 2), 0, dp(ctx, 4))
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

        // --- 2. SEARCH / FILTER BOX ---
        val searchBox = EditText(ctx).apply {
            hint = "🔍 Search ${allItems.size} catalogues..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 13f
            background = roundedDrawable(ctx, Color.parseColor("#0B0F19"), radiusDp = 8, strokeColor = Color.parseColor("#334155"), strokeWidthDp = 1)
            setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
            isSingleLine = true
            isFocusable = true
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, dp(ctx, 8))
            layoutParams = params
        }
        rootLayout.addView(searchBox)

        // --- 3. QUICK ACTION BUTTONS ---
        val actionsLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(ctx, 8))
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
            currentSelected.addAll(AllPornStream.defaultSelectedCatalogues)
            checkBoxMap.forEach { (name, cb) ->
                cb.isChecked = currentSelected.contains(name)
            }
            updateCounter()
        }

        val selectAllBtn = createPillButton("Select All (${allItems.size})") {
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

        // --- 4. SCROLLABLE CATALOGUE CHECKBOXES ---
        val displayMetrics = ctx.resources.displayMetrics
        val maxScrollHeight = (displayMetrics.heightPixels * 0.48).toInt()

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

        val categories = listOf("Essential", "Studios", "Categories")
        categories.forEach { cat ->
            val itemsInCat = allItems.filter { it.category == cat }
            if (itemsInCat.isNotEmpty()) {
                val catHeader = TextView(ctx).apply {
                    val label = when (cat) {
                        "Essential" -> "⚡ ESSENTIAL (${itemsInCat.size} - RECOMMENDED FOR SPEED)"
                        "Studios" -> "🎬 STUDIOS (${itemsInCat.size})"
                        "Categories" -> "🏷️ CATEGORIES & TAGS (${itemsInCat.size})"
                        else -> cat.uppercase()
                    }
                    text = label
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#38BDF8"))
                    setPadding(dp(ctx, 4), dp(ctx, 10), dp(ctx, 4), dp(ctx, 4))
                }
                categoryHeaderMap[cat] = catHeader
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

        // Live search listener
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                allItems.forEach { item ->
                    val cb = checkBoxMap[item.name] ?: return@forEach
                    val matches = q.isEmpty() || item.name.lowercase().contains(q)
                    cb.visibility = if (matches) View.VISIBLE else View.GONE
                }
                // Hide header if no matching items in that category
                categories.forEach { cat ->
                    val header = categoryHeaderMap[cat] ?: return@forEach
                    val itemsInCat = allItems.filter { it.category == cat }
                    val hasVisible = itemsInCat.any { (checkBoxMap[it.name]?.visibility ?: View.GONE) == View.VISIBLE }
                    header.visibility = if (hasVisible) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        rootLayout.addView(scrollView)

        // --- 5. BOTTOM ACTION BUTTONS ---
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
                AllPornStream.setSelectedCatalogues(ctx, toSave)
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