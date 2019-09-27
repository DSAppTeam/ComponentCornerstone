package com.plugin.component;
import android.app.Application;

import androidx.annotation.NonNull;

/**
 * 基础组件实现接口
 * Email yummyl.lau@gmail.com
 * Created by yummylau on 2018/01/25.
 */
public interface IComponent {

    void attachComponent(@NonNull Application application);

    void detachComponent();
}
