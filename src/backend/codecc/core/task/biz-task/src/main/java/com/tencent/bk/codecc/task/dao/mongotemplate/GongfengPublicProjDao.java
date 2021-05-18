/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 工蜂项目存取对象
 *
 * @version V1.1
 * @date 2020/3/24
 */

@Repository
public class GongfengPublicProjDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 插入和更新工蜂项目数据
     *
     * @param gongfengPublicProjEntity
     */
    public void upsertGongfengPublicProj(GongfengPublicProjEntity gongfengPublicProjEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(gongfengPublicProjEntity.getId()));

        Update update = new Update();
        update.set("id", gongfengPublicProjEntity.getId())
                .set("description", gongfengPublicProjEntity.getDescription())
                .set("public", gongfengPublicProjEntity.getPublicProj())
                .set("archived", gongfengPublicProjEntity.getArchived())
                .set("visibility_level", gongfengPublicProjEntity.getVisibilityLevel())
                .set("namespace", gongfengPublicProjEntity.getNameSpace())
                .set("owner", gongfengPublicProjEntity.getOwner())
                .set("name", gongfengPublicProjEntity.getName())
                .set("name_with_namespace", gongfengPublicProjEntity.getNameWithNameSpace())
                .set("path", gongfengPublicProjEntity.getPath())
                .set("path_with_namespace", gongfengPublicProjEntity.getPathWithNameSpace())
                .set("default_branch", gongfengPublicProjEntity.getDefaultBranch())
                .set("ssl_url_to_repo", gongfengPublicProjEntity.getSshUrlToRepo())
                .set("http_url_to_repo", gongfengPublicProjEntity.getHttpUrlToRepo())
                .set("https_url_to_repo", gongfengPublicProjEntity.getHttpsUrlToRepo())
                .set("web_url", gongfengPublicProjEntity.getWebUrl())
                .set("tag_list", gongfengPublicProjEntity.getTagList())
                .set("issues_enabled", gongfengPublicProjEntity.getIssuesEnabled())
                .set("merge_requests_enabled", gongfengPublicProjEntity.getMergeRequestsEnabled())
                .set("wiki_enabled", gongfengPublicProjEntity.getWikiEnabled())
                .set("snippets_enabled", gongfengPublicProjEntity.getSnippetsEnabled())
                .set("review_enabled", gongfengPublicProjEntity.getReviewEnabled())
                .set("fork_enabled", gongfengPublicProjEntity.getForkEnabled())
                .set("tag_name_regex", gongfengPublicProjEntity.getTagNameRegex())
                .set("tag_create_push_level", gongfengPublicProjEntity.getTagCreatePushLevel())
                .set("created_at", gongfengPublicProjEntity.getCreatedAt())
                .set("last_activity_at", gongfengPublicProjEntity.getLastActivityAt())
                .set("creator_id", gongfengPublicProjEntity.getCreatorId())
                .set("avatar_url", gongfengPublicProjEntity.getAvatarUrl())
                .set("watchs_count", gongfengPublicProjEntity.getWatchsCount())
                .set("stars_count", gongfengPublicProjEntity.getStarsCount())
                .set("forks_count", gongfengPublicProjEntity.getForksCount())
                .set("config_storage", gongfengPublicProjEntity.getConfigStorage())
                .set("forked_from_project", gongfengPublicProjEntity.getForkedFromProject())
                .set("statistics", gongfengPublicProjEntity.getStatistics())
                .set("synchronize_time", gongfengPublicProjEntity.getSynchronizeTime());
        mongoTemplate.upsert(query, update, GongfengPublicProjEntity.class);
    }
}