package com.tencent.devops.ai.service

import com.tencent.devops.ai.pojo.AiOverviewCountsVO
import com.tencent.devops.ai.pojo.AiOverviewVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AiOverviewService @Autowired constructor(
    private val agentSysPromptService: AgentSysPromptService,
    private val welcomeGuideService: WelcomeGuideService,
    private val mcpServerService: AiMcpServerService,
    private val skillService: AiSkillService,
    private val externalAgentService: ExternalAgentService,
    private val promptService: AiPromptService,
    private val userLlmConfigService: UserLlmConfigService,
    private val projectService: AiProjectService
) {

    fun getOverview(): AiOverviewVO {
        val mcp = mcpServerService.listAllForOp()
        val skills = skillService.listAllForOp()
        return AiOverviewVO(
            counts = AiOverviewCountsVO(
                sysPrompts = agentSysPromptService.listAllAgentSysPrompts().size,
                welcomeGuides = welcomeGuideService.listAllWelcomeGuides().size,
                hotQuestions = welcomeGuideService.listAllHotQuestions().size,
                mcpServers = mcp.size,
                systemMcpServers = mcp.count { it.scope == "SYSTEM" },
                skills = skills.size,
                systemSkills = skills.count { it.scope == "SYSTEM" },
                externalAgents = externalAgentService.listAllForOp().size,
                userPrompts = promptService.listAllForOp().size,
                userLlmConfigs = userLlmConfigService.listAllForOp().size
            ),
            projectCount = projectService.getProjectListForOp().size
        )
    }
}
