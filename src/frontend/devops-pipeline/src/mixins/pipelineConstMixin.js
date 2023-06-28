const pipelineConstMixin = {
    data () {
        return {
            BUILD_HISTORY_TABLE_COLUMNS_MAP: {
                buildNum: {
                    index: 0,
                    prop: 'buildNum',
                    label: this.$t('buildNum'),
                    width: 120
                },
                stageStatus: {
                    index: 1,
                    prop: 'stageStatus',
                    label: this.$t('history.stageStatus'),
                    width: localStorage.getItem('stageStatusWidth') ? localStorage.getItem('stageStatusWidth') : 520
                },
                material: {
                    index: 2,
                    prop: 'material',
                    label: this.$t('editPage.material'),
                    width: localStorage.getItem('materialWidth') ? localStorage.getItem('materialWidth') : 500
                },
                startType: {
                    index: 3,
                    prop: 'startType',
                    label: this.$t('history.triggerType'),
                    width: 120
                },
                queueTime: {
                    index: 4,
                    prop: 'queueTime',
                    label: this.$t('history.tableMap.queueTime'),
                    width: 120
                },
                startTime: {
                    index: 5,
                    prop: 'startTime',
                    label: this.$t('history.tableMap.startTime'),
                    width: 120
                },
                endTime: {
                    index: 6,
                    prop: 'endTime',
                    label: this.$t('history.tableMap.endTime'),
                    width: 120
                },
                executeTime: {
                    index: 7,
                    prop: 'executeTime',
                    label: this.$t('duration')
                },
                artifactList: {
                    index: 8,
                    prop: 'artifactList',
                    label: this.$t('history.artifactList'),
                    width: 180
                },
                appVersions: {
                    index: 9,
                    prop: 'appVersions',
                    label: this.$t('history.tableMap.appVersions')
                },
                remark: {
                    index: 10,
                    prop: 'remark',
                    label: this.$t('history.remark')
                },
                recommendVersion: {
                    index: 11,
                    prop: 'recommendVersion',
                    label: this.$t('history.tableMap.recommendVersion')
                },
                pipelineVersion: {
                    index: 12,
                    prop: 'pipelineVersion',
                    label: this.$t('history.tableMap.pipelineVersion')
                },
                entry: {
                    index: 12,
                    prop: 'entry',
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
                    prop: 'errorCode',
                    label: this.$t('history.errorCode')
                }
            }
        }
    },
    computed: {
        customColumn () {
            return ['material', 'stageStatus', 'errorCode']
        }
    }
}

export default pipelineConstMixin
