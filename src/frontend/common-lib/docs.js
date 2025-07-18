// const commonPrefix = `${DOCS_URL_PREFIX}/docs/markdown/持续集成平台/产品白皮书`
function getVersion (version) {
    const versionMatch = version?.match(/^(\d+)\.(\d+)/)
    if (versionMatch) {
        return `${versionMatch[1]}.${versionMatch[2]}`
    }
    return ''
}

const createDocs = (lang, version) => {
    const commonPrefix = `${DOCS_URL_PREFIX}/markdown/${lang}/Devops/${getVersion(version)}/UserGuide`
    const BkciDocs = {
        BKCI_DOC: `${commonPrefix}/intro/README.md`, // 首页跳转文档
        GATE_DOC: `${commonPrefix}/Services/Quailty-gate/quailty-gate.md`, // 质量红线文档
        TICKET_DOC: `${commonPrefix}/Services/Ticket/ticket.md`, // 凭据文档
        WIN_AGENT_GUIDE: `${commonPrefix}/Services/Pools/self-hosted-agents/windows-agent.md`, // 安装Windows构建机指引文档
        PLUGIN_SPECIFICATE_DOC: `${commonPrefix}/Developer/plugins/plugin-dev-standard/plugin-specification.md`, // 插件规范文档
        PLUGIN_ERROR_CODE_DOC: `${commonPrefix}/Developer/plugins/plugin-dev-standard/plugin-error-code.md`, // 插件错误码文档
        PLUGIN_GUIDE_DOC: `${commonPrefix}/Services/Store/start-new-task.md`, // 插件指引文档
        IMAGE_GUIDE_DOC: `${commonPrefix}/Services/Store/ci-images/docker-build.md`, // docker构建文档
        TEMPLATE_GUIDE_DOC: `${commonPrefix}/Services/Store/ci-templates/start-new-template.md`, // 模板指引文档
        TURBO_GUIDE_DOC: `${commonPrefix}/Services/Turbo/linux-tubo-speed/use_in_linux.md`, // turbo指引文档
        BKAPP_NAV_OPEN_SOURCE_URL: 'https://github.com/TencentBlueKing/bk-ci', // 开源社区
        FEED_BACK_URL: `${DOCS_URL_PREFIX}/s-mart/community/question`, // 问题反馈
        PAC_GUIDE_DOC: `${commonPrefix}/Services/Pipeline-as-Code/01-quick-start/01-quict-start.md`, // PAC快速上手文档
        BUILD_NODE_GUIDE_DOC: `${commonPrefix}/UserGuide/Services/Pools/host-to-bkci.md`, // 环境管理-安装节点失败文档
    }
    const pipelineDocs = {
        ALIAS_BUILD_NO_DOC: `${commonPrefix}/Services/Pipeline/pipeline-edit-guide/alias-buildno.md`, // 构建号别名文档
        PIPELINE_ERROR_GUIDE_DOC: `${commonPrefix}/Reference/faqs/pipelines/execute.md`, // 流水线编辑指引文档
        NAMESPACE_DOC: `${commonPrefix}/Services/Pipeline/pipeline-edit-guide/pipeline-variables/variables-custom.md`, // 命名空间文档
        AIAnalysis: `${commonPrefix}/UserGuide/Services/Pipeline/pipeline-build-detail/ai-analysis.md`,
        CUSTOM_EXPRESSIONS_DOC: `${commonPrefix}/UserGuide/Services/Pipeline-as-Code/02-generate-pipelines/09-conditional-execution/02-expression.md` // 自定义表达式
    }
    return {
        BkciDocs,
        pipelineDocs
    }
}

export default createDocs
