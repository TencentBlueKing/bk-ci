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

package com.tencent.bkrepo.webhook.pojo.payload

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.packages.VersionCreatedEvent
import com.tencent.bkrepo.webhook.pojo.payload.metadata.MetedataDeletedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.metadata.MetedataSavedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.node.NodeCopiedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.node.NodeCreatedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.node.NodeDeletedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.node.NodeMovedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.node.NodeRenamedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.project.ProjectCreatedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.repo.RepoCreatedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.repo.RepoDeletedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.repo.RepoUpdatedEventPayload
import com.tencent.bkrepo.webhook.pojo.payload.test.WebHookTestEventPayload

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ProjectCreatedEventPayload::class, name = "PROJECT_CREATED"),
    JsonSubTypes.Type(value = RepoCreatedEventPayload::class, name = "REPO_CREATED"),
    JsonSubTypes.Type(value = RepoUpdatedEventPayload::class, name = "REPO_UPDATED"),
    JsonSubTypes.Type(value = RepoDeletedEventPayload::class, name = "REPO_DELETED"),
    JsonSubTypes.Type(value = NodeCreatedEventPayload::class, name = "NODE_CREATED"),
    JsonSubTypes.Type(value = NodeRenamedEventPayload::class, name = "NODE_RENAMED"),
    JsonSubTypes.Type(value = NodeMovedEventPayload::class, name = "NODE_MOVED"),
    JsonSubTypes.Type(value = NodeCopiedEventPayload::class, name = "NODE_COPIED"),
    JsonSubTypes.Type(value = NodeDeletedEventPayload::class, name = "NODE_DELETED"),
    JsonSubTypes.Type(value = MetedataDeletedEventPayload::class, name = "METADATA_DELETED"),
    JsonSubTypes.Type(value = MetedataSavedEventPayload::class, name = "METADATA_SAVED"),
    JsonSubTypes.Type(value = VersionCreatedEvent::class, name = "VERSION_CREATED"),
    JsonSubTypes.Type(value = WebHookTestEventPayload::class, name = "WEBHOOK_TEST")
)
open class CommonEventPayload(
    open val eventType: EventType,
    open val user: UserInfo
)
