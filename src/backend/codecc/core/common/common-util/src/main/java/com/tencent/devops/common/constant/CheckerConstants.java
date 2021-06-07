package com.tencent.devops.common.constant;

/**
 * 规则常量
 *
 * @version V4.0
 * @date 2019/7/15
 */
public interface CheckerConstants
{
    /**
     * 规则集是否官方
     */
    enum CheckerSetOfficial
    {
        /**
         * 官方
         */
        OFFICIAL(1),

        /**
         * 非官方
         */
        NOT_OFFICIAL(2);

        /**
         * 编码
         */
        private int code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerSetOfficial(int code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public int code()
        {
            return code;
        }
    }

    /**
     * 规则集是否推荐
     */
    enum CheckerSetRecommended
    {
        /**
         * 推荐
         */
        RECOMMENDED(1),

        /**
         * 不推荐
         */
        NOT_RECOMMENDED(2);

        /**
         * 编码
         */
        private Integer code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerSetRecommended(Integer code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public Integer code()
        {
            return code;
        }
    }

    /**
     * 规则集可见范围
     */
    enum CheckerSetScope
    {
        /**
         * 公开
         */
        PUBLIC(1),

        /**
         * 本人项目可见
         */
        PRIVATE(2);

        /**
         * 编码
         */
        private int code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerSetScope(int code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public int code()
        {
            return code;
        }
    }

    /**
     * 规则集可见范围
     */
    enum CheckerIsNative
    {
        /**
         * 原生规则
         */
        NATIVE(true),

        /**
         * 定制规则
         */
        NOT_NATIVE(false);

        /**
         * 编码
         */
        private Boolean code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerIsNative(Boolean code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public Boolean code()
        {
            return code;
        }
    }

    /**
     * 规则集管理操作类型
     */
    enum CheckerSetManagementOperType
    {
        SET_WITHDRAW(1),
        CANCEL_WITHDRAW(2),
        SET_RECOMMENDED(3),
        CANCEL_RECOMMENDED(4),
        SET_OFFICIAL(5),
        CANCEL_OFFICIAL(6),
        SET_SORT_WEIGHT(7);

        /**
         * 编码
         */
        private int code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerSetManagementOperType(int code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public int code()
        {
            return code;
        }
    }

    /**
     * 规则集是否官方
     */
    enum CheckerSetEnable
    {
        /**
         * 启用
         */
        ENABLE(1),

        /**
         * 下架
         */
        DISABLE(2);

        /**
         * 编码
         */
        private int code;

        /**
         * 构造方法
         *
         * @param code
         */
        CheckerSetEnable(int code)
        {
            this.code = code;
        }

        /**
         * 查询code
         *
         * @return
         */
        public int code()
        {
            return code;
        }
    }

    /**
     * 规则属性，基础-0；进阶-1；
     */
    enum CheckerProperty
    {
        BASIC(0),
        ADVANCED(1);

        private int value;

        CheckerProperty(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }

    /**
     * 规则是否打开，0=>打开 1=>关闭；
     */
    enum CheckerOpenStatus
    {
        OPEN(0),
        CLOSE(1);

        private int value;

        CheckerOpenStatus(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }

    /**
     * 规则集排序字段
     */
    enum CheckerSetSortField
    {
        TASK_USAGE("taskUsage"),
        CREATE_TIME("createTime");

        private String value;

        CheckerSetSortField(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }

    /**
     * 规则集关系
     */
    enum CheckerSetRelationshipType
    {
        PROJECT,
        TASK;
    }

    /**
     * 默认版本号
     */
    int DEFAULT_VERSION = 1;
}
