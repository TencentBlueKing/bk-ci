export const jumpUrlPre = 'https://bkmonitor.woa.com/grafana/dashboard?'

export const jumpUrlPostBuildResource = `${window.BK_PAAS_PRIVATE_URL}/console/environment`
/**
 * 生成完整跳转URL
 * @param {string} urlParams - URL参数字符串
 * @returns {string} 完整的跳转URL
 */
export const generateJumpUrl = (urlParams) => {
    return `${jumpUrlPre}${urlParams}`
}

export const runningPipelines = (timeStamp)=>{
    return `sum(sum by (dimensions__bk_46__buildId, dimensions__bk_46__pipelineId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_START\"}[${timeStamp}s])) unless sum by (dimensions__bk_46__buildId, dimensions__bk_46__pipelineId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\"}[${timeStamp}s])))`
}

export const runningPipelinesUrl = (projectId, from, to) => {
    return `spaceUid=bkci__${projectId}&dashName=BKCI-运行中的任务&viewPanel=2&from=${from}&to=${to}`
}

export const waitingPipelines = (timeStamp)=>{
    return `sum(sum by (dimensions__bk_46__buildId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__status=\"QUEUE\"}[${timeStamp}s])) unless sum by (dimensions__bk_46__buildId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\",dimensions__bk_46__eventType!=\"BUILD_QUEUE\"}[${timeStamp}s])))`
}

export const waitingPipelinesUrl = (projectId, from, to) => {
    return `spaceUid=bkci__${projectId}&dashName=BKCI-运行中的任务&viewPanel=20&from=${from}&to=${to}`
}

export const waitingJob = (timeStamp)=>{
    return `sum(sum by (dimensions__bk_46__buildId, dimensions__bk_46__jobId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"JOB\", dimensions__bk_46__status=\"QUEUE\"}[${timeStamp}s])) unless sum by (dimensions__bk_46__buildId, dimensions__bk_46__jobId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"JOB\", dimensions__bk_46__eventType!=\"BUILD_JOB_QUEUE\"}[${timeStamp}s])))`
}

export const waitingJobUrl = (projectId, from, to) => {
    return `spaceUid=bkci__${projectId}&dashName=BKCI-运行中的任务&viewPanel=21&from=${from}&to=${to}`
}

export const auditPipelines = (timeStamp)=>{
    return `sum(sum by (dimensions__bk_46__buildId, dimensions__bk_46__stageId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"STAGE\", dimensions__bk_46__status=\"REVIEWING\"}[${timeStamp}s])) unless sum by (dimensions__bk_46__buildId, dimensions__bk_46__stageId)(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"STAGE\", dimensions__bk_46__status=~\"REVIEW_ABORT|REVIEW_PROCESSED\"}[${timeStamp}s])))`
}

export const auditPipelinesUrl = (projectId, from, to) => {
    return `spaceUid=bkci__${projectId}&dashName=BKCI-运行中的任务&viewPanel=17&from=${from}&to=${to}`
}

export const failuresNum = (timeStamp)=>{
    return `sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\", dimensions__bk_46__status=\"FAILED\"}[${timeStamp}s]))`
}

export const failuresNumUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-流水线运行趋势&var-status=FAILED&viewPanel=38&from=${from}&to=${to}`
}

export const cancelNum = (timeStamp)=>{
    return `sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\", dimensions__bk_46__status=\"CANCELED\"}[${timeStamp}s]))`
}

export const cancelNumUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-流水线运行趋势&var-status=CANCELED&viewPanel=38&from=${from}&to=${to}`
}

export const successNum = (timeStamp)=>{
    return `sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\", dimensions__bk_46__status=\"SUCCEED\"}[${timeStamp}s]))`
}

export const successNumUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-流水线运行趋势&var-status=SUCCEED&viewPanel=38&from=${from}&to=${to}`
}

export const successRate = (timeStamp)=>{
    return `sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\", dimensions__bk_46__status=~\"SUCCEED|STAGE_SUCCESS\"}[${timeStamp}s])) / sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId=\"{{projectId}}\", dimensions__bk_46__level=\"PIPELINE\", dimensions__bk_46__eventType=\"BUILD_END\", dimensions__bk_46__status!=\"QUEUE_TIMEOUT\"}[${timeStamp}s]))`
}

export const successRateUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-流水线运行趋势&viewPanel=8&from=${from}&to=${to}`
}

export const sourceCpu = 'count((100 - min by (hostIp, hostName) ({{table_agent}}:cpu_detail:idle{projectId=\"{{projectId}}\"})) > 80)'

export const sourceCpuUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-构建资源趋势&viewPanel=25&from=${from}&to=${to}`
}

export const sourceMemory ='count(max by (hostIp, hostName) ({{table_agent}}:mem:pct_used{projectId=\"{{projectId}}\"}) > 80)'

export const sourceMemoryUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-构建资源趋势&viewPanel=26&from=${from}&to=${to}`
}

export const sourceDisk ='count(max by (hostIp, hostName) ({{table_agent}}:disk:in_use{projectId=\"{{projectId}}\",  path!~\".+kubelet.+\"} offset 4m) > 80)'

export const sourceDiskUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-构建资源趋势&viewPanel=24&from=${from}&to=${to}`
}

export const totalArtifacts ='sum(last_over_time({{bkrepo_table}}:bkrepo_repository_size_bytes{projectId=\"{{projectId}}\"}[1h]))'

export const totalArtifactsUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&viewPanel=39&from=${from}&to=${to}`
}

export const addedArtifacts =(timeStamp)=>{
    return `sum(sum_over_time({{bkrepo_table}}:artifact_transfer_size_bytes_sum{projectId=\"{{projectId}}\", type=\"RECEIVE\"}[${timeStamp}s])) or vector(0)`
}

export const addedArtifactsUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&from=${from}&to=${to}`
}

export const deletedArtifacts =(timeStamp)=>{
    return `(sum(sum_over_time({{bkrepo_table}}:artifact_transfer_size_bytes_sum{projectId=\"{{projectId}}\", type=\"RECEIVE\"}[${timeStamp}s])) or vector(0)) - ((sum(last_over_time({{bkrepo_table}}:bkrepo_repository_size_bytes{projectId=\"{{projectId}}\"}[10m])) or vector(0)) - (sum(last_over_time({{bkrepo_table}}:bkrepo_repository_size_bytes{projectId=\"{{projectId}}\"}[10m] offset ${timeStamp}s))) or vector(0))`
}

export const deletedArtifactsUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&from=${from}&to=${to}`
}

export const avgUploadSpeed =(timeStamp)=>{
    return `avg(avg_over_time({{bkrepo_table}}:artifact_transfer_rate{projectId=\"{{projectId}}\", type=\"RECEIVE\"}[${timeStamp}s])) or vector(0)`
}

export const avgUploadSpeedUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&viewPanel=34&from=${from}&to=${to}`
}

export const avgDownloadSpeed =(timeStamp)=>{
    return `avg(avg_over_time({{bkrepo_table}}:artifact_transfer_rate{projectId=\"{{projectId}}\", type=\"RESPONSE\"}[${timeStamp}s])) or vector(0)`
}

export const avgDownloadSpeedUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&viewPanel=34&from=${from}&to=${to}`
}

export const uploadArtifacts = (timeStamp)=>{
    return `count(count_over_time({{bkrepo_table}}:artifact_transfer_rate{projectId=\"{{projectId}}\", type=\"RECEIVE\"}[${timeStamp}s])) or vector(0)`
}

export const uploadArtifactsUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&viewPanel=36&from=${from}&to=${to}`
}

export const downloadArtifacts = (timeStamp)=>{
    return `count(count_over_time({{bkrepo_table}}:artifact_transfer_rate{projectId=\"{{projectId}}\", type=\"RESPONSE\"}[${timeStamp}s])) or vector(0)`
}

export const downloadArtifactsUrl =(projectId, from, to)=>{
    return `spaceUid=bkci__${projectId}&dashName=BKCI-制品趋势&viewPanel=35&from=${from}&to=${to}`
}

export const goMonitorBoard = (projectId, from, to) => {
    return `spaceUid=bkci__${projectId}&dashName=BKCI-运行中的任务&from=${from}&to=${to}`
}

export const buildNodesUrl =(projectId,status)=>{
    return `${jumpUrlPostBuildResource}/${projectId}/pipeline/node/THIRDPARTY?nodeStatus=${status}`
}

export const urlMap = {

    runningPipelines: runningPipelinesUrl,

    waitingPipelines: waitingPipelinesUrl,

    waitingJob: waitingJobUrl,

    auditPipelines: auditPipelinesUrl,

    failuresNum: failuresNumUrl,

    cancelNum: cancelNumUrl,

    successNum: successNumUrl,

    successRate: successRateUrl,

    sourceCpu: sourceCpuUrl,

    sourceMemory: sourceMemoryUrl,

    sourceDisk: sourceDiskUrl,
    
    totalArtifacts: totalArtifactsUrl,

    addedArtifacts: addedArtifactsUrl,

    deletedArtifacts: deletedArtifactsUrl,

    avgUploadSpeed: avgUploadSpeedUrl,

    avgDownloadSpeed: avgDownloadSpeedUrl,
    
    uploadArtifacts: uploadArtifactsUrl,

    downloadArtifacts: downloadArtifactsUrl,

    goMonitorBoard: goMonitorBoard,



}