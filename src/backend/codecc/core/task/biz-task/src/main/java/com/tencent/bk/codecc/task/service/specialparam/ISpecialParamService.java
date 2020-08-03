package com.tencent.bk.codecc.task.service.specialparam;

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import org.json.JSONObject;

/**
 * 处理工具特殊参数接口
 *
 * @date 2019/3/12
 * @version V4.0
 */
public interface ISpecialParamService
{
    /**
     * 修改工具配置影响规则时清空规则集
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj);

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    boolean isSameParam(String toolName, String paramJson1, String paramJson2);

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    boolean checkerMatchParam(String toolName, CheckerDetailVO checkerModel, String paramJson);
}
