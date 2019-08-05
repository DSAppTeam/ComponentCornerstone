package com.plugin.module.utils

import com.plugin.module.Constants

class FileUtil {

    /**
     * 是否是有效目标文件
     * @param file
     * @return
     */
    static boolean isValidPackSource(File file){
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
