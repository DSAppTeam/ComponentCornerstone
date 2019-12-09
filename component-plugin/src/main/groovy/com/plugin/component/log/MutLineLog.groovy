package com.plugin.component.log

class MutLineLog {

    StringBuilder stringBuilder = new StringBuilder("\n")

    MutLineLog build(String line) {
        stringBuilder.append("  ")
        stringBuilder.append(line)
        stringBuilder.append("\n")
        return this
    }

    MutLineLog build2(String line) {
        stringBuilder.append("      ")
        stringBuilder.append(line)
        stringBuilder.append("\n")
        return this
    }

    MutLineLog build3(String line) {
        stringBuilder.append("         ")
        stringBuilder.append(line)
        stringBuilder.append("\n")
        return this
    }

    MutLineLog build4(String line) {
        stringBuilder.append("              ")
        stringBuilder.append(line)
        stringBuilder.append("\n")
        return this
    }


    String done() {
        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1)
    }
}
