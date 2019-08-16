package com.plugin.component.asm;

import com.plugin.component.anno.AutoInjectComponent;
import com.plugin.component.anno.AutoInjectImpl;
import com.plugin.component.anno.MethodCost;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;


/**
 * 扫描注解
 */
public class ComponentScanClassVisitor extends ClassVisitor {

    private String className;

    public ComponentScanClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(AutoInjectComponent.class).equals(descriptor)) {

            ScanComponentInfo scanComponentInfo = new ScanComponentInfo(className);
            /**
             *  ( visit | visitEnum | visitAnnotation | visitArray )* visitEnd
             */
            return new AnnotationVisitor(Opcodes.ASM7, annotationVisitor) {

                @Override
                public AnnotationVisitor visitArray(String arrayName) {
                    return new AnnotationVisitor(Opcodes.ASM7, super.visitArray(arrayName)) {
                        @Override
                        public void visit(String name, Object value) {
                            if (arrayName != null && arrayName.equals("impl")) {
                                if (value != null) {
                                    scanComponentInfo.impl.add(value);
                                }
                            }
                            super.visit(name, value);
                        }
                    };
                }

                @Override
                public void visitEnd() {
                    ScanRuntime.addComponentInfo(scanComponentInfo);
                    super.visitEnd();
                }
            };
        } else if (Type.getDescriptor(AutoInjectImpl.class).equals(descriptor)) {

            ScanSdkInfo scanSdkInfo = new ScanSdkInfo(className);

            return new AnnotationVisitor(Opcodes.ASM7, annotationVisitor) {
                @Override
                public AnnotationVisitor visitArray(String arrayName) {
                    return new AnnotationVisitor(Opcodes.ASM7, super.visitArray(arrayName)) {
                        @Override
                        public void visit(String name, Object value) {
                            if (arrayName != null && arrayName.equals("sdk")) {
                                if (value != null) {
                                    scanSdkInfo.sdk.add(value);
                                }
                            }
                            super.visit(name, value);
                        }
                    };
                }
                @Override
                public void visitEnd() {
                    ScanRuntime.addSdkInfo(scanSdkInfo);
                    super.visitEnd();
                }
            };
        }
        return annotationVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        String methodDescriptor = descriptor;
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, name, descriptor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (Type.getDescriptor(MethodCost.class).equals(descriptor)) {
                    ScanRuntime.addCostMethod(className, name, methodDescriptor);
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
        return mv;
    }

}
