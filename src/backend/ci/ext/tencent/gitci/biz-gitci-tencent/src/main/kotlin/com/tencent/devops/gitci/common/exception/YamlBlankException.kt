package com.tencent.devops.gitci.common.exception

class YamlBlankException(val filePath: String, val repo: String? = null) : RuntimeException()
