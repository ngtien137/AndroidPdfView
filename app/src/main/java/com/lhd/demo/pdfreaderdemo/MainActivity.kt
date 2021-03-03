package com.lhd.demo.pdfreaderdemo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        findViewById<View>(R.id.btnChoosePdf).setOnClickListener {
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
        if (File(PATH_PDF).exists()) {
            changeToScreenReader(PATH_PDF)
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

}