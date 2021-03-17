package com.lhd.demo.pdfview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.lhd.demo.pdfview.adapter.PagerAdapter
import com.lhd.demo.pdfview.fragments.ItemPdfPageFragment
import com.lhd.demo.pdfview.model.PageData
import com.lhd.demo.pdfview.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class AndroidPdfView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        val PAGE_OFFSET_LIMIT = 3
    }

    private val pager by lazy {
        findViewById<ViewPager2>(R.id.pager)
    }

    private val listPagerFragments by lazy {
        ArrayList<ItemPdfPageFragment>()
    }

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

    fun getPageOrientation() = pageData.pageOrientation

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_view, this)
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.AndroidPdfView)
            pageData.pageMargin =
                ta.getDimensionPixelSize(R.styleable.AndroidPdfView_apdf_page_margin, 0)
            pageData.pageBoundaries =
                ta.getDimensionPixelSize(R.styleable.AndroidPdfView_apdf_page_boundaries, 0)
            setBoundariesPadding(pageData.pageBoundaries)
            ta.recycle()
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPageIndex = position
            }
        })
    }

    fun load(path: String, fragmentManager: FragmentManager, lifecycle: Lifecycle) {
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
                        //path,
                        listPdfRenderer[indexPdfRendererForFragment],
                        pageData,
                        listPagerFragments
                    )
                )
            }
            withContext(Dispatchers.Main) {
                pager.offscreenPageLimit = PAGE_OFFSET_LIMIT - 2
                pagerAdapter = PagerAdapter(listPagerFragments, fragmentManager, lifecycle)
                pager.adapter = pagerAdapter
            }
        }
    }

    fun setBoundariesPadding(padding: Int) {
        val rv: RecyclerView = pager.getChildAt(0) as RecyclerView
        rv.clipToPadding = false
        this.pageData.pageBoundaries = padding
        if (getPageOrientation() == Orientation.VERTICAL)
            rv.setPadding(0, pageData.pageBoundaries, 0, pageData.pageBoundaries)
        else
            rv.setPadding(pageData.pageBoundaries, 0, pageData.pageBoundaries, 0)
    }

    fun setPageOrientation(orientation: Orientation) {
        pageData.pageOrientation = orientation
        if (orientation == Orientation.VERTICAL) {
            pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        } else {
            pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }
        setBoundariesPadding(pageData.pageBoundaries)
        pagerAdapter.notifyDataSetChanged()
    }

    fun getPageThumbnailBitmap(index: Int): Bitmap? {
        val pdfRenderer = PdfUtils.loadPdfRendererFromPath(pdfPath)
        val page = pdfRenderer.openPage(index)
        val bitmap =
            Bitmap.createBitmap(page.width, page.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    enum class Orientation {
        VERTICAL, HORIZONTAL
    }

}