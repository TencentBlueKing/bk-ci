package com.tencent.devops.process

import io.mockk.MockKMatcherScope
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.Mock
import org.jooq.tools.jdbc.MockConnection

open class BkAbstractTest {
    val dslContext: DSLContext = DSL.using(MockConnection(Mock.of(1)), SQLDialect.MYSQL)

    fun MockKMatcherScope.anyDslContext(): DSLContext = any() as DSLContext
}
