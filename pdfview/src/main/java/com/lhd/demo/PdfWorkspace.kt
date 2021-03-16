package com.lhd.demo

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.lhd.demo.pdfview.AndroidPdfSeekBar

class PdfWorkspace @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var pageThumbnail: Drawable? = null

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
            pageThumbnail?.draw(canvas)
        }
    }

}