<template>
    <div class="upgrade-template-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">

        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <span class="back-home" @click="toAtomStore()">研发商店</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des back-home" @click="toAtomList()">工作台</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">上架模板</span>
            </p>
            <a class="title-work" target="_blank" :href="docsLink">模板指引</a>
        </h3>

        <div class="upgrade-template-content" v-show="showContent">
            <div class="template-release-msg">
                <div class="detail-title release-progress-title">
                    <p class="form-title">发布进度
                        <span class="cancel-release-btn" v-if="!isOver" @click="handlerCancel()">取消发布</span>
                    </p>
                    <hr class="cut-line">
                    <div class="progress-step">
                        <div class="step-line-box">
                            <div class="step-card" v-for="(entry, index) in progressStatus" :key="index"
                                :class="{
                                    'processing-status': entry.status === 'doing',
                                    'success-status': entry.name === '结束' && entry.status === 'success',
                                    'fail-status': entry.status === 'fail'
                                }">
                                <div class="card-item">
                                    <i class="bk-icon icon-check-1" v-if="entry.status === 'success'"></i>
                                    <p class="step-label">{{ entry.name }}</p>
                                </div>
                                <div class="audit-tips" v-if="entry.name === '审核中' && entry.status === 'doing'">
                                    <i class="bk-icon icon-info-circle"></i>由蓝盾管理员审核
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="detail-title version-detail-title" v-if="!isOver">
                    <p class="form-title">版本详情</p>
                    <hr class="cut-line">
                    <div class="template-version-detail">
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label">名称：</div>
                                <div class="info-value">{{ templateDetail.templateName }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label">分类：</div>
                                <div class="info-value">{{ templateDetail.classifyName }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label">应用范畴：</div>
                                <div class="info-value">{{ templateDetail.categoryList }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">功能标签：</div>
                            <div class="info-value feature-label">
                                <div class="label-card" v-for="(label, index) in templateDetail.labels" :key="index">{{ label }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">简介：</div>
                            <div class="info-value">{{ templateDetail.summary }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">详细描述：</div>
                            <div class="info-value markdown-editor-show" ref="editor" :class="{ 'overflow': !isDropdownShow }">
                                <mavon-editor
                                    :editable="false"
                                    default-open="preview"
                                    :subfield="false"
                                    :toolbars-flag="false"
                                    :external-link="false"
                                    :box-shadow="false"
                                    v-model="templateDetail.description"
                                />
                            </div>
                        </div>
                        <div class="toggle-btn" v-if="isOverflow" @click="toggleShow()">{{ isDropdownShow ? '收起' : '展开' }}
                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">发布者：</div>
                            <div class="info-value">{{ templateDetail.publisher }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label">发布描述：</div>
                            <div class="info-value">{{ templateDetail.pubDescription }}</div>
                        </div>
                    </div>
                    <div class="template-logo-box">
                        <img :src="templateDetail.logoUrl" v-if="templateDetail.logoUrl">
                        <i class="bk-icon icon-placeholder template-logo" v-else></i>
                    </div>
                </div>
                <div class="released-tips" v-if="isOver">
                    <h3>恭喜，成功发布到商店!</h3>
                    <div class="handle-btn">
                        <bk-button class="bk-button bk-primary" size="small" @click="toAtomList()">工作台</bk-button>
                        <bk-button class="bk-button bk-default" size="small" @click="toAtomStore(1)">研发商店</bk-button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import mavonEditor from 'mavon-editor'
    import 'mavon-editor/dist/css/index.css'

    const Vue = window.Vue
    Vue.use(mavonEditor)

    export default {
        data () {
            return {
                showContent: false,
                isOverflow: false,
                isDropdownShow: false,
                timer: -1,
                docsLink: `${DOCS_URL_PREFIX}/所有服务/流水线模版/summary.html`,
                progressStatus: [{
                    'name': '开始',
                    'step': 1,
                    'status': 'success'
                }, {
                    'name': '审核中',
                    'step': 2,
                    'status': 'doing'
                }, {
                    'name': '结束',
                    'step': 3,
                    'status': 'undo'
                }],
                loading: {
                    isLoading: false,
                    title: ''
                },
                templateDetail: {
                    templateName: '',
                    templateType: '',
                    categoryList: '',
                    classifyName: '',
                    labels: [],
                    summary: '',
                    publisher: '',
                    pubDescription: '',
                    description: ''
                }
            }
        },
        computed: {
            templateId () {
                return this.$route.params.templateId
            },
            isOver () {
                return this.progressStatus.length && this.progressStatus[2].status === 'success'
            }
        },
        created () {
            this.requestTplRelease()
            this.requestTemplateDetail()
        },
        beforeDestroy () {
            clearTimeout(this.timer)
        },
        methods: {
            async requestTemplateDetail (atomId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestTempIdDetail', {
                        templateId: this.templateId
                    })

                    Object.assign(this.templateDetail, res)
                    this.templateDetail.categoryList = res.categoryList.map(item => {
                        return item.categoryName
                    }).join('，')
                    this.templateDetail.labels = res.labelList.map(item => {
                        return item.labelName
                    })
                    this.$nextTick(() => {
                        setTimeout(() => {
                            this.isOverflow = this.$refs.editor && this.$refs.editor.scrollHeight > 180
                        }, 1000)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 500)
                    this.showContent = true
                }
            },
            async requestTplRelease (atomId) {
                try {
                    const res = await this.$store.dispatch('store/requestTplRelease', {
                        templateId: this.templateId
                    })

                    this.progressStatus = res.processInfos
                    if (!this.isOver) {
                        this.loopCheck()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async cancelRelease () {
                let message, theme

                this.loading.isLoading = true
                try {
                    await this.$store.dispatch('store/cancelReleaseTemplate', {
                        templateId: this.templateId
                    })

                    message = '取消成功'
                    theme = 'success'
                    this.toAtomList()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 100)
                }
            },
            handlerCancel () {
                this.$bkInfo({
                    title: `取消发布`,
                    subTitle: '确定取消发布该模板？',
                    confirmFn: async () => {
                        this.cancelRelease()
                    }
                })
            },
            async loopCheck () {
                const { timer } = this

                clearTimeout(timer)

                if (!this.isOver) {
                    this.timer = setTimeout(async () => {
                        await this.requestTplRelease()
                    }, 5000)
                }
            },
            toggleShow () {
                this.isDropdownShow = !this.isDropdownShow
            },
            toAtomList () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'template'
                    }
                })
            },
            toAtomStore (val) {
                const query = {
                    pipeType: 'template'
                }
                this.$router.push({
                    name: 'atomHome',
                    query: val ? query : undefined
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';

    .upgrade-template-wrapper {
        height: 100%;
        .info-header {
            display: flex;
            justify-content: space-between;
            width: 100%;
            height: 50px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .sub_header_left {
                display: flex;
                padding: 14px 24px;
                .title {
                    display: flex;
                    align-items: center;
                }
                .first-level,
                .secondary {
                    color: $primaryColor;
                    cursor: pointer;
                }
                .third-leve {
                    color: $fontWeightColor;
                }
                .nav-icon {
                    width: 24px;
                    height: 24px;
                    margin-right: 10px;
                }
                .right-arrow {
                    display :inline-block;
                    position: relative;
                    width: 19px;
                    height: 36px;
                    margin-right: 4px;
                }
                .right-arrow::after {
                    display: inline-block;
                    content: " ";
                    height: 4px;
                    width: 4px;
                    border-width: 1px 1px 0 0;
                    border-color: $lineColor;
                    border-style: solid;
                    transform: matrix(0.71, 0.71, -0.71, 0.71, 0, 0);
                    position: absolute;
                    top: 50%;
                    right: 6px;
                    margin-top: -9px;
                }
            }
            .develop-guide-link {
                position: absolute;
                right: 36px;
                margin-top: 14px;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .upgrade-template-content {
            padding: 20px 0 40px;
            height: calc(100% - 50px);
            overflow: auto;
        }
        .template-release-msg {
            position: relative;
            margin: auto;
            width: 1200px;
            .detail-title {
                .form-title {
                    font-weight: bold
                }
                .cut-line {
                    margin-top: 8px;
                    height: 1px;
                    border: none;
                    background-color: #C3CDD7
                }
                .cancel-release-btn {
                    float: right;
                    font-weight: normal;
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
        }
        .progress-step {
            position: relative;
            .step-line-box {
                display: flex;
                justify-content: space-between;
                margin: 32px 33%;
                width: 34%;
                &:after {
                    content: '';
                    position: absolute;
                    top: 30px;
                    width: 34%;
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
                font-size: 12px;
                color: #A3C5FD;
                cursor: pointer;
                a,
                a:hover {
                    margin-right: 8px;
                    color: #A3C5FD;

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
        }
        .version-detail-title {
            padding-top: 16px;
            position: relative;
        }
        .template-version-detail {
            margin-top: 20px;
            width: 80%;
            .detail-form-item {
                display: flex;
                margin-top: 18px;
                width: 100%;
            }
            .info-label {
                width: 100px;
                min-width: 100px;
                text-align: right;
            }
            .info-value {
                margin-left: 16px;
                line-height: 1.5;
                color: #333C48;
            }
            .label-card {
                padding: 1px 10px;
                display: inline-block;
                border-radius: 20px;
                margin-right: 8px;
                margin-bottom: 8px;
                border: 1px solid $laberColor;
                text-align: center;
                font-size: 12px;
                color: $laberColor;
                background-color: $laberBackColor;
            }
            .multi-item {
                margin-top: 0;
            }
            .editor-item {
                margin-bottom: 10px;
            }
        }
        .template-logo-box {
            position: absolute;
            top: 75px;
            right: 0;
            width: 100px;
            height: 100px;
            background: #fff;
            text-align: center;
            img {
                position: relative;
                width: 100px;
                height: 100px;
                z-index: 99;
                object-fit: cover;
            }
            .template-logo {
                font-size: 100px;
                display: block;
                transform: scale(1.2, 1.2);
                color: #C3CDD7;
            }
        }
        .is-border {
            border: 1px solid $lineColor;
        }
        .overflow {
            max-height: 180px;
            overflow: hidden;
        }
        .toggle-btn {
            margin-left: 117px;
            font-size: 12px;
            color: $primaryColor;
            text-align: right;
            cursor: pointer;
            .bk-icon {
                display: inline-block;
                margin-left: 2px;
                transition: all ease 0.2s;
                &.icon-flip {
                    transform: rotate(180deg);
                }
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
    }
</style>
