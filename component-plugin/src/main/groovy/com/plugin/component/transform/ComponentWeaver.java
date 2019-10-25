package com.plugin.component.transform;

import com.plugin.component.transform.info.ScanRuntime;
import com.quinn.hunter.transform.asm.ExtendClassWriter;
import com.quinn.hunter.transform.asm.IWeaver;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * author : linzheng
 * e-mail : linzheng@corp.netease.com
 * time   : 2019/10/18
 * desc   :
 * version: 1.0
 */
public class ComponentWeaver implements IWeaver {

    private static final FileTime ZERO = FileTime.fromMillis(0);
    private static final String FILE_SEP = File.separator;

    private String injectClassFile;

    protected ClassLoader classLoader;

    public ComponentWeaver() {
    }


    public String getInjectClassFile() {
        return injectClassFile;
    }

    public final void weaveJar(File inputJar, File outputJar) throws IOException {
        String inputPath = inputJar.getAbsolutePath();
        String outputPath = outputJar.getAbsolutePath();
        ZipFile inputZip = new ZipFile(inputJar);
        ZipOutputStream outputZip = new ZipOutputStream(new BufferedOutputStream(java.nio.file.Files.newOutputStream(outputJar.toPath())));
        Enumeration<? extends ZipEntry> inEntries = inputZip.entries();
        while (inEntries.hasMoreElements()) {
            ZipEntry entry = inEntries.nextElement();
            InputStream originalFile = new BufferedInputStream(inputZip.getInputStream(entry));
            ZipEntry outEntry = new ZipEntry(entry.getName());
            byte[] newEntryContent;
            // seperator of entry name is always '/', even in windows
            String className = outEntry.getName().replace("/", ".");

            beforeWeaveClass(inputPath, outputPath, className);

            if (!isWeavableClass(className)) {
                newEntryContent = org.apache.commons.io.IOUtils.toByteArray(originalFile);
            } else {
                newEntryContent = weaveSingleClassToByteArray(inputPath, originalFile);
            }
            CRC32 crc32 = new CRC32();
            crc32.update(newEntryContent);
            outEntry.setCrc(crc32.getValue());
            outEntry.setMethod(ZipEntry.STORED);
            outEntry.setSize(newEntryContent.length);
            outEntry.setCompressedSize(newEntryContent.length);
            outEntry.setLastAccessTime(ZERO);
            outEntry.setLastModifiedTime(ZERO);
            outEntry.setCreationTime(ZERO);
            outputZip.putNextEntry(outEntry);
            outputZip.write(newEntryContent);
            outputZip.closeEntry();
        }
        outputZip.flush();
        outputZip.close();
    }

    public final void weaveSingleClassToFile(File inputFile, File outputFile, String inputBaseDir) throws IOException {
        if (!inputBaseDir.endsWith(FILE_SEP)) inputBaseDir = inputBaseDir + FILE_SEP;
        String className = inputFile.getAbsolutePath().replace(inputBaseDir, "").replace(FILE_SEP, ".");

        String inputPath = inputFile.getAbsolutePath();
        String outputPath = outputFile.getAbsolutePath();

        beforeWeaveClass(inputPath, outputPath, className);

        if (isWeavableClass(className)) {
            FileUtils.touch(outputFile);
            InputStream inputStream = new FileInputStream(inputFile);
            byte[] bytes = weaveSingleClassToByteArray(inputPath, inputStream);
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(bytes);
            fos.close();
            inputStream.close();
        } else {
            if (inputFile.isFile()) {
                FileUtils.touch(outputFile);
                FileUtils.copyFile(inputFile, outputFile);
            }
        }
    }

    public final void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public byte[] weaveSingleClassToByteArray(InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ExtendClassWriter(classLoader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriterWrapper = wrapClassWriter(classWriter);
        classReader.accept(classWriterWrapper, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    public byte[] weaveSingleClassToByteArray(String filePath, InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ExtendClassWriter(classLoader, ClassWriter.COMPUTE_MAXS);
        ScanCodeAdapter classWriterWrapper = new ScanCodeAdapter(classWriter);
        classWriterWrapper.setFilePath(filePath);
        classReader.accept(classWriterWrapper, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }


    private static final String sComponentManagerPath = "com.plugin.component.ComponentManager";


    public void beforeWeaveClass(String inputPath, String outputFile, String className) {
        if (injectClassFile == null && className != null && className.contains(sComponentManagerPath)) {
            System.out.println("find class ComponentManager : file is : " + outputFile);
            injectClassFile = outputFile;
            ScanRuntime.getsSummaryInfo().inputFilePath = inputPath;
            ScanRuntime.getsSummaryInfo().outputFilePath = outputFile;

        }
    }

    public void setExtension(Object extension) {

    }

    protected ClassVisitor wrapClassWriter(ClassWriter classWriter) {
        return new ScanCodeAdapter(classWriter);
    }

    @Override
    public boolean isWeavableClass(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class") && !fullQualifiedClassName.contains("R$") && !fullQualifiedClassName.contains("R.class") && !fullQualifiedClassName.contains("BuildConfig.class");
    }


}
