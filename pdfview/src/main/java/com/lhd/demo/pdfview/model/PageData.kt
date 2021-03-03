package com.lhd.demo.pdfview.model

import com.lhd.demo.pdfview.AndroidPdfView

class PageData(
    var pageMargin: Int = 0,
    var pageBoundaries: Int = 0,
    var pageOrientation: AndroidPdfView.Orientation = AndroidPdfView.Orientation.HORIZONTAL
) {
}