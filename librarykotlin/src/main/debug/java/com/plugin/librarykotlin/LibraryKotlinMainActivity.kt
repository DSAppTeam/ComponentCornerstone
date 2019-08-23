package com.plugin.librarykotlin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.plugin.librarykotlin.R

class LibraryKotlinMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
        Toast.makeText(this, KotlinSdkImpl().getKotlinSdkName(), Toast.LENGTH_LONG).show()
    }
}
