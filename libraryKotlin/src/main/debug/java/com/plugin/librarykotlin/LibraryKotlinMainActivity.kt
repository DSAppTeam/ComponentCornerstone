package com.plugin.librarykotlin

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.plugin.component.ComponentManager
import com.plugin.component.SdkManager

class LibraryKotlinMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
        ComponentManager.init(application)
        (findViewById<TextView>(R.id.text)).text = SdkManager.getSdk(IProvideFromKotlin::class.java)!!.provideString()
    }
}
