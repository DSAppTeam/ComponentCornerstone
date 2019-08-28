package com.plugin.component.support.transform

import com.plugin.component.support.anno.MethodCost
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

class MethodCostAdapter extends ClassVisitor {

    private static final String sCostCachePath = "com/plugin/component/support/MethodCostHelper"
    private String className

    MethodCostAdapter(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }


    @Override
    MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, methodDescriptor, signature, exceptions)
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, name, methodDescriptor) {

            private boolean injectMethodCostCode = false
            private String methodName

            @Override
            AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (Type.getDescriptor(MethodCost.class) == descriptor) {
                    ScanRuntime.addCostMethod(className, name, methodDescriptor)
                }
                return super.visitAnnotation(descriptor, visible)
            }

            @Override
            protected void onMethodEnter() {
                injectMethodCostCode = ScanRuntime.isCostMethod(className, name, descriptor)
                methodName = className + "#" + name

                if (injectMethodCostCode) {
                    mv.visitLdcInsn(methodName)
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "start",
                            "(Ljava/lang/String;J)V", false)
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                if (injectMethodCostCode) {
                    mv.visitLdcInsn(methodName)
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "end",
                            "(Ljava/lang/String;J)V", false)

                    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                    mv.visitLdcInsn(methodName)
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "cost",
                            "(Ljava/lang/String;)Ljava/lang/String;", false)
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                            "(Ljava/lang/String;)V", false)
                }
            }
        }
        return mv
    }
}
