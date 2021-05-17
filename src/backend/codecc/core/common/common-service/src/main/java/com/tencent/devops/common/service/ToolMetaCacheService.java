package com.tencent.devops.common.service;

import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具元数据接口
 *
 * @version V1.0
 * @date 2019/11/21
 */
public interface ToolMetaCacheService
{
    /**
     * 查询工具类型
     *
     * @param toolName
     * @return
     */
    String getToolPattern(String toolName);

    /**
     * 加载工具缓存
     */
    List<ToolMetaBaseVO> loadToolBaseCache();

    /**
     * 加载工具缓存
     */
    List<ToolMetaDetailVO> loadToolDetailCache();

    /**
     * 获取工具基础信息缓存
     *
     * @param toolName
     * @return
     */
    ToolMetaBaseVO getToolBaseMetaCache(String toolName);

    /**
     * 获取工具显示名称
     *
     * @param toolName
     * @return
     */
    String getToolDisplayName(String toolName);

    /**
     * 从缓存中获取所有工具
     *
     * @param isDetail
     * @return
     */
    Map<String, ToolMetaBaseVO> getToolMetaListFromCache(boolean isDetail, boolean isAdmin);

    /**
     * 根据工具名从缓存中获取工具完整信息
     *
     * @param toolName
     * @return
     */
    ToolMetaDetailVO getToolDetailFromCache(String toolName);


    /**
     * 根据工具维度从缓存中获取工具完整信息
     *
     * @param dimension
     * @return
     */
    List<String> getToolDetailByDimension(String dimension);
}
