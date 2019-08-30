package com.plugin.component.transform

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.plugin.component.Logger
import com.plugin.component.transform.info.ScanRuntime
import com.quinn.hunter.transform.HunterTransform
import com.quinn.hunter.transform.RunVariant
import org.gradle.api.Project

class InjectCodeTransform extends HunterTransform {

    InjectCodeTransform(Project project) {
        super(project)
        this.bytecodeWeaver = new InjectCodeWeaver()
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        long startTime = System.currentTimeMillis()
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        ScanRuntime.logInjectInfo()
        ScanRuntime.clearScanInfo()
        Logger.buildOutput("InjectCodeTransform cost : " + (System.currentTimeMillis() - startTime) + "ms")
    }

    @Override
    protected RunVariant getRunVariant() {
        return super.getRunVariant()
    }

    @Override
    protected boolean inDuplcatedClassSafeMode() {
        return super.inDuplcatedClassSafeMode()
    }
}
