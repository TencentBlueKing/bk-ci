package com.tencent.bk.devops.atom.spi;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.pojo.AtomBaseParam;

/**
 * 插件接口
 * @version 1.0
 */
public interface TaskAtom<T extends AtomBaseParam> {

    /**
     * 执行插件逻辑
     * @param atomContext 插件上下文
     */
    void execute(AtomContext<T> atomContext);
}
