package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.container.AgentReuseMutexType
import com.tencent.devops.common.pipeline.type.agent.AgentType
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
                "job_id_dep_8" to initReuseEnv("job_1")
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_2" to initReuseId("job_id_dep_1"),
                "job_id_dep_3" to initReuseId("job_env_dep_1"),
                "job_id_dep_4" to initReuseId("job_id_dep_2"),
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_5" to initReuseId("job_env_dep_1")
            )
        )
        stages.add(
            mapOf(
                "job_id_dep_6" to initReuseId("job_env_1"),
                "job_id_dep_7" to initReuseEnv("job_env_1"),
            )
        )
        val tree = AgentReuseMutexTree(mutableListOf())
        stages.forEachIndexed { index, stage ->
            stage.forEach { (jobId, dsp) ->
                tree.addNode(
                    jobId = jobId,
                    dispatchType = dsp,
                    existDep = jobId == "job_id_dep_8",
                    stageIndex = index,
                    containerId = null,
                    isEnv = dsp is ThirdPartyAgentEnvDispatchType
                )
            }
        }
        val expectRes = mapOf(
            "job_1" to Pair(AgentReuseMutex(null, "agent_1", AgentReuseMutexType.AGENT_ID, false), false),
            "job_id_dep_1" to Pair(AgentReuseMutex("job_1", "agent_1", AgentReuseMutexType.AGENT_ID, false), true),
            "job_env_dep_1" to Pair(
                AgentReuseMutex("job_env_1", "job_1", AgentReuseMutexType.AGENT_ENV_NAME, false),
                true
            ),
            "job_env_1" to Pair(AgentReuseMutex(null, "job_1", AgentReuseMutexType.AGENT_ENV_NAME, false), false),
            "job_id_dep_8" to Pair(AgentReuseMutex("job_1", "agent_1", AgentReuseMutexType.AGENT_DEP_VAR, false), true),
            "job_id_dep_2" to Pair(AgentReuseMutex("job_1", "agent_1", AgentReuseMutexType.AGENT_DEP_VAR, true), true),
            "job_id_dep_3" to Pair(
                AgentReuseMutex("job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, false),
                true
            ),
            "job_id_dep_4" to Pair(AgentReuseMutex("job_1", "agent_1", AgentReuseMutexType.AGENT_DEP_VAR, true), true),
            "job_id_dep_5" to Pair(
                AgentReuseMutex("job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, false),
                true
            ),
            "job_id_dep_6" to Pair(
                AgentReuseMutex("job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, true),
                true
            ),
            "job_id_dep_7" to Pair(
                AgentReuseMutex("job_env_1", "job_1", AgentReuseMutexType.AGENT_DEP_VAR, true),
                true
            )
        )
        Assertions.assertEquals(expectRes.toSortedMap(), tree.tranMap().toSortedMap())
    }

    @Test
    fun checkDepTypeError() {
        val tree = AgentReuseMutexTree(mutableListOf())
        val stages = mutableListOf<Map<String, ThirdPartyAgentDispatch>>()
        stages.add(
            mapOf(
                "job_env_dep_1" to initReuseId("job_env_1"),
                "job_env_1" to ThirdPartyAgentEnvDispatchType("job_1", null, null, AgentType.NAME, null, null),
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
                tree.addNode(
                    jobId = jobId,
                    dispatchType = dsp,
                    existDep = jobId == "job_id_dep_8",
                    stageIndex = 0,
                    containerId = null,
                    isEnv = dsp is ThirdPartyAgentEnvDispatchType
                )
            }
        }
        Assertions.assertTrue { thrown1.errorCode == ERROR_AGENT_REUSE_MUTEX_DEP_ERROR }

        val thrown2 = assertThrows<ErrorCodeException> {
            stages[1].forEach { (jobId, dsp) ->
                tree.addNode(
                    jobId = jobId,
                    dispatchType = dsp,
                    existDep = jobId == "job_id_dep_8",
                    stageIndex = 0,
                    containerId = null,
                    isEnv = dsp is ThirdPartyAgentEnvDispatchType
                )
            }
        }
        Assertions.assertTrue { thrown2.errorCode == ERROR_AGENT_REUSE_MUTEX_DEP_ERROR }
    }

    @Test
    fun checkDepCycleError() {
        val tree = AgentReuseMutexTree(mutableListOf())
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
                tree.addNode(
                    jobId = jobId,
                    dispatchType = dsp,
                    existDep = jobId == "job_id_dep_8",
                    stageIndex = 0,
                    containerId = null,
                    isEnv = dsp is ThirdPartyAgentEnvDispatchType
                )
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
