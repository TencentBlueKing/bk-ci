<template>
    <article v-bkloading="{ isLoading }" class="image-progress-home">
        <bread-crumbs :bread-crumbs="navList" type="image">
            <a class="g-title-work" target="_blank" :href="docsLink"> {{ $t('store.镜像指引') }} </a>
        </bread-crumbs>
        <main v-if="!isLoading" class="image-progress-main">
            <section class="image-progress-section">
                <h3>
                    <span> {{ $t('store.发布进度') }} </span>
                    <span @click="cancelRelease" :class="[{ disable: !permission }, 'cancel-release']" :title="permissionMsg"> {{ $t('store.取消发布') }} </span>
                </h3>
                <div class="progress-step">
                    <div class="step-line-box">
                        <div class="step-card" v-for="(entry, index) in progressStatus" :key="index"
                            :class="{ 'processing-status': entry.status === 'doing', 'fail-status': entry.status === 'fail', 'success-status': entry.code === 'end' && entry.status === 'success' }">
                            <div class="card-item">
                                <i class="devops-icon icon-check-1" v-if="entry.status === 'success'"></i>
                                <p class="step-label">{{ entry.name }}</p>
                            </div>
                            <div class="retry-bth">
                                <span :class="[{ disable: !permission }, 'rebuild-btn']"
                                    :title="permissionMsg"
                                    v-if="(entry.code === 'check' && entry.status === 'fail') || (entry.code === 'check' && entry.status === 'success' && progressStatus[index + 1].status === 'doing')"
                                    @click.stop="reCheck"
                                > {{ $t('store.重新验证') }} <i class="col-line"></i>
                                </span>
                                <span class="log-btn" v-if="entry.code === 'check' && entry.status !== 'undo'" @click.stop="readLog"> {{ $t('store.日志') }} </span>
                                <span class="test-btn" v-if="entry.code === 'test' && entry.status === 'doing'">
                                    <a target="_blank" :href="`/console/pipeline/${imageDetail.projectCode}/list`"> {{ $t('store.测试') }} </a>
                                </span>
                            </div>
                            <bk-button class="pass-btn"
                                theme="primary"
                                size="small"
                                v-if="entry.code === 'test'"
                                :disabled="entry.status !== 'doing' || !permission"
                                @click.native="passTest"
                                :loading="isTestLoading"
                                :title="permissionMsg"
                            > {{ $t('store.继续') }} </bk-button>
                            <div class="audit-tips" v-if="entry.code === 'approve' && entry.status === 'doing'">
                                <i class="devops-icon icon-info-circle"></i> {{ $t('store.由蓝盾管理员审核') }} </div>
                        </div>
                    </div>
                </div>
            </section>
            <section class="image-progress-section" v-if="!isOver">
                <h3> {{ $t('store.版本详情') }} </h3>
                <detail-info :detail="imageDetail"></detail-info>
            </section>
            <div class="released-tips" v-else>
                <h3> {{ $t('store.恭喜，成功发布到商店!') }} </h3>
                <div class="handle-btn">
                    <bk-button class="bk-button bk-primary" size="small" @click.native="toImageList"> {{ $t('store.工作台') }} </bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click.native="toAtomStore"> {{ $t('store.研发商店') }} </bk-button>
                </div>
            </div>
        </main>

        <bk-sideslider
            class="build-side-slider"
            :is-show.sync="sideSliderConfig.show"
            :title="sideSliderConfig.title"
            :quick-close="sideSliderConfig.quickClose"
            :width="sideSliderConfig.width">
            <template slot="content">
                <div style="width: 100%; height: 100%"
                    v-bkloading="{
                        isLoading: sideSliderConfig.loading.isLoading,
                        title: sideSliderConfig.loading.title
                    }">
                    <build-log v-if="currentBuildNo"
                        :build-no="currentBuildNo"
                        :log-url="`store/api/user/store/logs/types/IMAGE/projects/${currentProjectCode}/pipelines/${currentPipelineId}/builds`"
                    />
                </div>
            </template>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import BuildLog from '@/components/Log'
    import detailInfo from '../components/detailInfo'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    export default {
        components: {
            detailInfo,
            BuildLog,
            breadCrumbs
        },

        data () {
            return {
                docsLink: this.BKCI_DOCS.IMAGE_GUIDE_DOC,
                isLoading: false,
                isTestLoading: false,
                progressStatus: [],
                imageDetail: {},
                storeBuildInfo: {},
                permission: true,
                currentProjectId: '',
                currentBuildNo: '',
                currentPipelineId: '',
                sideSliderConfig: {
                    show: false,
                    title: this.$t('store.查看日志'),
                    quickClose: true,
                    width: 820,
                    value: '',
                    loading: {
                        isLoading: false,
                        title: ''
                    }
                }
            }
        },

        computed: {
            permissionMsg () {
                let str = ''
                if (!this.permission) str = this.$t('store.只有镜像管理员或当前流程创建者可以操作')
                return str
            },

            isOver () {
                const lastProgress = this.progressStatus[this.progressStatus.length - 1] || {}
                return lastProgress.status === 'success'
            },

            navList () {
                return [
                    { name: this.$t('store.工作台') },
                    { name: this.$t('store.容器镜像'), to: { name: 'imageWork' } },
                    { name: this.imageDetail.imageCode, to: { name: 'show', params: { code: this.imageDetail.imageCode, type: 'image' } } },
                    { name: this.$t('store.上架/升级镜像') }
                ]
            }
        },

        watch: {
            'sideSliderConfig.show' (val) {
                if (!val) {
                    this.currentProjectCode = ''
                    this.currentBuildNo = ''
                    this.currentPipelineId = ''
                }
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
                'requestImageDetail',
                'requestImageProcess',
                'requestCancelRelease',
                'requestImagePassTest',
                'requestRecheckImage'
            ]),

            readLog () {
                this.sideSliderConfig.show = true
                this.currentProjectCode = this.storeBuildInfo.projectCode
                this.currentBuildNo = this.storeBuildInfo.buildId
                this.currentPipelineId = this.storeBuildInfo.pipelineId
            },

            initData () {
                Promise.all([this.getImageDetail(), this.getImageProcess()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            reCheck () {
                if (!this.permission) return

                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                this.requestRecheckImage(imageId).then(() => {
                    this.$bkMessage({ message: this.$t('store.发起重新验证成功'), theme: 'success' })
                    this.getImageProcess()
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            },

            passTest () {
                if (!this.permission) return

                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                this.isTestLoading = true
                this.requestImagePassTest(imageId).then(() => {
                    this.getImageProcess()
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isTestLoading = false))
            },

            cancelRelease () {
                if (!this.permission) return

                const confirmFn = () => {
                    const params = this.$route.params || {}
                    const imageId = params.imageId || ''
                    this.requestCancelRelease(imageId).then(() => {
                        this.$bkMessage({ message: this.$t('store.取消发布成功'), theme: 'success' })
                        this.toImageList()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.isLoading = false))
                }
                this.$bkInfo({
                    title: this.$t('store.确认要取消发布吗？'),
                    confirmFn
                })
            },

            getImageDetail () {
                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                this.isLoading = true
                return this.requestImageDetail(imageId).then((res) => {
                    this.imageDetail = res
                })
            },

            getImageProcess () {
                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                return this.requestImageProcess(imageId).then((res) => {
                    this.progressStatus = res.processInfos || {}
                    this.permission = res.opPermission || false
                    this.storeBuildInfo = res.storeBuildInfo || {}
                })
            },

            loopProgress () {
                this.getImageProcess().catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' }))
                clearTimeout(this.loopProgress.timeId)
                if (!this.isOver) this.loopProgress.timeId = setTimeout(this.loopProgress, 5000)
            },

            toImageList () {
                this.$router.push({
                    name: 'imageWork'
                })
            },

            toAtomStore () {
                this.$router.push({
                    name: 'atomHome',
                    query: {
                        pipeType: 'image'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .image-progress-home {
        min-height: 100%;
    }

    .image-progress-main {
        height: calc(100% - 5.6vh);
        overflow-y: auto;
    }

    .disable {
        cursor: not-allowed !important;
        &:not(.pass-btn) {
            color: $fontWeightColor !important;
        }
    }

    .released-tips {
        margin-top: 60px;
        font-size: 20px;
        text-align: center;
        .handle-btn {
            margin-top: 40px;
        }
    }

    .image-progress-section {
        width: 1200px;
        margin: 28px auto;
        position: relative;
        z-index: 3;
        h3 {
            font-weight: bold;
            color: $fontWeightColor;
            line-height: 19px;
            font-size: 14px;
            padding-bottom: 10px;
            margin-bottom: 19px;
            border-bottom: 1px solid $fontLigtherColor;
            .cancel-release {
                float: right;
                color: $primaryColor;
                font-weight: normal;
                cursor: pointer;
            }
        }
    }
    .progress-step {
        position: relative;
        .step-line-box {
            display: flex;
            justify-content: space-between;
            margin: 32px 12%;
            width: 76%;
            &:after {
                content: '';
                position: absolute;
                top: 30px;
                width: 76%;
                height: 1px;
                border-top: 1px dashed #C3CDD7;
                z-index: 1;
            }
        }
        .step-card {
            display: flex;
            justify-content: center;
            align-items: center;
            position: relative;
            margin-right: 10%;
            width: 64px;
            height: 64px;
            border-radius: 50%;
            background: #fff;
            font-weight: bold;
            color: $lineColor;
            z-index: 2;
            &:last-child {
                margin-right: 0;
            }
            &:before {
                content: '';
                position: absolute;
                width: 64px;
                height: 64px;
                border-radius: 50%;
                border: 2px solid $lineColor;
                box-shadow: 0 0 0 2px #fff;
            }
        }
        @keyframes circle{
            0%{ transform:rotate(0deg); }
            100%{ transform:rotate(360deg); }
        }
        .card-item {
            text-align: center;
            i {
                font-size: 12px;
                font-weight: bold;
                color: $lineColor;
            }
        }
        .step-label {
            font-size: 12px;
        }
        .processing-status {
            color: #4A4A4A;
            &:before {
                border: 2px dotted #A3C5FD;
                animation: circle 10s infinite linear;
            }
        }
        .fail-status {
            color: $failColor;
            &:before {
                border-color: $failColor;
            }
        }
        .success-status {
            color: #00C873;
            .icon-check-1 {
                color: #00C873;
            }
        }
        .is-pointer {
            cursor: pointer;
        }
        .retry-bth,
        .audit-tips {
            position: absolute;
            top: 68px;
            width: 120px;
            font-size: 12px;
            font-weight: normal;
            color: $primaryColor;
            cursor: pointer;
            text-align: center;
            a,
            a:hover {
                color: $primaryColor;

            }
        }
        .audit-tips {
            width: 110px;
            color: #bcbcbc;
            cursor: default;
            i {
                position: relative;
                top: 1px;
                margin-right: 2px;
            }
        }
        .col-line {
            display: inline-block;
            margin-left: 5px;
            height: 10px;
            width: 1px;
            background-color: #DDE4EB;
        }
        .pass-btn {
            position: absolute;
            top: 17px;
            left: 83px;
            padding: 0 10px;
            font-weight: normal;
        }
    }

    ::v-deep .bk-sideslider-wrapper {
        top: 0;
        padding-bottom: 0;
        .bk-sideslider-content {
            height: calc(100% - 50px);
        }
    }
</style>
