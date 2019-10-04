package com.component.debug.librarykotlin;


import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.component.debug.R;
import com.plugin.component.ComponentManager;
import com.plugin.component.SdkManager;
import com.plugin.librarykotlin.IProvideFromKotlin;

public class LibraryKotlinMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_layout);
        ComponentManager.init(getApplication());
        ((TextView) findViewById(R.id.text)).setText(SdkManager.getSdk(IProvideFromKotlin.class).provideString());
    }
}
