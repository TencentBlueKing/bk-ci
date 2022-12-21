/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C)) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software")), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

dependencies {
    api(project(":core:process:plugin-load")) // Model检查
    api(project(":core:common:common-service"))
    api(project(":core:common:common-client")) // 其他微服务调用
    api(project(":core:common:common-archive"))
    api(project(":core:common:common-db-sharding"))
    api(project(":core:common:common-websocket")) // 依赖websocket枚举
    api(project(":core:common:common-expression")) // 依赖表达式解析
    api(project(":core:dispatch:api-dispatch")) // Dispatch配额实现在dispatch，考虑移除
    api(project(":core:project:api-project")) // 依赖读取项目VO
    api(project(":core:process:api-process"))
    api(project(":core:notify:api-notify")) // 消息通知API，考虑移除
    api(project(":core:process:model-process"))
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("com.zaxxer:HikariCP")
    api("org.jooq:jooq")
    api("mysql:mysql-connector-java")
    implementation("com.github.ben-manes.caffeine:caffeine")
    testImplementation(project(":core:common:common-test"))
}
