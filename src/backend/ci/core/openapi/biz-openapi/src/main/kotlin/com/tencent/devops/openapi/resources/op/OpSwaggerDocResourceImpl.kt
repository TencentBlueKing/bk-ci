package com.tencent.devops.openapi.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.op.OpSwaggerDocResource
import com.tencent.devops.openapi.pojo.SwaggerDocResponse
import com.tencent.devops.openapi.service.doc.DocumentService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpSwaggerDocResourceImpl @Autowired constructor(
    val docService: DocumentService
) : OpSwaggerDocResource {
    override fun docInit(checkMetaData: Boolean, checkMDData: Boolean): Result<Map<String, SwaggerDocResponse>> {
        return Result(docService.docInit(checkMetaData, checkMDData))
    }
}
