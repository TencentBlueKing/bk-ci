export default {
    props: {
        type: {
            type: String,
            default: 'file'
        },
        list: {
            type: Array,
            default: []
        },
        screenHeight: {
            type: Number,
            default: 336
        },
        fileIndex: {
            type: Number,
            default: 0
        },
        handleFileListRowClick: {
            type: Function
        },
        handleMark: {
            type: Function
        },
        handleIgnore: {
            type: Function
        },
        handleAuthor: {
            type: Function
        },
        handleSortChange: {
            type: Function
        },
        handleSelectionChange: {
            type: Function
        },
        toSelectAll: {
            type: Function
        }
    },
    data () {
        return {
            defectSeverityMap: {
                1: this.$t('严重'),
                2: this.$t('一般'),
                4: this.$t('提示')
            },
            hoverAuthorIndex: -1
        }
    },
    methods: {
        handleStatus (status) {
            let key = 1
            if (status === 1) {
                key = 1
            } else if (status & 2) {
                key = 2
            } else if (status & 4) {
                key = 4
            }
            const statusMap = {
                1: this.$t('待修复'),
                2: this.$t('已修复'),
                4: this.$t('已忽略')
            }
            return statusMap[key]
        },
        handleAuthorIndex (index) {
            this.hoverAuthorIndex = index
        },
        handleRowClassName ({ row, rowIndex }) {
            let rowClass = 'list-row'
            if (this.fileIndex === rowIndex) rowClass += ' current-row'
            return rowClass
        },
        formatSeverity (list = []) {
            const severityList = list.map(item => {
                return this.defectSeverityMap[item]
            })
            return severityList.join('、')
        },
        handleSelectable (row, index) {
            return !(row.status & 2)
        }
    }
}
