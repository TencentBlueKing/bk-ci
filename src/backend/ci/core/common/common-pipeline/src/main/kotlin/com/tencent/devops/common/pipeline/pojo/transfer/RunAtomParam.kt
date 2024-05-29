package com.tencent.devops.common.pipeline.pojo.transfer

data class RunAtomParam(
    val shell: String? = null,
    val script: String? = null,
    val charsetType: CharsetType? = null
) {
    enum class CharsetType {
        /*默认类型*/
        DEFAULT,

        /*UTF_8*/
        UTF_8,

        /*GBK*/
        GBK;

        companion object {
            fun parse(charset: String?): CharsetType {
                values().forEach {
                    if (it.name == charset) return it
                }
                return DEFAULT
            }
        }
    }

    enum class ShellType(val shellName: String) {
        /*bash*/
        BASH("bash"),

        /*cmd*/
        CMD("cmd"),

        /*powershell*/
        POWERSHELL_CORE("pwsh"),

        /*powershell*/
        POWERSHELL_DESKTOP("powershell"),

        /*python*/
        PYTHON("python"),

        /*sh命令*/
        SH("sh"),

        /*windows 执行 bash*/
        WIN_BASH("win_bash"),

        /*按系统默认*/
        AUTO("auto");
    }
}
