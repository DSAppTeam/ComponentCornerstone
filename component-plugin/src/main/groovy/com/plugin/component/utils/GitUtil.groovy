package com.plugin.component.utils

class GitUtil {

    static boolean canPublish(String filePath) {
        if (isGitExist()) {
            if (getGitBranch().matches("(^develop\$)|(^release/(.)*)")) {
                return getGitDiff(filePath).isEmpty()
            }
           return getGitDiff(filePath).isEmpty() && getGitCherry().isEmpty()
        } else {
            return true
        }
    }

    static boolean isGitExist() {
        return !FileUtil.shell("git --help").contains("git command not found")
    }

    static String getGitDiff(String filePath) {
        return FileUtil.shell("git diff ${filePath}")
    }

    static String getGitCherry() {
        return FileUtil.shell("git cherry")
    }

    static String getGitBranch() {
        return FileUtil.shell("git symbolic-ref --short -q HEAD")
    }

}