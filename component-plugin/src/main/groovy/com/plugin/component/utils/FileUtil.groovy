package com.plugin.component.utils

import com.plugin.component.Constants

class FileUtil {

    static boolean isValidClassFile(File file) {
        if (file == null) {
            return false
        }
        return isValidClassFile(file.name)
    }

    static boolean isValidClassFile(String fileName) {
        if (fileName == null) {
            return false
        }
        return fileName.endsWith(".class") && !fileName.contains("R\$") &&
                !fileName.contains("R.class") && !fileName.contains("BuildConfig.class")
    }


//    static boolean isFilterPackage(String fileName) {
//        if (PluginRuntime2.ignorePackages == null || fileName == null) return false
//        for (int i = 0; i < PluginRuntime2.ignorePackages.length; i++) {
//            if (fileName.startsWith(PluginRuntime2.ignorePackages[i])) {
//                return true
//            }
//        }
//        return false
//    }


    /**
     * 是否是有效目标文件
     * @param file
     * @return
     */
    static boolean isValidPackSource(File file) {
        return file != null &&
                (file.name.endsWith(Constants.JAVA_FILE_END) || file.name.endsWith(Constants.KOTLIN_FILE_END))
    }

    /**
     * 复制文件
     *
     * @param source
     * @param target
     */
    static void copyFile(File source, File target) {
        try {
            InputStream input = new FileInputStream(source)
            OutputStream output = new FileOutputStream(target)
            byte[] buf = new byte[1024]
            int bytesRead
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead)
            }
            input.close()
            output.close()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
