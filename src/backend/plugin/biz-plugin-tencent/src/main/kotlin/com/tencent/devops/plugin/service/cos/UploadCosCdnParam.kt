package com.tencent.devops.plugin.service.cos

import com.tencent.devops.common.cos.COSClientConfig

data class UploadCosCdnParam(
    var projectId: String,
    var pipelineId: String,
    var buildId: String,
    var elementId: String,
    var regexPaths: String,
    var customize: Boolean,
    var bucket: String,
    var cdnPath: String,
    var domain: String,
    var cosClientConfig: COSClientConfig
)
