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

package com.tencent.bk.codecc.task.component;

import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.DynamicEnumUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通过base表动态配置枚举类型工具
 *
 * @date 2020/3/25
 * @version V1.0
 */
@Slf4j
@Component
public class EnumValueByBaseDataComponent
{
    @Autowired
    private BaseDataService baseDataService;

    /**
     * 动态加载枚举类型
     * @param enumName
     * @param enumClass
     * @param <T>
     * @return
     */
    public <T extends Enum<T>> T getEnumValueByName(String enumName, Class<T> enumClass)
    {
        try{
            return Enum.valueOf(enumClass, enumName);
        } catch (Exception e){
            //如果取不到枚举，就从数据库查询配置文件，动态添加
            List<BaseDataVO> baseDataVOList = baseDataService.findBaseDataInfoByTypeAndCode("CUSTOM_ENUM",
                    enumName);
            if(CollectionUtils.isEmpty(baseDataVOList))
            {
                log.error("no enum config info found! enum name: {}", enumName);
                return null;
            }
            Class<?>[] clzArray = new Class[baseDataVOList.size()];
            Object[] objArray = new Object[baseDataVOList.size()];
            int i=0;
            try {
                baseDataVOList = baseDataVOList.stream().sorted(Comparator.comparingInt(o -> Integer.valueOf(o.getParamExtend2()))).
                        collect(Collectors.toList());
                for(BaseDataVO baseDataVO : baseDataVOList)
                {
                    String className = baseDataVO.getParamName();
                    Class<?> clz = Class.forName(className);
                    clzArray[i] = clz;
                    String value = baseDataVO.getParamValue();
                    //如果param_extend1不为空，则表示为bean_name
                    if(StringUtils.isNotBlank(baseDataVO.getParamExtend1()) &&
                            "beanName".equalsIgnoreCase(baseDataVO.getParamExtend1()))
                    {
                        try{
                            objArray[i] = SpringContextUtil.Companion.getBean(clz, value);
                        } catch (Exception e1){
                            objArray[i] = null;
                        }
                    }
                    else
                    {
                        objArray[i] = value;
                    }
                }
                DynamicEnumUtils.addEnum(enumClass, enumName, clzArray,
                        objArray);
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
                log.error("no class found by name: {}", enumName, e1);
                return null;
            } catch (Exception e2) {
                e2.printStackTrace();
                log.error("dynamic fetch enum value fail! enum name: {}", enumName, e2);
                return null;
            }
            return Enum.valueOf(enumClass, enumName);
        }
    }
}
