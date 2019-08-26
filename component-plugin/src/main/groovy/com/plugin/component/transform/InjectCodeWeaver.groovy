package com.plugin.component.transform

import com.quinn.hunter.transform.asm.BaseWeaver
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class InjectCodeWeaver extends BaseWeaver{

    @Override
    protected ClassVisitor wrapClassWriter(ClassWriter classWriter) {
        return new ScanCodeAdapter(classWriter)
    }
}
