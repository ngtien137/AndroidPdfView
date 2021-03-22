package com.lhd.demo.pdfreaderdemo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lhd.demo.pdfview.utils.FileChooser
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_PDF = 1000
        const val REQUEST_STORAGE = 1000
    }

    private val PATH_PDF by lazy {
        "${Environment.getExternalStorageDirectory()}/pdf/opengl.pdf"
    }

    private val listPermission by lazy {
        arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnChoosePdf)?.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                )
                    requestPermissions(
                        listPermission, REQUEST_STORAGE
                    )
                else {
                    choosePdf()
                }
            } else {
                choosePdf()
            }
        }
    }

    private fun choosePdf() {
        startChoosePdf()
    }

    private fun changeToScreenReaderWithCheck(path: String) {
        if (File(path).exists()) {
            changeToScreenReader(path)
        } else {
            Toast.makeText(this, "File not exist, please change path in code", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun changeToScreenReader(path: String) {
        val intent = Intent(this, ReaderActivity::class.java)
        intent.putExtra(ReaderActivity.EXTRA_PDF_PATH, path)
        startActivity(intent)
    }

    fun startChoosePdf() {
        val intentPDF = Intent(Intent.ACTION_GET_CONTENT)
        intentPDF.type = "application/pdf"
        intentPDF.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(
            Intent.createChooser(intentPDF, "Select PDF"),
            REQUEST_PDF
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PDF -> {
                data?.data?.also {
                    Log.e("FILE URI:", " ${it.path}")
                    try {
                        val path = FileChooser.getPath(this, it)
                        Log.e("FILE PATH:", " $path")
                        changeToScreenReaderWithCheck(path)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "This file can not be open, please choose other file",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

}