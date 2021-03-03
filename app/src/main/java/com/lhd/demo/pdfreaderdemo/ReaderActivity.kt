package com.lhd.demo.pdfreaderdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.lhd.demo.pdfview.AndroidPdfView
import kotlin.math.roundToInt

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PDF_PATH = "EXTRA_PDF_PATH"
    }

    private val androidPdfView by lazy {
        findViewById<AndroidPdfView>(R.id.androidPdfView)
    }

    private val btnChangeOrientation by lazy {
        findViewById<View>(R.id.btnChangeOrientation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        btnChangeOrientation.setOnClickListener {
            changeOrientation()
        }
        initView()
    }

    private fun initView() {
        val path = intent.getStringExtra(EXTRA_PDF_PATH) ?: ""
        androidPdfView.load(path, supportFragmentManager, lifecycle)
    }

    private fun changeOrientation() {
        if (androidPdfView.getPageOrientation() == AndroidPdfView.Orientation.VERTICAL) {
            androidPdfView.setPageOrientation(AndroidPdfView.Orientation.HORIZONTAL)
        } else {
            androidPdfView.setPageOrientation(AndroidPdfView.Orientation.VERTICAL)
        }
    }
}