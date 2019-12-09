package com.plugin.component;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.library.LibraryWithoutPlugin;
import com.plugin.library.IProvideFromLibrary;
import com.plugin.library.ISdk;
import com.plugin.library.ISdk2;
import com.plugin.librarykotlin.IGetFromLibrary;
import com.plugin.librarykotlin.IProvideFromKotlin;
import com.plugin.librarykotlin.JavaParcelable;
import com.plugin.librarykotlin.KotlinParcelable;
import com.plugin.module.R;
import com.plugin.pin.MainBase;
import com.plugin.pin.base.PBase;
import com.plugin.pin.common.PCommon;
import com.plugin.pin.home.PHome;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        initComponent();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("library 提供 sdk：" + "\n");
        stringBuilder.append("IProvideFromLibrary ->" + SdkManager.getSdk(IProvideFromLibrary.class).provideString() + "\n");
        stringBuilder.append("ISdk ->" + SdkManager.getSdk(ISdk.class).getSdkName() + "\n");
        stringBuilder.append("ISdk2 ->" + SdkManager.getSdk(ISdk2.class).getSdk2Name() + "\n");
        stringBuilder.append("libraryKotlin 提供 sdk:" + "\n");
        stringBuilder.append("IProvideFromKotlin ->" + SdkManager.getSdk(IProvideFromKotlin.class).provideString() + "\n");
        stringBuilder.append("IGetFromLibrary ->" + SdkManager.getSdk(IGetFromLibrary.class).provideString() + "\n");
        stringBuilder.append("pin子工程测试 \n");
        stringBuilder.append("main -> " + new MainBase().getString() + "\n");
        stringBuilder.append("base -> " + new PBase().getString() + "\n");
        stringBuilder.append("home -> " + new PHome().getString() + "\n");
        stringBuilder.append("common -> " + new PCommon().getString() + "\n");
        stringBuilder.append("普通模块测试 -> " + new LibraryWithoutPlugin().getString() + "\n");
        ((TextView) findViewById(R.id.text)).setText(stringBuilder);
        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JavaParcelable javaParcelable = new JavaParcelable();
                KotlinParcelable kotlinParcelable = new KotlinParcelable();
                Intent intent = new Intent(MainActivity.this, ParcelableActivity.class);
                intent.putExtra("javaParcelable",javaParcelable);
                intent.putExtra("kotlinParcelable",kotlinParcelable);
                startActivity(intent);
            }
        });

    }

    public void initComponent() {
        ComponentManager.init(getApplication());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
