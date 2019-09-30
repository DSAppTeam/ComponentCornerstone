package com.component.debug.librarykotlin2

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.plugin.component.ComponentManager
import com.plugin.component.SdkManager
import com.plugin.librarykotlin2.IProvideFromLibraryKotlin2
import com.component.debug.R


class LibraryKotlin2MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_layout)
        ComponentManager.init(application)
        (findViewById<TextView>(R.id.text)).text = SdkManager.getSdk(IProvideFromLibraryKotlin2::class.java)!!.provideString()
    }
}
