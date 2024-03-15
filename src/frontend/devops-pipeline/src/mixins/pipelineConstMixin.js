const pipelineConstMixin = {
    data () {
        return {
            BUILD_HISTORY_TABLE_COLUMNS_MAP: {
                buildNum: {
                    index: 0,
                    prop: 'buildNum',
                    label: this.$t('buildNum'),
                    width: localStorage.getItem('buildNumWidth') ? localStorage.getItem('buildNumWidth') : 120
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
                    width: localStorage.getItem('triggerTypeWidth') ? localStorage.getItem('triggerTypeWidth') : 120
                },
                queueTime: {
                    index: 4,
                    prop: 'queueTime',
                    label: this.$t('history.tableMap.queueTime'),
                    width: localStorage.getItem('queueTimeWidth') ? localStorage.getItem('queueTimeWidth') : 120
                },
                startTime: {
                    index: 5,
                    prop: 'startTime',
                    label: this.$t('history.tableMap.startTime'),
                    width: localStorage.getItem('startTimeWidth') ? localStorage.getItem('startTimeWidth') : 120
                },
                endTime: {
                    index: 6,
                    prop: 'endTime',
                    label: this.$t('history.tableMap.endTime'),
                    width: localStorage.getItem('endTimeWidth') ? localStorage.getItem('endTimeWidth') : 120
                },
                executeTime: {
                    index: 7,
                    prop: 'executeTime',
                    label: this.$t('details.totalCost'),
                    width: localStorage.getItem('executeTimeWidth') ? localStorage.getItem('executeTimeWidth') : 120
                },
                artifactList: {
                    index: 8,
                    prop: 'artifactList',
                    label: this.$t('history.artifactList'),
                    width: localStorage.getItem('artifactListWidth') ? localStorage.getItem('artifactListWidth') : 180
                },
                appVersions: {
                    index: 9,
                    prop: 'appVersions',
                    label: this.$t('history.tableMap.appVersions'),
                    width: localStorage.getItem('appVersionsWidth') ? localStorage.getItem('appVersionsWidth') : 120
                },
                remark: {
                    index: 10,
                    prop: 'remark',
                    label: this.$t('history.remark'),
                    width: localStorage.getItem('remarkWidth') ? localStorage.getItem('remarkWidth') : 120
                },
                recommendVersion: {
                    index: 11,
                    prop: 'recommendVersion',
                    label: this.$t('history.tableMap.recommendVersion'),
                    width: localStorage.getItem('recommendVersionWidth') ? localStorage.getItem('recommendVersionWidth') : 120
                },
                pipelineVersion: {
                    index: 12,
                    prop: 'pipelineVersion',
                    label: this.$t('history.tableMap.pipelineVersion'),
                    width: localStorage.getItem('pipelineVersionWidth') ? localStorage.getItem('pipelineVersionWidth') : 120
                },
                entry: {
                    index: 12,
                    prop: 'entry',
                    label: this.$t('history.tableMap.entry'),
                    width: localStorage.getItem('entryWidth') ? localStorage.getItem('entryWidth') : 120,
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
                    width: localStorage.getItem('errorCodeWidth') ? localStorage.getItem('errorCodeWidth') : 280,
                    prop: 'errorCode',
                    label: this.$t('history.errorCode')
                }
            }
        }
    }
}

export default pipelineConstMixin
