package com.tencent.bk.codecc.defect.service.specialparam;

import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import org.json.JSONObject;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/14
 */
public class AbstractSpecialParamServiceImpl implements ISpecialParamService
{
    /**
     * 修改工具配置影响规则时清空规则集
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    @Override
    public boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        return false;
    }

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    @Override
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2)
    {
        return true;
    }

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    @Override
    public boolean checkerMatchParam(String toolName, CheckerDetailEntity checkerModel, String paramJson)
    {
        return true;
    }

    /**
     * 向对象中写入特殊参数
     *
     * @param toolName
     * @param paramJson
     * @param dest
     * @param cls
     */
    @Override
    public <T> void setParam2Object(String toolName, String paramJson, T dest, Class<T> cls)
    {

    }

    public boolean isSameParam(JSONObject paramJsonObj1, JSONObject paramJsonObj2, String paramKey)
    {
        if (!paramJsonObj1.has(paramKey) && !paramJsonObj2.has(paramKey))
        {
            return true;
        }

        // 如果同时有并且值相同，则认为是相同的
        if (paramJsonObj1.has(paramKey) && paramJsonObj2.has(paramKey)
                && paramJsonObj1.getString(paramKey).equals(paramJsonObj2.getString(paramKey)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
