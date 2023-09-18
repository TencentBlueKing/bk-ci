
const pipelineConstMixin = {
    data () {
        return {
            BUILD_HISTORY_TABLE_COLUMNS_MAP: {
                buildNum: {
                    index: 0,
                    id: 'buildNum',
                    label: this.$t('buildNum'),
                    width: 120
                },
                stageStatus: {
                    index: 1,
                    id: 'stageStatus',
                    label: this.$t('history.stageStatus'),
                    width: localStorage.getItem('stageStatusWidth') ? localStorage.getItem('stageStatusWidth') : 520
                },
                material: {
                    index: 2,
                    id: 'material',
                    label: this.$t('editPage.material'),
                    width: localStorage.getItem('materialWidth') ? localStorage.getItem('materialWidth') : 500
                },
                startType: {
                    index: 3,
                    id: 'startType',
                    label: this.$t('history.triggerType'),
                    width: 120
                },
                queueTime: {
                    index: 4,
                    id: 'queueTime',
                    label: this.$t('history.tableMap.queueTime'),
                    width: 120
                },
                startTime: {
                    index: 5,
                    id: 'startTime',
                    label: this.$t('history.tableMap.startTime'),
                    width: 120
                },
                endTime: {
                    index: 6,
                    id: 'endTime',
                    label: this.$t('history.tableMap.endTime'),
                    width: 120
                },
                executeTime: {
                    index: 7,
                    id: 'executeTime',
                    label: this.$t('duration')
                },
                artifactList: {
                    index: 8,
                    id: 'artifactList',
                    label: this.$t('history.artifactList'),
                    width: 180
                },
                appVersions: {
                    index: 9,
                    id: 'appVersions',
                    label: this.$t('history.tableMap.appVersions')
                },
                remark: {
                    index: 10,
                    id: 'remark',
                    label: this.$t('history.remark'),
                    minWidth: 160
                },
                recommendVersion: {
                    index: 11,
                    id: 'recommendVersion',
                    label: this.$t('history.tableMap.recommendVersion')
                },
                pipelineVersion: {
                    index: 12,
                    id: 'pipelineVersion',
                    label: this.$t('history.tableMap.pipelineVersion')
                },
                entry: {
                    index: 12,
                    id: 'entry',
                    label: this.$t('history.tableMap.entry'),
                    width: 120,
                    hiddenInHistory: true,
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
                },
                errorCode: {
                    index: 13,
                    width: 280,
                    id: 'errorCode',
                    label: this.$t('history.errorCode')
                }
            }
        }
    },
    computed: {
        customColumn () {
            return ['material', 'stageStatus', 'errorCode']
        },
        sourceColumns () {
            const historyTableColumns = Object.values(this.BUILD_HISTORY_TABLE_COLUMNS_MAP).sort((c1, c2) => c1.index > c2.index)
            return historyTableColumns.filter(x => !x.hiddenInHistory)
        }
    }
}

export default pipelineConstMixin
