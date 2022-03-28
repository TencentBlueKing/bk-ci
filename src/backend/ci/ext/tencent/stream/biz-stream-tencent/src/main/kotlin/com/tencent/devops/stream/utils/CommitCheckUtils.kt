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

package com.tencent.devops.stream.utils

import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.isDeleteBranch
import com.tencent.devops.stream.pojo.isDeleteTag
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

object CommitCheckUtils {
    // 推送启动构建消息,当人工触发以及未开启的不推送构建消息
    fun needSendCheck(gitRequestEventForHandle: GitRequestEventForHandle, gitCIBasicSetting: GitCIBasicSetting): Boolean {
//        val event = request.gitEvent ?: return false
        return (gitCIBasicSetting.enableCommitCheck || gitRequestEventForHandle.checkRepoTrigger) &&
            gitRequestEventForHandle.gitRequestEvent.objectKind != TGitObjectKind.MANUAL.value &&
            gitRequestEventForHandle.gitRequestEvent.objectKind != TGitObjectKind.OPENAPI.value &&
            !gitRequestEventForHandle.gitRequestEvent.isDeleteBranch() &&
            !gitRequestEventForHandle.gitRequestEvent.isDeleteTag()
    }
}
