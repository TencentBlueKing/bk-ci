package com.tencent.devops.common.service;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

@Slf4j
public class BizComponentFactory<T> {


    /**
     * 为不同类型的业务创建相应的处理器
     *
     * @param businessType
     * @param clz
     * @return
     */
    public T createComponent(String createFrom,String businessType, Class<T> clz) {

        T processor = null;

        String createFromCommonBeanName = String.format("%s%s%s", ComConstants.COMMON_CREATE_FROM_BIZ_INFIX, businessType, ComConstants.Component_POSTFIX);
        String createFromGongFengBeanName = String.format("%s%s%s", ComConstants.GONGFENG_CREATE_FROM_BIZ_INFIX, businessType, ComConstants.Component_POSTFIX);

        // 获取创建来源是GongFeng名称开头的component
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)) {
            processor = getProcessor(clz, createFromGongFengBeanName);
        }

        //如果不是来源工蜂，或者没有成功获取到GongFeng名称开头的，则都采用共同的处理器
        if (processor == null){
            processor = getProcessor(clz, createFromCommonBeanName);
        }

        return processor;
    }

    private <T> T getProcessor(Class<T> clz, String beanName) {
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            //log.error("Bean Name [{}] Not Found:", beanName);
        }
        return processor;
    }
}
