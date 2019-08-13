package com.plugin.component.asm;

import com.plugin.component.Logger;
import com.plugin.component.anno.AutoInjectComponent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ComponentClassVisitor extends ClassVisitor {

    private String className;

    public ComponentClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name.replaceAll("/", ".");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);

        if (Type.getDescriptor(AutoInjectComponent.class).equals(descriptor)) {

            Logger.buildOutput("ComponentClassVisitor(@AutoInjectComponent) ==> " + className);

            return new AnnotationVisitor(Opcodes.ASM7, annotationVisitor) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                    Logger.buildOutput(name + " = " + value);
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    super.visitEnum(name, descriptor, value);
                    Logger.buildOutput("name =" + name + ", desc=" + descriptor + " , value=" + value);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return super.visitAnnotation(name, descriptor);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    Logger.buildOutput("Array:" + name);
                    return new AnnotationVisitor(Opcodes.ASM7, annotationVisitor) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                            Logger.buildOutput(name + " = " + value);
                        }
                    };
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                }
            };
        }
        return annotationVisitor;
    }
}
