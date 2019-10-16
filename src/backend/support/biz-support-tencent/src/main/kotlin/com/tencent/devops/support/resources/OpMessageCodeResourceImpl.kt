package com.tencent.devops.support.resources

import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.OpMessageCodeResource
import com.tencent.devops.support.model.code.AddMessageCodeRequest
import com.tencent.devops.support.model.code.MessageCodeResp
import com.tencent.devops.support.model.code.UpdateMessageCodeRequest
import com.tencent.devops.support.services.MessageCodeDetailService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMessageCodeResourceImpl @Autowired constructor(private val messageCodeDetailService: MessageCodeDetailService) :
    OpMessageCodeResource {

    override fun addMessageCodeDetail(addMessageCodeRequest: AddMessageCodeRequest): Result<Boolean> {
        return messageCodeDetailService.addMessageCodeDetail(addMessageCodeRequest)
    }

    override fun refreshMessageCodeCache(messageCode: String): Result<Boolean> {
        return messageCodeDetailService.refreshMessageCodeCache(messageCode)
    }

    override fun getMessageCodeDetails(
        messageCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MessageCodeResp> {
        return messageCodeDetailService.getMessageCodeDetails(messageCode, page, pageSize)
    }

    override fun updateMessageCodeDetail(messageCode: String, updateMessageCodeRequest: UpdateMessageCodeRequest): Result<Boolean> {
        return messageCodeDetailService.updateMessageCodeDetail(messageCode, updateMessageCodeRequest)
    }

    override fun getMessageCodeDetail(messageCode: String): Result<MessageCodeDetail?> {
        return messageCodeDetailService.getMessageCodeDetail(messageCode)
    }
}