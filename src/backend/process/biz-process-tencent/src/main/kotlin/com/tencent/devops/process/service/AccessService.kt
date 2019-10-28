package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SingleArchiveElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.AccessRepository
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AccessService @Autowired constructor(
    private val client: Client,
    private val pipelineService: PipelineService
) {

    private val CMAKE_REPO_URL = "git@git.code.oa.com:bkdevops/cmake.git"

    private val GIT_TOKEN = "g78IwpnylMm_h20UH_Gi"
    private val GIT_SSH_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAo7WkrPOMixAa/YQQ6WxSbdYV2j68v26vC+FaJXK4ie2TnJsQ\n" +
            "++zXIt8lWiAN6QFE/OA8Ux/irBbAkP3/+1GG8Ia+1dRijjxY5Umb5DDMoHaqClTt\n" +
            "O3uNoss4I9qmloT4kCkdhlV8rzvPH2ZxnfAP7IdrKjC4DHoeA4AEfGnQa6Mezq9E\n" +
            "Ckp6aQDgWyY6796VZcIEIiq2b8RrchlytvJ7n2Q5/sWdUsKdlbrlwsiueMgFiF1y\n" +
            "XyWyp3kfF3TJF6XYgZGJguuXyhqCoVi7tJB+nFIf/IC/KH1JUISYdjefgd3jHmqA\n" +
            "Ei1oytrl3woGnxamaGMX6VrqSEgy4ztUzwCUDQIDAQABAoIBABgmf7iT5TPOmGy1\n" +
            "wtjshex2HJspjiafaWtTPz0vA1I1ngUISyUe903JpXT6LZMmAMtdOQj6NzIz2QyK\n" +
            "q+yjRkjNV/Grdy6McIDCryCmokk5uDP1+1k/DNHrMMj5RIIH87MwnY5nphEjvZZg\n" +
            "QnqOI4s9Hu4GaeBNU4gopoDEC18VYNp45Dc9Hyj2vEZTI+jriJ/n9+WQ2Qs9XY5m\n" +
            "jrYqdiDbnqMipFk45HNzDMZPq5AoAQbLBzC4FrcLY4t8ttzfEMyvssL5zxm61Hri\n" +
            "9c6Ktsuj4gN1G7wE9dYWtwFXgg2BEj6bAL/m+2vr85jLCv4QsLOqnnF+PCziOVIy\n" +
            "UGWCHHECgYEAzwW/I2VZHLuayQmB1sUoBu+gqCyM3NBzaxLylK5OksD3unjt4Gsy\n" +
            "8uuZoaMOQ0+/R4QJecIp43P7ColOVTZ+0z79ZabfVROW88T7R4hxz2TYzdwCA17/\n" +
            "QLEH681A053h0+oOEBYY8n2Su69vKcZvRcyTfrE74+SUbAXjxxXcQWMCgYEAynCp\n" +
            "1XCwzJ9eHVP84NQxskap/yLSvTwCSNMglrubo+9uGaoIQuR07nEo/fiiHTXcizV4\n" +
            "xw0P8s/gFyCFFGglEd7l7BOjj2idPLrpCIV/4VvWluJYacckzYv72eKEGuCeVHuA\n" +
            "/uUaaU2dwgzmfiCvda0y/4gR8/u8FfkaDazKB88CgYATxXW6uKwpDVW8C3dl/pBT\n" +
            "EUGjrhWJ5TKQsE+QmZERfPJr0a7ONw63mn6irELpdM1M1DRfd4aunV4FZJWhl8HH\n" +
            "BQYIVkaQBn3tLAvfig1shDIcfv2GOuVf1UhvYbvmOfbeWUUcji+1wP5phFi2gagQ\n" +
            "33faqqyQmD0AkBNv6QuPBwKBgQCm47JfL4PRbSCddPvoLYa6vd6vYvnw32PSvZsE\n" +
            "KK3qvBw8NByTaNutJsTweuTKx/iFGxPypSYcupq29iw/4ouM7AEIWjhgpZHa2wv5\n" +
            "5nTCSH/j672PlokUmu6JdWAK+FoOs7JocF8RqNcBfrkWCcQccyiz2G1UgpdQVgfQ\n" +
            "dj4nqQKBgQCRCfigaSk6gsUGp0944BTq0hhhTKU9ScsKOKaUKP0QvMY/6xHe4afD\n" +
            "Z4Y/UEiL2wwWlgLox3vVzWG0cwnNjScoPy1YJPRhFsRtUd2wadqGXZwHqGpUfpkn\n" +
            "pu2idWFx3f0/CC71HOdM7MdDoizDSy43KDuyumCKxyOqFj3XWYyFKg==\n" +
            "-----END RSA PRIVATE KEY-----"

    fun create(userId: String, accessRepository: AccessRepository): String {
        val timestamp = LocalDateTime.now().timestamp()
        val projectId = accessRepository.projectId
        val url = CMAKE_REPO_URL
        val hashCode = HashUtil.encodeLongId(timestamp)

        val credentialId = "git_credential_$hashCode"
        val credentialRemark = "新手接入凭据"
        createCredential(userId, projectId, credentialId, credentialRemark)

        val projectName = "bkdevops/cmake"
        val repositoryAliasName = "Code Demo（新手接入专用）_$hashCode"
        val repositoryHashId = createRepository(userId, projectId, projectName, credentialId, url, repositoryAliasName)

        val pipelineName = "新手接入_$hashCode"
        return createPipeline(pipelineName, userId, projectId, repositoryHashId)
    }

    private fun createCredential(userId: String, projectId: String, credentialId: String, credentialRemark: String) {
        try {
            client.get(ServiceCredentialResource::class).check(projectId, credentialId)
        } catch (exception: RemoteServiceException) {
            if (exception.httpStatus != 404) {
                throw exception
            }
            val credentialType = CredentialType.TOKEN_SSH_PRIVATEKEY
            val credentialCreate = CredentialCreate(credentialId, credentialType, credentialRemark, GIT_TOKEN, GIT_SSH_KEY, null, null)
            client.get(ServiceCredentialResource::class).create(userId, projectId, credentialCreate)
        }
    }

    private fun createRepository(userId: String, projectId: String, projectName: String, credentialId: String, url: String, aliasName: String): String {
        val repository = CodeGitRepository(aliasName, url, credentialId, projectName, userId, RepoAuthType.SSH, projectId, null)
        return client.get(ServiceRepositoryResource::class).create(userId, projectId, repository).data!!.hashId
    }

    private fun createPipeline(pipelineName: String, userId: String, projectId: String, repositoryHashId: String): String {
        return createCmakePipeline(pipelineName, userId, projectId, repositoryHashId)
    }

    private fun createCmakePipeline(pipelineName: String, userId: String, projectId: String, repositoryHashId: String): String {
        val model = generateModel(
                pipelineName,
                "C++（CMake）构建",
                repositoryHashId,
                "",
                "cmake .\r\nmake",
                "make clean\r\ncmake .\r\nmake",
                "cmake")
        return pipelineService.createPipeline(userId, projectId, model, ChannelCode.BS)
    }

    private fun generateModel(name: String, desc: String, repositoryHashId: String, repositoryPath: String, codeCCScript: String, script: String, archivePath: String): Model {
        var containerSeqId = 0
        // stage-1
        val stageFirstElement = ManualTriggerElement(
                "手动触发",
                "T-1-1-1",
                null)
        val stageFirstElements = listOf<Element>(stageFirstElement)
        val stageFirstContainer = TriggerContainer(containerSeqId.toString(),
                "构建触发",
                stageFirstElements,
                null,
                null,
                null,
                null,
                emptyList(), null, null)
        containerSeqId++
        val stageFirstContainers = listOf<Container>(stageFirstContainer)
        val stageFirst = Stage(stageFirstContainers, "stage-1")

        // stage-2
        val stageSecondPullCodeElement = CodeGitElement(
                "拉取Git仓库代码",
                "T-2-1-1",
                null,
                repositoryHashId,
                "",
                "",
                CodePullStrategy.FRESH_CHECKOUT,
                repositoryPath, true, GitPullMode(GitPullModeType.BRANCH, "master"))
        val stageSecondCodeCCElement = LinuxPaasCodeCCScriptElement(
                "CodeCC代码检查任务",
                "T-2-1-2",
                null,
                BuildScriptType.SHELL,
                codeCCScript,
                null,
                null,
                null,
                false,
                "1",
                repositoryPath,
                listOf(ProjectLanguage.C_CPP)
        )
        stageSecondCodeCCElement.tools = listOf("COVERITY")
        val stageSecondLinuxScriptElement = LinuxScriptElement(
                "执行Linux脚本",
                "T-2-1-3",
                null,
                BuildScriptType.SHELL,
                script,
                false,
                false,
                null)
        val stageSecondSingleArchiveElement = SingleArchiveElement(
                "归档构件",
                "T-2-1-4",
                null,
                archivePath,
                "./",
                false)
        val stageSecondElements = listOf(stageSecondPullCodeElement, stageSecondCodeCCElement, stageSecondLinuxScriptElement, stageSecondSingleArchiveElement)

        // buildEnv
        val buildEnv = mutableMapOf<String, String>()
        buildEnv["cmake"] = "3.9.6"

        val stageSecondContainer = VMBuildContainer(
            id = containerSeqId.toString(),
            name = "构建环境",
            elements = stageSecondElements,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.LINUX,
            vmNames = emptySet(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 480,
            buildEnv = buildEnv,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = DockerDispatchType("tlinux2.2")
        )
        val stageSecondContainers = listOf<Container>(stageSecondContainer)
        val stageSecond = Stage(stageSecondContainers, "stage-2")

        val stages = mutableListOf(stageFirst, stageSecond)
        return Model(name, desc, stages)
    }
}
