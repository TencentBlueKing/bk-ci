<template>
    <article v-bkloading="{ isLoading }" class="service-progress-home">
        <bread-crumbs :bread-crumbs="navList" type="service">
            <a class="g-title-work" target="_blank" href="https://iwiki.oa.tencent.com/pages/viewpage.action?pageId=103523086"> {{ $t('store.微扩展指引') }} </a>
        </bread-crumbs>
        <article v-if="!isLoading" class="service-progress-main">
            <header class="progress-header">
                <bk-steps ext-cls="progress-steps" :status="currentStepStatus" :steps="progressStatus" :cur-step="currentStepIndex"></bk-steps>
                <bk-button class="progress-cancle" :loading="isCancleLoading" :disabled="isOver" @click="cancelRelease"> {{ $t('store.中止发布') }} </bk-button>
            </header>

            <section class="progress-main">
                <component :is="currentStep.code"
                    :store-build-info="storeBuildInfo"
                    :current-step="currentStep"
                    :detail="serviceDetail"
                    @freshProgress="getServiceProcess"
                    @loopProgress="loopProgress"
                ></component>
            </section>
        </article>

        <bk-sideslider :is-show.sync="isShowDetail"
            :title="$t('store.查看微扩展详情')"
            :quick-close="true"
            :width="640"
        >
            <template slot="content">
                <section class="progress-detail-content">
                    <h3 class="detail-title">
                        <img :src="serviceDetail.logoUrl">
                        <span>{{serviceDetail.serviceName}}</span>
                    </h3>
                    <bk-form label-width="80" :model="serviceDetail">
                        <bk-form-item :label="$t('store.微扩展标识') + '：'" property="serviceCode">
                            <span class="lh30">{{serviceDetail.serviceCode}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.功能标签') + '：'" property="labelList">
                            <h5 class="detail-label">
                                <span v-for="(label, index) in serviceDetail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                            </h5>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.扩展点') + '：'" property="itemName">
                            <span class="lh30">{{serviceDetail.itemName}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.简介') + '：'" property="summary">
                            <span class="lh30">{{serviceDetail.summary}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.详细描述') + '：'" property="description">
                            <p :class="{ 'overflow': !isDropdownShow }" ref="edit">
                                <mavon-editor class="image-remark-input"
                                    ref="mdHook"
                                    v-model="serviceDetail.description"
                                    :editable="false"
                                    :toolbars-flag="false"
                                    default-open="preview"
                                    :box-shadow="false"
                                    :subfield="false"
                                    preview-back-ground="#fafbfd"
                                />
                            </p>
                            <span class="toggle-btn" v-if="isOverflow" @click="isDropdownShow = !isDropdownShow">{{ isDropdownShow ? $t('store.收起') : $t('store.展开') }}
                                <i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                            </span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.发布者') + '：'" property="publisher">
                            <span class="lh30">{{serviceDetail.publisher}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.发布类型') + '：'" property="releaseType">
                            <span class="lh30">{{serviceDetail.releaseType | releaseTypeFilter}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.版本') + '：'" property="version">
                            <span class="lh30">{{serviceDetail.version}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('store.版本日志') + '：'" property="versionContent">
                            <span class="lh30">{{serviceDetail.versionContent}}</span>
                        </bk-form-item>
                    </bk-form>
                </section>
            </template>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import testEnvPrepare from '../components/common/progressSteps/test-env-prepare'
    import test from '../components/common/progressSteps/test'
    import commit from '../components/common/progressSteps/commit'
    import approve from '../components/common/progressSteps/approve'
    import begin from '../components/common/progressSteps/begin'
    import end from '../components/common/progressSteps/end'
    import online from '../components/common/progressSteps/online'

    export default {
        components: {
            breadCrumbs,
            testEnvPrepare,
            test,
            commit,
            approve,
            begin,
            end,
            online
        },

        filters: {
            releaseTypeFilter (val) {
                const local = window.devops || {}
                let res = ''
                switch (val) {
                    case 'NEW':
                        res = local.$t('store.新上架')
                        break
                    case 'INCOMPATIBILITY_UPGRADE':
                        res = local.$t('store.非兼容升级')
                        break
                    case 'COMPATIBILITY_UPGRADE':
                        res = local.$t('store.兼容式功能更新')
                        break
                    case 'COMPATIBILITY_FIX':
                        res = local.$t('store.兼容式问题修正')
                        break
                }
                return res
            }
        },

        data () {
            return {
                isLoading: false,
                isCancleLoading: false,
                progressStatus: [],
                currentStepIndex: 1,
                isOver: false,
                serviceDetail: {},
                storeBuildInfo: {},
                permission: true,
                currentProjectId: '',
                currentBuildNo: '',
                currentPipelineId: '',
                isShowDetail: false,
                isOverflow: false,
                isDropdownShow: false
            }
        },

        computed: {
            permissionMsg () {
                let str = ''
                if (!this.permission) str = this.$t('store.只有微扩展管理员或当前流程创建者可以操作')
                return str
            },

            currentStep () {
                return this.progressStatus[this.currentStepIndex - 1]
            },

            currentStepStatus () {
                let res
                const status = (this.currentStep || {}).status || ''
                switch (status) {
                    case 'fail':
                        res = 'error'
                        break
                    case 'doing':
                        res = 'loading'
                        break
                }
                return res
            },

            navList () {
                return [
                    { name: this.$t('store.工作台') },
                    { name: this.$t('store.微扩展'), to: { name: 'serviceWork' } },
                    { name: this.serviceDetail.serviceCode, to: { name: 'overView', params: { code: this.serviceDetail.serviceCode, type: 'service' } } },
                    { name: this.$t('store.上架/升级微扩展') }
                ]
            }
        },

        created () {
            this.initData()
            this.loopProgress()
        },

        beforeDestroy () {
            clearTimeout(this.loopProgress.timeId)
        },

        methods: {
            ...mapActions('store', [
                'requestServiceDetail',
                'requestServiceProcess',
                'requestServiceCancelRelease'
            ]),

            showDetail () {
                this.isShowDetail = true
                this.$nextTick(() => (this.isOverflow = this.$refs.edit.scrollHeight > 180))
            },

            initData () {
                this.isLoading = true
                Promise.all([this.getServiceDetail(), this.getServiceProcess()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            cancelRelease () {
                if (!this.permission) return

                const confirmFn = () => {
                    const params = this.$route.params || {}
                    const serviceId = params.serviceId || ''
                    this.isCancleLoading = true
                    this.requestServiceCancelRelease(serviceId).then(() => {
                        this.$bkMessage({ message: this.$t('store.取消发布成功'), theme: 'success' })
                        this.toServiceList()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.isCancleLoading = false))
                }
                this.$bkInfo({
                    title: this.$t('store.确认要取消发布吗？'),
                    confirmFn
                })
            },

            getServiceDetail () {
                const params = this.$route.params || {}
                const serviceId = params.serviceId || ''
                return this.requestServiceDetail(serviceId).then((res) => {
                    this.serviceDetail = res
                })
            },

            getServiceProcess (callBack) {
                const params = this.$route.params || {}
                const serviceId = params.serviceId || ''
                const iconMap = {
                    begin: 'order-shape',
                    testEnvPrepare: 'execute',
                    test: 'script-files',
                    commit: 'edit2',
                    approve: 'panel-permission',
                    end: 'check-1',
                    online: 'panels'
                }

                return this.requestServiceProcess(serviceId).then((res) => {
                    this.progressStatus = (res.processInfos || []).map((item) => ({
                        title: item.name,
                        icon: item.status === 'success' ? 'check-1' : iconMap[item.code],
                        status: item.status,
                        code: item.code
                    }))
                    this.permission = res.opPermission || false
                    this.storeBuildInfo = res.storeBuildInfo || {}
                    let index = this.progressStatus.findIndex(x => ['doing', 'fail'].includes(x.status))
                    if (index < 0) index = 0
                    this.currentStepIndex = index + 1
                    const lastStep = this.progressStatus[this.progressStatus.length - 1] || {}
                    this.isOver = lastStep.status === 'success'
                    if (this.isOver) this.currentStepIndex = this.progressStatus.length
                    if (this.isOver || this.currentStep.status === 'fail') clearTimeout(this.loopProgress.timeId)
                }).finally(() => {
                    if (callBack) callBack()
                })
            },

            loopProgress (callback) {
                this.getServiceProcess().then(() => {
                    if (callback) callback()
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' }))
                clearTimeout(this.loopProgress.timeId)
                if (!this.isOver) this.loopProgress.timeId = setTimeout(this.loopProgress, 5000)
            },

            toServiceList () {
                this.$router.push({ name: 'serviceWork' })
            },

            toAtomStore () {
                this.$router.push({
                    name: 'atomHome',
                    query: {
                        pipeType: 'service'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .progress-detail-content {
        padding: 32px;
        .detail-title {
            display: flex;
            align-items: center;
            margin-bottom: 20px;
            img {
                width: 56px;
                height: 56px;
                border-radius: 11px;
                margin: 0 13px 0 10px;
            }
            span {
                font-weight: normal;
                font-size: 20px;
                text-align: left;
                color: #222222;
                line-height: 24px;
            }
        }
        ::v-deep .bk-form-item {
            &+.bk-form-item {
                margin-top: 24px;
            }
            .bk-label {
                padding: 0 5px;
                color: #999999;
            }
            .bk-form-content {
                color: #222222;
                span {
                    word-break: break-all;
                }
            }
        }
        .toggle-btn {
            font-size: 12px;
            color: $primaryColor;
            text-align: right;
            cursor: pointer;
            .devops-icon {
                display: inline-block;
                margin-left: 2px;
                transition: all ease 0.2s;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
        .overflow {
            max-height: 180px;
            overflow: hidden;
        }
        .detail-label {
            display: inline-block;
            position: relative;
            span {
                overflow: inherit;
                margin-top: 7px;
            }
            span.info-label {
                font-weight: normal;
                display: inline-block;
                width: auto;
                height: 18px;
                padding: 0 12px;
                border: 1px solid $lightBorder;
                border-radius: 20px;
                margin-right: 8px;
                line-height: 16px;
                text-align: center;
                font-size: 12px;
                color: $fontDarkBlack;
                background-color: $white;
            }
        }
    }

    .service-progress-home {
        height: 100%;
        background: $grayBackGroundColor;
    }

    .progress-main {
        height: calc(100% - 65px);
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        background: $white;
        padding: 32px;
        ::v-deep .main-body {
            height: calc(100% - 50px);
        }
        ::v-deep .main-footer {
            margin-top: 16px;
            .bk-button + .bk-button {
                margin-left: 24px;
            }
        }
    }

    .service-progress-main {
        width: 95vw;
        height: calc(100% - 5.6vh - 66px);
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        margin: 33px auto;
        background: $white;
        .progress-header {
            height: 65px;
            position: relative;
            border-bottom: 1px solid $lightBorder;
            .progress-steps {
                padding: 22px 280px 0 180px;
                ::v-deep .icon-execute, ::v-deep .icon-panels, ::v-deep .icon-order-shape, ::v-deep .icon-script-files, ::v-deep .icon-panel-permission {
                    font-size: 14px;
                }
            }
            .progress-detail {
                position: absolute;
                right: 124px;
                top: 16px;
            }
            .progress-cancle {
                position: absolute;
                right: 32px;
                top: 16px;
            }
        }
    }
</style>
