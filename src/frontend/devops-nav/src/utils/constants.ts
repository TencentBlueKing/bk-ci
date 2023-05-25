export const titlesMap = {
    pipeline: 'documentTitlePipeline',
    codelib: 'documentTitleCodelib',
    artifactory: 'documentTitleArtifactory',
    codecc: 'documentTitleCodecc',
    experience: 'documentTitleExperience',
    turbo: 'documentTitleTurbo',
    repo: 'documentTitleRepo',
    preci: 'documentTitlePreci',
    stream: 'documentTitleStream',
    wetest: 'documentTitleWetest',
    quality: 'documentTitleQuality',
    xinghai: 'documentTitleXinghai',
    bcs: 'documentTitleBcs',
    job: 'documentTitleJob',
    environment: 'documentTitleEnvironment',
    vs: 'documentTitleVs',
    apk: 'documentTitleApk',
    monitor: 'documentTitleMonitor',
    perm: 'documentTitlePerm',
    ticket: 'documentTitleTicket',
    store: 'documentTitleStore',
    metrics: 'documentTitleMetrics'
}
export function mapDocumnetTitle (service) {
    return titlesMap[service] || 'documentTitleHome'
}
