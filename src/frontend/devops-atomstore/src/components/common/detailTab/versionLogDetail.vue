<template>
    <bk-table
        :data="versionList"
        :pagination="pagination"
        :outer-border="false"
        :max-height="460"
        v-bkloading="{ isLoading: isLoading }"
        @page-change="handlePageChange"
        @page-limit-change="handleLimitChange"
    >
        <bk-table-column
            type="expand"
            width="48"
        >
            <template slot-scope="{ row }">
                <div class="version-log">
                    <p>{{ $t('版本日志') }}</p>
                    <span>{{ row.updateLog || '--' }}</span>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('版本号')"
            prop="version"
        ></bk-table-column>
        <bk-table-column
            :label="$t('版本日志')"
            prop="updateLog"
        >
            <template slot-scope="{ row }">
                {{ row.updateLog || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            :label="$t('发布时间')"
            prop="lastUpdateTime"
            sortable
        ></bk-table-column>
        <bk-table-column
            :label="$t('发布人')"
            prop="publisher"
        ></bk-table-column>
        <bk-table-column
            label="Tag"
            prop="tag"
        ></bk-table-column>
        <bk-table-column
            :label="$t('包大小')"
            prop="packageSize"
        ></bk-table-column>
    </bk-table>
</template>

<script>
    import api from '@/api'

    export default {
        props: {
            name: String,
            currentTab: String
        },

        data () {
            return {
                isLoading: false,
                versionList: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
            }
        },

        computed: {
            type () {
                return this.$route.params.type
            },
            atomCode () {
                return this.$route.params.code
            },
            storeType () {
                const storeTypeMap = {
                    atom: 'ATOM',
                    template: 'TEMPLATE',
                    image: 'IMAGE'
                }
                return storeTypeMap[this.type]
            }
        },

        watch: {
            currentTab: {
                handler (currentVal) {
                    if (currentVal === this.name) {
                        this.pagination.current = 1
                        this.pagination.limit = 10
                        this.initVersionLog()
                    }
                },
                immediate: true
            }
        },

        methods: {
            async initVersionLog () {
                try {
                    this.isLoading = true
                    const params = {
                        page: this.pagination.current,
                        pageSize: this.pagination.limit
                    }
                    const res = await api.getVersionLogs(this.storeType, this.atomCode, params)
                    this.versionList = res.records
                    this.pagination.count = res.count
                } catch (error) {
                    this.$bkMessage({ theme: 'error', message: error.message || error })
                } finally {
                    this.isLoading = false
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.initVersionLog()
            },
            handleLimitChange (limit) {
                this.pagination.limit = limit
                this.initVersionLog()
            }
        }
    }
</script>

<style lang="scss" scoped>
.version-log {
    padding: 24px 48px;
    color: #4D4F56;

    p {
        font-weight: 700;
        font-size: 14px;
        line-height: 22px;
        margin-bottom: 6px;
    }

    span {
        font-size: 12px;
        line-height: 20px;
        white-space: pre-line;
    }
}
</style>
