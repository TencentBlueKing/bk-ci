const pipelineConstMixin = {
    data () {
        return {
            statusMap: {
                RUNNING: this.$t('details.statusMap.RUNNING'),
                PREPARE_ENV: this.$t('details.statusMap.PREPARE_ENV'),
                CANCELED: this.$t('details.statusMap.CANCELED'),
                FAILED: this.$t('details.statusMap.FAILED'),
                SUCCEED: this.$t('details.statusMap.SUCCEED'),
                REVIEW_ABORT: this.$t('details.statusMap.REVIEW_ABORT'),
                HEARTBEAT_TIMEOUT: this.$t('details.statusMap.HEARTBEAT_TIMEOUT'),
                QUALITY_CHECK_FAIL: this.$t('details.statusMap.QUALITY_CHECK_FAIL'),
                QUEUE: this.$t('details.statusMap.QUEUE'),
                QUEUE_TIMEOUT: this.$t('details.statusMap.QUEUE_TIMEOUT'),
                EXEC_TIMEOUT: this.$t('details.statusMap.EXEC_TIMEOUT')
            },
            BUILD_HISTORY_TABLE_COLUMNS_MAP: {
                buildNum: {
                    index: 0,
                    prop: 'buildNum',
                    label: this.$t('buildNum'),
                    width: 120
                },
                material: {
                    index: 1,
                    prop: 'material',
                    label: this.$t('editPage.material'),
                    width: localStorage.getItem('materialWidth') ? localStorage.getItem('materialWidth') : 500
                },
                startType: {
                    index: 2,
                    prop: 'startType',
                    label: this.$t('history.triggerType'),
                    width: 120
                },
                queueTime: {
                    index: 3,
                    prop: 'queueTime',
                    label: this.$t('history.tableMap.queueTime'),
                    width: 120
                },
                startTime: {
                    index: 4,
                    prop: 'startTime',
                    label: this.$t('history.tableMap.startTime'),
                    width: 120
                },
                endTime: {
                    index: 5,
                    prop: 'endTime',
                    label: this.$t('history.tableMap.endTime'),
                    width: 120
                },
                totalTime: {
                    index: 6,
                    prop: 'totalTime',
                    label: this.$t('history.tableMap.totalTime')
                },
                artifactList: {
                    index: 7,
                    prop: 'artifactList',
                    label: this.$t('history.artifactList'),
                    width: 180
                },
                appVersions: {
                    index: 8,
                    prop: 'appVersions',
                    label: this.$t('history.tableMap.appVersions')
                },
                remark: {
                    index: 9,
                    prop: 'remark',
                    label: this.$t('history.remark')
                },
                recommendVersion: {
                    index: 10,
                    prop: 'recommendVersion',
                    label: this.$t('history.tableMap.recommendVersion')
                },
                pipelineVersion: {
                    index: 11,
                    prop: 'pipelineVersion',
                    label: this.$t('history.tableMap.pipelineVersion')
                },
                entry: {
                    index: 12,
                    prop: 'entry',
                    label: this.$t('history.tableMap.entry'),
                    width: 120,
                    entries: [{
                        type: '',
                        label: this.$t('detail')

                    }, {
                        type: 'partView',
                        label: this.$t('details.partView')

                    }, {
                        type: 'codeRecords',
                        label: this.$t('details.codeRecords')
                    }, {
                        type: 'output',
                        label: this.$t('details.outputReport')
                    }]
                }
            }
        }
    }
}

export default pipelineConstMixin
