package com.tencent.devops.dockerhost.docker.annotation

/**
 * Docker环境变量生成器注解，标示生成器
 */
annotation class EnvGenerator(
    /**
     * 生成器说明
     */
    val description: String
)