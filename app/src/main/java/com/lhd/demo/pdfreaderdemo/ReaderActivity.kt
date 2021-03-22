package com.lhd.demo.pdfreaderdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lhd.demo.pdfview.AndroidPdfView


class ReaderActivity : AppCompatActivity(), AndroidPdfView.PageListener,
    AndroidPdfView.LoadingListener {

    companion object {
        const val EXTRA_PDF_PATH = "EXTRA_PDF_PATH"
    }

    private var toast: Toast? = null

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
        androidPdfView.pageListener = this
        androidPdfView.loadingListener = this
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

    override fun onPageChanged(pageIndex: Int) {
        toast?.cancel()
        toast = Toast.makeText(
            this,
            "Page: $pageIndex/${androidPdfView.getPageCount()}",
            Toast.LENGTH_SHORT
        )
        toast?.show()
    }

    override fun onLoadPdfStarted() {
        Log.e("Loading Listener:", " Started")
    }

    override fun onLoadPdfCompleted(success: Boolean) {
        Log.e("Loading Listener:", " Success - $success")
    }

}