package com.gunterhellrigl.opencvtinysample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkOpenCV()
    }

    private fun checkOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Log.i("OpenCV Tiny", "OpenCV library loaded successfully :)")
        } else {
            Log.e("OpenCV Tiny", "OpenCV library couldn't loaded successfully :(")
        }
    }
}