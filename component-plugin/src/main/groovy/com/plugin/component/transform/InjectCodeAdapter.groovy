package com.plugin.component.transform


import com.plugin.component.transform.info.ComponentSdkInfo
import com.plugin.component.transform.info.ScanRuntime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

/**
 * component插件注入：
 * 收集 {@link MethodCost} 目标方法，添加统计代码
 * 收集 {@link com.plugin.component.anno.AutoInjectComponent} 信息
 * 收集 {@link com.plugin.component.anno.AutoInjectImpl} 信息
 * <p>
 * visit visitSource? visitOuterClass? ( visitAnnotation | visitAttribute )* ( visitInnerClass | visitField | visitMethod )* visitEnd
 */
class InjectCodeAdapter extends ClassVisitor {

    private static final String sCostCachePath = "com/plugin/component/CostCache"
    private static final String sComponentManagerPath = "com/plugin/component/ComponentManager"

    private String className

    InjectCodeAdapter(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }


    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, name, descriptor) {

            private boolean injectMethodCostCode = false
            private boolean injectComponentAutoInitCode = false
            private String methodName

            @Override
            protected void onMethodEnter() {
                injectMethodCostCode = ScanRuntime.isCostMethod(className, name, descriptor)
                injectComponentAutoInitCode =
                        className == sComponentManagerPath && name == "init" && descriptor == "(Landroid/app/Application;)V"
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

                if (injectComponentAutoInitCode) {
                    for (ComponentSdkInfo item : ScanRuntime.getComponentSdkInfoList()) {
                        if (item.isValid()) {
                            mv.visitLdcInsn(Type.getType(item.componentClassName))
                            mv.visitLdcInsn(Type.getType(item.sdk))
                            mv.visitLdcInsn(Type.getType(item.impl))
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/plugin/component/SdkManager", "register", "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Object;)V", false)
                        }
                    }
                }
            }
        }
        return mv
    }
}
