package com.lhd.demo.pdfview.utils

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat


object ViewUtils {
    fun View.dpToPx(dp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    )

    fun View.spToPx(sp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        context.resources.displayMetrics
    )

    fun Rect.set(left: Number, top: Number, right: Number, bottom: Number) {
        this.left = left.toInt()
        this.top = top.toInt()
        this.right = right.toInt()
        this.bottom = bottom.toInt()
    }

    fun RectF.set(left: Number, top: Number, right: Number, bottom: Number) {
        this.left = left.toFloat()
        this.top = top.toFloat()
        this.right = right.toFloat()
        this.bottom = bottom.toFloat()
    }

    fun RectF.setCenter(centerX: Number, centerY: Number, width: Number, height: Number) {
        set(
            centerX.toFloat() - width.toFloat() / 2,
            centerY.toFloat() - height.toFloat() / 2,
            centerX.toFloat() + width.toFloat() / 2,
            centerY.toFloat() + height.toFloat() / 2
        )
    }

    fun Rect.setCenter(centerX: Number, centerY: Number, width: Number, height: Number) {
        set(
            centerX.toInt() - width.toInt() / 2,
            centerY.toInt() - height.toInt() / 2,
            centerX.toInt() + width.toInt() / 2,
            centerY.toInt() + height.toInt() / 2
        )
    }

    fun Context.getAppTypeFace(@FontRes fontId: Int): Typeface? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            resources.getFont(fontId)
        } else
            ResourcesCompat.getFont(this, fontId)
    }
}