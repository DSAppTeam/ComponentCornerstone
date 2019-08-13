package com.plugin.component.asm;

import com.plugin.component.Logger;
import com.plugin.component.anno.MethodCost;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class MethodCostClassVisitor extends ClassVisitor {

    private static final String sCostCachePath = "com/plugin/component/CostCache";
    private String className;

    public MethodCostClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name.replaceAll("/", ".");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, name, descriptor) {

            private boolean cost = false;
            private String methodName = className + "#" + name;


            @Override
            protected void onMethodEnter() {
                if (cost) {
                    mv.visitLdcInsn(methodName);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "start",
                            "(Ljava/lang/String;J)V", false);
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                if (cost) {
                    mv.visitLdcInsn(methodName);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "end",
                            "(Ljava/lang/String;J)V", false);

                    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                    mv.visitLdcInsn(methodName);
                    mv.visitMethodInsn(INVOKESTATIC, sCostCachePath, "cost",
                            "(Ljava/lang/String;)Ljava/lang/String;", false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                            "(Ljava/lang/String;)V", false);
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                //判断是否使用某个注解
                if (Type.getDescriptor(MethodCost.class).equals(descriptor)) {
                    cost = true;
                    Logger.buildOutput("MethodCostClassVisitor(@MethodCost) ==> " + methodName);
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
        return mv;
    }
}
