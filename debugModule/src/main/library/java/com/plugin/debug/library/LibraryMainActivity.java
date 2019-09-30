package com.plugin.debug.library;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.plugin.debug.R;

public class LibraryMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);
        Toast.makeText(this,"LibraryMainActivity",Toast.LENGTH_SHORT).show();
//        setContentView(R.layout.main_activity_layout);
//        ComponentManager.init(getApplication());
//        ((TextView) findViewById(R.id.text)).setText(SdkManager.getSdk(IProvideFromLibrary.class).provideString());
    }
}
