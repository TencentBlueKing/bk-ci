package com.tencent.devops.plugin.codecc.exception
import com.tencent.devops.common.api.enums.OSType

class CodeccDownloadException constructor(osType: OSType): RuntimeException("not support os: $osType")