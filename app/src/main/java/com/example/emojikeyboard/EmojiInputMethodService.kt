package com.example.emojikeyboard

import android.content.Context
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * A self-contained custom keyboard (IME) that shows an emoji picker.
 *
 * How the "iOS-style" part works:
 *   - We try to load a font file the USER supplies at
 *     app/src/main/assets/fonts/ios_emoji.ttf
 *   - If present, that typeface is applied to the emoji keys' TextViews.
 *   - If missing, we silently fall back to the system default so the
 *     keyboard still works (just with your device's normal emoji look).
 *
 * Note: Android's text renderer has its own color-emoji fallback logic,
 * so on some Android versions the custom font may not change the emoji
 * glyph shapes even though it's applied. If that happens here, the fix
 * is to switch this picker from font-based glyphs to image-based emoji
 * (draw a PNG/WebP per emoji instead of relying on a font) - ask if you
 * want that version instead once you've tested this one.
 */
class EmojiInputMethodService : InputMethodService() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var emojiTypeface: Typeface

    override fun onCreateInputView(): View {
        emojiTypeface = loadEmojiTypeface()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(0xFFEDEDED.toInt())
        }

        // --- Category tab bar ---
        val tabScroll = HorizontalScrollView(this)
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        EmojiData.categories.forEachIndexed { index, category ->
            val tabBtn = Button(this).apply {
                text = category.tabLabel
                textSize = 18f
                setOnClickListener { showCategory(index) }
            }
            tabRow.addView(tabBtn)
        }
        tabScroll.addView(tabRow)
        root.addView(tabScroll)

        // --- Scrollable emoji grid ---
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
            )
        }
        contentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(contentContainer)
        root.addView(scrollView)

        // --- Bottom row: switch keyboard / backspace / space ---
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val switchBtn = Button(this).apply {
            text = "⌨"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        }
        val spaceBtn = Button(this).apply {
            text = "space"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentInputConnection?.commitText(" ", 1) }
        }
        val backspaceBtn = Button(this).apply {
            text = "⌫"
            setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) }
        }

        bottomRow.addView(switchBtn)
        bottomRow.addView(spaceBtn)
        bottomRow.addView(backspaceBtn)
        root.addView(bottomRow)

        showCategory(0)
        return root
    }

    private fun showCategory(index: Int) {
        contentContainer.removeAllViews()
        val category = EmojiData.categories[index]

        val grid = GridLayout(this).apply {
            columnCount = 8
        }

        category.emojis.forEach { emoji ->
            val key = TextView(this).apply {
                text = emoji
                textSize = 26f
                typeface = emojiTypeface
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(10), dp(6), dp(10))
                setOnClickListener {
                    currentInputConnection?.commitText(emoji, 1)
                }
            }
            grid.addView(key)
        }

        contentContainer.addView(grid)
    }

    private fun loadEmojiTypeface(): Typeface {
        return try {
            Typeface.createFromAsset(assets, "fonts/ios_emoji.ttf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
