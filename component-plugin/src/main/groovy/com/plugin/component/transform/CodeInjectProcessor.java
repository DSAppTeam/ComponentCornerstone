package com.plugin.component.transform;

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
 * time   : 2019/10/21
 * desc   :
 * version: 1.0
 */
public class CodeInjectProcessor {


    private static final FileTime ZERO = FileTime.fromMillis(0);
    private static final String FILE_SEP = File.separator;


    public void injectCode(String filePath) {
        try {
            if (filePath != null && filePath.endsWith(".jar")) {
                File file = new File(filePath);
                weaveJar(file);
            } else if (filePath != null && filePath.endsWith(".class")) {
                File file = new File(filePath);
                weaveSingleClassToFile(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public boolean isWeavableClass(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class") && !fullQualifiedClassName.contains("R$") && !fullQualifiedClassName.contains("R.class") && !fullQualifiedClassName.contains("BuildConfig.class");
    }


    public final void weaveSingleClassToFile(File inputFile) throws IOException {
        File outputFile = new File(inputFile.getParent(), inputFile.getName() + ".temp");
        FileUtils.touch(outputFile);
        InputStream inputStream = new FileInputStream(inputFile);
        byte[] bytes = doGenerateCode(inputStream);
        if (bytes != null) {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(bytes);
            fos.close();
            inputStream.close();
            if (inputFile.exists()) {
                inputFile.delete();
            }
            outputFile.renameTo(inputFile);
        }
    }


    public final void weaveJar(File inputJar) throws IOException {

        File outputJar = new File(inputJar.getParent(), inputJar.getName() + ".temp");
        if (outputJar.exists()) {
            outputJar.delete();
        }

        ZipFile inputZip = new ZipFile(inputJar);
        ZipOutputStream outputZip = new ZipOutputStream(new BufferedOutputStream(java.nio.file.Files.newOutputStream(outputJar.toPath())));
        Enumeration<? extends ZipEntry> inEntries = inputZip.entries();


        String outputPath = outputJar.getAbsolutePath();
        while (inEntries.hasMoreElements()) {
            ZipEntry entry = inEntries.nextElement();
            InputStream originalFile = new BufferedInputStream(inputZip.getInputStream(entry));
            ZipEntry outEntry = new ZipEntry(entry.getName());
            byte[] newEntryContent;
            // seperator of entry name is always '/', even in windows
            String className = outEntry.getName().replace("/", ".");


            if (!isWeavableClass(className)) {
                newEntryContent = org.apache.commons.io.IOUtils.toByteArray(originalFile);
            } else {
                newEntryContent = doGenerateCode(originalFile);
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

        inputZip.close();

        if (inputJar.exists()) {
            boolean result = inputJar.delete();
            System.out.println("delete jar result = " + result);
        }
        boolean rename = outputJar.renameTo(inputJar);
        System.out.println("rename jar result = " + rename);


    }


    private byte[] doGenerateCode(InputStream inputStream) {
        try {
            ClassReader classReader = new ClassReader(inputStream);
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            ClassVisitor classVisitor = new InjectCodeAdapter(classWriter);
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
            return classWriter.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
