<template>
    <div>
        <bk-sideslider quick-close="true" :title="$t('操作记录')" width="776" :is-show.sync="visiable" @hidden="closeSlider">
            <div class="slider-content" slot="content">
                <bk-table
                    style="margin-top: 15px;"
                    :data="operateRecords instanceof Array ? operateRecords.slice((pagination.current - 1) * pagination.limit, pagination.current * pagination.limit) : ''"
                    :size="size"
                >
                    <bk-table-column type="index" :label="$t('序号')" align="center" width="70"></bk-table-column>
                    <bk-table-column :label="$t('操作人')" prop="operator" width="100"></bk-table-column>
                    <bk-table-column :label="$t('操作类型')" prop="operTypeName" width="125"></bk-table-column>
                    <bk-table-column :label="$t('相关信息')" prop="operMsg">
                        <template slot-scope="props"> <span class="operMsg" :title="props.row.operMsg">{{ props.row.operMsg }}</span></template>
                    </bk-table-column>
                    <bk-table-column :label="$t('操作时间')" prop="time" width="200"></bk-table-column>
                    <div slot="empty">
                        <div class="codecc-table-empty-text">
                            <img src="../../images/empty.png" class="empty-img">
                            <div>{{$t('暂无操作记录')}}</div>
                        </div>
                    </div>
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
            },
            funcId: {
                type: Array,
                default: []
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
            })
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
                for (const i in this.operateRecords) {
                    this.operateRecords[i].time = format(this.operateRecords[i].time, 'YYYY-MM-DD HH:mm:ss')
                    this.pagination.count = this.operateRecords.length
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
    .operMsg {
        display: block;
        overflow: hidden;
        text-overflow:ellipsis;
        white-space: nowrap;
    }
    .bk-sideslider-content div {
    }
    .bk-sideslider-content #logContainer, .bk-sideslider-content div {
    }
</style>
