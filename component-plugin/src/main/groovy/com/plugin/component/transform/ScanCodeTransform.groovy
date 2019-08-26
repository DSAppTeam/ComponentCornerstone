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

class ScanCodeTransform extends HunterTransform {

    ScanCodeTransform(Project project) {
        super(project)
        this.bytecodeWeaver = new ScanCodeWeaver()
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        long startTime = System.currentTimeMillis()
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        ScanRuntime.logScanInfo()
        ScanRuntime.buildComponentSdkInfo()
        Logger.buildOutput("scan code cost : " + (System.currentTimeMillis() - startTime) + "ms")
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
