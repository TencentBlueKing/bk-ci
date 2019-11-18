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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.scm.code.git.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
 *   "id": 21382,
 *   "forks_count": 0,
 *   "config_storage": {
 *     "limit_lfs_file_size": 512000,
 *     "limit_size": 512000,
 *     "limit_file_size": 512000,
 *     "limit_lfs_size": 512000
 *   },
 *   "description": "abcdefg",
 *   "public": false,
 *   "archived": false,
 *   "visibility_level": 0,
 *   "name": "devops",
 *   "name_with_namespace": "devops/devops",
 *   "path": "devops",
 *   "path_with_namespace": "devops/devops",
 *   "default_branch": "master",
 *   "ssh_url_to_repo": "git@git.com:devops/devops.git",
 *   "http_url_to_repo": "http://git.com/devops/devops.git",
 *   "web_url": "http://git.com/devops/devops",
 *   "issues_enabled": true,
 *   "merge_requests_enabled": true,
 *   "wiki_enabled": true,
 *   "snippets_enabled": true,
 *   "created_at": "2017-01-13T09:25:27+0000",
 *   "last_activity_at": "2018-01-08T06:53:14+0000",
 *   "creator_id": 8919,
 *   "namespace": {
 *     "created_at": "2016-11-24T07:18:38+0000",
 *     "description": "DEVOPS",
 *     "id": 9456,
 *     "name": "devops",
 *     "owner_id": null,
 *     "path": "devops",
 *     "updated_at": "2016-11-24T07:18:38+0000"
 *   },
 *   "avatar_url": null,
 *   "star_count": 0
 *   }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitProject(
    val id: Long,
    val description: String
)