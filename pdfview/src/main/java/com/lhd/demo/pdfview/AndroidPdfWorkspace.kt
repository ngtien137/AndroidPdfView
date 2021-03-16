package com.lhd.demo.pdfview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children

class AndroidPdfWorkspace @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {

    }

    fun getPdfSeekBar(): AndroidPdfSeekBar? {
        for (viewChild in children) {
            if (viewChild is AndroidPdfSeekBar) {
                return viewChild
            }
        }
        return null
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.let {
            getPdfSeekBar()?.pageThumbnail?.draw(canvas)
        }
    }

}