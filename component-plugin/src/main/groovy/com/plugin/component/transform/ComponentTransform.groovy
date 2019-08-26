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
import com.android.utils.FileUtils
import com.plugin.component.Logger
import com.plugin.component.asm.ComponentInjectClassVisitor
import com.plugin.component.asm.ComponentScanClassVisitor
import com.plugin.component.utils.FileUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import com.plugin.component.asm.ScanRuntime

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
        return false
    }


    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        long startTime = System.currentTimeMillis();
        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                scanClass(directoryInput.file)
            }
            input.jarInputs.each { JarInput jarInput ->
                scanClass(jarInput.file)
            }
        }

        //log
        ScanRuntime.logScanInfo()
        ScanRuntime.buildComponentSdkInfo()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
                injectClass(dest)
            }
            input.jarInputs.each { JarInput jarInput ->
                def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
                injectClass(dest)
            }
        }

        ScanRuntime.loadInjectInfo()
        ScanRuntime.clearScanInfo()
        Logger.buildOutput("transform cost : " + System.currentTimeMillis() - startTime + "ms")
    }


    private boolean filterClass(String fileName) {
        return !FileUtil.isValidClassFile(fileName)
    }

    private boolean filterPackage(String fileName) {
        return false
    }

    private void scanClass(File source) {
        if (source.isDirectory()) {
            source.eachFileRecurse { File file ->
                String filename = file.getName()
                if (filterClass(filename)) return
                ClassReader cr = new ClassReader(file.readBytes())
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                ClassVisitor cv = new ComponentScanClassVisitor(cw)
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
            }
        } else {
            JarFile jarFile = new JarFile(source)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                String filename = entry.getName()
                if (filterPackage(filename)) break

                if (filterClass(filename)) continue

                InputStream stream = jarFile.getInputStream(entry)
                if (stream != null) {
                    ClassReader cr = new ClassReader(stream.bytes)
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ComponentScanClassVisitor(cw)
                    cr.accept(cv, ClassReader.EXPAND_FRAMES)
                    stream.close()
                }
            }
            jarFile.close()
        }
    }

    private void injectClass(File source) {

        if (source.isDirectory()) {
            source.eachFileRecurse { File file ->
                String filename = file.getName()
                if (filterClass(filename)) return
                ClassReader cr = new ClassReader(file.bytes)
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                ClassVisitor cv = new ComponentInjectClassVisitor(cw)
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
                FileOutputStream outputStream = new FileOutputStream(file)
                outputStream.write(cw.toByteArray())
                outputStream.close()
            }
        } else {
            Map<String, byte[]> tempModifiedClassByteMap = new HashMap()
            JarFile jarFile = new JarFile(source)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                String filename = entry.getName()
                if (filterPackage(filename)) break

                if (filterClass(filename)) continue

                InputStream stream = jarFile.getInputStream(entry)
                if (stream != null) {
                    ClassReader cr = new ClassReader(stream.bytes)
                    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new ComponentInjectClassVisitor(cw)
                    cr.accept(cv, ClassReader.EXPAND_FRAMES)
                    tempModifiedClassByteMap.put(filename, cw.toByteArray())
                    stream.close()
                }
            }
            if (tempModifiedClassByteMap.size() != 0) {
                File tempJar = new File(source.absolutePath.replace('.jar', 'temp.jar'))
                if (tempJar.exists()) {
                    tempJar.delete()
                }

                entries = jarFile.entries()
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempJar))
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement()
                    String filename = jarEntry.getName()
                    ZipEntry zipEntry = new ZipEntry(filename)
                    jarOutputStream.putNextEntry(zipEntry)
                    if (tempModifiedClassByteMap.containsKey(filename)) {
                        jarOutputStream.write(tempModifiedClassByteMap.get(filename))
                    } else {
                        InputStream inputStream = jarFile.getInputStream(jarEntry)
                        jarOutputStream.write(inputStream.bytes)
                        inputStream.close()
                    }
                    jarOutputStream.closeEntry()
                }
                jarOutputStream.close()
                FileOutputStream outputStream = new FileOutputStream(source)
                outputStream.write(tempJar.bytes)
                outputStream.close()
                tempJar.delete()
            }
            jarFile.close()
        }
    }
}
