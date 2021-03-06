package com.lhd.demo.pdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.lhd.demo.pdfview.utils.ViewUtils.dpToPx
import com.lhd.demo.pdfview.utils.ViewUtils.set
import kotlin.math.abs

class AndroidPdfSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG)

    private var barCap: BarCap = BarCap.ROUND
    private var indicatorMode = IndicatorMode.INSIDE_THUMB
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

    /**
     * View Value
     */

    private var currentPage = 0f
    private var totalPage = 100f

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
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_bar_height, dpToPx(10f))
            paintBar.strokeCap = Paint.Cap.ROUND

            paintProgress.color =
                ta.getColor(R.styleable.AndroidPdfSeekBar_aps_progress_color, Color.GREEN)
            paintProgress.strokeWidth =
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_bar_height, paintBar.strokeWidth)
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
                ta.getDimension(R.styleable.AndroidPdfSeekBar_aps_thumb_width, dpToPx(20f))
            thumbDrawable = ta.getDrawable(R.styleable.AndroidPdfSeekBar_aps_thumb)

            currentPage = ta.getInteger(R.styleable.AndroidPdfSeekBar_aps_current_page, 0).toFloat()
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
        thumbDrawable?.let { thumbDrawable ->
            thumbDrawable.bounds = rectThumb
            thumbDrawable.draw(canvas)
        }
    }

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

    private fun Float.PixelToProgress(): Float {
        return if (orientation == Orientation.HORIZONTAL) {
            ((this - rectView.left) / rectView.width()) * totalPage
        } else {
            ((this - rectView.top) / rectView.height()) * totalPage
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
                currentPage = event.x.PixelToProgress()
                pointDown.set(event.x, event.y)
                validateThumbWithProgress()
                postInvalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val disX = event.x - pointDown.x
                if (isThumbMoving) {
                    currentPage = (rectThumb.centerX() + disX).PixelToProgress()
                    validateThumbWithProgress()
                    pointDown.set(event.x, event.y)
                    postInvalidate()
                } else {
                    return if (abs(disX) >= touchSlop) {
                        isThumbMoving = true
                        pointDown.set(event.x, event.y)
                        true
                    } else {
                        false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

            }
        }
        return super.onTouchEvent(event)
    }

    enum class Orientation(val value: Int) {
        HORIZONTAL(0), VERTICAL(1)
    }

    enum class IndicatorMode(val value: Int) {
        HIDDEN(-1), INSIDE_THUMB(0), ABOVE_THUMB(1), BELOW_THUMB(2)
    }

    enum class BarCap(val value: Int) {
        BUTT(0), ROUND(1)
    }

}