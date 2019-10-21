package com.component.debug;

import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.component.debug.R;
import com.plugin.component.ComponentManager;
import com.plugin.component.anno.AutoInject;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_layout);
        Toast.makeText(this,"DebugActivity",Toast.LENGTH_SHORT).show();
        initComponent(getApplication());
    }

    @AutoInject
    public void initComponent(Application application){
        ComponentManager.init(application);
    }
}
