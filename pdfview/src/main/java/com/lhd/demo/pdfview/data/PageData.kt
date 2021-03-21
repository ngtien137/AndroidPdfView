package com.lhd.demo.pdfview.data

import com.lhd.demo.pdfview.AndroidPdfView

class PageData(
    var pageMargin: Int = 0,
    var pageBoundaries: Int = 0,
    var horizontalDivider: Boolean = false,
    var pageOrientation: AndroidPdfView.Orientation = AndroidPdfView.Orientation.HORIZONTAL
) {
}