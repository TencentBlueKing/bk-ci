<template>
    <div class="release-progress-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <bread-crumbs :bread-crumbs="navList" type="atom">
            <a class="g-title-work" target="_blank" :href="docsLink"> {{ $t('store.插件指引') }} </a>
        </bread-crumbs>

        <div class="release-progress-content" v-show="showContent">
            <div class="atom-release-msg">
                <div class="detail-title release-progress-title">
                    <p class="form-title"> {{ $t('store.发布进度') }} <span :class="[{ disable: !permission }, 'cancel-release-btn']" v-if="!isOver" @click="handlerCancel" :title="permissionMsg"> {{ $t('store.取消发布') }} </span>
                    </p>
                    <hr class="cut-line">
                    <div class="progress-step">
                        <div class="step-line-box">
                            <div class="step-card" v-for="(entry, index) in progressStatus" :key="index"
                                :class="{ 'processing-status': entry.status === 'doing',
                                          'fail-status': entry.status === 'fail',
                                          'success-status': entry.code === 'end' && entry.status === 'success' }">
                                <div class="card-item">
                                    <i class="devops-icon icon-check-1" v-if="entry.status === 'success'"></i>
                                    <p class="step-label">{{ entry.name }}</p>
                                </div>
                                <div class="retry-bth">
                                    <span class="test-btn"
                                        v-if="entry.code === 'commit' && ['doing','success'].includes(entry.status) && !isOver">
                                        <span @click="$refs.upload[0].click()">{{ $t('store.重新传包') }}</span>
                                        <span class="retry-pkgName">{{ versionDetail.pkgName }}</span>
                                        <input ref="upload" type="file" title="" class="upload-input" @change="selectFile" accept="application/zip">
                                    </span>
                                </div>
                                <div class="retry-bth">
                                    <span class="test-btn"
                                        v-if="entry.code === 'test' && entry.status === 'doing'">
                                        <a target="_blank" :href="`/console/pipeline/${versionDetail.projectCode}/list`"> {{ $t('store.测试') }} </a>
                                    </span>
                                </div>
                                <bk-button :class="[{ 'small-left': progressStatus.length === 6 }, 'pass-btn']"
                                    theme="primary"
                                    size="small"
                                    v-if="entry.code === 'test'"
                                    :disabled="entry.status !== 'doing' || !permission"
                                    @click.stop="passTest"
                                    :title="permissionMsg"
                                > {{ $t('store.继续') }} </bk-button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="detail-title version-detail-title" v-if="!isOver">
                    <p class="form-title"> {{ $t('store.版本详情') }} </p>
                    <hr class="cut-line">
                    <div class="atom-version-detail">
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('store.名称：') }} </div>
                                <div class="info-value">{{ versionDetail.name }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('store.标识：') }} </div>
                                <div class="info-value">{{ versionDetail.atomCode }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('store.范畴：') }} </div>
                                <div class="info-value">{{ categoryMap[versionDetail.category] }}</div>
                            </div>
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('store.分类：') }} </div>
                                <div class="info-value">{{ versionDetail.classifyName }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item multi-item">
                            <div class="detail-form-item">
                                <div class="info-label"> {{ $t('store.操作系统：') }} </div>
                                <div class="info-value" v-if="versionDetail.os">
                                    <span v-if="versionDetail.jobType === 'AGENT'">
                                        <i class="devops-icon icon-linux-view" v-if="versionDetail.os.indexOf('LINUX') !== -1"></i>
                                        <i class="devops-icon icon-windows" v-if="versionDetail.os.indexOf('WINDOWS') !== -1"></i>
                                        <i class="devops-icon icon-macos" v-if="versionDetail.os.indexOf('MACOS') !== -1"></i>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.功能标签：') }} </div>
                            <div class="info-value feature-label">
                                <div class="label-card" v-for="(label, index) in versionDetail.labels" :key="index">{{ label }}</div>
                            </div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.简介：') }} </div>
                            <div class="info-value">{{ versionDetail.summary }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.详细描述：') }} </div>
                            <div class="info-value markdown-editor-show" ref="editor" :class="{ 'overflow': !isDropdownShow }">
                                <mavon-editor
                                    :editable="false"
                                    default-open="preview"
                                    :subfield="false"
                                    :toolbars-flag="false"
                                    :external-link="false"
                                    :box-shadow="false"
                                    :language="mavenLang"
                                    preview-background="#fafbfd"
                                    v-model="versionDetail.description"
                                >
                                </mavon-editor>
                            </div>
                        </div>
                        <div class="toggle-btn" v-if="isOverflow" @click="toggleShow()">{{ isDropdownShow ? $t('store.收起') : $t('store.展开') }}
                            <i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.发布者：') }} </div>
                            <div class="info-value">{{ versionDetail.publisher }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.发布类型：') }} </div>
                            <div class="info-value">{{ releaseMap[versionDetail.releaseType] }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.版本：') }} </div>
                            <div class="info-value">{{ versionDetail.version }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.发布包：') }} </div>
                            <div class="info-value">{{ versionDetail.pkgName }}</div>
                        </div>
                        <div class="detail-form-item">
                            <div class="info-label"> {{ $t('store.发布描述：') }} </div>
                            <div class="info-value">
                                <mavon-editor
                                    :editable="false"
                                    default-open="preview"
                                    :subfield="false"
                                    :toolbars-flag="false"
                                    :external-link="false"
                                    :box-shadow="false"
                                    :language="mavenLang"
                                    preview-background="#fafbfd"
                                    v-model="versionDetail.versionContent"
                                >
                                </mavon-editor>
                            </div>
                        </div>
                    </div>
                    <div class="atom-logo-box">
                        <img :src="versionDetail.logoUrl" v-if="versionDetail.logoUrl">
                        <i class="devops-icon icon-placeholder atom-logo" v-else></i>
                    </div>
                </div>
            </div>
            <div class="released-tips" v-if="isOver">
                <h3> {{ $t('store.恭喜，成功发布到商店!') }} </h3>
                <div class="handle-btn">
                    <bk-button class="bk-button bk-primary" size="small" @click="toAtomList"> {{ $t('store.工作台') }} </bk-button>
                    <bk-button class="bk-button bk-default" size="small" @click="toAtomStore"> {{ $t('store.研发商店') }} </bk-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import cookie from 'js-cookie'
    import webSocketMessage from '@/utils/webSocketMessage'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    const CSRFToken = cookie.get('backend_csrftoken')

    export default {
        components: {
            breadCrumbs
        },

        data () {
            return {
                permission: true,
                atomlogoUrl: '',
                currentProjectId: '',
                currentBuildNo: '',
                currentPipelineId: '',
                timer: -1,
                docsLink: this.BKCI_DOCS.PLUGIN_GUIDE_DOC,
                showContent: false,
                isOverflow: false,
                isDropdownShow: false,
                progressStatus: [],
                storeBuildInfo: {},
                loading: {
                    isLoading: false,
                    title: ''
                },
                categoryMap: {
                    TASK: this.$t('store.流水线插件'),
                    TRIGGER: this.$t('store.流水线触发器')
                },
                jobTypeMap: {
                    AGENT: this.$t('store.编译环境'),
                    AGENT_LESS: this.$t('store.无编译环境')
                },
                osMap: {
                    LINUX: 'Linux',
                    WINDOWS: 'Windows',
                    MACOS: 'macOS'
                    // 'NONE': '无构建环境'
                },
                releaseMap: {
                    NEW: this.$t('store.新上架'),
                    INCOMPATIBILITY_UPGRADE: this.$t('store.非兼容式升级'),
                    COMPATIBILITY_UPGRADE: this.$t('store.兼容式功能更新'),
                    COMPATIBILITY_FIX: this.$t('store.兼容式问题修正'),
                    HIS_VERSION_UPGRADE: this.$t('store.历史大版本问题修复')
                },
                versionDetail: {
                    atomCode: '',
                    description: '',
                    visibilityLevel: ''
                }
            }
        },
        computed: {
            routerParams () {
                return this.$route.params
            },
            curTitle () {
                return this.routerParams.releaseType === 'shelf' ? this.$t('store.上架插件') : this.$t('store.升级插件')
            },
            isOver () {
                return this.progressStatus.length && this.progressStatus[this.progressStatus.length - 1].status === 'success'
            },
            permissionMsg () {
                let str = ''
                if (!this.permission) str = this.$t('store.只有插件管理员或当前流程创建者可以操作')
                return str
            },
            postUrl () {
                return `${API_URL_PREFIX}/artifactory/api/user/artifactories/projects/${this.versionDetail.projectCode}/ids/${this.versionDetail.atomId}/codes/${this.versionDetail.atomCode}/versions/${this.versionDetail.version}/re/archive`
            },
            navList () {
                return [
                    { name: this.$t('store.工作台') },
                    { name: this.$t('store.流水线插件'), to: { name: 'atomWork' } },
                    { name: this.versionDetail.atomCode, to: { name: 'statisticData', params: { code: this.versionDetail.atomCode, type: 'atom' } } },
                    { name: this.curTitle }
                ]
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },

        async created () {
            await this.requestRelease(this.routerParams.atomId)
            await this.requestAtomDetail(this.routerParams.atomId)
            webSocketMessage.installWsMessage(this.handleRelease)
        },
        beforeDestroy () {
            // clearTimeout(this.timer)
            webSocketMessage.unInstallWsMessage()
        },
        methods: {
            toAtomList () {
                this.$router.push({
                    name: 'atomWork'
                })
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            async requestAtomDetail (atomId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestAtomDetail', {
                        atomId: atomId
                    })

                    Object.assign(this.versionDetail, res)
                    this.versionDetail.labels = res.labelList.map(item => {
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
            handleRelease (res) {
                this.progressStatus = res.processInfos
                this.permission = res.opPermission
                if (res.storeBuildInfo) {
                    this.storeBuildInfo = res.storeBuildInfo
                }
            },
            async requestRelease (atomId) {
                try {
                    const res = await this.$store.dispatch('store/requestRelease', {
                        atomId: atomId
                    })

                    this.handleRelease(res)
                    // if (!this.isOver) {
                    //     this.loopCheck()
                    // }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async passTest () {
                if (!this.permission) return

                let message, theme
                try {
                    await this.$store.dispatch('store/passTest', {
                        atomId: this.routerParams.atomId
                    })

                    message = this.$t('store.操作成功')
                    theme = 'success'
                    // this.requestRelease(this.routerParams.atomId)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            handlerCancel () {
                if (!this.permission) return

                const h = this.$createElement
                const subHeader = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('store.确定取消发布该插件？')}`)

                this.$bkInfo({
                    title: this.$t('store.取消发布'),
                    subHeader,
                    maskClose: true,
                    confirmFn: async () => {
                        this.cancelRelease()
                    }
                })
            },
            async cancelRelease () {
                let message, theme

                this.loading.isLoading = true
                try {
                    await this.$store.dispatch('store/cancelRelease', {
                        atomId: this.routerParams.atomId
                    })

                    message = this.$t('store.取消成功')
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
                    }, 1000)
                }
            },
            selectFile () {
                const target = event.target
                const files = target.files

                if (!files.length) return
                for (const file of files) {
                    const fileObj = {
                        name: file.name,
                        size: file.size / 1000 / 1000,
                        type: file.type,
                        origin: file
                    }
                    const pos = fileObj.name.lastIndexOf('.')
                    const lastname = fileObj.name.substring(pos, fileObj.name.length)
                    if (lastname.toLowerCase() !== '.zip') {
                        this.$bkMessage({
                            message: this.$t('store.只允许上传 zip 格式的文件'),
                            theme: 'error'
                        })
                    } else {
                        this.uploadFile(fileObj)
                    }
                }
            },
            uploadFile (fileObj, fieldCheckConfirmFlag = false) {
                const formData = new FormData()
                formData.append('file', fileObj.origin)
                formData.append('os', `["${this.versionDetail.os.join('","')}"]`)

                const xhr = new XMLHttpRequest()
                fileObj.xhr = xhr // 保存，用于中断请求

                xhr.withCredentials = true
                const url = this.postUrl + `?fieldCheckConfirmFlag=${fieldCheckConfirmFlag}`
                xhr.open('POST', url, true)
                xhr.onreadystatechange = () => {
                    if (xhr.readyState === 4) {
                        let theme, message
                        if (xhr.status === 200) {
                            const response = JSON.parse(xhr.responseText)

                            if (response.status === 0) {
                                theme = 'success'
                                message = this.$t('store.上传成功')

                                this.requestRelease(this.routerParams.atomId)
                                this.requestAtomDetail(this.routerParams.atomId)
                            } else if ([2120030, 2120031].includes(response.status)) {
                                this.confirmSubmit(response.message, () => this.uploadFile(fileObj, true))
                                return
                            } else {
                                theme = 'error'
                                message = response.message
                            }
                        } else {
                            const errResponse = JSON.parse(xhr.responseText)
                            theme = 'error'
                            message = errResponse.message
                        }
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
                if (xhr.upload) {
                    xhr.upload.onprogress = event => {
                        if (event.lengthComputable) {
                            const progress = Math.floor(event.loaded / event.total * 100)

                            this.progress = progress >= 1 ? progress - 1 : 0
                        }
                    }
                }
                xhr.setRequestHeader('X-CSRFToken', CSRFToken)
                xhr.send(formData)
                document.querySelector('.upload-input').value = ''
            },
            confirmSubmit (message, confirmFn) {
                const h = this.$createElement
                const subHeader = h('p', {
                    style: {
                        textDecoration: 'none',
                        cursor: 'pointer',
                        whiteSpace: 'normal',
                        textAlign: 'left',
                        lineHeight: '24px'
                    }
                }, message)
                this.$bkInfo({
                    type: 'warning',
                    subHeader,
                    width: 440,
                    okText: this.$t('store.已确认兼容新增参数，继续'),
                    confirmFn
                })
            },
            atomOs (os) {
                const target = []
                os.forEach(item => {
                    target.push(this.osMap[item])
                })
                return target.join('，')
            },
            toggleShow () {
                this.isDropdownShow = !this.isDropdownShow
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    @import '@/assets/scss/markdown-body.scss';

    .disable {
        cursor: not-allowed !important;
        &:not(.pass-btn) {
            color: $fontWeightColor !important;
        }
    }

    .release-progress-wrapper {
        height: 100%;
        .release-progress-content {
            padding: 20px 0 40px;
            height: calc(100% - 5.6vh);
            overflow: auto;
        }
        .atom-release-msg {
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
                .upload-input {
                    display: none;
                }
                .retry-pkgName {
                    display: inline-block;
                    margin-left: 3px;
                    cursor: default;
                    color: #7b7d8a;
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
                left: 100px;
                padding: 0 10px;
                font-weight: normal;
                &.small-left {
                    left: 80px;
                }
            }
        }
        .version-detail-title {
            padding-top: 16px;
            z-index: 3;
            position: relative;
        }
        .atom-version-detail {
            margin-top: 20px;
            width: 80%;
            position: relative;
            z-index: 100;
            .detail-form-item {
                display: flex;
                margin-top: 18px;
                width: 100%;
            }
            .is-open {
                height: 21px;
                pointer-events: none;
                .bk-form-radio {
                    padding: 0;
                }
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
            .markdown-editor-show.info-value {
                overflow-y: auto;
                ::v-deep .v-note-panel {
                    border: none;
                }
                ::v-deep .v-show-content {
                    background: #FAFBFD;
                }
            }
        }
        .atom-logo-box {
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
            .atom-logo {
                font-size: 100px;
                display: block;
                transform: scale(1.2, 1.2);
                color: #C3CDD7;
            }
        }
        .is-border {
            border: 1px solid $lineColor;
        }
        .info-value.markdown-editor-show.overflow {
            max-height: 180px;
            overflow: hidden;
        }
        .toggle-btn {
            margin-left: 117px;
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
        .released-tips {
            margin-top: 60px;
            font-size: 20px;
            text-align: center;
            .handle-btn {
                margin-top: 40px;
            }
        }
        ::v-deep .bk-sideslider-wrapper {
            top: 0;
            padding-bottom: 0;
            .bk-sideslider-content {
                height: calc(100% - 50px);
            }
        }
    }
</style>
