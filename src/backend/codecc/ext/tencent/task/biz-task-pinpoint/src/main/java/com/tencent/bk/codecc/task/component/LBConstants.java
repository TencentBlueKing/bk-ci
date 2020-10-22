package com.tencent.bk.codecc.task.component;

/**
 * 负载均衡相关的常量
 *
 * @version V1.0
 * @date 2019/10/1
 */
public interface LBConstants
{
    /**
     * 负载均衡算法，随机-0，根据机器性能均衡-1
     */
    enum LB_ALGOL
    {
        RANDOM("0"),
        PERFORMANCE_BALANCE("1");

        private String value;

        LB_ALGOL(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }

    /**
     * platform类型
     */
    enum PlatformType
    {
        // codecc通用
        CC_COMMON(1),

        // 蓝盾通用
        LD_COMMON(2),

        // openapi通用
        OPEN_COMMON(4);

        private int value;

        PlatformType(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }
}
