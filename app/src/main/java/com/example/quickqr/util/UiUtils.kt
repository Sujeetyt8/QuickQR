package com.example.quickqr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.*
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.quickqr.R
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * Utility class for common UI operations and extensions.
 */
object UiUtils {

    //region Dimension Conversions
    
    /**
     * Convert dp to pixels.
     */
    fun dpToPx(context: Context, dp: Float): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * Convert pixels to dp.
     */
    fun pxToDp(context: Context, px: Float): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * Convert sp to pixels.
     */
    fun spToPx(context: Context, sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }

    //endregion

    //region Color Utils
    
    /**
     * Lighten a color by a factor.
     * @param color The color to lighten.
     * @param factor The factor to lighten the color (0.0f to 1.0f).
     */
    fun lightenColor(@ColorInt color: Int, factor: Float): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, factor)
    }

    /**
     * Darken a color by a factor.
     * @param color The color to darken.
     * @param factor The factor to darken the color (0.0f to 1.0f).
     */
    fun darkenColor(@ColorInt color: Int, factor: Float): Int {
        return ColorUtils.blendARGB(color, Color.BLACK, factor)
    }

    /**
     * Check if a color is dark.
     */
    fun isColorDark(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    /**
     * Get a contrasting color (black or white) for the given background color.
     */
    @ColorInt
    fun getContrastColor(@ColorInt color: Int): Int {
        return if (isColorDark(color)) Color.WHITE else Color.BLACK
    }

    //endregion

    //region View Utils
    
    /**
     * Show a snackbar.
     */
    fun showSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        actionText?.let {
            snackbar.setAction(it) { action?.invoke() }
        }
        snackbar.show()
    }

    /**
     * Hide the keyboard.
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Show the keyboard.
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Toggle the keyboard visibility.
     */
    fun toggleKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    //endregion

    //region Drawable Utils
    
    /**
     * Tint a drawable with the given color.
     */
    fun tintDrawable(drawable: Drawable, @ColorInt color: Int): Drawable {
        val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTint(wrappedDrawable, color)
        return wrappedDrawable
    }

    /**
     * Create a circular drawable with the given color and size.
     */
    fun createCircularDrawable(
        @ColorInt color: Int,
        sizeInDp: Float = 24f,
        strokeColor: Int? = null,
        strokeWidthInDp: Float = 0f
    ): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        shape.setColor(color)
        
        strokeColor?.let {
            val strokeWidth = (strokeWidthInDp * 1.5f).toInt()
            shape.setStroke(strokeWidth, it)
        }
        
        shape.setSize(
            (sizeInDp * 1.5f).toInt(),
            (sizeInDp * 1.5f).toInt()
        )
        
        return shape
    }

    //endregion

    //region Text Utils
    
    /**
     * Make text bold.
     */
    fun makeTextBold(text: String): SpannableString {
        return SpannableString(text).apply {
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Make text italic.
     */
    fun makeTextItalic(text: String): SpannableString {
        return SpannableString(text).apply {
            setSpan(StyleSpan(android.graphics.Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Change text color.
     */
    fun changeTextColor(text: String, @ColorInt color: Int): SpannableString {
        return SpannableString(text).apply {
            setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Change text size.
     */
    fun changeTextSize(text: String, sizeInSp: Float): SpannableString {
        return SpannableString(text).apply {
            setSpan(AbsoluteSizeSpan(sizeInSp.toInt(), true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Add a clickable span to text.
     */
    fun addClickableSpan(
        text: String,
        clickableText: String,
        onClick: () -> Unit
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val start = text.indexOf(clickableText)
        if (start >= 0) {
            val end = start + clickableText.length
            builder.setSpan(
                clickableSpan,
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    //endregion

    //region Image Loading
    
    /**
     * Load an image into an ImageView with Glide.
     */
    fun loadImage(
        context: Context,
        imageView: ImageView,
        url: String?,
        @DrawableRes placeholder: Int = R.drawable.ic_placeholder,
        @DrawableRes error: Int = R.drawable.ic_error,
        centerCrop: Boolean = true,
        circleCrop: Boolean = false
    ) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholder)
            return
        }

        val requestOptions = RequestOptions()
            .placeholder(placeholder)
            .error(error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply {
                if (centerCrop) centerCrop()
                if (circleCrop) circleCrop()
            }

        Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }

    //endregion

    //region View Extensions
    
    /**
     * Set visibility to View.VISIBLE.
     */
    fun View.visible() {
        visibility = View.VISIBLE
    }

    /**
     * Set visibility to View.INVISIBLE.
     */
    fun View.invisible() {
        visibility = View.INVISIBLE
    }

    /**
     * Set visibility to View.GONE.
     */
    fun View.gone() {
        visibility = View.GONE
    }

    /**
     * Set visibility based on a condition.
     */
    fun View.visibleIf(condition: Boolean) {
        visibility = if (condition) View.VISIBLE else View.GONE
    }

    /**
     * Set enabled state with alpha animation.
     */
    fun View.setEnabledWithAlpha(enabled: Boolean, disabledAlpha: Float = 0.5f) {
        isEnabled = enabled
        alpha = if (enabled) 1f else disabledAlpha
    }

    //endregion

    //region Text View Extensions
    
    /**
     * Set HTML text to a TextView.
     */
    fun TextView.setHtmlText(html: String) {
        text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Set text appearance with compatibility.
     */
    fun TextView.setTextAppearanceCompat(@StyleRes resId: Int) {
        TextViewCompat.setTextAppearance(this, resId)
    }

    //endregion

    //region Context Extensions
    
    /**
     * Get a color with compatibility.
     */
    @ColorInt
    fun Context.getColorCompat(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }

    /**
     * Get a drawable with compatibility.
     */
    fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable? {
        return ContextCompat.getDrawable(this, resId)
    }

    //endregion

    //region Other Utils
    
    /**
     * Generate a random color.
     */
    @ColorInt
    fun generateRandomColor(): Int {
        val random = Random()
        return Color.argb(
            255,
            random.nextInt(256),
            random.nextInt(256),
            random.nextInt(256)
        )
    }

    /**
     * Create a bitmap from a view.
     */
    fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    //endregion
}
