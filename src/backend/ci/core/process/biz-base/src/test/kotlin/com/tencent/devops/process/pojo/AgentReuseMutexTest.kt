package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.container.AgentReuseMutexType
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ReusedInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_NULL_NODE
import com.tencent.devops.process.engine.pojo.AgentReuseMutexTree
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AgentReuseMutexTest {
    @Test
    fun addNodeTest() {
        val stages = mutableListOf<Map<String, ThirdPartyAgentDispatch>>()
        stages.add(
            mapOf(
                "job_1" to ThirdPartyAgentIDDispatchType("agent_1", null, AgentType.ID, null, null),
                "job_id_dep_1" to initReuseId("job_1"),
                "job_env_dep_1" to initReuseEnv("job_env_1"),
                "job_env_1" to ThirdPartyAgentEnvDispatchType("job_1", null, null, AgentType.NAME, null, null),
                "job_env_dep_2" to initReuseEnv("job_env_dep_1"),
                "job_id_dep_8" to initReuseEnv("\${{variables.TEST_JOB_ID}}")
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_2" to initReuseId("job_id_dep_1"),
                "job_id_dep_3" to initReuseId("job_env_dep_1"),
                "job_id_dep_4" to initReuseId("job_id_dep_2")
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_5" to initReuseId("job_env_dep_2")
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_6" to initReuseId("job_env_1"),
                "job_id_dep_7" to initReuseEnv("job_env_1")
            )
        )
        val tree = AgentReuseMutexTree(1, mutableListOf())
        val variables = mapOf("variables.TEST_JOB_ID" to "job_1")
        stages.forEachIndexed { index, stage ->
            stage.forEach { (jobId, dsp) ->
                val con = if (jobId == "job_id_dep_8") {
                    VMBuildContainer(
                        jobId = jobId,
                        dispatchType = dsp,
                        baseOS = VMBaseOS.ALL,
                        jobControlOption = JobControlOption(dependOnName = "job_1")
                    )
                } else {
                    VMBuildContainer(jobId = jobId, dispatchType = dsp, baseOS = VMBaseOS.ALL)
                }
                tree.addNode(con, index, variables)
            }
        }
        val expectRes = mapOf(
            "job_1" to Triple(
                AgentReuseMutex("job_1", null, "agent_1", AgentReuseMutexType.AGENT_ID, false),
                false,
                ThirdPartyAgentIDDispatchType(
                    "agent_1", null, AgentType.ID, null, ReusedInfo(
                        "agent_1",
                        AgentType.ID,
                        null
                    )
                )
            ),
            "job_id_dep_1" to Triple(
                AgentReuseMutex(
                    "job_id_dep_1",
                    "job_1",
                    "agent_1",
                    AgentReuseMutexType.AGENT_ID,
                    false
                ),
                true,
                ThirdPartyAgentIDDispatchType(
                    "job_1", null, AgentType.REUSE_JOB_ID, null, ReusedInfo(
                        "agent_1",
                        AgentType.ID,
                        "job_1"
                    )
                )
            ),
            "job_env_dep_1" to Triple(
                AgentReuseMutex("job_env_dep_1", "job_env_1", "job_1", AgentReuseMutexType.AGENT_ENV_NAME, false),
                true,
                ThirdPartyAgentEnvDispatchType(
                    envName = "job_env_1",
                    null, null, AgentType.REUSE_JOB_ID, null, ReusedInfo(
                        "job_1",
                        AgentType.NAME,
                        "job_env_1"
                    )
                )
            ),
            "job_env_1" to Triple(
                AgentReuseMutex("job_env_1", null, "job_1", AgentReuseMutexType.AGENT_ENV_NAME, false),
                false,
                ThirdPartyAgentEnvDispatchType(
                    envName = "job_1",
                    null, null, AgentType.NAME, null, ReusedInfo(
                        "job_1",
                        AgentType.NAME,
                        null
                    )
                )
            ),
            "job_env_dep_2" to Triple(
                AgentReuseMutex("job_env_dep_2", "job_env_1", "job_1", AgentReuseMutexType.AGENT_ENV_NAME, false),
                true,
                ThirdPartyAgentEnvDispatchType(
                    envName = "job_env_1",
                    null, null, AgentType.REUSE_JOB_ID, null, ReusedInfo(
                        "job_1",
                        AgentType.NAME,
                        "job_env_1"
                    )
                )
            ),
            "job_id_dep_8" to Triple(
                AgentReuseMutex(
                    "job_id_dep_8",
                    "job_1",
                    "agent_1",
                    AgentReuseMutexType.AGENT_DEP_VAR,
                    false
                ), true, initReuseEnv("job_1")
            ),
            "job_id_dep_2" to Triple(
                AgentReuseMutex(
                    "job_id_dep_2",
                    "job_1",
                    "agent_1",
                    AgentReuseMutexType.AGENT_DEP_VAR,
                    true
                ), true, initReuseId("job_1")
            ),
            "job_id_dep_3" to Triple(
                AgentReuseMutex("job_id_dep_3", "job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, false),
                true, initReuseId("job_env_1")
            ),
            "job_id_dep_4" to Triple(
                AgentReuseMutex(
                    "job_id_dep_4",
                    "job_1",
                    "agent_1",
                    AgentReuseMutexType.AGENT_DEP_VAR,
                    true
                ), true, initReuseId("job_1")
            ),
            "job_id_dep_5" to Triple(
                AgentReuseMutex("job_id_dep_5", "job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, false),
                true, initReuseId("job_env_1")
            ),
            "job_id_dep_6" to Triple(
                AgentReuseMutex("job_id_dep_6", "job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, true),
                true, initReuseId("job_env_1")
            ),
            "job_id_dep_7" to Triple(
                AgentReuseMutex("job_id_dep_7", "job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, true),
                true, initReuseEnv("job_env_1")
            )
        )
        val treeMap = tree.tranMap()
        println(treeMap)
        Assertions.assertEquals(
            expectRes.toSortedMap().map { it.key to Pair(it.value.first, it.value.second) }.toMap().toSortedMap(),
            treeMap.toSortedMap()
        )
        stages.forEach { stage ->
            stage.forEach { (jobId, dis) ->
                tree.rewriteDispatch(treeMap[jobId]!!.first, dis, false)
                Assertions.assertEquals(expectRes[jobId]!!.third, dis)
            }
        }
    }

    @Test
    fun checkDepTypeError() {
        val tree = AgentReuseMutexTree(1, mutableListOf())
        val stages = mutableListOf<Map<String, ThirdPartyAgentDispatch>>()
        stages.add(
            mapOf(
                "job_env_dep_1" to initReuseId("job_env_1"),
                "job_env_1" to ThirdPartyAgentEnvDispatchType("job_1", null, null, AgentType.NAME, null, null)
            )
        )
        stages.add(
            mapOf(
                "job_1" to ThirdPartyAgentIDDispatchType("agent_1", null, AgentType.ID, null, null),
                "job_id_dep_1" to initReuseEnv("job_1")
            )
        )
        val thrown1 = assertThrows<ErrorCodeException> {
            stages[0].forEach { (jobId, dsp) ->
                val con = VMBuildContainer(jobId = jobId, dispatchType = dsp, baseOS = VMBaseOS.ALL)
                tree.addNode(con, 0, emptyMap())
            }
        }
        Assertions.assertTrue { thrown1.errorCode == ERROR_AGENT_REUSE_MUTEX_DEP_ERROR }

        val thrown2 = assertThrows<ErrorCodeException> {
            stages[1].forEach { (jobId, dsp) ->
                val con = VMBuildContainer(jobId = jobId, dispatchType = dsp, baseOS = VMBaseOS.ALL)
                tree.addNode(con, 0, emptyMap())
            }
        }
        Assertions.assertTrue { thrown2.errorCode == ERROR_AGENT_REUSE_MUTEX_DEP_ERROR }
    }

    @Test
    fun checkDepCycleError() {
        val tree = AgentReuseMutexTree(1, mutableListOf())
        val stages = mutableListOf<Map<String, ThirdPartyAgentDispatch>>()
        stages.add(
            mapOf(
                "job_env_dep_1" to initReuseId("job_env_dep_2"),
                "job_env_1" to ThirdPartyAgentEnvDispatchType("job_1", null, null, AgentType.NAME, null, null),
                "job_env_dep_2" to initReuseId("job_env_dep_1")
            )
        )
        val thrown1 = assertThrows<ErrorCodeException> {
            stages[0].forEach { (jobId, dsp) ->
                val con = VMBuildContainer(jobId = jobId, dispatchType = dsp, baseOS = VMBaseOS.ALL)
                tree.addNode(con, 0, emptyMap())
            }
            tree.checkVirtualRootAndResetJobType()
        }
        Assertions.assertTrue { thrown1.errorCode == ERROR_AGENT_REUSE_MUTEX_DEP_NULL_NODE }
    }

    private fun initReuseId(reuseName: String) =
        ThirdPartyAgentIDDispatchType(reuseName, null, AgentType.REUSE_JOB_ID, null, null)

    private fun initReuseEnv(reuseName: String) =
        ThirdPartyAgentEnvDispatchType(reuseName, null, null, AgentType.REUSE_JOB_ID, null, null)
}
