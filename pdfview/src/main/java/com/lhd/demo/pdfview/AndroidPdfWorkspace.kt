package com.lhd.demo.pdfview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.children
import com.lhd.demo.pdfview.utils.ViewUtils.set

class AndroidPdfWorkspace @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var pageThumbnail: Drawable? = null

    private var thumbnailEnable = true

    init {

    }

    fun getPdfView(): AndroidPdfView? {
        val pdfView = findViewById<AndroidPdfView>(R.id.pdf_view)
        if (pdfView != null)
            return pdfView
        for (viewChild in children) {
            if (viewChild is AndroidPdfView) {
                return viewChild
            }
        }
        return null
    }

    private fun getHorizontalPdfSeekBar(): AndroidPdfSeekBar? {
        return this.findViewById(R.id.pdf_seek_bar_horizontal)
    }

    private fun getVerticalPdfSeekBar(): AndroidPdfSeekBar? {
        return this.findViewById(R.id.pdf_seek_bar_vertical)
    }

    fun getPdfSeekBar(): AndroidPdfSeekBar? {
        val orientationForSeekBar =
            if (getPdfView()?.getPageOrientation() == AndroidPdfView.Orientation.VERTICAL)
                AndroidPdfSeekBar.Orientation.VERTICAL else AndroidPdfSeekBar.Orientation.HORIZONTAL
        if (orientationForSeekBar == AndroidPdfSeekBar.Orientation.HORIZONTAL && getHorizontalPdfSeekBar() != null) {
            return getHorizontalPdfSeekBar()
        } else if (orientationForSeekBar == AndroidPdfSeekBar.Orientation.VERTICAL && getVerticalPdfSeekBar() != null) {
            return getVerticalPdfSeekBar()
        }
        for (viewChild in children) {
            if (viewChild is AndroidPdfSeekBar) {
                if (viewChild.getOrientation() == orientationForSeekBar)
                    return viewChild
            }
        }
        return null
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        if (thumbnailEnable && pageThumbnail != null) {
            canvas?.let {
                val seekBar = getPdfSeekBar()
                val thumbnailWidth = width / 3
                val thumbnailHeight = (thumbnailWidth * 297f / 210).toInt()
                var centerY = (seekBar?.top ?: 0f - thumbnailHeight / 2).toInt()
                var centerX = (seekBar?.getCenterThumbX() ?: 0f + thumbnailWidth / 2).toInt()
                if (getPdfView()?.getPageOrientation() == AndroidPdfView.Orientation.VERTICAL) {
                    centerX = (seekBar?.left ?: 0f - thumbnailWidth / 2f).toInt()
                    centerY = (seekBar?.getCenterThumbY() ?: 0f + thumbnailHeight / 2f).toInt()
                }

                if (centerX < thumbnailWidth / 2f)
                    centerX = thumbnailWidth / 2
                if (centerY < thumbnailHeight / 2f)
                    centerY = thumbnailHeight / 2
                pageThumbnail!!.bounds = Rect().also {
                    it.set(
                        centerX - thumbnailWidth / 2f,
                        centerY - thumbnailHeight / 2f,
                        centerX + thumbnailWidth / 2f,
                        centerY + thumbnailHeight / 2f
                    )
                }
                pageThumbnail?.draw(canvas)
            }
        }
    }

    fun showPageThumbnail(index: Int) {
        pageThumbnail = getPdfView()?.getPageThumbnailBitmap(index)?.toDrawable(resources)
        invalidate()
    }

    fun clearPageThumbnail() {
        pageThumbnail = null
        invalidate()
    }

}