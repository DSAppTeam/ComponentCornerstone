package com.plugin.component.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.plugin.component.PluginRuntime
import com.plugin.component.extension.option.RunAloneOption
import com.plugin.component.utils.Utils
import javassist.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * https://juejin.im/post/5cbffc7af265da03a97aed41
 * created by yummylau 2019/08/09
 */
class CodeTransform extends Transform {

    private Project project
    ClassPool classPool
    String applicationName
    private static final String COMPONENT_LIKE = "com.effective.router.core.IComponentLike"

    CodeTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        def inputs = transformInvocation.getInputs()

        //读取项目配置的 application，后续考虑使用注解？？？
        RunAloneOption aloneConfiguration = PluginRuntime.sRunAloneMap.get(project.name)
        applicationName = aloneConfiguration.applicationName
        if (applicationName == null || applicationName.isEmpty()) {
            throw new RuntimeException("you should set applicationName in runAlone")
        }

        classPool = new ClassPool()
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
        }
        def box = Utils.toCtClasses(inputs, classPool)

        //要收集的application，一般情况下只有一个
        List<CtClass> applications = new ArrayList<>()

        //要收集的componentLikes，一般情况下有几个组件就有几个componentLikes
        List<CtClass> componentLikes = new ArrayList<>()

        for (CtClass ctClass : box) {
            if (isApplication(ctClass)) {
                applications.add(ctClass)
                continue
            }
            if (isComponentLike(ctClass)) {
                componentLikes.add(ctClass)
            }
        }
        for (CtClass ctClass : applications) {
            System.out.println("application is   " + ctClass.getName())
        }
        for (CtClass ctClass : componentLikes) {
            System.out.println("componentLike is   " + ctClass.getName())
        }

        transformInvocation.inputs.each { TransformInput input ->

            input.jarInputs.each { JarInput jarInput ->

                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)

            }

            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                boolean isRegisterComponentAuto = aloneConfiguration.isRegisterComponentAuto
                //如果是自动注入组件，则
                if (isRegisterComponentAuto) {
                    String fileName = directoryInput.file.absolutePath
                    File dir = new File(fileName)
                    dir.eachFileRecurse { File file ->
                        String filePath = file.absolutePath
                        String classNameTemp = filePath.replace(fileName, "")
                                .replace("\\", ".")
                                .replace("/", ".")
                        if (classNameTemp.endsWith(".class")) {
                            String className = classNameTemp.substring(1, classNameTemp.length() - 6)
                            if (className.equals(applicationName)) {
                                injectApplicationCode(applications.get(0), componentLikes, fileName)
                            }
                        }
                    }
                }
                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }


    /**
     * 注入application code
     * @param ctClassApplication
     * @param componentLikes
     * @param patch
     */
    private void injectApplicationCode(CtClass ctClassApplication, List<CtClass> componentLikes, String patch) {
        System.out.println("injectApplicationCode begin")
        ctClassApplication.defrost()
        //注入componentLike的onCreate的代码
        StringBuilder autoLoadComCode = new StringBuilder()
        for (CtClass ctClass : componentLikes) {
            autoLoadComCode.append("new " + ctClass.getName() + "()" + ".onCreate();")
        }
        try {
            CtMethod attachBaseContextMethod = ctClassApplication.getDeclaredMethod("onCreate", null)
            attachBaseContextMethod.insertAfter(autoLoadComCode.toString())
        } catch (CannotCompileException | NotFoundException e) {
            StringBuilder methodBody = new StringBuilder()
            methodBody.append("protected void onCreate() {")
            methodBody.append("super.onCreate();")
            methodBody.append(autoLoadComCode.toString())
            methodBody.append("}")
            ctClassApplication.addMethod(CtMethod.make(methodBody.toString(), ctClassApplication))
        } catch (Exception e) {

        }
        ctClassApplication.writeFile(patch)
        ctClassApplication.detach()

        System.out.println("injectApplicationCode success ")
    }


    private boolean isApplication(CtClass ctClass) {
        try {
            if (applicationName != null && applicationName.equals(ctClass.getName())) {
                return true
            }
        } catch (Exception e) {
            println "class not found exception class name:  " + ctClass.getName()
        }
        return false
    }

    private boolean isComponentLike(CtClass ctClass) {
        try {
            for (CtClass ctClassInter : ctClass.getInterfaces()) {
                if (COMPONENT_LIKE.equals(ctClassInter.name)) {
                    return true
                }
            }
        } catch (Exception e) {
            println "class not found exception class name:  " + ctClass.getName()
        }

        return false
    }

    @Override
    String getName() {
        return "ComponentCode"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * application 注册一般需要指定 SCOPE_FULL_PROJECT
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

}