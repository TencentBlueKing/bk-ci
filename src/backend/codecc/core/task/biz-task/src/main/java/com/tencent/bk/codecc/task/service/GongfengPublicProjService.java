/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.model.GongfengActiveProjEntity;
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity;
import com.tencent.bk.codecc.task.vo.CustomProjVO;
import com.tencent.bk.codecc.task.vo.GongfengPublicProjVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ProjectStatVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.Page;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工蜂公共项目服务接口
 *
 * @version V1.0
 * @date 2019/9/26
 */
public interface GongfengPublicProjService
{

    /**
     * 寻找所有项目
     *
     * @return
     */
    List<GongfengPublicProjEntity> findAllProjects();


    /**
     * 根据id查询项目信息
     * @param id
     * @return
     */
    GongfengPublicProjEntity findProjectById(Integer id);

    /**
     * 保存项目信息
     *
     * @param gongfengPublicProjEntity
     */
    void saveProject(GongfengPublicProjEntity gongfengPublicProjEntity);

    /**
     * 保存活跃项目信息
     * @param gongfengActiveProjEntity
     */
    void saveActiveProject(GongfengActiveProjEntity gongfengActiveProjEntity);

    /**
     * 获取工蜂项目路径
     * @param taskId
     * @return
     */
    String getGongfengUrl(Long taskId);

    /**
     * 查找所有活跃项目
     * @return
     */
    List<GongfengActiveProjEntity> findAllActiveProjects();

    /**
     * 判断活跃项目是否存在
     * @param id
     * @return
     */
    Boolean judgeActiveProjExists(Integer id);

    /**
     * 延伸工蜂扫描范围
     * @param startPage
     * @param endPage
     * @param startHour
     * @param startMinute
     * @return
     */
    Boolean extendGongfengScanRange(Integer startPage, Integer endPage, Integer startHour, Integer startMinute);


    /**
     * 根据工蜂id查询工蜂任务信息
     *
     * @param idSet 工蜂ID集合
     * @return list
     */
    List<GongfengPublicProjEntity> findProjectListByIds(Collection<Integer> idSet);


    /**
     * 获取工蜂项目id Map
     *
     * @param idSet 工蜂ID集合
     * @return map
     */
    Map<Integer, GongfengPublicProjVO> queryGongfengProjectMapById(Collection<Integer> idSet);


    /**
     * 保存项目度量数据
     *
     * @param bgId 事业群ID
     */
    Boolean saveStatProject(Integer bgId);


    /**
     * 根据工蜂id查询工蜂任务信息
     *
     * @param idSet 工蜂ID集合
     * @return list
     */
    List<GongfengStatProjEntity> findStatProjectList(Integer bgId, Collection<Integer> idSet);

    /**
     * 通过项目id查询统计信息
     * @param projectId
     * @return
     */
    GongfengStatProjEntity findStatByProjectId(Integer projectId);


    /**
     * 批量查询工蜂项目度量数据
     *
     * @param idSet 工蜂项目ID集合
     * @return map
     */
    Map<Integer, ProjectStatVO> queryGongfengStatProjectById(Integer bgId, Collection<Integer> idSet);

    /**
     * 分页查询个性化任务信息
     * @param reqVO
     * @return
     */
    Page<CustomProjVO> queryCustomTaskByPageable(QueryTaskListReqVO reqVO);

    /**
     * 通过项目id删除记录
     * @param id
     * @return
     */
    Boolean delete(Integer id);

    /**
     * 通过任务ID查询工蜂任务信息
     * @param taskId
     * @return
     */
    Map<Long, GongfengPublicProjVO> queryGongfengProjectMapByTaskId(List<Long> taskId);
}
