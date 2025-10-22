/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.MemberGroupJoinedDTO
import com.tencent.devops.auth.pojo.enum.OperateChannel
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组成员续期")
data class GroupMemberRenewalConditionReq(
    @get:Schema(title = "组IDs")
    override val groupIds: List<MemberGroupJoinedDTO>,
    @get:Schema(title = "全选某种资源类型下的用户组")
    override val resourceTypes: List<String> = emptyList(),
    @get:Schema(title = "全量选择")
    override val allSelection: Boolean = false,
    @get:Schema(title = "目标对象")
    override val targetMember: ResourceMemberInfo,
    @get:Schema(title = "操作渠道")
    override val operateChannel: OperateChannel = OperateChannel.MANAGER,
    @get:Schema(title = "续期时长(天)")
    val renewalDuration: Int
) : GroupMemberCommonConditionReq(
    groupIds = groupIds,
    resourceTypes = resourceTypes,
    allSelection = allSelection,
    operateChannel = operateChannel,
    targetMember = targetMember
)
