package com.tencent.bk.codecc.task.service.specialparam;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.devops.common.service.BizServiceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 特殊参数工具类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Service
public class SpecialParamUtil
{
    private static final String BUSINESS_TYPE = "SpecialParam";

    @Autowired
    private BizServiceFactory<ISpecialParamService> specialParamServiceBizServiceFactory;

    @Autowired
    private ToolService toolService;

    /**
     * 修改工具配置影响规则时清空规则集
     *
     * @param taskId
     * @param toolNames
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    public void clearCheckerSetWhenModifyJsonParam(long taskId, List<String> toolNames, JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        if (CollectionUtils.isNotEmpty(toolNames))
        {
            Set<String> modifiedTools = Sets.newHashSet();
            for (String toolName : toolNames)
            {
                ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
                if (processor.paramJsonModified(reqParamJsonObj, currentParamJsonObj))
                {
                    modifiedTools.add(toolName);
                }
            }
            toolService.clearCheckerSet(taskId, Lists.newArrayList(modifiedTools));
        }
    }

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2)
    {
        if (StringUtils.isEmpty(paramJson1))
        {
            paramJson1 = new JSONObject().toString();
        }
        if (StringUtils.isEmpty(paramJson2))
        {
            paramJson2 = new JSONObject().toString();
        }
        ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
        return processor.isSameParam(toolName, paramJson1, paramJson2);
    }

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    public boolean checkerMatchParam(String toolName, CheckerDetailVO checkerModel, String paramJson)
    {
        if (StringUtils.isEmpty(paramJson))
        {
            paramJson = new JSONObject().toString();
        }
        ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
        return processor.checkerMatchParam(toolName, checkerModel, paramJson);
    }
}
