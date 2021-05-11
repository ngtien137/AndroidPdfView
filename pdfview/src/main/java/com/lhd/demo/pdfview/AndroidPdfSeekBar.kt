package com.lhd.demo.pdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.lhd.demo.pdfview.utils.ViewUtils.dpToPx
import com.lhd.demo.pdfview.utils.ViewUtils.getAppTypeFace
import com.lhd.demo.pdfview.utils.ViewUtils.set
import kotlin.math.abs
import kotlin.math.roundToInt

class AndroidPdfSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG)

    private var barCap: BarCap = BarCap.ROUND
    private var orientation = Orientation.HORIZONTAL

    private val rectView = RectF()

    /**
     * Thumb
     */

    private var thumbWidth = 0f
    private var thumbHeight = 0f
    private val paintThumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thumbDrawable: Drawable? = null
    private val rectThumb = Rect()
    private var isEnableThumbShadow = false

    /**
     * Indicator
     */
    private var indicatorMode = IndicatorMode.INSIDE_THUMB
    private var rectTextIndicator = Rect()
    private var paintText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    /**
     * View Value
     */

    var currentPage = 0f
        private set
    var totalPage = 100f
        private set

    /**
     * Action Value
     */

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    private val pointDown by lazy {
        PointF()
    }
    var isThumbMoving = false
        private set

    var listener: Listener? = null

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.AndroidPdfSeekBar)

            orientation = when (ta.getInt(
                R.styleable.AndroidPdfSeekBar_aps_orientation,
                Orientation.HORIZONTAL.value
            )) {
                Orientation.VERTICAL.value -> {
                    Orientation.VERTICAL
                }
                else -> {
                    Orientation.HORIZONTAL
                }
            }

            barCap =
                when (ta.getInt(R.styleable.AndroidPdfSeekBar_aps_bar_cap, BarCap.ROUND.value)) {
                    BarCap.BUTT.value -> {
                        BarCap.BUTT
                    }
                    else -> {
                        BarCap.ROUND
                    }
                }
            paintBar.color = ta.getColor(R.styleable.AndroidPdfSeekBar_aps_bar_color, Color.GRAY)
            paintBar.strokeWidth =
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_bar_height, dpToPx(6f))
            paintBar.strokeCap = Paint.Cap.ROUND

            paintProgress.color =
                ta.getColor(R.styleable.AndroidPdfSeekBar_aps_progress_color, Color.GREEN)
            paintProgress.strokeWidth =
                ta.getDimension(
                    R.styleable.AndroidPdfSeekBar_aps_progress_height,
                    paintBar.strokeWidth
                )
            paintProgress.strokeCap = Paint.Cap.ROUND

            indicatorMode = when (ta.getInt(
                R.styleable.AndroidPdfSeekBar_aps_indicator_mode,
                IndicatorMode.INSIDE_THUMB.value
            )) {
                IndicatorMode.HIDDEN.value -> {
                    IndicatorMode.HIDDEN
                }
                IndicatorMode.ABOVE_THUMB.value -> {
                    IndicatorMode.ABOVE_THUMB
                }
                IndicatorMode.BELOW_THUMB.value -> {
                    IndicatorMode.BELOW_THUMB
                }
                else -> {
                    IndicatorMode.INSIDE_THUMB
                }
            }

            thumbWidth = ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_thumb_width, dpToPx(20f))
            thumbHeight =
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_thumb_height, dpToPx(20f))
            thumbDrawable = ta.getDrawable(R.styleable.AndroidPdfSeekBar_aps_thumb)
            isEnableThumbShadow =
                ta.getBoolean(R.styleable.AndroidPdfSeekBar_aps_enable_shadow_for_thumb, false)

            val shadowColor =
                ta.getColor(R.styleable.AndroidPdfSeekBar_aps_shadow_color, Color.GRAY)
            val shadowRadius = ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_shadow_radius, 0f)
            paintBar.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
            paintThumb.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
            paintThumb.color = shadowColor

            paintText.textSize =
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_indicator_text_size, 20f)
            val fontId = ta.getResourceId(R.styleable.AndroidPdfSeekBar_aps_indicator_text_font, -1)
            if (fontId != -1) {
                paintText.typeface = context.getAppTypeFace(fontId)
            }
            paintText.color =
                ta.getColor(R.styleable.AndroidPdfSeekBar_aps_indicator_text_color, Color.WHITE)

            currentPage =
                ta.getInteger(R.styleable.AndroidPdfSeekBar_aps_current_page, 0).toFloat()
            totalPage = ta.getInteger(R.styleable.AndroidPdfSeekBar_aps_total_page, 100).toFloat()

            ta.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == Orientation.HORIZONTAL) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val minHeightForWrap =
                maxOf(paintBar.strokeWidth, paintProgress.strokeWidth, thumbHeight).toInt()
            val height = measureDimension(minHeightForWrap, heightMeasureSpec)
            setMeasuredDimension(width, height)
        } else { //Orientation.VERTICAL
            val height = MeasureSpec.getSize(heightMeasureSpec)
            val minWidthForWrap =
                maxOf(paintBar.strokeWidth, paintProgress.strokeWidth, thumbWidth).toInt()
            val width = measureDimension(minWidthForWrap, widthMeasureSpec)
            setMeasuredDimension(width, height)
        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == MeasureSpec.AT_MOST) {
                result = kotlin.math.min(result, specSize)
            }
        }
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (orientation == Orientation.HORIZONTAL) {
            rectView.set(
                paddingStart + thumbWidth / 2f,
                paddingTop.toFloat(),
                w - paddingEnd - thumbWidth / 2f,
                (h - paddingBottom).toFloat()
            )
        } else {
            rectView.set(
                paddingStart.toFloat(),
                paddingTop + thumbHeight / 2f,
                (w - paddingEnd).toFloat(),
                h - paddingBottom - thumbHeight / 2f
            )
        }
        validateThumbWithProgress()
    }

    /**
     *
     */

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { canvas ->
            drawBar(canvas)
            drawProgress(canvas)
            drawThumb(canvas)
            drawTextIndicator(canvas)
        }
    }

    private fun drawBar(canvas: Canvas) {
        if (orientation == Orientation.HORIZONTAL) {
            canvas.drawLine(
                rectView.left,
                rectView.centerY(),
                rectView.right,
                rectView.centerY(),
                paintBar
            )
        } else {
            canvas.drawLine(
                rectView.centerX(),
                rectView.top,
                rectView.centerX(),
                rectView.bottom,
                paintBar
            )
        }
    }

    private fun drawProgress(canvas: Canvas) {
        if (orientation == Orientation.HORIZONTAL) {
            canvas.drawLine(
                rectView.left,
                rectView.centerY(),
                currentPage.ProgressToPixel(),
                rectView.centerY(),
                paintProgress
            )
        } else {
            canvas.drawLine(
                rectView.centerX(),
                rectView.top,
                rectView.centerX(),
                currentPage.ProgressToPixel(),
                paintProgress
            )
        }
    }

    private fun drawThumb(canvas: Canvas) {
        if (isEnableThumbShadow) {
            canvas.drawOval(RectF(rectThumb), paintThumb)
        }
        thumbDrawable?.let { thumbDrawable ->
            thumbDrawable.bounds = Rect(rectThumb)
            thumbDrawable.draw(canvas)
        }
    }

    private fun drawTextIndicator(canvas: Canvas) {
        val textPage = currentPage.toInt().toString()
        paintText.getTextBounds(textPage, 0, textPage.length, rectTextIndicator)
        //val textSize = calculateTextSize(textPage)
        val textWidth = paintText.measureText(textPage)
        val thumbWidthScale = thumbWidth * 0.7f
        val thumbHeightScale = thumbHeight * 0.7f
        if (textWidth > thumbWidthScale) {
            val ratio = thumbWidthScale / textWidth
            val bitmapText = Bitmap.createBitmap(
                textWidth.roundToInt(),
                (thumbHeightScale / ratio).roundToInt(), Bitmap.Config.ARGB_8888
            )
            val canvasText = Canvas(bitmapText)
            val textX = bitmapText.width / 2f
            val textY = bitmapText.height / 2f + rectTextIndicator.height() / 2f
            canvasText.drawText(textPage, textX, textY, paintText)
            val scaleBitmapText = Bitmap.createScaledBitmap(
                bitmapText,
                (bitmapText.width * ratio).roundToInt(),
                (bitmapText.height * ratio).roundToInt(), true
            )
            val textBitmapX = rectThumb.centerX() - scaleBitmapText.width / 2f
            val textBitmapY = rectThumb.centerY() - scaleBitmapText.height / 2f
            canvas.drawBitmap(scaleBitmapText, textBitmapX, textBitmapY, Paint())
        } else {
            val textX = rectThumb.centerX().toFloat()
            val textY = rectThumb.centerY() + rectTextIndicator.height() / 2f
            canvas.drawText(textPage, textX, textY, paintText)
        }

    }

    private fun getPdfView(): AndroidPdfView? {
        val pdfView = parent.parent
        return if (pdfView is AndroidPdfView) pdfView else null
    }


    fun getCenterThumbX() = rectThumb.centerX()
    fun getCenterThumbY() = rectThumb.centerY()

    /**
     * Calculate
     */

    private fun Float.ProgressToPixel(): Float {
        return if (orientation == Orientation.HORIZONTAL) {
            (this / totalPage) * rectView.width() + rectView.left
        } else {
            (this / totalPage) * rectView.height() + rectView.top
        }
    }

    private fun Number.PixelToProgress(): Float {
        return if (orientation == Orientation.HORIZONTAL) {
            ((this.toFloat() - rectView.left) / rectView.width()) * totalPage
        } else {
            ((this.toFloat() - rectView.top) / rectView.height()) * totalPage
        }
    }

    /**
     * Validate Data
     */

    private fun validateThumbWithProgress() {
        val centerProgress = currentPage.ProgressToPixel()
        if (orientation == Orientation.HORIZONTAL) {
            rectThumb.set(
                centerProgress - thumbWidth / 2f,
                rectView.centerY() - thumbHeight / 2f,
                centerProgress + thumbWidth / 2f,
                rectView.centerY() + thumbHeight / 2f
            )
        } else {
            rectThumb.set(
                rectView.centerX() - thumbWidth / 2f,
                centerProgress - thumbHeight / 2f,
                rectView.centerX() + thumbWidth / 2f,
                centerProgress + thumbHeight / 2f
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentPage = if (orientation == Orientation.HORIZONTAL) event.x.PixelToProgress()
                else event.y.PixelToProgress()
                if (currentPage < 0) {
                    currentPage = 0f
                } else if (currentPage > totalPage) {
                    currentPage = totalPage
                }
                pointDown.set(event.x, event.y)
                validateThumbWithProgress()
                postInvalidate()
                getPdfView()?.showPageThumbnail(currentPage.toInt())
                return true
            }
            MotionEvent.ACTION_MOVE -> {

                val disX: Float = event.x - pointDown.x
                val disY: Float = event.y - pointDown.y
                return if (isThumbMoving) {
                    moveThumb(disX, disY)
                    pointDown.set(event.x, event.y)
                    postInvalidate()
                    getPdfView()?.showPageThumbnail(currentPage.toInt())
                    true
                } else {
                    val distanceMove =
                        if (orientation == Orientation.HORIZONTAL) abs(disX) else abs(disY)
                    if (distanceMove >= touchSlop) {
                        isThumbMoving = true
                        pointDown.set(event.x, event.y)
                        true
                    } else {
                        false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                getPdfView()?.clearPageThumbnail()
                listener?.onPageSelectedChanged(currentPage.toInt(), true)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun moveThumb(disX: Float, disY: Float) {
        if (orientation == Orientation.HORIZONTAL) {
            val minX = rectView.left
            val maxX = rectView.right
            val newCenterX = when {
                rectThumb.centerX() + disX < minX -> minX
                rectThumb.centerX() + disX > maxX -> maxX
                else -> rectThumb.centerX() + disX
            }
            rectThumb.left = (newCenterX - thumbWidth / 2f).roundToInt()
            rectThumb.right = (newCenterX + thumbWidth / 2f).roundToInt()
            currentPage = rectThumb.centerX().PixelToProgress()
        } else {
            val minY = rectView.top
            val maxY = rectView.bottom
            val newCenterY = when {
                rectThumb.centerY() + disY < minY -> minY
                rectThumb.centerY() + disY > maxY -> maxY
                else -> rectThumb.centerY() + disY
            }
            rectThumb.top = (newCenterY - thumbHeight / 2f).roundToInt()
            rectThumb.bottom = (newCenterY + thumbHeight / 2f).roundToInt()
            currentPage = rectThumb.centerY().PixelToProgress()
        }
    }

    /**
     * Properties action
     */

    fun getOrientation() = orientation

    fun setTotalPage(totalPage: Int) {
        this.totalPage = (totalPage - 1).toFloat()
        currentPage = 0f
        postInvalidate()
    }

    fun setCurrentPage(pageIndex: Int) {
        val index = if (pageIndex < 0) 0f else if (pageIndex > totalPage) totalPage else pageIndex
        this.currentPage = index.toFloat()
        validateThumbWithProgress()
        postInvalidate()
    }

    /**
     * Child properties
     */

    enum class Orientation(val value: Int) {
        HORIZONTAL(0), VERTICAL(1)
    }

    enum class IndicatorMode(val value: Int) {
        HIDDEN(-1), INSIDE_THUMB(0), ABOVE_THUMB(1), BELOW_THUMB(2)
    }

    enum class BarCap(val value: Int) {
        BUTT(0), ROUND(1)
    }

    interface Listener {
        fun onPageSelectedChanged(pageIndex: Int, isSelectedByTouch: Boolean)
    }

}