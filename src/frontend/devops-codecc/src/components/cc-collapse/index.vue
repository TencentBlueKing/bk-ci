<template>
    <div class="cc-collapse">
        <bk-collapse v-model="activeName">
            <bk-collapse-item v-for="item in searchFormat" :key="item.name" :name="item.name">
                {{nameMap(item.name)}}
                <div slot="content">
                    <cc-collapse-item
                        :need-search="false"
                        :id="item.name"
                        :data="item.checkerCountList"
                        :max-length="item.maxLength"
                        :red-point="hasRedPoint[item.name]"
                        :selected="selectedList[item.name]"
                        @handleSelect="handleSelect">
                    </cc-collapse-item>
                </div>
            </bk-collapse-item>
        </bk-collapse>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ccCollapseItem from '@/components/cc-collapse-item'

    export default {
        name: 'cc-collapse',
        components: {
            ccCollapseItem
        },
        props: {
            isCkeckerSet: {
                type: Boolean,
                default: false
            },
            search: {
                type: Array,
                default: []
            },
            activeName: {
                type: Array,
                default: []
            },
            updateActiveName: {
                type: Function
            }
        },
        data () {
            return {
                selectedList: {},
                hasRedPoint: {
                    checkerCategory: window.localStorage.getItem('redtips-category-cloc-20200704') ? [] : ['CODE_STATISTIC']
                }
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            searchFormat () {
                const list = this.search.filter(item => item.name !== 'total')
                const selectedList = this.selectedList
                const severityMap = ['', '严重', '一般', '', '提示']
                const editableMap = {
                    'false': '不可修改',
                    'true': '可修改'
                }
                list.map(item => {
                    if (item.name === 'toolName') {
                        item.checkerCountList.map(count => {
                            count.name = this.toolMap[count.key] && this.toolMap[count.key].displayName
                        })
                    } else if (item.name === 'severity') {
                        item.checkerCountList.map(count => {
                            count.name = severityMap[count.key]
                        })
                    } else if (item.name === 'editable') {
                        item.checkerCountList.map(count => {
                            count.name = editableMap[count.key]
                        })
                    }
                    item.selectedList = selectedList[item.name] || []
                })
                return list
            },
            checkerSetQueryStorage () {
                return localStorage.getItem('checkerSetQueryObj') ? JSON.parse(localStorage.getItem('checkerSetQueryObj')) : {}
            }
        },
        watch: {
            activeName (newVal) {
                this.$emit('updateActiveName', newVal)
            }
        },
        created () {
            if (this.isCkeckerSet && Object.keys(this.checkerSetQueryStorage).length) {
                this.selectedList = this.checkerSetQueryStorage
                this.$emit('handleSelect', this.selectedList)
            }
        },
        methods: {
            nameMap (name) {
                const checkerMap = {
                    'checkerLanguage': '适用语言',
                    'checkerCategory': '类别',
                    'toolName': '工具',
                    'tag': '标签',
                    'severity': '严重级别',
                    'editable': '参数策略',
                    'checkerRecommend': '来源',
                    'checkerSetSelected': '使用状态'
                }
                const checkerSetMap = {
                    'checkerSetLanguage': '适用语言',
                    'checkerSetCategory': '类别',
                    'toolName': '工具',
                    'checkerSetSource': '来源'
                }
                return this.isCkeckerSet ? checkerSetMap[name] : checkerMap[name]
            },
            handleSelect (value, id) {
                if (id === 'checkerCategory' && value === 'CODE_STATISTIC') {
                    window.localStorage.setItem('redtips-category-cloc-20200704', '1')
                    this.hasRedPoint = {}
                }
                let selected = this.selectedList[id] || []
                if (selected.includes(value)) {
                    selected = selected.filter(item => item !== value)
                } else {
                    selected.push(value)
                }
                const selectedList = {}
                selectedList[id] = selected
                this.selectedList = Object.assign({}, this.selectedList, selectedList)
                this.$emit('handleSelect', this.selectedList)
            },
            handleClear () {
                this.selectedList = {}
            }
        }
    }
</script>

<style lang="postcss" scoped>
    >>>.bk-collapse-item .bk-collapse-item-header {
        color: #313238;
    }
</style>
