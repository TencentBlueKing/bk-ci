package com.tencent.devops.stream.common.exception

class YamlBlankException(val filePath: String, val repo: String? = null) : RuntimeException()
