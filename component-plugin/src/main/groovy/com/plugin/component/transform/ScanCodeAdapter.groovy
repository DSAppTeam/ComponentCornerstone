package com.plugin.component.transform

import com.plugin.component.anno.AutoInjectComponent
import com.plugin.component.anno.AutoInjectImpl
import com.plugin.component.transform.info.ScanComponentInfo
import com.plugin.component.transform.info.ScanRuntime
import com.plugin.component.transform.info.ScanSdkInfo
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * 扫描注解并且收集
 */
class ScanCodeAdapter extends ClassVisitor {

    private String className
    private String filePath

    ScanCodeAdapter(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor)
    }

    void setFilePath(String filePath) {
        this.filePath = filePath
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }


    @Override
    AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible)
        ScanComponentInfo scanComponentInfo = null
        ScanSdkInfo scanSdkInfo = null
        return new AnnotationVisitor(Opcodes.ASM7, annotationVisitor) {

            @Override
            AnnotationVisitor visitArray(String arrayName) {

                return new AnnotationVisitor(Opcodes.ASM7, super.visitArray(arrayName)) {
                    @Override
                    void visit(String name, Object value) {
                        if (Type.getDescriptor(AutoInjectComponent.class) == descriptor) {
                            if (arrayName != null && arrayName == "impl") {
                                if (value != null) {
                                    if (scanComponentInfo == null) {
                                        scanComponentInfo = new ScanComponentInfo(className)
                                    }
                                    scanComponentInfo.impl.add((String) value)
                                }
                            }
                        }

                        if (Type.getDescriptor(AutoInjectImpl.class) == descriptor) {
                            if (arrayName != null && arrayName == "sdk") {
                                if (value != null) {
                                    if (scanSdkInfo == null) {
                                        scanSdkInfo = new ScanSdkInfo(className)
                                    }
                                    scanSdkInfo.sdk.add((String) value)
                                }
                            }
                        }
                        super.visit(name, value)
                    }

                }
            }

            @Override
            void visitEnd() {
                if (scanComponentInfo != null) {
//                    ScanRuntime.addComponentInfo(scanComponentInfo)
                    ScanRuntime.addComponentInfo(filePath, scanComponentInfo)
                    scanComponentInfo = null
                }
                if (scanSdkInfo != null) {
//                    ScanRuntime.addSdkInfo(scanSdkInfo)
                    ScanRuntime.addSdkInfo(filePath, scanSdkInfo)
                    scanSdkInfo = null
                }
                super.visitEnd()
            }
        }
    }
}
