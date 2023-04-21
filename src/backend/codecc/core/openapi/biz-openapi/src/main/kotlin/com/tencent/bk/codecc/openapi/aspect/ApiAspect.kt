package com.tencent.bk.codecc.openapi.aspect

import com.tencent.bk.codecc.openapi.config.ApiGatewayAuthProperties
import com.tencent.bk.codecc.openapi.exception.PermissionForbiddenException
import com.tencent.bk.codecc.openapi.service.AppCodeService
import com.tencent.bk.codecc.openapi.filter.ApiFilter
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before

import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

@Aspect
@Component
class ApiAspect(
    private val appCodeService: AppCodeService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
    }

    /**
     * 前置增强：目标方法执行之前执行
     *
     * @param jp
     */
    @Before(
        "execution(* com.tencent.bk.codecc.openapi.resources.*.*(..))"
    ) // 所有controller包下面的所有方法的所有参数
    fun beforeMethod(jp: JoinPoint) {
        val enabled = ApiGatewayAuthProperties.properties?.enabled ?: ""
        if(!StringUtils.hasLength(enabled) || enabled == "false"){
            return
        }
        val methodName: String = jp.signature.name
        logger.info("【前置增强】the method 【$methodName】")
        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        var projectId: String? = null
        var appCode: String? = null
        var apigwType: String? = null
        var taskId : String? = null
        parameterNames.forEach {
            logger.info("参数名[$it]")
        }

        parameterValue.forEach {
            logger.info("参数值[$it]")
        }
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "taskId" -> {
                    taskId = parameterValue[index]?.toString()
                }
                "projectId" -> {
                    projectId = parameterValue[index]?.toString()
                }
                "appCode" -> {
                    appCode = parameterValue[index]?.toString()
                }
                "apigw" -> {
                    apigwType = parameterValue[index]?.toString()
                }
                else -> null
            }
        }
        logger.info("请求类型apigwType[$apigwType],appCode[$appCode],项目[$projectId]")
        if (projectId != null && appCode != null && taskId != null && (apigwType == "apigw-app")) {
            logger.info("判断！！！！请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限.")
            if (appCodeService.validAppCode(appCode, projectId, taskId)) {
                logger.info("请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限【验证通过】")
            } else {
                val message = "请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限【验证失败】"
                throw PermissionForbiddenException(
                    message = message
                )
            }
        }
    }
//
//    /**
//     * 后置增强：目标方法执行之后执行以下方法体的内容，不管是否发生异常。
//     *
//     * @param jp
//     */
//    @After("execution(* com.tencent.devops.openapi.resources.*.*(..))")
//    fun afterMethod(jp: JoinPoint?) {
//        logger.info("【后置增强】this is a afterMethod advice...")
//    }
//
//    /**
//     * 返回增强：目标方法正常执行完毕时执行
//     *
//     * @param jp
//     * @param result
//     */
//    @AfterReturning(value = "execution(* com.tencent.devops.openapi.resources.*.*(..)))", returning = "result")
//    fun afterReturningMethod(jp: JoinPoint, result: Any) {
//        val methodName: String = jp.signature.name
//        logger.info("【返回增强】the method 【$methodName】 ends with 【$result】")
//    }
//
//    /**
//     * 异常增强：目标方法发生异常的时候执行，第二个参数表示补货异常的类型
//     *
//     * @param jp
//     * @param e
//     */
//    @AfterThrowing(value = "execution(* com.tencent.devops.openapi.resources.*.*(..))", throwing = "e")
//    fun afterThorwingMethod(jp: JoinPoint, e: Exception?) {
//        val methodName: String = jp.signature.name
//        logger.error("【异常增强】the method 【$methodName】 occurs exception: ", e)
//    }
//    /**
//     * 环绕增强：目标方法执行前后分别执行一些代码，发生异常的时候执行另外一些代码
//     *
//     * @return
//     */
/*    @Around(value = "execution(* com.wuychn.springbootaspect.controller.*.*(..))")
    public Object aroundMethod(ProceedingJoinPoint jp) {
        String methodName = jp.getSignature().getName();
        Object result = null;
        try {
            logger.info("【环绕增强中的--->前置增强】：the method 【" + methodName + "】 begins with " + Arrays.asList(jp.getArgs()));
            //执行目标方法
            result = jp.proceed();
            logger.info("【环绕增强中的--->返回增强】：the method 【" + methodName + "】 ends with " + result);
        } catch (Throwable e) {
            result = "error";
            logger.info("【环绕增强中的--->异常增强】：the method 【" + methodName + "】 occurs exception " + e);
        }
        logger.info("【环绕增强中的--->后置增强】：-----------------end.----------------------");
        return result;
    }*/
}