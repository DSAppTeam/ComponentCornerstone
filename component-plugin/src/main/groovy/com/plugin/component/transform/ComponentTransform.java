package com.plugin.component.transform;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.ide.common.internal.WaitableExecutor;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.plugin.component.Constants;
import com.plugin.component.transform.info.ScanRuntime;
import com.plugin.component.transform.info.ScanSummaryInfo;
import com.plugin.component.utils.CacheDiskUtils;
import com.quinn.hunter.transform.RunVariant;
import com.quinn.hunter.transform.asm.ClassLoaderHelper;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * author : linzheng
 * e-mail : linzheng@corp.netease.com
 * time   : 2019/10/18
 * desc   :
 * version: 1.0
 */
public class ComponentTransform extends Transform {

    private final Logger logger;

    private static final Set<QualifiedContent.Scope> SCOPES = new HashSet<>();

    static {
        SCOPES.add(QualifiedContent.Scope.PROJECT);
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS);
        SCOPES.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    private Project project;
    protected ComponentWeaver bytecodeWeaver;
    private WaitableExecutor waitableExecutor;
    private boolean emptyRun = false;

    public ComponentTransform(Project project) {
        this.project = project;
        this.logger = project.getLogger();
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
        this.bytecodeWeaver = new ComponentWeaver();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return SCOPES;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }


    @SuppressWarnings("deprecation")
    @Override
    public void transform(Context context,
                          Collection<TransformInput> inputs,
                          Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider,
                          boolean isIncremental) throws IOException, TransformException, InterruptedException {
        RunVariant runVariant = getRunVariant();
        if ("debug".equals(context.getVariantName())) {
            emptyRun = runVariant == RunVariant.RELEASE || runVariant == RunVariant.NEVER;
        } else if ("release".equals(context.getVariantName())) {
            emptyRun = runVariant == RunVariant.DEBUG || runVariant == RunVariant.NEVER;
        }
        logger.warn(getName() + " isIncremental = " + isIncremental + ", runVariant = " + runVariant + ", emptyRun = " + emptyRun + ", inDuplcatedClassSafeMode = " + inDuplcatedClassSafeMode());
        long startTime = System.currentTimeMillis();
        if (!isIncremental) {
            outputProvider.deleteAll();
        }
        URLClassLoader urlClassLoader = ClassLoaderHelper.getClassLoader(inputs, referencedInputs, project);
        this.bytecodeWeaver.setClassLoader(urlClassLoader);
        boolean flagForCleanDexBuilderFolder = false;
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus();

                String filePath = jarInput.getFile().getAbsolutePath();

                File dest = outputProvider.getContentLocation(jarInput.getFile().getAbsolutePath(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if (isIncremental && !emptyRun) {
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
                            transformJar(jarInput.getFile(), dest, status);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest);
                            }
                            ScanRuntime.removedFile(filePath);
                            break;
                    }
                } else {
                    //Forgive me!, Some project will store 3rd-party aar for serveral copies in dexbuilder folder,,unknown issue.
                    if (inDuplcatedClassSafeMode() & !isIncremental && !flagForCleanDexBuilderFolder) {
                        cleanDexBuilderFolder(dest);
                        flagForCleanDexBuilderFolder = true;
                    }
                    transformJar(jarInput.getFile(), dest, status);
                }
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                FileUtils.forceMkdir(dest);
                if (isIncremental && !emptyRun) {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if (destFile.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    destFile.delete();
                                }
                                ScanRuntime.removedFile(inputFile.getAbsolutePath());
                                break;
                            case ADDED:
                            case CHANGED:
                                try {
                                    FileUtils.touch(destFile);
                                } catch (IOException e) {
                                    //maybe mkdirs fail for some strange reason, try again.
                                    Files.createParentDirs(destFile);
                                }
                                transformSingleFile(inputFile, destFile, srcDirPath);
                                break;
                        }
                    }
                } else {
                    transformDir(directoryInput.getFile(), dest);
                }

            }

        }
        waitableExecutor.waitForTasksWithQuickFail(true);

        ScanSummaryInfo cacheSummary = readPluginCache(); // 读取缓存

        ScanSummaryInfo scanSummaryInfo = ScanRuntime.updateSummaryInfo(cacheSummary); // 整理本次扫码结果，返回最新的模块结构
        ScanRuntime.buildComponentSdkInfo();
        ScanRuntime.logScanInfo();

        updatePluginCache(cacheSummary);   // 保存缓存

        long costTime = System.currentTimeMillis() - startTime;
        logger.warn((getName() + "scan code  costed " + costTime + "ms"));

        injectCode(scanSummaryInfo);
    }

    private void injectCode(@NotNull ScanSummaryInfo scanSummaryInfo) {
        long startTime = System.currentTimeMillis();

        String injectInputPath = scanSummaryInfo.inputFilePath; // 输入文件
        String injectOutputPath = scanSummaryInfo.outputFilePath; // 输出文件
        if (injectInputPath != null) {
            logger.warn(" inject file find : file = " + injectInputPath);
            try {
                CodeInjectProcessor codeInjectProcessor = new CodeInjectProcessor();
                codeInjectProcessor.injectCode(injectInputPath, injectOutputPath);
            } catch (Exception e) {
                e.printStackTrace();
                logger.warn("inject Code error");
            }
            logger.warn("inject Code end");
        }

        long injectCostTime = System.currentTimeMillis() - startTime;
        logger.warn((getName() + " inject code costed " + injectCostTime + "ms"));
        ScanRuntime.clearScanInfo();
        ScanRuntime.clearSummaryInfo();
    }

    private void updatePluginCache(ScanSummaryInfo cacheSummary) {
        try {
            String json = new Gson().toJson(cacheSummary);
            CacheDiskUtils.getInstance(project.getBuildDir()).put(Constants.PLUGIN_CACHE, json);
            logger.warn((getName() + "save cache success"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private ScanSummaryInfo readPluginCache() {
        ScanSummaryInfo cacheSummary = null;
        try {
            String json = CacheDiskUtils.getInstance(project.getBuildDir()).getString(Constants.PLUGIN_CACHE);
            cacheSummary = new Gson().fromJson(json, ScanSummaryInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cacheSummary == null) {
            cacheSummary = new ScanSummaryInfo();
        }
        return cacheSummary;
    }

    private void transformSingleFile(final File inputFile, final File outputFile, final String srcBaseDir) {
        waitableExecutor.execute(() -> {
            bytecodeWeaver.weaveSingleClassToFile(inputFile, outputFile, srcBaseDir);
            return null;
        });
    }

    private void transformDir(final File inputDir, final File outputDir) throws IOException {
        if (emptyRun) {
            FileUtils.copyDirectory(inputDir, outputDir);
            return;
        }
        final String inputDirPath = inputDir.getAbsolutePath();
        final String outputDirPath = outputDir.getAbsolutePath();
        if (inputDir.isDirectory()) {
            for (final File file : com.android.utils.FileUtils.getAllFiles(inputDir)) {
                waitableExecutor.execute(() -> {
                    String filePath = file.getAbsolutePath();
                    File outputFile = new File(filePath.replace(inputDirPath, outputDirPath));
                    bytecodeWeaver.weaveSingleClassToFile(file, outputFile, inputDirPath);
                    return null;
                });
            }
        }
    }

    private void transformJar(final File srcJar, final File destJar, Status status) {
        waitableExecutor.execute(() -> {
            if (emptyRun) {
                FileUtils.copyFile(srcJar, destJar);
                return null;
            }
            bytecodeWeaver.weaveJar(srcJar, destJar);
            return null;
        });
    }

    private void cleanDexBuilderFolder(File dest) {
        waitableExecutor.execute(() -> {
            try {
                String dexBuilderDir = replaceLastPart(dest.getAbsolutePath(), getName(), "dexBuilder");
                //intermediates/transforms/dexBuilder/debug
                File file = new File(dexBuilderDir).getParentFile();
                project.getLogger().warn("clean dexBuilder folder = " + file.getAbsolutePath());
                if (file.exists() && file.isDirectory()) {
                    com.android.utils.FileUtils.deleteDirectoryContents(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private String replaceLastPart(String originString, String replacement, String toreplace) {
        int start = originString.lastIndexOf(replacement);
        StringBuilder builder = new StringBuilder();
        builder.append(originString.substring(0, start));
        builder.append(toreplace);
        builder.append(originString.substring(start + replacement.length()));
        return builder.toString();
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    protected RunVariant getRunVariant() {
        return RunVariant.ALWAYS;
    }

    protected boolean inDuplcatedClassSafeMode() {
        return false;
    }

}
