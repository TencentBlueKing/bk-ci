/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.constant.AuthMessageCode.INVALID_HANDOVER_TO
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.common.api.exception.ErrorCodeException
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组成员交接条件请求体")
data class GroupMemberHandoverConditionReq(
    @get:Schema(title = "组IDs")
    override val groupIds: List<MemberGroupJoinedDTO> = emptyList(),
    @get:Schema(title = "全选的资源类型")
    override val resourceTypes: List<String> = emptyList(),
    @get:Schema(title = "全量选择")
    override val allSelection: Boolean = false,
    @get:Schema(title = "目标对象")
    override val targetMember: ResourceMemberInfo,
    @get:Schema(title = "操作渠道")
    override val operateChannel: OperateChannel = OperateChannel.MANAGER,
    @get:Schema(title = "是否检查代码库交接")
    val checkRepertoryAuthorization: Boolean = true,
    @get:Schema(title = "授予人")
    val handoverTo: ResourceMemberInfo
) : GroupMemberCommonConditionReq(
    groupIds = groupIds,
    resourceTypes = resourceTypes,
    allSelection = allSelection,
    operateChannel = operateChannel,
    targetMember = targetMember
) {
    fun checkHandoverTo() {
        if (handoverTo.id == targetMember.id) {
            throw ErrorCodeException(
                errorCode = INVALID_HANDOVER_TO
            )
        }
    }
}
