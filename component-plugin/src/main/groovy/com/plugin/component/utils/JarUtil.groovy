package com.plugin.component.utils

import com.plugin.component.Constants
import com.plugin.component.extension.option.sdk.PublicationDependencyModuleOption
import com.plugin.component.log.Logger
import com.plugin.component.Runtimes
import com.plugin.component.extension.PublicationManager
import com.plugin.component.extension.option.sdk.CompileOptions
import com.plugin.component.extension.option.sdk.PublicationOption
import org.apache.http.util.TextUtils
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

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
                                  CompileOptions compileOptions, boolean vars) {

        publication.buildDir.deleteDir()
        publication.buildDir.mkdirs()

        def sourceDir = new File(publication.buildDir, Constants.BUILD_SOURCE_DIR)
        def classesDir = new File(publication.buildDir, Constants.BUILD_CLASSES_DIR)
        def outputDir = new File(publication.buildDir, Constants.BUILD_OUTPUT_DIR)
        sourceDir.mkdirs()
        classesDir.mkdirs()
        outputDir.mkdirs()

        def argFiles = []
        File file = new File(publication.sdkSourceSet.path)
        String prefix = publication.sdkSourceSet.path
        filterJavaSource(file, prefix, sourceDir, argFiles, publication.sourceFilter)
        if (argFiles.size() == 0) {
            return null
        }

        //迁移发布依赖到project依赖，从{name}路径重点读取该依赖
        def name = "component[${publication.groupId}-${publication.artifactId}]Classpath"
        Configuration configuration = project.configurations.create(name)
        if (publication.dependencies != null) {
            if (publication.dependencies.implementation != null) {
                publication.dependencies.implementation.each { dependency ->
//                    Logger.buildOutput("packJar: publication:${PublicationUtil.getPublicationId(publication)} move dependencies:${it}")
                    if (dependency instanceof PublicationDependencyModuleOption) {
                        project.dependencies.add(name, dependency.path)
                    } else {
                        project.dependencies.add(name, dependency)
                    }
                }
            }
            if (publication.dependencies.compileOnly != null) {
                publication.dependencies.compileOnly.each { dependency ->
                    if (dependency instanceof PublicationDependencyModuleOption) {
                        project.dependencies.add(name, dependency.path)
                    } else {
                        project.dependencies.add(name, dependency)
                    }
                }
            }
        }

        def classPath = [androidJarPath]
        configuration.copy().files.each {
//            Logger.buildOutput("packJar: copyFile name:${it.name}")
            //添加所有打包的classpatch
            if (it.name.endsWith('.aar')) {
                classPath << getAARClassesJar(it)
            } else {
                classPath << it.absolutePath
            }
        }
        project.configurations.remove(configuration)
        Logger.buildOutput("packing ${project.name} sdk jar")
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
                                              CompileOptions compileOptions,
                                              boolean vars) {

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
                throw new GradleException("Failure to compile component kotlin source to bytecode: unknown JVM target sdkVersion: $target, supported versions: 1.6, 1.8\nTry:\n " +
                        "   module {\n" +
                        "       ...\n" +
                        "       compileOption {\n" +
                        "           sourceCompatibility JavaVersion.VERSION_1_8\n" +
                        "           targetCompatibility JavaVersion.VERSION_1_8\n" +
                        "       }\n" +
                        "   }")
            }


            args.add('-jvm-target')
            args.add(target)

            if (classPath.size() > 0) {
                args.add('-classpath')
                args.add(classPath.join(File.pathSeparator))
            }

            ExitCode exitCode = compiler.exec(System.out, (String[]) args.toArray())
            Logger.buildOutput("编译参数： " + (String[]) args.toArray())
            Logger.buildOutput("kotlin 编译结果: +${exitCode}")
            if (exitCode != ExitCode.OK) {
                throw new GradleException("Failure to compile component kotlin source to bytecode.")
            }

            new File(classesDir, '/META-INF').deleteDir()

            classPath.add(classesDir.absolutePath)
        }

        if (!javaFiles.isEmpty()) {
            LinkedList<String> paras = new LinkedList();
            paras.add('javac')
            paras.add('-parameters')
            paras.add('-d')
            paras.add(classesDir.getAbsolutePath())
            paras.add('-encoding')
            paras.add('UTF-8')
            paras.add('-target')
            paras.add(compileOptions.targetCompatibility.toString())
            paras.add('-source')
            paras.add(compileOptions.sourceCompatibility.toString())

            paras.add('-classpath')
            paras.add(classPath.join(File.pathSeparator))
            paras.addAll(javaFiles);

            String[] javacParameters = (String[]) paras.toArray(new String[paras.size()])

            Runtime runtime = Runtime.getRuntime()
            def p = runtime.exec(javacParameters, null, classesDir);
            Logger.buildOutput("javac execute command:${javacParameters}")
            def shellErrorResultReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            def shellInfoResultReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            //需要输出缓冲区数据 不然会阻塞在waitFor
            String infoLine;
            while ((infoLine = shellInfoResultReader.readLine()) != null) {
                Logger.buildOutput("脚本文件执行信息:{}", infoLine);
            }
            String errorLine;
            while ((errorLine = shellErrorResultReader.readLine()) != null) {
                Logger.buildOutput("脚本文件执行信息:{}", errorLine);
            }
            def result = p.waitFor()
            Logger.buildOutput("javac execute result:${result}")
            if (result != 0) {
                throw new GradleException("Failure to compile component java source to bytecode: \n" + p.err.text + "\nExecute command:\n" + javacParameters)
            }
            p.destroy()
        }
        Logger.buildOutput("jar execute command: jar cf outputs/classes.jar -C classes .  \nclasses path ${classesDir.parentFile.absolutePath}")
//        def p = "jar cvf outputs/classes.jar -C classes . ".execute(null, classesDir.parentFile)
        Runtime runtime = Runtime.getRuntime()
        def p = runtime.exec("jar cf outputs/classes.jar -C classes . ", null, classesDir.parentFile);
        def shellErrorResultReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        def shellInfoResultReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        //需要输出缓冲区数据 不然会阻塞在waitFor
        String infoLine;
        while ((infoLine = shellInfoResultReader.readLine()) != null) {
            Logger.buildOutput("脚本文件执行信息:{}", infoLine);
        }
        String errorLine;
        while ((errorLine = shellErrorResultReader.readLine()) != null) {
            Logger.buildOutput("脚本文件执行信息:{}", errorLine);
        }
        def result = p.waitFor()
        Logger.buildOutput("jar excute result:${result}")
//        p.waitFor()
        if (result != 0) {
            throw new RuntimeException("failure to package classes.jar: \n" + p.err.text)
        }
        p.destroy()
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
        def outputDir = new File(publication.buildDir, Constants.BUILD_OUTPUT_DIR)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        filterJavaDocSource(new File(publication.sdkSourceSet.path), publication.sdkSourceSet.path, javaSource)
        return generateJavaDocSourceJar(javaSource)
    }

    private static File generateJavaDocSourceJar(File sourceDir) {
        def p = "jar cvf ../outputs/classes-source.jar .".execute(null, sourceDir)

        def shellInfoResultReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        //需要输出缓冲区数据 不然会阻塞在waitFor
        String infoLine;
        while ((infoLine = shellInfoResultReader.readLine()) != null) {
            Logger.buildOutput("脚本文件执行信息:{}", infoLine);
        }
        def result = p.waitFor()
        Logger.buildOutput("jar excute result:${result}")
        if (result != 0) {
            throw new RuntimeException("failure to make component-sdk java source directory: \n" + p.err.text)
        }
        def sourceJar = new File(sourceDir.parentFile, 'outputs/classes-source.jar')
        return sourceJar
    }


    /**
     * 递归过滤目录，找到文件copy到目标输出路径
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

    static boolean isMavenJarExists(Project project, PublicationOption publication) {
        String filePath = null
        String fileName = publication.artifactId + "-" + publication.sdkVersion + ".jar"
//  http://172.16.xxx.xxx:8081/nexus/content/groups/public/com/xxx/cif/xxx-cif-api/0.0.1-SNAPSHOT/xxx-cif-api-0.0.1-20170515.040917-89.jar
        String url = Runtimes.sSdkOption.getMavenUrl()
        if (TextUtils.isEmpty(url)) return false
        String line = HttpUrlConnectHelper.sendRequest("$url/${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.sdkVersion}/$fileName", "HEAD")
        if (!TextUtils.isEmpty(line)) {
            filePath = "$url/${publication.groupId.replace('.', '/')}/${publication.artifactId}/${publication.sdkVersion}/$fileName"
        }
        return filePath != null
    }

    static boolean compareMavenJar(Project project, PublicationOption publication, String localPath) {
        String filePath = null
        String fileName = publication.artifactId + "-" + publication.sdkVersion + ".jar"
        def name = "component[${publication.groupId}-${publication.artifactId}]Classpath"
        Configuration configuration = project.configurations.create(name)
        project.dependencies.add(name, PublicationUtil.getMavenGAV(publication))
        try {
            configuration.copy().files.each {
                if (it.name.endsWith(fileName)) {
                    filePath = it.absolutePath
                }
            }
        } catch (Exception e) {
//            e.printStackTrace()
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

    static String handleMavenJar(Project project, PublicationOption publication) {
        long startTime = System.currentTimeMillis()
        File target = new File(Runtimes.sSdkDir, PublicationUtil.getJarName(publication))
        if (publication.invalid) {
            PublicationManager.getInstance().addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return "Handle Maven jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
        }
        boolean isMavenJarExists = isMavenJarExists(project, publication)
        boolean hasGitDiff = PublicationManager.getInstance().hasModifyWithGitDiff(publication.sdkSourceSet)
        boolean hasModifiedSource = PublicationManager.getInstance().hasSdkModified(publication)

        if (!hasGitDiff && isMavenJarExists) {
            publication.invalid = false
            publication.useLocal = false
            PublicationManager.getInstance().addPublication(publication)
            return "Handle Maven jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
        } else if (target.exists()) {
            if (hasModifiedSource) {
                def releaseJar = JarUtil.packJavaSourceJar(project, publication, Runtimes.getAndroidJarPath(), Runtimes.getCompileOption(), true)
                if (releaseJar == null) {
                    publication.invalid = true
                    PublicationManager.getInstance().addPublication(publication)
                    if (target.exists()) {
                        target.delete()
                    }
                    return "Handle Maven jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
                }
                FileUtil.copyFile(releaseJar, target)
            }
            publication.invalid = false
            publication.useLocal = true
            PublicationManager.getInstance().addPublication(publication)
            return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
        } else {
            def releaseJar = JarUtil.packJavaSourceJar(project, publication, Runtimes.getAndroidJarPath(), Runtimes.getCompileOption(), true)
            if (releaseJar == null) {
                publication.invalid = true
                PublicationManager.getInstance().addPublication(publication)
                if (target.exists()) {
                    target.delete()
                }
                return "Handle Maven jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
            }
            FileUtil.copyFile(releaseJar, target)
            publication.useLocal = true
            publication.invalid = false
            PublicationManager.getInstance().addPublication(publication)
        }
        return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
    }

    static String handleLocalJar(Project project, PublicationOption publication) {
        long startTime = System.currentTimeMillis()
        File target = new File(Runtimes.sSdkDir, PublicationUtil.getJarName(publication))

        if (publication.invalid) {
            PublicationManager.getInstance().addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
        }

        if (target.exists()) {
            boolean hasModifiedSource = PublicationManager.getInstance().hasSdkModified(publication)
            if (!hasModifiedSource) {
                publication.invalid = false
                publication.useLocal = true
                PublicationManager.getInstance().addPublication(publication)
                Logger.buildOutput("${project.name} sdk nothing change,use local cache")
                return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
            }
        }

        File releaseJar = packJavaSourceJar(project, publication, Runtimes.getAndroidJarPath(), Runtimes.getCompileOption(), true)
        if (releaseJar == null) {
            publication.invalid = true
            PublicationManager.getInstance().addPublication(publication)
            if (target.exists()) {
                target.delete()
            }
            return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
        }

        FileUtil.copyFile(releaseJar, target)
        publication.invalid = false
        publication.useLocal = true
        PublicationManager.getInstance().addPublication(publication)
        return "Handle Local jar " + PublicationUtil.getJarName(publication) + " cost " + (System.currentTimeMillis() - startTime) + "ms"
    }
}
