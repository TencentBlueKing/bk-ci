package com.tencent.devops.environment.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.environment.dao.DockerConfigDao
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.AgentResourceSnapshotDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.AgentResourceSnapshot
import com.tencent.devops.environment.pojo.BatchDockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigScope
import com.tencent.devops.environment.service.thirdpartyagent.AgentResourceParser
import com.tencent.devops.environment.service.thirdpartyagent.AgentResourceSnapshotService
import com.tencent.devops.model.environment.tables.TAgentResourceSnapshot.T_AGENT_RESOURCE_SNAPSHOT
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT
import io.mockk.every
import io.mockk.mockk
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

/** Opt-in localhost test: clones schema only, never modifies the source database's rows. */
@EnabledIfEnvironmentVariable(named = "BK_CI_FEATURE_TEST_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentFeaturesMysqlTest {
    private val database = "devops_ci_feature_test_" + UUID.randomUUID().toString().replace("-", "")
    private val tables = listOf(
        "T_ENVIRONMENT_THIRDPARTY_AGENT", "T_AGENT_RESOURCE_SNAPSHOT", "T_ENV", "T_NODE",
        "T_ENV_DOCKER_CONFIG", "T_NODE_DOCKER_CONFIG"
    )
    private lateinit var connection: Connection
    private lateinit var context: DSLContext
    private val snapshotDao = AgentResourceSnapshotDao()
    private val agentDao = ThirdPartyAgentDao()
    private val configDao = DockerConfigDao()
    private val project = "feature-test"

    private fun connect(): Connection = DriverManager.getConnection(
        System.getenv("BK_CI_FEATURE_TEST_URL"),
        System.getenv("BK_CI_FEATURE_TEST_USER"),
        System.getenv("BK_CI_FEATURE_TEST_PASSWORD")
    )

    private fun dsl(c: Connection) = DSL.using(
        c, SQLDialect.MYSQL, Settings().withRenderSchema(false).withExecuteLogging(false)
    )

    @BeforeAll
    fun createIsolatedDatabase() {
        val url = System.getenv("BK_CI_FEATURE_TEST_URL")
        require(url.matches(Regex("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/[A-Za-z0-9_]+.*")))
        connection = connect()
        val source = connection.catalog
        require(source.matches(Regex("[A-Za-z0-9_]+")))
        connection.createStatement().use { statement ->
            statement.execute("CREATE DATABASE `$database`")
            tables.forEach { table ->
                statement.execute("CREATE TABLE `$database`.`$table` LIKE `$source`.`$table`")
            }
        }
        connection.catalog = database
        context = dsl(connection)
    }

    @AfterAll
    fun removeIsolatedDatabase() {
        if (!::connection.isInitialized) return
        require(database.matches(Regex("devops_ci_feature_test_[a-f0-9]{32}")))
        connection.createStatement().use { it.execute("DROP DATABASE `$database`") }
        connection.close()
    }

    @BeforeEach
    fun resetFixtures() {
        tables.forEach { context.execute("DELETE FROM `$it`") }
    }

    private fun agent(id: Long) {
        context.execute(
            """INSERT INTO T_ENVIRONMENT_THIRDPARTY_AGENT
                (ID,PROJECT_ID,OS,STATUS,SECRET_KEY,CREATED_USER,CREATED_TIME,AGENT_INSTALL_PATH)
                VALUES (?,?,'LINUX',?,'test-only','test',NOW(),'/data/agent')""".trimIndent(),
            id, project, AgentStatus.IMPORT_OK.status
        )
    }

    private fun node(id: Long, os: String = "LINUX", type: String = "THIRDPARTY") {
        context.execute(
            """INSERT INTO T_NODE
                (NODE_ID,PROJECT_ID,NODE_IP,NODE_NAME,NODE_STATUS,NODE_TYPE,CREATED_USER,OS_NAME)
                VALUES (?,?,'127.0.0.1','test','NORMAL',?,'test',?)""".trimIndent(), id, project, type, os
        )
    }

    private fun service(tx: DSLContext = context, deny: Long? = null): DockerConfigService {
        val permissions = mockk<EnvironmentPermissionService>()
        every { permissions.checkNodePermission(any(), any(), any(), any(), any()) } answers {
            thirdArg<Long>() != deny
        }
        every { permissions.checkEnvPermission(any(), any(), any<Long>(), any()) } returns true
        return DockerConfigService(tx, configDao, NodeDao(), EnvDao(), permissions)
    }

    private fun saveSnapshot(id: Long, value: AgentResourceSnapshot) {
        context.transaction { configuration ->
            val tx = DSL.using(configuration)
            with(T_ENVIRONMENT_THIRDPARTY_AGENT) {
                tx.select(ID).from(this).where(ID.eq(id)).forUpdate().fetchOne()
            }
            snapshotDao.save(tx, project, id, value)
        }
    }

    @Test
    fun `out of order partial reports invalidation recovery restart and physical deletion`() {
        agent(1)
        val now = System.currentTimeMillis()
        saveSnapshot(1, AgentResourceSnapshot(40.0, now - 1000, 50.0, now - 1000))
        saveSnapshot(1, AgentResourceSnapshot(10.0, now - 2000, disk = 20.0, diskTime = now))
        assertEquals(40.0, snapshotDao.list(context, listOf(1))[1]?.cpu)
        assertEquals(20.0, snapshotDao.list(context, listOf(1))[1]?.disk)
        agentDao.updateStatus(context, 1, null, project, AgentStatus.IMPORT_EXCEPTION)
        assertNull(snapshotDao.list(context, listOf(1))[1]?.cpu)
        agentDao.updateStatus(context, 1, null, project, AgentStatus.IMPORT_OK)
        saveSnapshot(1, AgentResourceSnapshot(1.0, now - 1000))
        assertNull(snapshotDao.list(context, listOf(1))[1]?.cpu)
        val after = System.currentTimeMillis() + 1
        saveSnapshot(1, AgentResourceSnapshot(80.0, after))
        assertEquals(80.0, snapshotDao.list(context, listOf(1))[1]?.cpu)
        agentDao.saveAgent(context, agentDao.getAgent(context, 1)!!, invalidateSnapshot = true)
        assertNull(snapshotDao.list(context, listOf(1))[1]?.cpu)
        agentDao.delete(context, 1, project)
        assertTrue(snapshotDao.list(context, listOf(1)).isEmpty())
    }

    @Test
    fun `snapshot cleanup is bounded and sparse defaults leave no row`() {
        val now = System.currentTimeMillis()
        with(T_AGENT_RESOURCE_SNAPSHOT) {
            context.batch((1L..1005L).map { id ->
                context.insertInto(this).set(AGENT_ID, id).set(PROJECT_ID, project)
                    .set(VALID_AFTER, 0L).set(UPDATED_TIME, now - 181000)
            }).execute()
        }
        AgentResourceSnapshotService(context, snapshotDao).cleanExpired()
        assertEquals(5, context.fetchCount(T_AGENT_RESOURCE_SNAPSHOT))
        configDao.save(context, DockerConfigScope.NODE, project, 1, AgentDockerConfig(), "test")
        assertTrue(configDao.list(context, DockerConfigScope.NODE, project, listOf(1)).isEmpty())
        configDao.save(context, DockerConfigScope.NODE, project, 1, AgentDockerConfig("2", "6m"), "test")
        configDao.save(context, DockerConfigScope.NODE, project, 1, AgentDockerConfig(), "test")
        assertTrue(configDao.list(context, DockerConfigScope.NODE, project, listOf(1)).isEmpty())
    }

    @Test
    fun `batch permissions are atomic and mixed OS uses independent tri state fields`() {
        node(1)
        node(2, "WINDOWS")
        node(3, type = "CREATE")
        configDao.save(context, DockerConfigScope.NODE, project, 1, AgentDockerConfig("2", "512m", "podman"), "t")
        val ids = setOf(1L, 2L, 3L).map(HashUtil::encodeLongId).toSet()
        val request = BatchDockerConfigRequest(ids, DockerConfigRequest(memory = ""))
        assertThrows(Exception::class.java) { service(deny = 2).batch("u", project, request) }
        assertEquals("512m", service().forAgents(project, listOf(1))[1]?.memory)
        assertEquals(1, service().batch("u", project, request))
        assertEquals(AgentDockerConfig("2", null, "podman"), service().forAgents(project, listOf(1))[1])
        assertTrue(configDao.list(context, DockerConfigScope.NODE, project, listOf(2, 3)).isEmpty())
        assertThrows(Exception::class.java) {
            service().save("u", project, DockerConfigScope.NODE, 3, DockerConfigRequest(cpus = "1"))
        }
        service().save("u", project, DockerConfigScope.NODE, 3, DockerConfigRequest(containerRuntime = "podman"))
        assertEquals("podman", service().forAgents(project, listOf(3))[3]?.containerRuntime)
        context.execute("UPDATE T_NODE SET NODE_STATUS = 'DELETED' WHERE NODE_ID = 1")
        service().cleanDeletedResources()
        assertTrue(configDao.list(context, DockerConfigScope.NODE, project, listOf(1)).isEmpty())
        assertEquals("podman", service().forAgents(project, listOf(3))[3]?.containerRuntime)
    }

    @Test
    fun `concurrent first time partial config saves do not lose fields`() {
        node(1)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val jobs = listOf(DockerConfigRequest(cpus = "4"), DockerConfigRequest(memory = "2g")).map { request ->
                pool.submit {
                    connect().use { c ->
                        c.catalog = database
                        start.await(5, TimeUnit.SECONDS)
                        service(dsl(c)).save("u", project, DockerConfigScope.NODE, 1, request)
                    }
                }
            }
            start.countDown()
            jobs.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
        assertEquals(AgentDockerConfig("4", "2g"), service().forAgents(project, listOf(1))[1])
    }

    @Test
    fun `late report waiting for lifecycle lock cannot resurrect invalid metrics`() {
        agent(1)
        val old = System.currentTimeMillis() - 1000
        val pool = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        lateinit var late: java.util.concurrent.Future<*>
        try {
            context.transaction { configuration ->
                val tx = DSL.using(configuration)
                with(T_ENVIRONMENT_THIRDPARTY_AGENT) {
                    tx.select(ID).from(this).where(ID.eq(1)).forUpdate().fetchOne()
                }
                late = pool.submit {
                    connect().use { c ->
                        c.catalog = database
                        dsl(c).transaction { config ->
                            val report = DSL.using(config)
                            entered.countDown()
                            with(T_ENVIRONMENT_THIRDPARTY_AGENT) {
                                report.select(ID).from(this).where(ID.eq(1)).forUpdate().fetchOne()
                            }
                            snapshotDao.save(report, project, 1, AgentResourceSnapshot(1.0, old))
                        }
                    }
                }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                AgentResourceSnapshotDao.invalidate(tx, project, listOf(1))
                assertFalse(late.isDone)
            }
            late.get(10, TimeUnit.SECONDS)
            pool.shutdown()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
            assertNull(snapshotDao.list(context, listOf(1))[1]?.cpu)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `local snapshot write and batch read microbenchmark`() {
        val size = 1000L
        (1L..size).forEach { agent(it) }
        val time = System.currentTimeMillis()
        (1L..size).forEach { saveSnapshot(it, AgentResourceSnapshot(50.0, time, 60.0, time, 70.0, time)) }
        fun benchmark(name: String, repeats: Int, work: (Int) -> Unit) {
            repeat(5) { work(it) }
            val times = (0 until repeats).map { index -> measureNanoTime { work(index) } / 1_000_000.0 }.sorted()
            println(
                "FEATURE_BENCH $name repeats=$repeats p50_ms=${times[repeats / 2]} " +
                    "p95_ms=${times[repeats * 95 / 100]}"
            )
        }
        benchmark("parse_lock_merge_upsert", 100) { index ->
            val seconds = System.currentTimeMillis() / 1000
            val value = AgentResourceParser.parse(
                """{"name":"mem","timestamp":$seconds,"fields":{"available":60,"total":100}}""",
                "/data/agent"
            ).copy(memoryTime = time + index + 1)
            saveSnapshot(index.toLong() + 1, value)
        }
        for (count in listOf(50L, 500L, 1000L)) {
            benchmark("snapshot_batch_$count", 50) {
                assertEquals(count.toInt(), snapshotDao.list(context, (1L..count).toList()).size)
            }
        }
        val explain = context.fetch(
            "EXPLAIN SELECT * FROM T_AGENT_RESOURCE_SNAPSHOT WHERE AGENT_ID IN (1,2,3) AND UPDATED_TIME >= ?",
            time - 180000
        )
        println("FEATURE_BENCH explain=$explain")
    }
}
