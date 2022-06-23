<template>
    <section class="show-version g-scroll-table">
        <bk-button theme="primary"
            class="version-button"
            :disabled="disableAddVersion"
            @click="$router.push({
                name: 'editService',
                params: {
                    serviceId: versionList[0].serviceId
                }
            })"
        > {{ $t('store.新增版本') }} </bk-button>
        <bk-table :data="versionList"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            :pagination="pagination"
            @page-change="(page) => $emit('pageChanged', page)"
            @page-limit-change="(currentLimit, prevLimit) => $emit('pageLimitChanged', currentLimit, prevLimit)"
        >
            <bk-table-column :label="$t('store.版本')" prop="version" :formatter="versionFormatter"></bk-table-column>
            <bk-table-column :label="$t('store.状态')" prop="serviceStatus" :formatter="statusFormatter"></bk-table-column>
            <bk-table-column :label="$t('store.创建人')" prop="creator"></bk-table-column>
            <bk-table-column :label="$t('store.创建时间')" prop="createTime" :formatter="convertTime"></bk-table-column>
            <bk-table-column :label="$t('store.操作')" width="150" class-name="handler-btn">
                <template slot-scope="props">
                    <section v-show="!index">
                        <span class="update-btn" @click="showDetail(props.row.serviceId)">{{ $t('store.查看') }}</span>
                        <span class="update-btn"
                            v-if="props.row.serviceStatus === 'INIT'"
                            @click="$router.push({ name: 'editService', params: { serviceId: props.row.serviceId } })"
                        > {{ $t('store.上架') }} </span>
                        <span class="update-btn"
                            v-if="progressStatus.indexOf(props.row.serviceStatus) > -1"
                            @click="$router.push({ name: 'serviceProgress', params: { serviceId: props.row.serviceId } })"
                        > {{ $t('store.进度') }} </span>
                    </section>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider quick-close
            class="offline-atom-slider"
            :is-show.sync="hasShowDetail"
            :title="$t('store.查看详情')"
            :width="800">
            <template slot="content">
                <service-detail :detail="detail" v-bkloading="{ isLoading: detailLoading }" class="version-detail">
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.发布者：') }}：</span>
                        <span>{{ detail.publisher || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.发布类型：') }}：</span>
                        <span>{{ releaseMap[detail.releaseType] || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.版本：') }}：</span>
                        <span>{{ detail.version || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.版本日志') }}：</span>
                        <mavon-editor
                            :editable="false"
                            default-open="preview"
                            :subfield="false"
                            :toolbars-flag="false"
                            :external-link="false"
                            :box-shadow="false"
                            preview-background="#fff"
                            v-model="detail.versionContent"
                        />
                    </li>
                </service-detail>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { serviceStatusMap } from '@/store/constants'
    import { convertTime } from '@/utils/index'
    import serviceDetail from '../../detail/service-detail/show.vue'

    export default {
        components: {
            serviceDetail
        },

        props: {
            versionList: Array,
            pagination: Object
        },

        data () {
            return {
                progressStatus: ['AUDITING', 'COMMITTING', 'BUILDING', 'EDIT', 'BUILD_FAIL', 'TESTING', 'RELEASE_DEPLOYING', 'RELEASE_DEPLOY_FAIL'],
                upgradeStatus: ['INIT', 'UNDERCARRIAGED', 'AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
                hasShowDetail: false,
                detailLoading: false,
                detail: {},
                releaseMap: {
                    NEW: this.$t('store.新上架'),
                    INCOMPATIBILITY_UPGRADE: this.$t('store.非兼容式升级'),
                    COMPATIBILITY_UPGRADE: this.$t('store.兼容式功能更新'),
                    COMPATIBILITY_FIX: this.$t('store.兼容式问题修正')
                }
            }
        },

        computed: {
            disableAddVersion () {
                const firstVersion = this.versionList[0] || {}
                return this.upgradeStatus.indexOf(firstVersion.serviceStatus) === -1
            }
        },

        methods: {
            showDetail (serviceId) {
                this.hasShowDetail = true
                this.detailLoading = true
                this.$store.dispatch('store/requestServiceDetail', serviceId).then((res) => {
                    this.detail = res || {}
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.detailLoading = false
                })
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(serviceStatusMap[cellValue])
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            },

            versionFormatter (row, column, cellValue, index) {
                return cellValue || 'init'
            }
        }
    }
</script>

<style lang="scss" scoped>
    .version-detail {
        padding: 20px;
    }
</style>
