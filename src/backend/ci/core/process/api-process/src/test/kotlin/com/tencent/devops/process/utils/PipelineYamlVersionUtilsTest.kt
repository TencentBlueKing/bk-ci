/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
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

package com.tencent.devops.process.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ALL")
class PipelineYamlVersionUtilsTest {

    @Test
    fun getVersions() {
        val model = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val sameModel = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel1 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer()
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel2 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer(),
                        NormalContainer()
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel3 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer(),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel4 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer(),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = false)
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffTrigger = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t2"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val version = 1
        assertEquals(PipelineVersionUtils.getPipelineVersion(version, model, sameModel), version)
        assertEquals(PipelineVersionUtils.getTriggerVersion(version, model, sameModel), version)
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model, diffModel1))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model, diffModel2))
        assertEquals(version + 1, PipelineVersionUtils.getTriggerVersion(version, model, diffTrigger))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, diffModel2, diffModel3))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, diffModel3, diffModel4))
    }

    @Test
    fun testModelStr() {
        val modelStr1 = "{\"name\":\"插件enable\",\"desc\":\"插件enable\",\"stages\":[{\"containers\":[{\"@type\":\"trigger\",\"id\":\"0\",\"name\":\"trigger\",\"elements\":[{\"@type\":\"manualTrigger\",\"name\":\"手动触发\",\"id\":\"T-1-1-1\",\"canElementSkip\":false,\"useLatestParameters\":false,\"executeCount\":1,\"version\":\"1.*\",\"classType\":\"manualTrigger\",\"elementEnable\":true,\"atomCode\":\"manualTrigger\",\"taskAtom\":\"\"}],\"params\":[],\"containerId\":\"0\",\"containerHashId\":\"c-9658289a5be3436dbc5c96ae4b66f3ec\",\"matrixGroupFlag\":false,\"classType\":\"trigger\",\"containerEnable\":true}],\"id\":\"stage-1\",\"name\":\"stage-1\",\"tag\":[\"28ee946a59f64949a74f3dee40a1bda4\"],\"fastKill\":false,\"finally\":false,\"stageEnable\":true},{\"containers\":[{\"@type\":\"normal\",\"id\":\"1\",\"name\":\"无编译环境\",\"elements\":[{\"@type\":\"marketBuildLess\",\"name\":\"子流水线调用新\",\"id\":\"e-53f57f458b5745c6b1d20a5a20726253\",\"atomCode\":\"SubPipelineExec\",\"version\":\"1.*\",\"data\":{\"input\":{\"projectId\":\"codecc-tool-auto\",\"subPipelineType\":\"ID\",\"subPip\":\"p-3d2d4022cf3e476bb7c060e3634abff4\",\"subPipelineName\":\"\",\"runMode\":\"syn\",\"params\":\"[{\\\"key\\\":\\\"BK_CI_BUILD_MSG\\\",\\\"value\\\":\\\"1\\\",\\\"enable\\\":true},{\\\"key\\\":\\\"CODECC_BKCHECK_MinHeapFreeRatio\\\",\\\"value\\\":\\\"30\\\",\\\"enable\\\":true},{\\\"key\\\":\\\"CODECC_BKCHECK_MaxHeapFreeRatio\\\",\\\"value\\\":\\\"50\\\",\\\"enable\\\":true}]\",\"fieldNamespace\":\"sub_pipeline\"},\"output\":{\"sub_pipeline_buildId\":\"string\",\"sub_pipeline_url\":\"string\"},\"namespace\":\"\"},\"executeCount\":1,\"additionalOptions\":{\"enable\":false,\"continueWhenFailed\":false,\"manualSkip\":false,\"retryWhenFailed\":false,\"retryCount\":1,\"manualRetry\":false,\"timeout\":900,\"timeoutVar\":\"900\",\"runCondition\":\"PRE_TASK_SUCCESS\",\"pauseBeforeExec\":false,\"subscriptionPauseUser\":\"user1\",\"otherTask\":\"\",\"customVariables\":[{\"key\":\"param1\",\"value\":\"\"}],\"customCondition\":\"\",\"enableCustomEnv\":true},\"classType\":\"marketBuildLess\",\"elementEnable\":false,\"taskAtom\":\"\"}],\"enableSkip\":false,\"containerId\":\"1\",\"containerHashId\":\"c-5c8c288bce3c417a80d74e7d578bdab7\",\"maxQueueMinutes\":60,\"maxRunningMinutes\":1440,\"jobControlOption\":{\"enable\":true,\"prepareTimeout\":10,\"timeout\":900,\"timeoutVar\":\"900\",\"runCondition\":\"STAGE_RUNNING\",\"customVariables\":[{\"key\":\"param1\",\"value\":\"\"}],\"customCondition\":\"\",\"dependOnType\":\"ID\",\"dependOnId\":[],\"dependOnName\":\"\",\"continueWhenFailed\":false},\"jobId\":\"job_fLR\",\"matrixGroupFlag\":false,\"classType\":\"normal\",\"containerEnable\":true}],\"id\":\"stage-2\",\"name\":\"stage-1\",\"tag\":[\"28ee946a59f64949a74f3dee40a1bda4\"],\"fastKill\":false,\"finally\":false,\"checkIn\":{\"manualTrigger\":false,\"timeout\":24,\"markdownContent\":false,\"notifyType\":[\"RTX\"]},\"checkOut\":{\"manualTrigger\":false,\"timeout\":24,\"markdownContent\":false,\"notifyType\":[\"RTX\"]},\"stageEnable\":true}],\"labels\":[],\"instanceFromTemplate\":false,\"pipelineCreator\":\"user1\",\"events\":{},\"staticViews\":[],\"latestVersion\":1}\n"
        val modelStr2 = "{\"name\":\"插件enable\",\"desc\":\"插件enable\",\"stages\":[{\"containers\":[{\"@type\":\"trigger\",\"id\":\"0\",\"name\":\"trigger\",\"elements\":[{\"@type\":\"manualTrigger\",\"name\":\"手动触发\",\"id\":\"T-1-1-1\",\"canElementSkip\":false,\"useLatestParameters\":false,\"executeCount\":1,\"version\":\"1.*\",\"classType\":\"manualTrigger\",\"elementEnable\":true,\"atomCode\":\"manualTrigger\",\"taskAtom\":\"\"}],\"params\":[],\"containerId\":\"0\",\"containerHashId\":\"c-9658289a5be3436dbc5c96ae4b66f3ec\",\"matrixGroupFlag\":false,\"classType\":\"trigger\",\"containerEnable\":true}],\"id\":\"stage-1\",\"name\":\"stage-1\",\"tag\":[\"28ee946a59f64949a74f3dee40a1bda4\"],\"fastKill\":false,\"finally\":false,\"stageEnable\":true},{\"containers\":[{\"@type\":\"normal\",\"id\":\"1\",\"name\":\"无编译环境\",\"elements\":[{\"@type\":\"marketBuildLess\",\"name\":\"子流水线调用新\",\"id\":\"e-53f57f458b5745c6b1d20a5a20726253\",\"atomCode\":\"SubPipelineExec\",\"version\":\"1.*\",\"data\":{\"input\":{\"projectId\":\"codecc-tool-auto\",\"subPipelineType\":\"ID\",\"subPip\":\"p-3d2d4022cf3e476bb7c060e3634abff4\",\"subPipelineName\":\"\",\"runMode\":\"syn\",\"params\":\"[{\\\"key\\\":\\\"BK_CI_BUILD_MSG\\\",\\\"value\\\":\\\"1\\\",\\\"enable\\\":true},{\\\"key\\\":\\\"CODECC_BKCHECK_MinHeapFreeRatio\\\",\\\"value\\\":\\\"30\\\",\\\"enable\\\":true},{\\\"key\\\":\\\"CODECC_BKCHECK_MaxHeapFreeRatio\\\",\\\"value\\\":\\\"50\\\",\\\"enable\\\":true}]\",\"fieldNamespace\":\"sub_pipeline\"},\"output\":{\"sub_pipeline_buildId\":\"string\",\"sub_pipeline_url\":\"string\"},\"namespace\":\"\"},\"executeCount\":1,\"additionalOptions\":{\"enable\":true,\"continueWhenFailed\":false,\"manualSkip\":false,\"retryWhenFailed\":false,\"retryCount\":1,\"manualRetry\":false,\"timeout\":900,\"timeoutVar\":\"900\",\"runCondition\":\"PRE_TASK_SUCCESS\",\"pauseBeforeExec\":false,\"subscriptionPauseUser\":\"user1\",\"otherTask\":\"\",\"customVariables\":[{\"key\":\"param1\",\"value\":\"\"}],\"customCondition\":\"\",\"enableCustomEnv\":true},\"classType\":\"marketBuildLess\",\"elementEnable\":true,\"taskAtom\":\"\"}],\"enableSkip\":false,\"containerId\":\"1\",\"containerHashId\":\"c-5c8c288bce3c417a80d74e7d578bdab7\",\"maxQueueMinutes\":60,\"maxRunningMinutes\":1440,\"jobControlOption\":{\"enable\":true,\"prepareTimeout\":10,\"timeout\":900,\"timeoutVar\":\"900\",\"runCondition\":\"STAGE_RUNNING\",\"customVariables\":[{\"key\":\"param1\",\"value\":\"\"}],\"customCondition\":\"\",\"dependOnType\":\"ID\",\"dependOnId\":[],\"dependOnName\":\"\",\"continueWhenFailed\":false},\"jobId\":\"job_fLR\",\"matrixGroupFlag\":false,\"classType\":\"normal\",\"containerEnable\":true}],\"id\":\"stage-2\",\"name\":\"stage-1\",\"tag\":[\"28ee946a59f64949a74f3dee40a1bda4\"],\"fastKill\":false,\"finally\":false,\"checkIn\":{\"manualTrigger\":false,\"timeout\":24,\"markdownContent\":false,\"notifyType\":[\"RTX\"]},\"checkOut\":{\"manualTrigger\":false,\"timeout\":24,\"markdownContent\":false,\"notifyType\":[\"RTX\"]},\"stageEnable\":true}],\"labels\":[],\"instanceFromTemplate\":false,\"pipelineCreator\":\"user1\",\"events\":{},\"staticViews\":[],\"latestVersion\":2}"
        val model1 = JsonUtil.to(modelStr1, Model::class.java)
        val model2 = JsonUtil.to(modelStr2, Model::class.java)
        println(model1)
        println(model2)
        val version = 1
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model1, model2))
    }

    @Test
    fun testEnvDiffer() {
        val model1 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        VMBuildContainer(
                            customEnv = listOf(
                                NameAndValue("a", "1")
                            ),
                            baseOS = VMBaseOS.LINUX
                        ),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val model2 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        VMBuildContainer(
                            customEnv = emptyList(),
                            baseOS = VMBaseOS.LINUX
                        ),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val model3 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        VMBuildContainer(
                            customEnv = listOf(
                                NameAndValue("a", "1")
                            ),
                            baseOS = VMBaseOS.LINUX
                        ),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    customEnv = listOf(
                                        NameAndValue("b", "2")
                                    ),
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val version = 1
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model1, model2))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model1, model3))
    }

    @Test
    fun testRunConditionDiffer() {
        val model1 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        VMBuildContainer(
                            baseOS = VMBaseOS.LINUX
                        ),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    ),
                    stageControlOption = StageControlOption(
                        runCondition = StageRunCondition.AFTER_LAST_FINISHED
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val model2 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        VMBuildContainer(
                            baseOS = VMBaseOS.LINUX
                        ),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL,
                                    additionalOptions = ElementAdditionalOptions(enable = true)
                                )
                            )
                        )
                    ),
                    stageControlOption = StageControlOption(
                        runCondition = StageRunCondition.CUSTOM_VARIABLE_MATCH
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val version = 1
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model1, model2))
    }

    @Test
    fun getSettingVersions() {
        val setting = PipelineSettingVersion(
            projectId = "proj1",
            pipelineId = "p-xxx",
            version = 1,
            pipelineName = "p-name",
            desc = "setting.desc",
            runLockType = PipelineRunLockType.LOCK,
            successSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX, PipelineSubscriptionType.EMAIL)
                )
            ),
            failSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX, PipelineSubscriptionType.EMAIL)
                )
            ),
            labels = null,
            waitQueueTimeMinute = 1,
            maxQueueSize = 1,
            buildNumRule = null,
            concurrencyCancelInProgress = null,
            concurrencyGroup = null
        )
        val sameSetting = PipelineSettingVersion(
            projectId = "proj1",
            pipelineId = "p-xxx",
            version = 1,
            pipelineName = "p-name",
            desc = "setting.desc",
            runLockType = PipelineRunLockType.LOCK,
            successSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX, PipelineSubscriptionType.EMAIL)
                )
            ),
            failSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX, PipelineSubscriptionType.EMAIL)
                )
            ),
            labels = null,
            waitQueueTimeMinute = 1,
            maxQueueSize = 1,
            buildNumRule = null,
            concurrencyCancelInProgress = null,
            concurrencyGroup = null
        )
        val diffSetting = PipelineSettingVersion(
            projectId = "proj1",
            pipelineId = "p-xxx",
            version = 1,
            pipelineName = "p-name",
            desc = "setting.desc",
            runLockType = PipelineRunLockType.LOCK,
            successSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX)
                )
            ),
            failSubscriptionList = listOf(
                Subscription(
                    types = setOf(PipelineSubscriptionType.RTX, PipelineSubscriptionType.EMAIL)
                )
            ),
            labels = null,
            waitQueueTimeMinute = 1,
            maxQueueSize = 1,
            buildNumRule = null,
            concurrencyCancelInProgress = null,
            concurrencyGroup = null
        )
        assertEquals(setting.version, PipelineVersionUtils.getSettingVersion(setting.version, setting, sameSetting))
        assertEquals(setting.version + 1, PipelineVersionUtils.getSettingVersion(setting.version, setting, diffSetting))
    }
}
