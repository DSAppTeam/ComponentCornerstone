package com.plugin.component.asm

import com.plugin.component.anno.AutoInjectComponent
import com.plugin.component.anno.AutoInjectImpl
import com.plugin.component.anno.MethodCost
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class ComponentScanClassVisitor extends ClassVisitor {

    private String className

    ComponentScanClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor)
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
                    ScanRuntime.addComponentInfo(scanComponentInfo)
                    scanComponentInfo = null
                }
                if (scanSdkInfo != null) {
                    ScanRuntime.addSdkInfo(scanSdkInfo)
                    scanSdkInfo = null
                }
                super.visitEnd()
            }
        }
    }


    @Override
    MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, methodDescriptor, signature, exceptions)
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, name, methodDescriptor) {
            @Override
            AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (Type.getDescriptor(MethodCost.class) == descriptor) {
                    ScanRuntime.addCostMethod(className, name, methodDescriptor)
                }
                return super.visitAnnotation(descriptor, visible)
            }
        }
        return mv
    }
}
