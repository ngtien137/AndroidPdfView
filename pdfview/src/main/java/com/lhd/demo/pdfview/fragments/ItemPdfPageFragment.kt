package com.lhd.demo.pdfview.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.github.chrisbanes.photoview.PhotoView
import com.lhd.demo.pdfview.AndroidPdfView
import com.lhd.demo.pdfview.R
import com.lhd.demo.pdfview.model.PageData
import com.lhd.demo.pdfview.utils.PdfUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemPdfPageFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(
            pdfPath: String,
            pageData: PageData,
            listFragments: MutableList<ItemPdfPageFragment>
        ) =
            ItemPdfPageFragment().apply {
                //this.pdfRenderer = pdfRenderer
                this.pdfPath = pdfPath
                this.listFragments = listFragments
                this.pageData = pageData
            }

        @JvmStatic
        fun newInstance(
            pdfRenderer: PdfRenderer,
            pageData: PageData,
            listFragments: MutableList<ItemPdfPageFragment>
        ) =
            ItemPdfPageFragment().apply {
                this.pdfRenderer = pdfRenderer
                this.listFragments = listFragments
                this.pageData = pageData
            }
    }

    private var pdfPath: String = ""

    private lateinit var pageData: PageData

    private lateinit var pdfRenderer: PdfRenderer

    private lateinit var listFragments: MutableList<ItemPdfPageFragment>

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var bitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_pdf_page, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    private fun initView() {
        requireView().findViewById<PhotoView>(R.id.imgPdf)?.let { imgPdf ->
            imgPdf.layoutParams =
                (imgPdf.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    if (pageData.pageOrientation == AndroidPdfView.Orientation.HORIZONTAL) {
                        marginStart = pageData.pageMargin
                        marginEnd = marginStart
                        topMargin = 0
                        bottomMargin = 0
                    } else {
                        topMargin = 0//pageData.pageMargin
                        bottomMargin = 0//topMargin
                        marginStart = 0
                        marginEnd = 0
                    }
                }
        }
        loadBitmapByPath()
    }

    private fun loadBitmapByPath() {
        coroutineScope.launch {
            if (!::pdfRenderer.isInitialized) {
                pdfRenderer = PdfUtils.loadPdfRendererFromPath(pdfPath)
            }
            val index = listFragments.indexOf(this@ItemPdfPageFragment)
            try {
                //Nếu try catch cái này có thể gây ra lỗi một trang nó đó bị trắng vì ko load được (Trùng renderer - page not close)
                val page = pdfRenderer.openPage(index)
                bitmap =
                    Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                withContext(Dispatchers.Main) {
                    requireView().findViewById<PhotoView>(R.id.imgPdf).setImageBitmap(bitmap)
                }
            } catch (e: IllegalStateException) {

            }

        }
    }

    override fun onDestroyView() {
        if (::bitmap.isInitialized)
            bitmap.recycle()
        super.onDestroyView()
    }

}