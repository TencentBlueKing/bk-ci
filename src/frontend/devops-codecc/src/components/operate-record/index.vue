<template>
    <div>
        <bk-sideslider style="z-index: 4" quick-close="true" :title="$t('nav.操作记录')" width="776" :is-show.sync="visiable" @hidden="closeSlider">
            <div class="slider-content" slot="content">
                <bk-table
                    style="margin-top: 15px;"
                    :empty-text="$t('records.暂无操作记录')"
                    :data="operateRecords instanceof Array ? operateRecords.slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit) : ''"
                    :size="size"
                >
                    <bk-table-column type="index" :label="$t('records.序号')" align="center" width="70"></bk-table-column>
                    <bk-table-column :label="$t('records.操作人')" prop="operator" width="100"></bk-table-column>
                    <bk-table-column :label="$t('records.操作类型')" prop="operTypeName" width="125"></bk-table-column>
                    <bk-table-column :label="$t('records.相关信息')" prop="operMsg">
                        <template slot-scope="props"> <span :title="props.row.operMsg">{{ props.row.operMsg }}</span></template>
                    </bk-table-column>
                    <bk-table-column :label="$t('records.操作时间')" prop="time" width="200"></bk-table-column>
                </bk-table>
                <bk-pagination
                    class="pagination"
                    :current.sync="pagination.current"
                    :count="pagination.count"
                    :limit="pagination.limit"
                    size="small"
                    :align="pagination.align"
                    @change="handlePageChange"
                    @limit-change="limitChange"
                >
                </bk-pagination>
            </div>
        </bk-sideslider>
    </div>
</template>
<script>
    import { mapState } from 'vuex'
    import { format } from 'date-fns'
    export default {
        props: {
            data: {
                type: Object,
                default: {}
            },
            visiable: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    align: 'right'
                }
            }
        },
        computed: {
            ...mapState('defect', {
                operateRecords: 'records'
            }),
            realFuncId () {
                const realFuncId = []
                if (this.data) {
                    if (this.data.indexOf('task-settings-tools') === 0) {
                        realFuncId.push('register_tool', 'tool_switch')
                    } else if (this.data.indexOf('task-settings-basic') === 0) {
                        realFuncId.push('task_info')
                    } else if (this.data.indexOf('task-settings-manage') === 0) {
                        realFuncId.push('task_switch')
                    } else if (this.data.indexOf('task-settings-code') === 0) {
                        realFuncId.push('task_code')
                    } else if (this.data.indexOf('tool-rules') === 0) {
                        realFuncId.push('checker_config')
                    } else if (this.data.indexOf('task-settings-trigger') === 0) {
                        realFuncId.push('scan_schedule')
                    } else if (this.data.indexOf('task-settings-ignore') === 0) {
                        realFuncId.push('filter_path')
                    } else if (this.data.indexOf('-list') !== -1) {
                        realFuncId.push('defect_manage')
                    } else if (this.data.indexOf('task-detail') === 0) {
                        realFuncId.push('trigger_analysis')
                    }
                }
                return realFuncId
            }
        },
        watch: {
            'visiable': {
                handler (newVal, oldVal) {
                    this.init()
                }
            }
        },
        mounted () {
        },
        created () {
        },
        methods: {
            closeSlider () {
                this.$emit('update:visiable', false)
            },
            init () {
                window.scrollTo(0, 0)
                this.pagination = {
                    current: 1,
                    count: 0,
                    limit: 10,
                    align: 'right'
                }
                if (this.realFuncId.length > 0) {
                    const postData = {
                        taskId: this.$route.params.taskId,
                        funcId: this.realFuncId,
                        toolName: this.$route.params.toolId ? this.$route.params.toolId : ''
                    }
                    this.$store.dispatch('defect/getOperatreRecords', postData).then(res => {
                        if (res) {
                            for (const i in this.operateRecords) {
                                this.operateRecords[i].time = format(this.operateRecords[i].time, 'YYYY-MM-DD HH:mm:ss')
                                this.pagination.count = this.operateRecords.length
                            }
                        }
                    })
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
            },
            limitChange (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
            }
        }
    }
</script>
<style lang="postcss" scoped>
    .slider-content {
        height: auto;
        padding: 30px;
        .pagination {
            padding-top: 10px;
        }
    }
    .bk-sideslider-content div {
    }
    .bk-sideslider-content #logContainer, .bk-sideslider-content div {
    }
</style>
