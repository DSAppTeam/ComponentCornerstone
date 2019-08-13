package com.plugin.component.transform


import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.plugin.component.asm.ComponentClassVisitor
import com.plugin.component.asm.MethodCostClassVisitor
import com.plugin.component.utils.FileUtil
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 * 插入方法统计transform
 * created by yummylau 2019/08/12
 */
class ComponentTransform extends Transform {


    @Override
    String getName() {
        return "ComponentTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }


    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        transformInvocation.inputs.each { TransformInput input ->

            input.jarInputs.each { JarInput jarInput ->

                if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                    // ...对jar进行插入字节码
                }

                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }

            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->

                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        injectComponentCode(file)
                        injectMethodCostCode(file)
                    }
                }
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }

    /**
     * 自动插件IComponent组件
     * @param file
     */
    private void injectComponentCode(File file) {
        if(FileUtil.isValidClassFile(file)){
            def name = file.name
            if (name.endsWith(".class") && !name.startsWith("R\$") &&
                    !("R.class" == name) && !("BuildConfig.class" == name)) {
                ClassReader cr = new ClassReader(file.bytes)
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                ClassVisitor cv = new ComponentClassVisitor(cw)
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
                byte[] code = cw.toByteArray()
                FileOutputStream fos = new FileOutputStream(
                        file.parentFile.absolutePath + File.separator + name)
                fos.write(code)
                fos.close()
            }
        }
    }


    /**
     * 插入方法耗时统计
     * @param file
     */
    private void injectMethodCostCode(File file) {
        if(FileUtil.isValidClassFile(file)){
            def name = file.name
            if (name.endsWith(".class") && !name.startsWith("R\$") &&
                    !("R.class" == name) && !("BuildConfig.class" == name)) {
                ClassReader cr = new ClassReader(file.bytes)
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                ClassVisitor cv = new MethodCostClassVisitor(cw)
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
                byte[] code = cw.toByteArray()
                FileOutputStream fos = new FileOutputStream(
                        file.parentFile.absolutePath + File.separator + name)
                fos.write(code)
                fos.close()
            }
        }
    }
}
