<template>
    <article class="atom-manage-home" v-bkloading="{ isLoading }">
        <h3 class="atom-manage-title">
            {{ $t('atomManage.installedAtom') }}
            <span @click="goToStore">{{ $t('atomManage.moreAtom') }}</span>
        </h3>
        <bk-tab :active.sync="active" class="atom-manage-main" @tab-change="tabChange">
            <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
                <template slot="label">
                    <span>{{ panel.label }}</span>
                </template>
                <bk-table :data="atomList" size="large" :empty-text="$t('noData')" :show-header="false">
                    <bk-table-column prop="logoUrl" width="80">
                        <template slot-scope="props">
                            <img class="atom-logo" :src="props.row.logoUrl" v-if="props.row.logoUrl">
                            <logo class="atom-logo" v-else name="placeholder" size="38" style="fill:#C3CDD7" />
                        </template>
                    </bk-table-column>
                    <bk-table-column class-name="atom-manage-des">
                        <template slot-scope="props">
                            <h5 class="text-overflow" :title="props.row.name">{{ props.row.name }}</h5>
                            <span class="text-overflow" :title="props.row.summary" v-if="props.row.summary">{{ props.row.summary }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column prop="publisher" width="200"></bk-table-column>
                    <bk-table-column width="400">
                        <template slot-scope="props">
                            <span class="text-overflow" :title="getInstallInfo(props.row)">{{ getInstallInfo(props.row) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column class-name="primary-color" width="120">
                        <template slot-scope="props">
                            <bk-popover :content="$t('atomManage.relatedNumTips', [props.row.pipelineCnt])" placement="top">
                                <span @click="showDetail(props.row)" class="cursor-pointer">{{ props.row.pipelineCnt }}</span>
                            </bk-popover>
                        </template>
                    </bk-table-column>
                    <bk-table-column width="80" class-name="primary-color">
                        <template slot-scope="props">
                            <bk-button :title="!props.row.hasPermission ? $t('atomManage.installedAtom') : ''" :disabled="!props.row.hasPermission" class="cursor-pointer" theme="primary" text @click="showDeletaDialog(props.row)" v-if="!props.row.default">{{ $t('atomManage.uninstall') }}</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </bk-tab-panel>
        </bk-tab>

        <bk-pagination @change="pageChange"
            @limit-change="limitChange"
            :current.sync="defaultPaging.current"
            :count.sync="defaultPaging.count"
            :limit="defaultPaging.limit"
            class="atom-pagination">
        </bk-pagination>

        <bk-dialog v-model="deleteObj.showDialog" :title="`${$t('atomManage.uninstall')}${deleteObj.detail.name}：`" :close-icon="false" :width="538" @confirm="deleteAtom" @cancel="clearReason">
            <span class="choose-reason-title">{{ $t('atomManage.uninstallReason') }}</span>
            <bk-checkbox-group v-model="deleteObj.reasonList">
                <bk-checkbox :value="reason.id" v-for="reason in deleteReasons" :key="reason.id" class="delete-reasons">{{reason.content}}</bk-checkbox>
            </bk-checkbox-group>
            <template v-if="showOtherReason">
                <span class="other-reason">{{ $t('atomManage.otherReason') }}：</span>
                <textarea class="reason-text" v-model="deleteObj.otherStr"></textarea>
            </template>
        </bk-dialog>

        <bk-sideslider :is-show.sync="detailObj.showSlide" :title="detailObj.detail.name" :width="644" :quick-close="true">
            <section slot="content" class="atom-slide">
                <hgroup class="slide-title">
                    <h5 class="slide-link">
                        <span>{{ $t('name') }}：</span>
                        <span class="text-overflow link-width">{{detailObj.detail.name}}</span>
                        <logo class="logo-link" name="loadout" size="14" style="fill:#3C96FF" @click.native="goToStoreDetail(detailObj.detail.atomCode)" />
                    </h5>
                    <h5><span>{{ $t('atomManage.publisher') }}：</span>{{detailObj.detail.publisher}}</h5>
                    <h5><span>{{ $t('atomManage.installer') }}：</span>{{detailObj.detail.installer}}</h5>
                    <h5><span>{{ $t('atomManage.installTime') }}：</span>{{detailObj.detail.installTime}}</h5>
                    <h5 class="slide-summary"><span>{{ $t('atomManage.summary') }}：</span><span>{{detailObj.detail.summary}}</span></h5>
                </hgroup>

                <h5 class="related-pipeline">{{ $t('atomManage.relatedPipeline') }}（{{detailObj.list && detailObj.list.length}}）</h5>
                <bk-table :data="detailObj.list" :empty-text="$t('noReleatedPipeline')">
                    <bk-table-column :label="$t('pipelineName')" prop="pipelineName" width="235">
                        <template slot-scope="props">
                            <h3 class="slide-link">
                                <span @click="goToPipeline(props.row.pipelineId)" class="link-text text-overflow" :title="props.row.pipelineName">{{ props.row.pipelineName }}</span>
                            </h3>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('lastExecUser')" width="180">
                        <template slot-scope="props">
                            <span>{{ props.row.owner || '-' }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('lastExecTime')" width="180">
                        <template slot-scope="props">
                            <span>{{ props.row.latestExecTime || '-' }}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </bk-sideslider>
    </article>
</template>

<script>
    import Logo from '@/components/Logo'
    import { mapActions } from 'vuex'

    export default {
        components: {
            Logo
        },

        data () {
            return {
                panels: [],
                active: 'all',
                atomList: [],
                deleteObj: {
                    showDialog: false,
                    detail: {},
                    otherStr: '',
                    reasonList: []
                },
                deleteReasons: [],
                detailObj: {
                    showSlide: false,
                    detail: {},
                    list: []
                },
                isLoading: false,
                defaultPaging: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },

            showOtherReason () {
                const lastReason = this.deleteReasons.slice(-1)[0] || {}
                const otherId = lastReason.id
                const selectIds = this.deleteObj.reasonList || []
                return selectIds.includes(otherId)
            }
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions('atom', ['getInstallAtomList', 'getInstallAtomDetail', 'unInstallAtom', 'getDeleteReasons', 'getAtomClassify']),

            async initData () {
                this.isLoading = true
                await this.fetchAtomList()
                Promise.all([this.getAtomClassify(), this.getDeleteReasons()])
                    .then(([{ data: classifyList }, { data: reasons }]) => {
                        this.deleteReasons = reasons || []
                        this.panels = (classifyList || []).map(item => {
                            item.name = item.classifyCode
                            item.label = item.classifyName
                            return item
                        })
                        this.panels.unshift({ name: 'all', label: this.$t('atomManage.all') })
                    }).catch(err => this.$bkMessage({ theme: 'error', message: err.message })).finally(() => {
                        this.isLoading = false
                    })
            },

            fetchAtomList () {
                const classifyCode = this.active === 'all' ? '' : this.active
                return this.getInstallAtomList({
                    projectCode: this.projectId,
                    page: this.defaultPaging.current,
                    pageSize: this.defaultPaging.limit,
                    classifyCode: classifyCode
                }).then(res => {
                    const data = res.data || {}
                    this.atomList = data.records || []
                    this.defaultPaging.count = data.count || 0
                })
            },

            tabChange () {
                this.defaultPaging.current = 1
                this.fetchAtomList()
            },

            showDetail (row) {
                this.isLoading = true
                this.detailObj.detail = row
                this.getInstallAtomDetail({ projectCode: this.projectId, atomCode: row.atomCode }).then(({ data }) => {
                    this.detailObj.list = data.records || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message })
                    this.detailObj.list = []
                }).finally(() => {
                    this.isLoading = false
                    this.detailObj.showSlide = true
                })
            },

            showDeletaDialog (row) {
                this.deleteObj.detail = row
                this.deleteObj.showDialog = true
            },

            deleteAtom () {
                this.isLoading = true
                const lastReason = this.deleteReasons.slice(-1)[0] || {}
                const otherId = lastReason.id
                const selectIds = this.deleteObj.reasonList || []
                const reasonList = selectIds.map((reasonId) => {
                    let note = ''
                    if (reasonId === otherId) note = this.deleteObj.otherStr || ''
                    return { reasonId, note }
                })
                this.unInstallAtom({ projectCode: this.projectId, atomCode: this.deleteObj.detail.atomCode, reasonList }).then(() => {
                    this.$bkMessage({ theme: 'success', message: `${this.$t('atomManage.uninstall')}${this.deleteObj.detail.name}${this.$t('success')}` })
                    this.initData()
                }).catch(err => this.$bkMessage({ theme: 'error', message: err.message })).finally(() => {
                    this.isLoading = false
                    this.deleteObj.showDialog = false
                    this.clearReason()
                })
            },

            pageChange (page) {
                if (page) this.defaultPaging.current = page
                this.fetchAtomList()
            },

            limitChange (limit) {
                if (limit === this.defaultPaging.limit) return

                this.defaultPaging.limit = limit
                this.defaultPaging.current = 1
                this.fetchAtomList()
            },

            clearReason () {
                this.deleteObj.reasonList = []
                this.deleteObj.otherStr = ''
            },

            getInstallInfo (row) {
                let des = this.$t('atomManage.installedAt')
                if (row.default) des = this.$t('atomManage.createdAt')
                return `${row.installer} ${des} ${row.installTime}`
            },

            goToStoreDetail (atomCode) {
                window.open(`${WEB_URL_PREFIX}/store/atomStore/detail/atom/${atomCode}`, '_blank')
            },

            goToPipeline (pipelineId) {
                window.open(`${WEB_URL_PREFIX}/pipeline/${this.projectId}/${pipelineId}/edit`, '_blank')
            },

            goToStore () {
                window.open(`${WEB_URL_PREFIX}/store/market/home?pipeType=atom`, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../scss/conf';

    .atom-manage-home {
        min-height: calc(100% - 60px);
        background: #fff;
        padding-top: 24px;
        padding-bottom: 30px;
        width: auto !important;
        .atom-manage-title {
            margin: 0 150px 15px;
            margin-right: calc(150px - 100vw + 100%);
            min-width: 1000px;
            font-size: 16px;
            font-weight: normal;
            span {
                float: right;
                color: $primaryColor;
                font-size: 14px;
                cursor: pointer;
            }
        }
        .atom-pagination {
            margin: 10px 150px;
            min-width: 1000px;
        }
        .atom-manage-main {
            margin: 0 150px;
            margin-right: calc(150px - 100vw + 100%);
            min-width: 1000px;
            .atom-panel-count {
                line-height: 16px;
                font-size: 12px;
                background: #C2CCD7;
                color: #fff;
                border-radius: 100px;
                padding: 0 4px;
                font-style: normal;
            }
            .bk-table.bk-table-fit.bk-table-outer-border {
                border: none;
                &:after, &:before {
                    background-color: white;
                }
            }
            ::v-deep .bk-tab-label:hover .atom-panel-count {
                background: #3a84ff;
            }
            ::v-deep .atom-manage-des {
                h5 {
                    margin-top: 0;
                    margin-bottom: 4px;
                    font-size: 14px;
                    line-height: 21px;
                    color: $fontWeightColor;
                }
                span {
                    color: $fontWeightColor;
                    line-height: 16px;
                    display: inline-block;
                    width: 100%;
                }
            }
            ::v-deep div.bk-tab-section {
                padding: 0;
                .bk-table-large .cell {
                    -webkit-line-clamp: 1;
                }
            }
            ::v-deep .bk-table-body-wrapper {
                max-height: calc(100vh - 250px);
                overflow-y: auto;
                .primary-color {
                    color: $primaryColor;
                }
            }
            ::v-deep .bk-table-body-wrapper .cursor-pointer {
                cursor: pointer;
            }
            ::v-deep thead tr th:first-child div {
                margin-left: 10px;
            }
            .atom-logo {
                height: 38px;
                width: 38px;
                margin-left: 10px;
            }
        }
        .atom-slide {
            padding: 0 24px;
            .slide-title {
                margin: 10px 0 30px;
                &::after {
                    content: '';
                    display: table;
                    clear: both;
                }
                h5 {
                    font-weight: normal;
                    margin: 0;
                    padding: 7px 0 8px;
                    width: 50%;
                    float: left;
                    font-size: 14px;
                    color: #4a4a4a;
                    line-height: 19px;
                    span:nth-child(1) {
                        display: inline-block;
                        width: 70px;
                        text-align: right;
                        margin-right: 14px;
                        color: $fontWeightColor;
                    }
                }
                .slide-summary {
                    display: flex;
                    width: 100%;
                    span:nth-child(1) {
                        min-width: 70px;
                    }
                    span:nth-child(2) {
                        width: auto;
                        text-align: left;
                    }
                }
            }
            .related-pipeline {
                margin: 0 0 14px;
            }
            .slide-link {
                display: flex;
                align-items: center;
                font-weight: normal;
                font-size: 12px;
                .link-width {
                    max-width: 200px;
                }
                .link-text {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .logo-link {
                margin-left: 4px;
                cursor: pointer;
            }
        }
    }

    .delete-reasons {
        display: block;
        padding: 6px 0;
        ::v-deep .bk-checkbox-text {
            font-size: 12px;
        }
    }

    .other-reason {
        font-size: 12px;
        line-height: 32px;
    }

    .reason-text {
        width: 498px;
        height: 36px;
        padding: 4px;
        line-height: 12px;
        font-size: 12px;
        border-radius: 2px;
        border: 1px solid $fontLighterColor;
        resize: none;
    }

    .choose-reason-title {
        display: inline-block;
        margin-bottom: 5px;
        height: 17px;
        font-size: 13px;
        color: $fontWeightColor;
        line-height: 17px;
    }
</style>
