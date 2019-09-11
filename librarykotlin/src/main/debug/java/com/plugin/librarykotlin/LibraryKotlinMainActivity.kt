package com.plugin.librarykotlin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.plugin.component.ComponentManager
import com.plugin.component.SdkManager
import com.plugin.library.ISdk

import com.plugin.librarykotlin.R

class LibraryKotlinMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
        ComponentManager.init(application)
        Toast.makeText(this, SdkManager.getSdk(ISdk::class.java)!!.sdkName, Toast.LENGTH_LONG).show()
    }
}
