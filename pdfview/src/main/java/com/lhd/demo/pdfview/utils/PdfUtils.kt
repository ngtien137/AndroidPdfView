package com.lhd.demo.pdfview.utils

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfUtils {

    fun loadPdfRendererFromPath(path: String): PdfRenderer {
        val file = File(path)
        val fileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return PdfRenderer(fileDescriptor)
    }

}