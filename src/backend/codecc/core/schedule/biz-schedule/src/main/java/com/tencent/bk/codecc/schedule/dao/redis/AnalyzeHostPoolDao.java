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

package com.tencent.bk.codecc.schedule.dao.redis;

import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.redis.lock.JRedisLock;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 分析主机线程池池
 *
 * @version V1.0
 * @date 2019/10/23
 */
@Repository
@Slf4j
public class AnalyzeHostPoolDao
{
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int EXPIRY_TIME_MILLIS = Integer.getInteger("lock.expiry.millis", 100);

    /**
     * 停止分析标志
     *
     * @return
     */
    public String getStopDispatchFlag()
    {
        String flag = redisTemplate.opsForValue().get(RedisKeyConstants.DISPATCH_FLAG);
        return flag;
    }

    /**
     * 挑选出当前空闲线程数最多的机器
     *
     * @param pushVO
     */
    public AnalyzeHostPoolModel getMostIdleHost(PushVO pushVO)
    {
        String toolName = pushVO.getToolName();
        String streamName = pushVO.getStreamName();
        String buildId = pushVO.getBuildId();
        String createFrom = pushVO.getCreateFrom();

        String lockKey = RedisKeyConstants.LOCK_HOST_POOL;
        if (StringUtils.isNotEmpty(createFrom))
        {
            lockKey = RedisKeyConstants.LOCK_HOST_POOL_OPENSOURCE;
        }
        JRedisLock lock = new JRedisLock(redisTemplate, lockKey, JRedisLock.DEFAULT_ACQUIRE_TIMEOUT_MILLIS, EXPIRY_TIME_MILLIS);

        if (!lock.acquire())
        {
            return null;
        }

        AnalyzeHostPoolModel mostIdleHost = null;
        try
        {
            mostIdleHost = getAndUpdateAnalyzeHost(toolName, streamName, buildId, createFrom);
        }
        catch (Exception e)
        {
            log.error("get most idle host failed, try again later!", e);
        }
        finally
        {
            lock.release();
        }
        return mostIdleHost;
    }

    @Nullable
    private AnalyzeHostPoolModel getAndUpdateAnalyzeHost(String toolName, String streamName, String buildId, String createFrom)
    {
        AnalyzeHostPoolModel mostIdleHost = null;
        int maxIdle = 0;
        Map<Object, Object> analyzeHostMap = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_ANALYZE_HOST);
        for (Map.Entry<Object, Object> entry : analyzeHostMap.entrySet())
        {
            String analyzeHostStr = (String) entry.getValue();
            AnalyzeHostPoolModel analyzeHostPoolModel = JsonUtil.INSTANCE.to(analyzeHostStr, AnalyzeHostPoolModel.class);
            Set<String> supportTools = analyzeHostPoolModel.getSupportTools();
            Set<String> supportTaskTypes = analyzeHostPoolModel.getSupportTaskTypes();

            /*
             *判断机器是否支持该来源的任务分析，以下情况支持:
             * createFrom为空（默认就是来源于bs_codecc或者bs_pipeline），且机器的supportTaskTypes包含bs_codecc或者bs_pipeline
             * createFrom不为空，且机器的supportTaskTypes包含该来源
             */
            boolean supportTaskType = false;
            if (StringUtils.isEmpty(createFrom))
            {
                if (supportTaskTypes.contains(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value())
                        || supportTaskTypes.contains(ComConstants.BsTaskCreateFrom.BS_CODECC.value()))
                {
                    supportTaskType = true;
                }
            }
            else if (supportTaskTypes.contains(createFrom))
            {
                supportTaskType = true;
            }

            int idle = analyzeHostPoolModel.getIdle();
            if (supportTaskType && supportTools.contains(toolName) && idle > 0)
            {
                if (idle > maxIdle)
                {
                    maxIdle = idle;
                    mostIdleHost = analyzeHostPoolModel;
                }
                // 如果相等，则取当前空闲率（idle/pool）最高的机器
                else if (idle == maxIdle)
                {
                    if (mostIdleHost == null)
                    {
                        mostIdleHost = analyzeHostPoolModel;
                    }
                    else
                    {
                        int currHostPool = analyzeHostPoolModel.getPool();
                        int mostIdleHostPool = mostIdleHost.getPool();
                        double currHostIdleRate = (double) idle / (double) currHostPool;
                        double mostIdleHostIdleRate = (double) maxIdle / (double) mostIdleHostPool;
                        if (currHostIdleRate > mostIdleHostIdleRate)
                        {
                            mostIdleHost = analyzeHostPoolModel;
                        }
                    }
                }
            }
        }

        if (mostIdleHost != null)
        {
            mostIdleHost.setIdle(maxIdle - 1);
            List<AnalyzeHostPoolModel.AnalyzeJob> jobList = mostIdleHost.getJobList();
            if (jobList == null)
            {
                jobList = new ArrayList<>();
                mostIdleHost.setJobList(jobList);
            }
            jobList.add(new AnalyzeHostPoolModel.AnalyzeJob(streamName, toolName, buildId));
            redisTemplate.opsForHash().put(RedisKeyConstants.KEY_ANALYZE_HOST, mostIdleHost.getIp(), JsonUtil.INSTANCE.toJson(mostIdleHost));
        }
        return mostIdleHost;
    }

    /**
     * 释放主机的线程
     *
     * @param toolName
     * @param streamName
     * @param hostIp
     */
    public boolean freeHostThread(String toolName, String streamName, String hostIp, String buildId)
    {
        log.info("begin freeHostThread: {}, {}, {}, {}", toolName, streamName, hostIp, buildId);
        JRedisLock lock = new JRedisLock(redisTemplate, RedisKeyConstants.LOCK_HOST_POOL, JRedisLock.DEFAULT_ACQUIRE_TIMEOUT_MILLIS, EXPIRY_TIME_MILLIS);

        if (!lock.acquire())
        {
            return false;
        }

        try
        {
            Object analyzeHostObj = redisTemplate.opsForHash().get(RedisKeyConstants.KEY_ANALYZE_HOST, hostIp);
            if (analyzeHostObj != null)
            {
                AnalyzeHostPoolModel analyzeHostPoolModel = JsonUtil.INSTANCE.to((String) analyzeHostObj, AnalyzeHostPoolModel.class);

                List<AnalyzeHostPoolModel.AnalyzeJob> jobList = analyzeHostPoolModel.getJobList();
                if (CollectionUtils.isNotEmpty(jobList))
                {
                    Iterator<AnalyzeHostPoolModel.AnalyzeJob> it = jobList.iterator();
                    boolean removeSuccess = false;
                    while (it.hasNext())
                    {
                        AnalyzeHostPoolModel.AnalyzeJob analyzeJob = it.next();
                        if (analyzeJob.getStreamName().equals(streamName) && analyzeJob.getToolName().equals(toolName) &&
                                analyzeJob.getBuildId().equals(buildId))
                        {
                            it.remove();
                            removeSuccess = true;
                            break;
                        }
                    }
                    if (removeSuccess)
                    {
                        analyzeHostPoolModel.setIdle(analyzeHostPoolModel.getIdle() + 1);
                        redisTemplate.opsForHash().put(RedisKeyConstants.KEY_ANALYZE_HOST, hostIp, JsonUtil.INSTANCE.toJson(analyzeHostPoolModel));
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("free host thread failed!", e);
        }
        finally
        {
            lock.release();
        }
        log.info("end freeHostThread: {}, {}, {}", toolName, streamName, hostIp);
        return true;
    }

    /**
     * 获取所有的分析机器
     *
     * @return
     */
    public List<AnalyzeHostPoolModel> getAllAnalyzeHosts()
    {
        List<AnalyzeHostPoolModel> hostList = new ArrayList<>();
        Map<Object, Object> analyzeHostMap = redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_ANALYZE_HOST);
        for (Map.Entry<Object, Object> entry : analyzeHostMap.entrySet())
        {
            String analyzeHostStr = (String) entry.getValue();
            AnalyzeHostPoolModel analyzeHostPoolModel = JsonUtil.INSTANCE.to(analyzeHostStr, AnalyzeHostPoolModel.class);
            hostList.add(analyzeHostPoolModel);
        }
        return hostList;
    }

    /**
     * 批量释放主机的线程
     *
     * @param needFreeHostMap
     * @return
     */
    public boolean batchFreeHostThread(Map<String, List<AnalyzeHostPoolModel.AnalyzeJob>> needFreeHostMap)
    {
        log.info("begin batchFreeHostThread");
        JRedisLock lock = new JRedisLock(redisTemplate, RedisKeyConstants.LOCK_HOST_POOL, JRedisLock.DEFAULT_ACQUIRE_TIMEOUT_MILLIS, EXPIRY_TIME_MILLIS);
        if (!lock.acquire())
        {
            return false;
        }

        try
        {
            Set<Object> keys = new HashSet<>(needFreeHostMap.keySet());
            List<Object> res = redisTemplate.opsForHash().multiGet(RedisKeyConstants.KEY_ANALYZE_HOST, keys);

            Map<String, String> updateHostMap = new HashMap<>();
            res.forEach(analyzeHostObj ->
            {
                if (analyzeHostObj != null)
                {
                    AnalyzeHostPoolModel analyzeHostPoolModel = JsonUtil.INSTANCE.to((String) analyzeHostObj, AnalyzeHostPoolModel.class);
                    List<AnalyzeHostPoolModel.AnalyzeJob> jobList = analyzeHostPoolModel.getJobList();
                    if (CollectionUtils.isNotEmpty(jobList))
                    {
                        // 释放的线程数
                        int freeThreadCount = 0;
                        Iterator<AnalyzeHostPoolModel.AnalyzeJob> it = jobList.iterator();
                        String hostIp = analyzeHostPoolModel.getIp();
                        while (it.hasNext())
                        {
                            AnalyzeHostPoolModel.AnalyzeJob analyzeJob = it.next();
                            List<AnalyzeHostPoolModel.AnalyzeJob> needFreeThreadList = needFreeHostMap.get(hostIp);
                            for (AnalyzeHostPoolModel.AnalyzeJob needFreeJob : needFreeThreadList)
                            {
                                if (analyzeJob.getStreamName().equals(needFreeJob.getStreamName())
                                        && analyzeJob.getToolName().equals(needFreeJob.getToolName())
                                        && analyzeJob.getBuildId().equals(needFreeJob.getBuildId()))
                                {
                                    it.remove();
                                    freeThreadCount++;
                                    break;
                                }
                            }
                        }
                        if (freeThreadCount > 0)
                        {
                            analyzeHostPoolModel.setIdle(analyzeHostPoolModel.getIdle() + freeThreadCount);
                            updateHostMap.put(hostIp, JsonUtil.INSTANCE.toJson(analyzeHostPoolModel));
                        }
                    }
                }
            });
            redisTemplate.opsForHash().putAll(RedisKeyConstants.KEY_ANALYZE_HOST, updateHostMap);
        }
        catch (Exception e)
        {
            log.error("free host thread failed!", e);
        }
        finally
        {
            lock.release();
        }
        log.info("end batchFreeHostThread");
        return true;
    }
}
