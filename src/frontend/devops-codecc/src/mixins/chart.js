import chartBarOption from '@/mixins/chart-bar-option'
import chartLineOption from '@/mixins/chart-line-option'
import echarts from 'echarts/lib/echarts'
import 'echarts/lib/chart/bar'
import 'echarts/lib/chart/line'
import 'echarts/lib/component/tooltip'
import 'echarts/lib/component/title'
import 'echarts/lib/component/legend'
import { mapState } from 'vuex'

export default {
    mixins: [chartBarOption, chartLineOption],
    data () {
        return {
            toolId: this.$route.params.toolId,
            active: 'report'
        }
    },
    computed: {
        ...mapState('tool', {
            toolMap: 'mapList'
        }),
        ...mapState('task', {
            taskDetail: 'detail'
        }),
        ...mapState([
            'toolMeta'
        ]),
        toolList () {
            const toolType = this.toolMeta['TOOL_TYPE'].filter(item => {
                return item.key !== 'CCN' && item.key !== 'DUPC' && item.key !== 'CLOC'
            })
            const enTooList = this.taskDetail.enableToolList.filter(item => {
                return item.toolName !== 'CCN' && item.toolName !== 'DUPC' && item.key !== 'CLOC'
            })
            if (this.toolMap) {
                toolType.forEach(item => {
                    item['toolList'] = []
                    enTooList.forEach(i => {
                        if (this.toolMap[i.toolName] && this.toolMap[i.toolName].type === item.key) {
                            item['toolList'].push(i)
                        }
                    })
                })
            }
            return toolType
        }
    },
    methods: {
        handleInitAuthor (authorTypeMap, authorType, list = {}) {
            const elemList = list.authorList || [{}]

            const authorName = elemList.map(item => item.authorName || '')
            const serious = elemList.map(item => item.serious || 0)
            const normal = elemList.map(item => item.normal || 0)
            const prompt = elemList.map(item => item.prompt || 0)

            elemList.push(list.totalAuthor)
            this[authorTypeMap[authorType]['data']] = elemList

            const option = {
                title: {
                    text: authorTypeMap[authorType]['title']
                },
                xAxis: {
                    data: authorName
                },
                yAxis: {
                    splitNumber: 4,
                    minInterval: 1
                },
                grid: {
                    left: '65'
                },
                series: [
                    {
                        data: prompt
                    },
                    {
                        data: normal
                    },
                    {
                        data: serious
                    }
                ]
            }
            this.handleChartOption(authorTypeMap[authorType]['chart'], option, 'chartBarOption')
        },
        handleChartOption (chartName, option, optionType) {
            this[chartName] && this[chartName].clear()
            this[chartName] = echarts.init(this.$refs[chartName])
            
            this[chartName].setOption(this[optionType])
            this[chartName].setOption(option)
            window.addEventListener('resize', () => {
                this[chartName].resize()
            })
        },
        resolveHref (name, query) {
            this.$router.push({
                name,
                query
            })
            // const resolved = this.$router.resolve({
            //     name,
            //     params: this.$route.params,
            //     query
            // })
            // const href = `${window.DEVOPS_SITE_URL}/console${resolved.href}`
            // window.open(href, '_blank')
        },
        handleSelectTool (toolName) {
            const tool = this.taskDetail.enableToolList.find(item => item.toolName === toolName)
            const toolPattern = tool.toolPattern.toLocaleLowerCase()
            this.$router.push({
                name: `defect-${toolPattern}-charts`,
                params: { ...this.$route.params, toolId: toolName }
            })
        },
        handleTableChange (value) {
            const toolName = this.toolId
            const toolPattern = this.toolMap[toolName].pattern.toLocaleLowerCase()
            const params = { ...this.$route.params, toolId: toolName }
            if (value === 'defect') {
                this.$router.push({
                    name: `defect-${toolPattern}-list`,
                    params
                })
            } else {
                this.$router.push({
                    name: `defect-${toolPattern}-charts`,
                    params
                })
            }
        }
    }
}
