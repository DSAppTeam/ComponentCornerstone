package com.plugin.component;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.plugin.librarykotlin.JavaParcelable;
import com.plugin.librarykotlin.KotlinParcelable;
import com.plugin.module.R;

public class ParcelableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcelable);

        JavaParcelable javaParcelable = getIntent().getParcelableExtra("javaParcelable");
        KotlinParcelable kotlinParcelable = getIntent().getParcelableExtra("kotlinParcelable");
        TextView textView = this.findViewById(R.id.text);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("javaParcelable ->" + "\n");
        stringBuilder.append("int : " + javaParcelable.i + "\n");
        stringBuilder.append("string : " + javaParcelable.string + "\n");
        stringBuilder.append("kotlinParcelable ->" + "\n");
        stringBuilder.append("int : " + kotlinParcelable.getInt() + "\n");
        stringBuilder.append("string : " + kotlinParcelable.getString() + "\n");
        textView.setText(stringBuilder.toString());
    }
}
