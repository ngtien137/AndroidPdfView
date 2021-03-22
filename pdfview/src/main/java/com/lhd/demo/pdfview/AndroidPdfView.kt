package com.lhd.demo.pdfview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.children
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.lhd.demo.pdfview.adapter.PagerAdapter
import com.lhd.demo.pdfview.fragments.ItemPdfPageFragment
import com.lhd.demo.pdfview.data.PageData
import com.lhd.demo.pdfview.utils.FileChooser
import com.lhd.demo.pdfview.utils.PdfUtils
import com.lhd.demo.pdfview.utils.ViewUtils.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class AndroidPdfView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), AndroidPdfSeekBar.Listener {

    companion object {
        val PAGE_OFFSET_LIMIT = 3
    }

    private val pager by lazy {
        findViewById<ViewPager2>(R.id.pager)
    }

    private val listPagerFragments by lazy {
        ArrayList<ItemPdfPageFragment>()
    }

    var loadingListener: LoadingListener? = null

    var pageListener: PageListener? = null

    private lateinit var pdfRenderer: PdfRenderer

    private lateinit var pagerAdapter: PagerAdapter

    private var pdfPath = ""

    private var currentPageIndex = 0

    private val coroutineScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    private val pageData by lazy {
        PageData()
    }

    private var extraMarginHorizontalOfVerticalPage = 0

    //endregion

    //region thumbnail properties

    private lateinit var thumbnailRenderer: PdfRenderer

    private var pageThumbnail: Drawable? = null
    private var pageThumbnailBitmap: Bitmap? = null
    private var thumbnailIndex = -1

    private val paintThumbnail by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
    }
    private var thumbnailShadowColor = Color.GRAY

    private var thumbnailEnable = true

    private var seekBarMode = SeekBarMode.SHOW_WHEN_SCROLL

    private var handlerVisibleSeekBar: Handler? = null

    private val runnableSeekBar = Runnable {
        getVerticalPdfSeekBar()?.visibility = View.GONE
        getHorizontalPdfSeekBar()?.visibility = View.GONE
        needShowAfterScroll = false
    }
    private var needShowAfterScroll = false
    private var isTouchSeekBar = false

    private val TIME_HIDE_SEEKBAR_AFTER_SCROLLING = 3000L

    //endregion

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_view, this)
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AndroidPdfView)
            pageData.pageMargin =
                ta.getDimensionPixelSize(R.styleable.AndroidPdfView_apdf_page_margin, 0)
            pageData.pageBoundaries =
                ta.getDimensionPixelSize(R.styleable.AndroidPdfView_apdf_page_boundaries, 0)
            setBoundariesPadding(pageData.pageBoundaries)
            pageData.horizontalDivider =
                ta.getBoolean(R.styleable.AndroidPdfView_apdf_horizontal_divider_for_page, false)

            extraMarginHorizontalOfVerticalPage = ta.getDimensionPixelSize(
                R.styleable.AndroidPdfView_apdf_vertical_page_extra_margin_horizontal,
                0
            )
            thumbnailShadowColor =
                ta.getColor(R.styleable.AndroidPdfView_apdf_thumbnail_shadow_color, Color.GRAY)

            seekBarMode = when (ta.getInt(
                R.styleable.AndroidPdfView_apdf_seekbar_visible_mode,
                SeekBarMode.SHOW_WHEN_SCROLL.value
            )) {
                SeekBarMode.HIDDEN.value -> {
                    SeekBarMode.HIDDEN
                }
                SeekBarMode.VISIBLE.value -> {
                    SeekBarMode.VISIBLE
                }
                else -> {
                    SeekBarMode.SHOW_WHEN_SCROLL
                }
            }

            ta.recycle()
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                /**
                 * Nhảy vào đây khi action up seekbar hoặc vuốt page
                 * Khi nhảy vào đây thì cần hiển thị seekbar lên và chạy handler ẩn seekbar
                 * nếu seekbar đang ở mode chỉ hiển thị khi scroll
                 */
                currentPageIndex = position
                needShowAfterScroll = true
                validateOrientationWithSeekBar()
                startHandlerSeekBar()
                getVerticalPdfSeekBar()?.setCurrentPage(currentPageIndex)
                getHorizontalPdfSeekBar()?.setCurrentPage(currentPageIndex)
                pageListener?.onPageChanged(position)
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initListenerForSeekBar()
        validateOrientationWithSeekBar()
    }

    private fun initListenerForSeekBar() {
        getHorizontalPdfSeekBar()?.listener = this
        getVerticalPdfSeekBar()?.listener = this
    }

    private fun validateOrientationWithSeekBar() {
        if (getPageOrientation() == Orientation.VERTICAL) {
            getHorizontalPdfSeekBar()?.visibility = View.GONE
            if ((seekBarMode == SeekBarMode.SHOW_WHEN_SCROLL && needShowAfterScroll) || seekBarMode == SeekBarMode.VISIBLE)
                getVerticalPdfSeekBar()?.visibility = View.VISIBLE
            else
                getVerticalPdfSeekBar()?.visibility = View.GONE
        } else {
            getVerticalPdfSeekBar()?.visibility = View.GONE
            if ((seekBarMode == SeekBarMode.SHOW_WHEN_SCROLL && needShowAfterScroll) || seekBarMode == SeekBarMode.VISIBLE)
                getHorizontalPdfSeekBar()?.visibility = View.VISIBLE
            else
                getHorizontalPdfSeekBar()?.visibility = View.GONE
        }
    }

    fun load(uri: Uri, activity: AppCompatActivity) {
        load(uri, activity.supportFragmentManager, activity.lifecycle, activity)
    }

    fun load(uri: Uri, fragmentManager: FragmentManager, lifecycle: Lifecycle, context: Context) {
        try {
            this.pdfPath = FileChooser.getPath(context, uri)
            load(this.pdfPath, fragmentManager, lifecycle)
        } catch (e: Exception) {
            loadingListener?.onLoadPdfCompleted(false)
        }
    }

    fun load(path: String, activity: AppCompatActivity) {
        load(path, activity.supportFragmentManager, activity.lifecycle)
    }

    fun load(path: String, fragmentManager: FragmentManager, lifecycle: Lifecycle) {
        loadingListener?.onLoadPdfStarted()
        this.pdfPath = path
        val file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File [$path] is not exists")
        }
        listPagerFragments.clear()
        if (::pdfRenderer.isInitialized) {
            pdfRenderer.close()
        }
        coroutineScope.launch(Dispatchers.IO) {
            pdfRenderer = PdfUtils.loadPdfRendererFromPath(path)
            val pageCount = pdfRenderer.pageCount
            val listPdfRenderer = arrayListOf<PdfRenderer>().also {
                for (i in 0 until PAGE_OFFSET_LIMIT) {
                    it.add(PdfUtils.loadPdfRendererFromPath(path))
                }
            }
            for (i in 0 until pageCount) {
                val indexPdfRendererForFragment = i % PAGE_OFFSET_LIMIT
                listPagerFragments.add(
                    ItemPdfPageFragment.newInstance(
                        listPdfRenderer[indexPdfRendererForFragment],
                        pageData,
                        listPagerFragments
                    )
                )
            }
            withContext(Dispatchers.Main) {
                getHorizontalPdfSeekBar()?.setTotalPage(pageCount)
                getVerticalPdfSeekBar()?.setTotalPage(pageCount)
                pager.offscreenPageLimit = PAGE_OFFSET_LIMIT - 2
                pagerAdapter = PagerAdapter(listPagerFragments, fragmentManager, lifecycle)
                pager.adapter = pagerAdapter
                loadingListener?.onLoadPdfCompleted(true)
            }
        }
    }

    //region get set

    fun setBoundariesPadding(padding: Int) {
        val rv: RecyclerView = pager.getChildAt(0) as RecyclerView
        rv.clipToPadding = false
        this.pageData.pageBoundaries = padding
        if (getPageOrientation() == Orientation.VERTICAL) {
            val ratio = 2 * height.toFloat() / width
            rv.setPadding(
                0, (pageData.pageBoundaries * ratio).roundToInt(), 0,
                (pageData.pageBoundaries * ratio).roundToInt()
            )
        } else
            rv.setPadding(pageData.pageBoundaries, 0, pageData.pageBoundaries, 0)
    }

    fun getPageOrientation() = pageData.pageOrientation

    fun setPageOrientation(orientation: Orientation) {
        pageData.pageOrientation = orientation
        if (orientation == Orientation.VERTICAL) {
            pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        } else {
            pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
        setBoundariesPadding(pageData.pageBoundaries)
        pager.adapter = pagerAdapter
        validateOrientationWithSeekBar()
        pager.layoutParams =
            (pager.layoutParams as MarginLayoutParams).apply {
                if (pageData.pageOrientation == Orientation.HORIZONTAL) {
                    setMarginToLayoutParams(0, 0, 0, 0)
                } else {
                    setMarginToLayoutParams(
                        extraMarginHorizontalOfVerticalPage,
                        extraMarginHorizontalOfVerticalPage,
                        0,
                        0
                    )
                }
            }
    }

    private fun MarginLayoutParams.setMarginToLayoutParams(
        start: Number,
        end: Number,
        top: Number,
        bottom: Number
    ) {
        marginStart = start.toInt()
        marginEnd = end.toInt()
        topMargin = top.toInt()
        bottomMargin = bottom.toInt()
    }

    private fun getPageThumbnailBitmap(index: Int): Bitmap? {
        val bitmap = if (thumbnailIndex == -1) {
            thumbnailIndex = index
            generateThumbnailByIndex(index)
        } else {
            if (thumbnailIndex != index || pageThumbnailBitmap == null) {
                thumbnailIndex = index
                generateThumbnailByIndex(index)
            } else {
                pageThumbnailBitmap
            }

        }
        return bitmap
    }

    private fun generateThumbnailByIndex(index: Int): Bitmap? {
        val pdfRenderer = if (::thumbnailRenderer.isInitialized) {
            thumbnailRenderer
        } else {
            PdfUtils.loadPdfRendererFromPath(pdfPath).also {
                thumbnailRenderer = it
            }
        }
        val page = pdfRenderer.openPage(index)
        val bitmap =
            Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rectBackground = RectF().apply {
            left = 0.01f * bitmap.width
            top = 0.01f * bitmap.height
            right = bitmap.width - left
            bottom = bitmap.height - top
        }
        paintThumbnail.setShadowLayer(rectBackground.left, 0f, 0f, thumbnailShadowColor)
        canvas.drawRect(rectBackground, paintThumbnail)
        page.render(bitmap, null, Matrix().apply {
            setTranslate(0.01f * bitmap.width, 0.01f * bitmap.height)
            postScale(0.98f, 0.98f)
        }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    private fun getHorizontalPdfSeekBar(): AndroidPdfSeekBar? {
        return this.findViewById(R.id.pdf_seek_bar_horizontal)
    }

    private fun getVerticalPdfSeekBar(): AndroidPdfSeekBar? {
        return this.findViewById(R.id.pdf_seek_bar_vertical)
    }

    fun getPdfSeekBar(): AndroidPdfSeekBar? {
        val orientationForSeekBar =
            if (getPageOrientation() == Orientation.VERTICAL)
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

    //endregion

    //region draw

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        if (thumbnailEnable && pageThumbnail != null) {
            canvas?.let {
                val seekBar = getPdfSeekBar()
                val seekBarTop = seekBar?.y ?: 0f
                val seekBarLeft = seekBar?.x ?: 0f
                val seekBarCenterThumbX = seekBar?.getCenterThumbX()?.toFloat() ?: 0f
                val seekBarCenterThumbY = seekBar?.getCenterThumbY()?.toFloat() ?: 0f
                val thumbnailWidth = width / 3
                val thumbnailHeight = (thumbnailWidth * 297f / 210).toInt()
                var centerY = (seekBarTop - thumbnailHeight / 2f).toInt()
                var centerX = (seekBarLeft + seekBarCenterThumbX).toInt()
                if (getPageOrientation() == Orientation.VERTICAL) {
                    centerX = (seekBarLeft - thumbnailWidth / 2f).toInt()
                    centerY = (seekBarTop + seekBarCenterThumbY).toInt()
                }

                if (centerX < thumbnailWidth / 2f)
                    centerX = thumbnailWidth / 2
                else if (centerX > width - thumbnailWidth / 2f)
                    centerX = (width - thumbnailWidth / 2f).roundToInt()
                if (centerY < thumbnailHeight / 2f)
                    centerY = thumbnailHeight / 2
                else if (centerY > height - thumbnailHeight / 2f)
                    centerY = (height - thumbnailHeight / 2f).roundToInt()
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

    //endregion

    //region action

    fun showPageThumbnail(index: Int) {
        /**
         * Show thumbnail and validate seekbar when touch seekbar
         */
        isTouchSeekBar = true
        clearHandlerSeekBar()
        validateOrientationWithSeekBar()
        pageThumbnailBitmap = getPageThumbnailBitmap(index)
        pageThumbnail = pageThumbnailBitmap?.toDrawable(resources)
        postInvalidate()
    }

    fun clearPageThumbnail() {
        /**
         * Show thumbnail and validate seekbar when release seekbar
         */
        isTouchSeekBar = false
        startHandlerSeekBar()
        pageThumbnail = null
        pageThumbnailBitmap?.recycle()
        pageThumbnailBitmap = null
        thumbnailIndex = -1
        invalidate()
    }

    private fun startHandlerSeekBar() {
        clearHandlerSeekBar()
        if (seekBarMode == SeekBarMode.SHOW_WHEN_SCROLL) {
            handlerVisibleSeekBar = Handler(Looper.getMainLooper())
            handlerVisibleSeekBar?.postDelayed(runnableSeekBar, TIME_HIDE_SEEKBAR_AFTER_SCROLLING)
        }
    }

    private fun clearHandlerSeekBar() {
        if (seekBarMode == SeekBarMode.SHOW_WHEN_SCROLL) {
            handlerVisibleSeekBar?.removeCallbacks(runnableSeekBar)
            handlerVisibleSeekBar = null
        }
    }

    fun getPageCount() = listPagerFragments.size

    //endregion

    //region SeekBar Listener
    override fun onPageSelectedChanged(pageIndex: Int, isSelectedByTouch: Boolean) {
        if (isSelectedByTouch) {
            pager.setCurrentItem(pageIndex, false)
        }
    }

    //endregion

    //region inner class

    enum class Orientation {
        VERTICAL, HORIZONTAL
    }

    enum class SeekBarMode(val value: Int) {
        HIDDEN(-1), VISIBLE(0), SHOW_WHEN_SCROLL(1)
    }

    interface PageListener {
        fun onPageChanged(pageIndex: Int)
    }

    interface LoadingListener {
        fun onLoadPdfStarted()
        fun onLoadPdfCompleted(success: Boolean)
    }

    //endregion

}