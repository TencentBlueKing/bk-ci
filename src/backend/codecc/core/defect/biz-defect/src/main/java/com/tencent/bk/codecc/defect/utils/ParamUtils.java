package com.tencent.bk.codecc.defect.utils;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ParamUtils {

    public static List<String> getToolsByDimension(String toolName, String dimension, Long taskId) {
        if (StringUtils.isBlank(toolName) && StringUtils.isBlank(dimension)) {
            return new ArrayList<>();
        }

        ToolMetaCacheService toolMetaCacheService = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);

        final Set<String> toolNameSet;
        if (StringUtils.isNotBlank(toolName) && StringUtils.isNotBlank(dimension)) {
            // 求并集
            toolNameSet = new HashSet<>(toolMetaCacheService.getToolDetailByDimension(dimension));
            toolNameSet.retainAll(Sets.newHashSet(toolName));
        } else if (StringUtils.isNotBlank(toolName)) {
            toolNameSet = Sets.newHashSet(toolName);
        } else if (StringUtils.isNotBlank(dimension)) {
            toolNameSet = new HashSet<>(toolMetaCacheService.getToolDetailByDimension(dimension));

            // filter by task tool
            Client client = SpringContextUtil.Companion.getBean(Client.class);
            List<ToolConfigInfoVO> taskToolConfigList = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData().getToolConfigInfoList();
            List<String> taskToolSet = taskToolConfigList.stream().map(ToolConfigInfoVO::getToolName).collect(Collectors.toList());
            toolNameSet.retainAll(taskToolSet);
        } else {
            toolNameSet = new HashSet<>();
        }

        return new ArrayList<>(toolNameSet);
    }
}
