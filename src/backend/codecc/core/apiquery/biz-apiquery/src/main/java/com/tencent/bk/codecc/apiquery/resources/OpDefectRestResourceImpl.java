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

package com.tencent.bk.codecc.apiquery.resources;

import com.tencent.bk.codecc.apiquery.api.OpDefectRestResource;
import com.tencent.bk.codecc.apiquery.service.CodeLineStatisticService;
import com.tencent.bk.codecc.apiquery.service.IOpDefectDataService;
import com.tencent.bk.codecc.apiquery.service.openapi.ApiBizService;
import com.tencent.bk.codecc.apiquery.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.apiquery.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.apiquery.vo.CodeLineStatisticVO;
import com.tencent.bk.codecc.apiquery.vo.TaskToolInfoReqVO;
import com.tencent.bk.codecc.apiquery.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.apiquery.vo.op.FileStatusVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectSummaryVO;
import com.tencent.bk.codecc.apiquery.vo.op.TaskDefectVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.util.AuthApiUtils;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.ThreadPoolUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * op告警查询接口实现
 *
 * @version V3.0
 * @date 2020/9/2
 */
@Slf4j
@RestResource
public class OpDefectRestResourceImpl implements OpDefectRestResource {

    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCacheService;

    @Autowired
    private CodeLineStatisticService codeLineStatisticService;
    @Autowired
    private ApiBizService apiBizService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @NotNull
    private <T> T generateBizService(String toolName, String businessType, Class<T> clz) {
        String beanName = String.format("%s%s%s", toolName, businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        // 获取工具名称开头的处理类
        T processor = getProcessor(clz, beanName);

        // 获取工具类型开头的处理类
        if (processor == null) {
            beanName = String.format("%s%s%s", toolMetaCacheService.getToolPattern(toolName), businessType,
                    ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, beanName);
        }

        // 如果没找到工具的具体处理类，则采用通用的处理器
        if (processor == null) {
            beanName = String.format("%s%s%s", ComConstants.COMMON_BIZ_SERVICE_PREFIX, businessType,
                    ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, beanName);
        }

        if (processor == null) {
            log.error("get bean name [{}] fail!", beanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR, new String[]{"Bean Name Not Found!"});
        }

        return processor;
    }


    private <T> T getProcessor(Class<T> clz, String beanName) {
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            // log.error("Bean Name [{}] Not Found:", beanName);
        }
        return processor;
    }


    @Override
    public Result<Page<TaskDefectVO>> queryDeptTaskDefect(String userName, TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {

        // 判断是否为管理员
        if (!AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{userName});
        }

        IOpDefectDataService bizService =
                generateBizService(reqVO.getToolName(), ComConstants.BusinessType.DEFECT_DATA.value(),
                        IOpDefectDataService.class);

        return new Result<>(bizService.queryDeptTaskDefect(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<ToolDefectRspVO> queryDeptDefectList(String userName, TaskToolInfoReqVO reqVO, Integer pageNum,
            Integer pageSize) {
        // 判断是否为管理员
        if (!AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{userName});
        }

        IOpDefectDataService bizService =
                generateBizService(reqVO.getToolName(), ComConstants.BusinessType.DEFECT_DATA.value(),
                        IOpDefectDataService.class);
        return new Result<>(bizService.batchQueryDeptDefectList(reqVO, pageNum, pageSize));
    }

    @Override
    public Result<List<CodeLineStatisticVO>> codeLineTotalAndCodeLineDailyStatData(TaskToolInfoReqVO reqVO) {
        return new Result<>(codeLineStatisticService.getCodeLineTotalAndCodeLineDailyStatData(reqVO));
    }

    @Override
    public Result<Page<TaskDefectSummaryVO>> queryTaskDefectSumPage(String userName, TaskToolInfoReqVO reqVO,
            Integer pageNum, Integer pageSize, String sortField, String sortType) {

        // 判断是否为管理员
        if (!AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{userName});
        }
        return new Result<>(apiBizService.queryTaskDefectSum(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<String> exportDimensionToFile(String userName, TaskToolInfoReqVO reqVO) {
        // 判断是否为管理员
        if (!AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{userName});
        }

        String isFreeTime = redisTemplate.opsForValue().get(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG);
        // 如果正在导出,则返回false
        if (Boolean.toString(false).equals(isFreeTime)) {
            log.warn("exportDimensionToFile: the export interface is busy!");
            return new Result<>(isFreeTime);
        }

        String startDate = reqVO.getStartTime().split(" ")[0];
        String endDate = reqVO.getEndTime().split(" ")[0];
        String fileName =
                String.format("TaskDimensionDefect_%s_%s_%s.xlsx", startDate, endDate, System.currentTimeMillis());
        // 生成文件索引
        String fileIndex = thirdPartySystemCaller.getFileIndex(fileName, "OP_EXCEL");

        ThreadPoolUtil.addRunnableTask(() -> apiBizService.exportDimensionToFile(reqVO, fileIndex));
        // 返回文件名
        return new Result<>(fileName);
    }

    @Override
    public Result<String> queryDimensionExportFlag() {
        // 导出状态：false表示繁忙; null或者true表示空闲 可以导
        String isFreeTime = redisTemplate.opsForValue().get(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG);
        return new Result<>(Boolean.toString(!Boolean.toString(false).equals(isFreeTime)));
    }

    @Override
    public Result<String> updateDimensionExportFlag() {
        String isFreeTime = redisTemplate.opsForValue().get(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG);
        String change;
        if (Boolean.toString(true).equals(isFreeTime)) {
            change = Boolean.toString(false);
        } else {
            change = Boolean.toString(true);
        }
        redisTemplate.opsForValue().set(RedisKeyConstants.DIMENSION_FILE_EXPORT_FLAG, change);
        String msg = String.format("updateDimensionExportFlag: %s status is changed to %s status", isFreeTime, change);
        log.info(msg);
        return new Result<>(msg);
    }

    @Override
    public Result<FileStatusVO> getDimensionFileFlag() {
        // 查询当前文件名
        String fileName = redisTemplate.opsForValue().get(RedisKeyConstants.DIMENSION_FILE_NAME);
        if (null == fileName) {
            fileName = "--";
        }
        String fileFlag = redisTemplate.opsForValue().get(RedisKeyConstants.DIMENSION_FILE_FLAG + fileName);
        if (null == fileFlag) {
            fileFlag = ComConstants.FileStatus.NOT_STARTED.getCode();
        }
        FileStatusVO fileStatusVO = new FileStatusVO(fileName, fileFlag);
        return new Result<>(fileStatusVO);
    }

}
