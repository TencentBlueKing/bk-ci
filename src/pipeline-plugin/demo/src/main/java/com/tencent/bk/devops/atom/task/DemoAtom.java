package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.DataField;
import com.tencent.bk.devops.atom.pojo.StringData;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @version 1.0
 */
@AtomService(paramClass = AtomParam.class)
public class DemoAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(DemoAtom.class);

    /**
     * 执行主入口
     * @param atomContext 插件上下文
     */
    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        // 1.1 拿到请求参数
        AtomParam param = atomContext.getParam();
        logger.info("the param is :{}", JsonUtil.toJson(param));
        // 1.2 拿到初始化好的返回结果对象
        AtomResult result = atomContext.getResult();
        // 2. 校验参数失败直接返回
        checkParam(param, result);
        if (result.getStatus() != Status.success) {
            return;
        }
        // 3. 模拟处理插件业务逻辑
        logger.info("the desc is :{}", param.getDesc()); //打印描述信息
        // 4. 输出参数，如果有的话
        // 输出参数是一个Map,Key是参数名， value是值对象
        Map<String, DataField> data = result.getData();
        // 假设这个是输出参数的内容
        StringData testResult = new StringData("hello");
        // 设置一个名称为testResult的出参
        data.put("testResult", testResult);
        logger.info("the testResult is :{}", JsonUtil.toJson(testResult));
        // 结束。
    }

    /**
     * 检查参数
     * @param param  请求参数
     * @param result 结果
     */
    private void checkParam(AtomParam param, AtomResult result) {
        // 参数检查
        if (StringUtils.isBlank(param.getDesc())) {
            result.setStatus(Status.failure);// 状态设置为失败
            result.setMessage("描述不能为空!"); // 失败信息回传给插件执行框架会打印出结果
        }

        /*
         其他比如判空等要自己业务检测处理，否则后面执行可能会抛出异常，状态将会是 Status.error
         这种属于插件处理不到位，算是bug行为，需要插件的开发去定位
          */
    }

}
