const commonPrefix = `${DOCS_URL_PREFIX}/docs/markdown/持续集成平台/产品白皮书`
const gatePrefix = `${DOCS_URL_PREFIX}/docs/markdown/质量红线/产品白皮书`
const turboPrefix = `${DOCS_URL_PREFIX}/docs/markdown/编译加速/产品白皮书`
export const BkciDocs = {
    BKCI_DOC: `${commonPrefix}/产品简介/README.md`, // 首页跳转文档
    GATE_DOC: `${gatePrefix}/Intro/README.md`, // 质量红线文档
    TICKET_DOC: `${commonPrefix}/Services/Ticket/ticket-add.md`, // 凭据文档
    WIN_AGENT_GUIDE: `${commonPrefix}/Services/Resource/bkci-hosted-windows-agent.md`, // 安装Windows构建机指引文档
    PLUGIN_SPECIFICATE_DOC: `${commonPrefix}/Services/Store/plugins/plugin-specification.md`, // 插件规范文档
    PLUGIN_ERROR_CODE_DOC: `${commonPrefix}/Services/Store/plugins/plugin-error-code.md`, // 插件错误码文档
    PLUGIN_GUIDE_DOC: `${commonPrefix}/Services/Store/start-new-task.md`, // 插件指引文档
    IMAGE_GUIDE_DOC: `${commonPrefix}/Services/Store/docker-build.md`, // docker构建文档
    TEMPLATE_GUIDE_DOC: `${commonPrefix}/Services/Store/start-new-template.md`, // 模板指引文档
    TURBO_GUIDE_DOC: `${turboPrefix}/Quickstart/linux_c_cpp.md` // turbo指引文档
}
export const pipelineDocs = {
    ALIAS_BUILD_NO_DOC: `${commonPrefix}/Services/Pipeline/pipeline-edit-guide/alias-buildno.md`, // 构建号别名文档
    PIPELINE_ERROR_GUIDE_DOC: `${commonPrefix}/FAQS/FAQ.md`, // 流水线编辑指引文档
    NAMESPACE_DOC: `${DOCS_URL_PREFIX}/docs/document/6.1/183/22916` // 命名空间文档
}
