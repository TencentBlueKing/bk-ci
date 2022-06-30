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

package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.dao.mongorepository.UserLogInfoStatRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.UserLogDao;
import com.tencent.bk.codecc.task.model.UserLogInfoEntity;
import com.tencent.bk.codecc.task.model.UserLogInfoStatEntity;
import com.tencent.bk.codecc.task.pojo.Response;
import com.tencent.bk.codecc.task.pojo.TofOrganizationInfo;
import com.tencent.bk.codecc.task.pojo.TofStaffInfo;
import com.tencent.bk.codecc.task.service.UserLogInfoService;
import com.tencent.bk.codecc.task.tof.TofClientApi;
import com.tencent.devops.common.api.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 用户日志统计服务实现
 *
 * @version V1.0
 * @date 2020/10/20
 */
@Slf4j
@Service
public class UserLogInfoServiceImpl implements UserLogInfoService {

    @Autowired
    private UserLogInfoStatRepository userLogInfoStatRepository;

    @Autowired
    private TofClientApi tofClientApi;

    @Autowired
    private UserLogDao userLogDao;



    private TofOrganizationInfo getTofOrgInfoByUserName(String userName) {
        Response<TofStaffInfo> tofStaffInfoResponse = tofClientApi.getStaffInfoByUserName(userName);
        TofStaffInfo tofStaffInfo = tofStaffInfoResponse.getData();
        if (tofStaffInfo == null) {
            return null;
        }
        return tofClientApi.getOrganizationInfoByGroupId(tofStaffInfo.getGroupId());
    }

    /**
     * 更新用户日志统计
     *
     * @param statEntity entity
     */
    @Override
    public void findAndUpdateLogInfo(UserLogInfoStatEntity statEntity) {

        if (statEntity == null) {
            return;
        }
        String userName = statEntity.getUserName();

        UserLogInfoStatEntity entity = userLogInfoStatRepository.findFirstByUserName(userName);
        if (entity == null) {
            TofOrganizationInfo organizationInfo = getTofOrgInfoByUserName(userName);
            // 异常账号查不到组织架构,则仅保存名字及时间
            if (organizationInfo != null) {
                statEntity.setBgId(organizationInfo.getBgId());
                statEntity.setDeptId(organizationInfo.getDeptId());
                statEntity.setCenterId(organizationInfo.getCenterId());
            }
            entity = statEntity;
        }
        // 更新最后登录时间
        entity.setLastLogin(statEntity.getFirstLogin());
        userLogInfoStatRepository.save(entity);
    }


    /**
     * 仅用于刷一次存量用户统计数据
     */
    @Override
    public Boolean intiUserLogInfoStatScript() {
        List<String> distinctUserName = userLogDao.findDistinctUserName();
        if (CollectionUtils.isEmpty(distinctUserName)) {
            log.warn("distinctUserName list is empty");
            return false;
        }
        List<UserLogInfoStatEntity> dataList = Lists.newArrayList();

        // 等长分割List
        List<List<String>> partitionList = ListUtils.partition(distinctUserName, 200);
        partitionList.forEach(userNameList -> {
            List<UserLogInfoEntity> userLogInfoEntityList = userLogDao.findByLoginTimeDesc(userNameList);
            if (CollectionUtils.isEmpty(userLogInfoEntityList)) {
                return;
            }

            userLogInfoEntityList.forEach(userLogInfoEntity -> {
                String userName = userLogInfoEntity.getUserName();
                TofOrganizationInfo tofOrgInfo = getTofOrgInfoByUserName(userName);
                if (tofOrgInfo == null) {
                    return;
                }
                UserLogInfoStatEntity entity = new UserLogInfoStatEntity();
                entity.setUserName(userName);
                entity.setFirstLogin(
                        DateTimeUtil.INSTANCE.convertLocalDateTimeToTimestamp(userLogInfoEntity.getLoginTime()));
                entity.setLastLogin(
                        DateTimeUtil.INSTANCE.convertLocalDateToTimestamp(userLogInfoEntity.getLoginDate()));
                entity.setBgId(tofOrgInfo.getBgId());
                entity.setDeptId(tofOrgInfo.getDeptId());
                entity.setCenterId(tofOrgInfo.getCenterId());
                dataList.add(entity);
            });
        });

        userLogInfoStatRepository.saveAll(dataList);
        return true;
    }
}
