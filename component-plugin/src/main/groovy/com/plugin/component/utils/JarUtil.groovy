package com.plugin.component.utils

import com.plugin.component.Constants
import com.plugin.component.PluginRuntime
import com.plugin.component.extension.option.CompileOption
import com.plugin.component.extension.option.PublicationOption
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipFile

class JarUtil {


    /**
     * 打包Java源码为jar包依赖
     * @param project
     * @param publication
     * @param androidJarPath
     * @param compileOptions
     * @param vars
     * @return
     */
    static File packJavaSourceJar(Project project, PublicationOption publication, String androidJarPath,
                                  CompileOption compileOptions, boolean vars) {

        publication.buildDir.deleteDir()
        publication.buildDir.mkdirs()

        def sourceDir = new File(publication.buildDir, Constants.BUILD_SOURCE_DIR)
        def classesDir = new File(publication.buildDir, Constants.BUILD_CLASSES_DIR)
        def outputDir = new File(publication.buildDir, Constants.BUILD_OUTPUT_DIR)
        sourceDir.mkdirs()
        classesDir.mkdirs()
        outputDir.mkdirs()

        def argFiles = []
        File file = new File(publication.misSourceSet.path)
        String prefix = publication.misSourceSet.path
        filterJavaSource(file, prefix, sourceDir, argFiles, publication.sourceFilter)
        if (argFiles.size() == 0) {
            return null
        }

        //迁移发布依赖到project依赖，从{name}路径重点读取该依赖
        def name = "component[${publication.groupId}-${publication.artifactId}]Classpath"
        Configuration configuration = project.configurations.create(name)
        if (publication.dependencies != null) {
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each {
                    project.dependencies.add(name, it)
                }
            }
            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each {
                    project.dependencies.add(name, it)
                }
            }
        }

        def classPath = [androidJarPath]
        configuration.copy().files.each {
            if (it.name.endsWith('.aar')) {
                classPath << getAARClassesJar(it)
            } else {
                classPath << it.absolutePath
            }
        }
        project.configurations.remove(configuration)

        return generateJavaSourceJar(classesDir, argFiles, classPath, compileOptions, vars)
    }


    /**
     * 生成java jar包
     * @param classesDir
     * @param argFiles
     * @param classPath
     * @param compileOptions
     * @param vars
     * @return
     */
    private static File generateJavaSourceJar(File classesDir,
                                              List<String> argFiles,
                                              List<String> classPath,
                                              CompileOption compileOptions,
                                              boolean vars) {

        //window classpath 路径分割符号与 mac/linux需要做区分
        def classpathSeparator = ";"
        if (!System.properties['os.name'].toLowerCase().contains('windows')) {
            classpathSeparator = ":"
        }

        boolean keepParameters = vars && Jvm.current().javaVersion >= JavaVersion.VERSION_1_8

        //读取java和kotlin文件
        List<String> javaFiles = new ArrayList<>()
        List<String> kotlinFiles = new ArrayList<>()
        argFiles.each {
            if (it.endsWith('.java')) {
                javaFiles.add(it)
            } else if (it.endsWith('.kt')) {
                kotlinFiles.add(it)
            }
        }


        if (!kotlinFiles.isEmpty()) {
            K2JVMCompiler compiler = new K2JVMCompiler()
            def args = new ArrayList<String>()
            args.addAll(argFiles)
            args.add('-d')
            args.add(classesDir.absolutePath)
            args.add('-no-stdlib')
            if (keepParameters) {
                args.add('-java-parameters')
            }

            JavaVersion targetCompatibility = compileOptions.targetCompatibility
            def target = targetCompatibility.toString()
            if (!targetCompatibility.isJava8() && !targetCompatibility.isJava6()) {
                throw new GradleException("Failure to compile component kotlin source to bytecode: unknown JVM target version: $target, supported versions: 1.6, 1.8\nTry:\n " +
                        "   module {\n" +
                        "       ...\n" +
                        "       compileOptions {\n" +
                        "           sourceCompatibility JavaVersion.VERSION_1_8\n" +
                        "           targetCompatibility JavaVersion.VERSION_1_8\n" +
                        "       }\n" +
                        "   }")
            }


            args.add('-jvm-target')
            args.add(target)

            if (classPath.size() > 0) {
                args.add('-classpath')
                args.add(classPath.join(classpathSeparator))
            }

            ExitCode exitCode = compiler.exec(System.out, (String[]) args.toArray())
            if (exitCode != ExitCode.OK) {
                throw new GradleException("Failure to compile component kotlin source to bytecode.")
            }

            new File(classesDir, '/META-INF').deleteDir()

            classPath.add(classesDir.absolutePath)
        }

        if (!javaFiles.isEmpty()) {
            def command = "javac " + (keepParameters ? "-parameters" : "") + " -d . -encoding UTF-8 -target " + compileOptions.targetCompatibility.toString() + " -source " + compileOptions.sourceCompatibility.toString() + (classPath.size() > 0 ? (" -classpath " + classPath.join(classpathSeparator) + " ") : "") + javaFiles.join(' ')
            def p = (command).execute(null, classesDir)

            def result = p.waitFor(30, TimeUnit.SECONDS)
            if (!result) {
                throw new GradleException("Timed out when compile component java source to bytecode with command.\nExecute command:\n" + command)
            }

            if (p.exitValue() != 0) {
                throw new GradleException("Failure to compile component java source to bytecode: \n" + p.err.text + "\nExecute command:\n" + command)
            }
        }

        def p = "jar cvf outputs/classes.jar -C classes . ".execute(null, classesDir.parentFile)
        def result = p.waitFor()
        p.waitFor()
        if (result != 0) {
            throw new RuntimeException("failure to package classes.jar: \n" + p.err.text)
        }
        return new File(classesDir.parentFile, 'outputs/classes.jar')
    }

    /**
     * 打包java文档
     * @param publication
     * @return
     */
    static File packJavaDocSourceJar(PublicationOption publication) {
        def javaSource = new File(publication.buildDir, "javaSource")
        javaSource.deleteDir()
        javaSource.mkdirs()
        filterJavaDocSource(new File(publication.misSourceSet.path), publication.misSourceSet.path, javaSource)
        return generateJavaDocSourceJar(javaSource)
    }

    private static File generateJavaDocSourceJar(File sourceDir) {
        def p = "jar cvf ../outputs/classes-source.jar .".execute(null, sourceDir)
        def result = p.waitFor()
        if (result != 0) {
            throw new RuntimeException("failure to make component-sdk java source directory: \n" + p.err.text)
        }
        def sourceJar = new File(sourceDir.parentFile, 'outputs/classes-source.jar')
        return sourceJar
    }


    /**
     *
     * @param file
     * @param prefix
     * @param sourceDir
     * @param argFiles
     * @param sourceFilter
     */
    private static void filterJavaSource(File file, String prefix, File sourceDir,
                                         def argFiles, Closure sourceFilter) {
        //如果是目录，则递归过滤
        if (file.isDirectory()) {
            file.listFiles().each { File childFile ->
                filterJavaSource(childFile, prefix, sourceDir, argFiles, sourceFilter)
            }
        } else {
            //如果是java或者kt文件
            if (file.name.endsWith(".java") || file.name.endsWith(".kt")) {
                def packageName = file.parent.replace(prefix, "")
                def targetParent = new File(sourceDir, packageName)
                if (!targetParent.exists()) targetParent.mkdirs()
                def target = new File(targetParent, file.name)
                FileUtil.copyFile(file, target)
                argFiles << target.absolutePath

                if (sourceFilter != null) {
                    sourceFilter.call(target)
                }
            }
        }
    }

    private static void filterJavaDocSource(File file, String prefix, File javaDocDir) {
        if (file.isDirectory()) {
            file.listFiles().each { File childFile ->
                filterJavaDocSource(childFile, prefix, javaDocDir)
            }
        } else {
            if (file.name.endsWith(".java") || file.name.endsWith('.kt')) {
                def packageName = file.parent.replace(prefix, "")
                def targetParent = new File(javaDocDir, packageName)
                if (!targetParent.exists()) targetParent.mkdirs()
                def target = new File(targetParent, file.name)
                FileUtil.copyFile(file, target)
            }
        }
    }

    static boolean compareMavenJar(Project project, PublicationOption publication, String localPath) {
        String filePath = null
        String fileName = publication.artifactId + "-" + publication.version + ".jar"
        def name = "component[${publication.groupId}-${publication.artifactId}]Classpath"
        Configuration configuration = project.configurations.create(name)
        project.dependencies.add(name, publication.groupId + ":" + publication.artifactId + ":" + publication.version)
        configuration.copy().files.each {
            if (it.name.endsWith(fileName)) {
                filePath = it.absolutePath
            }
        }
        project.configurations.remove(configuration)
        if (filePath == null) return false
        return compareJar(localPath, filePath)
    }

    private static boolean compareJar(String jar1, String jar2) {
        try {
            JarFile jarFile1 = new JarFile(jar1)
            JarFile jarFile2 = new JarFile(jar2)
            if (jarFile1.size() != jarFile2.size())
                return false

            Enumeration entries = jarFile1.entries()
            while (entries.hasMoreElements()) {
                JarEntry jarEntry1 = (JarEntry) entries.nextElement()
                if (!jarEntry1.name.endsWith(".class"))
                    continue

                JarEntry jarEntry2 = jarFile2.getJarEntry(jarEntry1.getName())
                if (jarEntry2 == null) {
                    return false
                }
                InputStream stream1 = jarFile1.getInputStream(jarEntry1)
                byte[] bytes1 = stream1.bytes
                bytes1 = Arrays.copyOfRange(bytes1, 8, bytes1.length)
                stream1.close()

                InputStream stream2 = jarFile2.getInputStream(jarEntry2)
                byte[] bytes2 = stream2.bytes
                bytes2 = Arrays.copyOfRange(bytes2, 8, bytes2.length)
                stream2.close()

                if (!Arrays.equals(bytes1, bytes2)) {
                    return false
                }
            }
            jarFile1.close()
            jarFile2.close()
        } catch (IOException e) {
            return false
        }
        return true
    }


    /**
     * 获取目标文件中包含 classes.jar 的文件并写入jarFile
     * @param input
     * @return
     */
    private static String getAARClassesJar(File input) {
        def jarFile = new File(input.getParent(), 'classes.jar')
        if (jarFile.exists()) return jarFile
        def zip = new ZipFile(input)
        zip.entries().each {
            if (it.isDirectory()) return
            if (it.name == 'classes.jar') {
                def fos = new FileOutputStream(jarFile)
                fos.write(zip.getInputStream(it).bytes)
                fos.close()
            }
        }
        zip.close()
        return jarFile.absolutePath
    }

    static void handleMavenJar(Project project, PublicationOption publication) {
        File target = new File(PluginRuntime.sSdkDir, PublicationUtil.getJarName(publication))
        if (publication.invalid) {
            PluginRuntime.sPublicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        boolean hasModifiedSource = PluginRuntime.sPublicationManager.hasModified(publication)

        if (target.exists()) {
            if (hasModifiedSource) {
                def releaseJar = JarUtil.packJavaSourceJar(project, publication, PluginRuntime.sAndroidJarPath, PluginRuntime.sModuleExtension.compileOptions, true)
                if (releaseJar == null) {
                    publication.invalid = true
                    PluginRuntime.sPublicationManager.addPublication(publication)
                    if (target.exists()) {
                        target.delete()
                    }
                    return
                }
                FileUtil.copyFile(releaseJar, target)
            }
            publication.invalid = false
            publication.useLocal = true
            PluginRuntime.sPublicationManager.addPublication(publication)
        } else if (!hasModifiedSource) {
            PublicationOption lastPublication = PluginRuntime.sPublicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication.version != publication.version) {
                publication.versionNew = publication.version
                publication.version = lastPublication.version
            }
            publication.invalid = false
            publication.useLocal = false
            PluginRuntime.sPublicationManager.addPublication(publication)
            return
        } else {
            def releaseJar = JarUtil.packJavaSourceJar(project, publication, PluginRuntime.sAndroidJarPath, PluginRuntime.sModuleExtension.compileOptions, false)
            if (releaseJar == null) {
                publication.invalid = true
                PluginRuntime.sPublicationManager.addPublication(publication)
                if (target.exists()) {
                    target.delete()
                }
                return
            }

            PublicationOption lastPublication = PluginRuntime.sPublicationManager.getPublication(publication.groupId, publication.artifactId)
            if (lastPublication == null) {
                lastPublication = publication
            }
            boolean equals = JarUtil.compareMavenJar(project, lastPublication, releaseJar.absolutePath)
            if (equals) {
                if (target.exists()) {
                    target.delete()
                }
                publication.useLocal = false
            } else {
                releaseJar = JarUtil.packJavaSourceJar(project, publication, PluginRuntime.sAndroidJarPath, PluginRuntime.sModuleExtension.compileOptions, true)
                FileUtil.copyFile(releaseJar, target)
                publication.useLocal = true
            }
            publication.invalid = false
            PluginRuntime.sPublicationManager.addPublication(publication)
        }
    }

    static void handleLocalJar(Project project, PublicationOption publication) {
        File target = new File(PluginRuntime.sSdkDir, PublicationUtil.getJarName(publication))

        if (publication.invalid) {
            PluginRuntime.sPublicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        if (target.exists()) {
            boolean hasModifiedSource = PluginRuntime.sPublicationManager.hasModified(publication)
            if (!hasModifiedSource) {
                publication.invalid = false
                publication.useLocal = true
                PluginRuntime.sPublicationManager.addPublication(publication)
                return
            }
        }

        File releaseJar = JarUtil.packJavaSourceJar(project, publication, PluginRuntime.sAndroidJarPath, PluginRuntime.sModuleExtension.compileOptions, true)
        if (releaseJar == null) {
            publication.invalid = true
            PluginRuntime.sPublicationManager.addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return
        }

        FileUtil.copyFile(releaseJar, target)
        publication.invalid = false
        publication.useLocal = true
        PluginRuntime.sPublicationManager.addPublication(publication)
    }
}
