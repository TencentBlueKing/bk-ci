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

package com.tencent.bk.codecc.schedule.component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 并发分析的参数配置的缓存
 *
 * @version V1.0
 * @date 2020/11/5
 */
@Component
@Slf4j
public class ConcurrentAnalyzeConfigCache {
    /**
     * 定时刷新缓存的时间间隔(分钟)
     */
    final String REFRESH_DURATION = "REFRESH_DURATION";
    final String DEFAULT_MAX_CONCURRENT = "DEFAULT_MAX_CONCURRENT";
    final String CUSTOM_CONCURRENT = "CUSTOM_CONCURRENT";
    final String VIP_PROJECTS = "VIP_PROJECTS";
    /**
     * 并发分析的参数配置
     */
    final String CONCURRENT_ANALYZE_CONFIG = "CONCURRENT_ANALYZE_CONFIG";

    @Autowired
    private Client client;

    /**
     * 并发分析的参数配置的缓存
     *
     * @param paramCode
     * @return
     */
    private List<BaseDataVO> getConcurrentAnalyzeConfigs(String paramCode) {
        Result<List<BaseDataVO>> paramsResult = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCode(CONCURRENT_ANALYZE_CONFIG, paramCode);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}, param code: {}", CONCURRENT_ANALYZE_CONFIG, paramCode);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> concurrentAnalyzeConfigs = paramsResult.getData();
        return concurrentAnalyzeConfigs;
    }

    /**
     * 并发分析的通用配置参数缓存，每2小时刷新一次
     */
    private LoadingCache<String, String> concurrentAnalyzeCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) {
                    List<BaseDataVO> concurrentAnalyzeConfigs = getConcurrentAnalyzeConfigs(key);
                    return concurrentAnalyzeConfigs.get(0).getParamValue();
                }
            });

    /**
     * 项目自定义最高并发数缓存
     */
    private LoadingCache<String, Map<String, Integer>> customConcurrentCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(60, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Map<String, Integer>>() {
                @Override
                public Map<String, Integer> load(String key) {
                    return getCustomConcurrent(key);
                }
            });

    /**
     * VIP项目缓存
     */
    private LoadingCache<String, Set<String>> vipProjectCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(60, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String key) {
                    return getVipProject(key);
                }

                private Set<String> getVipProject(String paramCode) {
                    List<BaseDataVO> vipProjects = getConcurrentAnalyzeConfigs(paramCode);
                    String vipProjectStr = vipProjects.get(0).getParamValue();
                    Set<String> vipProjectSet = new HashSet<>();
                    if (StringUtils.isNotEmpty(vipProjectStr)) {
                        String[] vipProjectArr = vipProjectStr.split(",");
                        vipProjectSet.addAll(Arrays.asList(vipProjectArr.clone()));
                    }
                    return vipProjectSet;
                }
            });

    /**
     * 并发分析的参数配置的缓存
     *
     * @param paramCode
     * @return
     */
    private Map<String, Integer> getCustomConcurrent(String paramCode) {
        List<BaseDataVO> concurrentAnalyzeConfigs = getConcurrentAnalyzeConfigs(paramCode);
        String paramValue = concurrentAnalyzeConfigs.get(0).getParamValue();
        Map<String, Integer> projectConcurrentMap = new HashMap<>();
        if (StringUtils.isNotEmpty(paramValue)) {
            String[] projectConcurrentArr = paramValue.split(",");
            for (String projectConcurrentStr : projectConcurrentArr) {
                String[] projectConcurrent = projectConcurrentStr.split(":");
                if (projectConcurrent.length != 2 || !StringUtils.isNumeric(projectConcurrent[1])) {
                    log.error("illegal param value [{}] for param code [{}]", projectConcurrentStr, paramCode);
                } else {
                    projectConcurrentMap.put(projectConcurrent[0], Integer.valueOf(projectConcurrent[1]));
                }
            }
        }
        return projectConcurrentMap;
    }

    /**
     * 获取刷新缓存的时间间隔
     *
     * @return
     */
//    public int getRefreshDuration() {
//        String refreshDuration = concurrentAnalyzeCache.getUnchecked(REFRESH_DURATION);
//        return StringUtils.isEmpty(refreshDuration) ? 30 : Integer.valueOf(refreshDuration);
//    }

    /**
     * 获取默认最大并发数
     *
     * @return
     */
    public int getDefaultMaxConcurrent() {
        String defaultMaxConcurrent = concurrentAnalyzeCache.getUnchecked(DEFAULT_MAX_CONCURRENT);
        return StringUtils.isEmpty(defaultMaxConcurrent) ? 10 : Integer.valueOf(defaultMaxConcurrent);
    }

    /**
     * 获取项目的自定义最大并发数
     *
     * @param projectId
     * @return
     */
    public Integer getProjectCustomConcurrent(String projectId) {
        Map<String, Integer> customConcurrentMap = customConcurrentCache.getUnchecked(CUSTOM_CONCURRENT);
        return customConcurrentMap.get(projectId);
    }

    /**
     * 获取项目的最大并发数
     * @param projectId
     * @return
     */
    @NotNull
    public Integer getProjectMaxConcurrent(String projectId) {
        Integer projectMaxConcurrent = getProjectCustomConcurrent(projectId);
        if (projectMaxConcurrent == null) {
            projectMaxConcurrent = getDefaultMaxConcurrent();
        }
        return projectMaxConcurrent;
    }

    /**
     * 获取VIP项目
     *
     * @return
     */
    public Set<String> getVipProject() {
        return vipProjectCache.getUnchecked(VIP_PROJECTS);
    }
}
