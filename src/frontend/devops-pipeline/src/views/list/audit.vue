<template>
    <div class="p20 pipeline-audit-list" v-bkloading="{ isLoading }">
        <div class="audit-list-header">
            <bk-form form-type="inline">
                <bk-form-item :label="$t('pipelineId')">
                    <bk-input
                        v-model="parameters.resourceId"
                        class="w220"
                        @enter="handlepPaginationChange"
                        :placeholder="$t('audit.enterPlaceholder')">
                    </bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('pipelineName')">
                    <bk-input
                        v-model="parameters.resourceName"
                        class="w220"
                        @enter="handlepPaginationChange"
                        :placeholder="$t('audit.enterPlaceholder')">
                    </bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('audit.operator')">
                    <bk-input
                        v-model="parameters.userId"
                        class="w220"
                        @enter="handlepPaginationChange"
                        :placeholder="$t('audit.enterPlaceholder')">
                    </bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('audit.operateTime')">
                    <bk-date-picker
                        v-model="parameters.date"
                        class="w220"
                        @change="handlepPaginationChange"
                        type="daterange"
                        :shortcuts="shortcuts"
                        :options="{
                            disabledDate: time => time.getTime() > Date.now()
                        }"
                        :placeholder="$t('audit.operateTimePlaceholder')">
                    </bk-date-picker>
                </bk-form-item>
                <!-- <bk-form-item :label="$t('status')">
                    <bk-select
                        v-model="parameters.status"
                        class="w220"
                        @change="handlepPaginationChange">
                        <bk-option :id="1" :name="$t('success')"></bk-option>
                        <bk-option :id="0" :name="$t('fail')"></bk-option>
                    </bk-select>
                </bk-form-item> -->
            </bk-form>
        </div>
        <bk-table
            class="mt20"
            height="calc(100% - 54px)"
            :data="auditData"
            :pagination="pagination"
            @page-change="current => handlepPaginationChange({ current })"
            @page-limit-change="limit => handlepPaginationChange({ limit })">
            <bk-table-column :label="$t('pipelineId')" prop="resourceId"></bk-table-column>
            <bk-table-column :label="$t('pipelineName')" prop="resourceName"></bk-table-column>
            <bk-table-column :label="$t('operate')" prop="actionContent"></bk-table-column>
            <bk-table-column :label="$t('audit.operator')" prop="userId"></bk-table-column>
            <bk-table-column :label="$t('audit.operateTime')" :prop="'updatedTime'">
                <template slot-scope="props">
                    {{ convertTime(props.row.updatedTime * 1000) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('status')" prop="status"></bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import { convertTime } from '@/utils/util'
    export default {
        data () {
            const query = this.$route.query
            return {
                isLoading: false,
                exportLoading: false,
                parameters: {
                    userId: '',
                    resourceId: query.resourceId || '',
                    resourceName: query.resourceName || '',
                    status: '',
                    date: [query.startTime || '', query.endTime || '']
                },
                auditData: [],
                shortcuts: [
                    {
                        text: this.$t('history.today'),
                        value () {
                            return [new Date(), new Date()]
                        }
                    },
                    {
                        text: this.$t('history.last7days'),
                        value () {
                            return [new Date(new Date().getTime() - 3600 * 1000 * 24 * 7), new Date()]
                        }
                    },
                    {
                        text: this.$t('history.last15days'),
                        value () {
                            return [new Date(new Date().getTime() - 3600 * 1000 * 24 * 15), new Date()]
                        }
                    },
                    {
                        text: this.$t('history.last30days'),
                        value () {
                            return [new Date(new Date().getTime() - 3600 * 1000 * 24 * 30), new Date()]
                        }
                    }
                ],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [20, 40, 80]
                }
            }
        },
        created () {
            this.getAuditData()
        },
        methods: {
            convertTime,
            ...mapActions('pipelines', ['getUserAudit']),
            async getAuditData () {
                this.isLoading = true
                this.getUserAudit({
                    projectId: this.$route.params.projectId,
                    userId: this.parameters.userId,
                    resourceName: this.parameters.resourceName,
                    resourceId: this.parameters.resourceId,
                    startTime: this.parameters.date[0] && this.convertTime(this.parameters.date[0]),
                    endTime: this.parameters.date[1] && this.convertTime(this.parameters.date[1]),
                    status: this.parameters.status,
                    current: this.pagination.current,
                    limit: this.pagination.limit
                }).then(res => {
                    this.auditData = res.records
                    this.pagination.count = res.count
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handlepPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getAuditData()
            }
        }
    }
</script>

<style lang='scss'>
    .pipeline-audit-list {
        height: calc(100% - 60px);
        .w220 {
            width: 220px;
        }
        .audit-list-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
    }
</style>
