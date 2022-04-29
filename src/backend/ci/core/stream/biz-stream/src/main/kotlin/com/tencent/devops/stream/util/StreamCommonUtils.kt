package com.tencent.devops.stream.util

import com.tencent.devops.stream.common.Constansts

object StreamCommonUtils {

    fun isCiFile(name: String): Boolean {
        if (name == Constansts.ciFileName) {
            return true
        }
        if (name.startsWith(Constansts.ciFileDirectoryName) &&
            (name.endsWith(Constansts.ciFileExtensionYml) || name.endsWith(Constansts.ciFileExtensionYaml))
        ) {
            return true
        }
        return false
    }
}
