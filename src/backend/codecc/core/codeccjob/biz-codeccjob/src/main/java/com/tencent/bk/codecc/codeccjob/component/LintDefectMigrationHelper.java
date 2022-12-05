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

package com.tencent.bk.codecc.codeccjob.component;

import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintFileQueryRepository;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * lint告警表迁移
 *
 * @version V3.0
 * @date 2020/06/10
 */
@Component
@Slf4j
public class LintDefectMigrationHelper
{
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DefectIdGenerator defectIdGenerator;
    @Autowired
    private LintFileQueryRepository lintDefectRepository;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    public Object getMigration(long taskId, String toolName)
    {
        Object res = redisTemplate.opsForValue().get(String.format("%s%d:%s", RedisKeyConstants.KEY_MIGRATION_FLAG, taskId, toolName));
        return res;
    }

    public void setMigrationFlag(long taskId, String toolName, String status)
    {
        redisTemplate.opsForValue().set(String.format("%s%d:%s", RedisKeyConstants.KEY_MIGRATION_FLAG, taskId, toolName), status, 60L, DAYS);
    }

    public void delMigrationFlag(long taskId, String toolName)
    {
        redisTemplate.delete(String.format("%s%d:%s", RedisKeyConstants.KEY_MIGRATION_FLAG, taskId, toolName));
    }

    public void migration(long taskId, String toolName)
    {
        Object migrationFlag = getMigration(taskId, toolName);
        if (migrationFlag == null)
        {
            // 标志开始数据迁移doing
            setMigrationFlag(taskId, toolName, "D");

            try
            {
                doMigration(taskId, toolName);
            }
            catch (Throwable t)
            {
                // 清除迁移标志
                log.error("migration lint defect error, taskId:{}, toolName: {}", taskId, toolName, t);
                delMigrationFlag(taskId, toolName);
            }

            // 标志数据已经迁移成功 true
            setMigrationFlag(taskId, toolName, "T");
        }
        else if (migrationFlag != null && "D".equals(migrationFlag.toString()))
        {
            do
            {
                try
                {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                migrationFlag = getMigration(taskId, toolName);
            }
            while("D".equals(migrationFlag.toString()));
        }
    }

    protected void doMigration(long taskId, String toolName)
    {
        List<LintDefectV2Entity> allDefects = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        List<LintFileEntity> allFileEntityList = lintDefectRepository.findByTaskIdAndToolName(taskId, toolName);
        allFileEntityList.forEach(file ->
        {
            if (StringUtils.isNotEmpty(file.getFilePath()) && file.getTaskId() != 0L
                    && StringUtils.isNotEmpty(file.getToolName()) && !CollectionUtils.isEmpty(file.getDefectList()))
            {
                int increment = file.getDefectList().size();
                Long currMaxId = defectIdGenerator.generateDefectId(file.getTaskId(), file.getToolName(), increment);
                AtomicLong currMinIdAtom = new AtomicLong(currMaxId - increment + 1);
                List<LintDefectV2Entity> defects = file.getDefectList().stream()
                        .map(oldDefect ->
                        {
                            String entityId = oldDefect.getDefectId();
                            if (StringUtils.isEmpty(entityId) || ids.contains(entityId))
                            {
                                entityId = ObjectId.get().toString();
                            }
                            ids.add(entityId);
                            LintDefectV2Entity defectV2Entity = new LintDefectV2Entity();
                            BeanUtils.copyProperties(oldDefect, defectV2Entity);
                            defectV2Entity.setEntityId(entityId);
                            defectV2Entity.setId(String.valueOf(currMinIdAtom.getAndIncrement()));
                            defectV2Entity.setTaskId(file.getTaskId());
                            defectV2Entity.setToolName(file.getToolName());
                            defectV2Entity.setFilePath(file.getFilePath());
                            defectV2Entity.setRelPath(file.getRelPath());
                            defectV2Entity.setUrl(file.getUrl());
                            defectV2Entity.setRepoId(file.getRepoId());
                            defectV2Entity.setRevision(file.getRevision());
                            defectV2Entity.setBranch(file.getBranch());
                            defectV2Entity.setSubModule(file.getSubModule());
                            defectV2Entity.setFileUpdateTime(file.getFileUpdateTime());
                            defectV2Entity.setFileMd5(file.getMd5());
                            String filePath = defectV2Entity.getFilePath();
                            int fileNameIndex = filePath.lastIndexOf("/");
                            if (fileNameIndex == -1)
                            {
                                fileNameIndex = filePath.lastIndexOf("\\");
                            }
                            defectV2Entity.setFileName(filePath.substring(fileNameIndex + 1));
                            if ((oldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0
                                    && StringUtils.isNotEmpty(oldDefect.getFixedBranch()))
                            {
                                defectV2Entity.setRepoId(oldDefect.getFixedRepoId());
                                defectV2Entity.setRevision(oldDefect.getFixedRevision());
                                defectV2Entity.setBranch(oldDefect.getFixedBranch());
                            }
                            return defectV2Entity;
                        }).collect(Collectors.toList());
                allDefects.addAll(defects);
            }
        });

        if (!CollectionUtils.isEmpty(allDefects))
        {
            lintDefectV2Repository.saveAll(allDefects);
        }
        log.info("success migration lint defect, taskId: {}, toolName:{}, defectCount: {}", taskId, toolName, allDefects.size());
    }
}
