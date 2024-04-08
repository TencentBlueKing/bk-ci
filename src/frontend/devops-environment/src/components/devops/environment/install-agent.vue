<template>
    <bk-sideslider
        ext-cls="install-agent-side"
        :is-show.sync="isShow"
        quick-close
        :width="700"
        :before-close="handleBeforeClose"
    >
        <div class="sideslider-title" slot="header">
            {{ $t('environment.installGseAgent') }}
            <span class="node-title">{{ $t('environment.nodeTitle') }}: {{ innerIp }}</span>
        </div>
        <div slot="content" v-if="isEditing">
            <bk-alert type="info" :title="$t('environment.installAgentTips')" closable></bk-alert>
            <bk-form
                ref="form"
                class="mt20"
                :label-width="labelWidth"
                :model="formData"
                :rules="rules"
            >
                <bk-form-item
                    :label="$t('environment.nodeInfo.os')"
                    :required="true"
                    property="os"
                >
                    <bk-radio-group
                        v-model="formData.osType"
                        @change="handleChangeData"
                    >
                        <bk-radio value="LINUX" class="mr20">Linux</bk-radio>
                        <bk-radio value="WINDOWS" class="mr20">Windows</bk-radio>
                        <bk-radio value="AIX" class="mr20">Aix</bk-radio>
                        <bk-radio value="SOLARIS">Solaris</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.loginAccount')"
                    :required="true"
                    property="account"
                    error-display-type="normal"
                >
                    <bk-input
                        v-model="formData.account"
                        :placeholder="$t('environment.accountPlaceholder')"
                        @change="handleChangeData"
                    />
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.oauthMethod')"
                    :required="true"
                    property="authType"
                >
                    <bk-radio-group
                        v-model="formData.authType"
                        @change="handleChangeData"
                    >
                        <bk-radio value="PASSWORD" class="mr20">{{ $t('environment.password') }}</bk-radio>
                        <bk-radio value="KEY" class="mr20">{{ $t('environment.key') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    v-if="formData.authType === 'PASSWORD'"
                    :label="$t('environment.loginPassword')"
                    :required="true"
                    property="password"
                    error-display-type="normal"
                >
                    <bk-input
                        v-model="formData.password"
                        type="password"
                        :placeholder="$t('environment.passwordPlaceholder')"
                        @change="handleChangeData"
                    />
                    <div v-if="formData.authType === 'PASSWORD'" class="password-tips">{{ $t('environment.passwordTips', [formData.osType === 'WINDOWS' ? 'Administrator' : 'root']) }}</div>
                </bk-form-item>
                <bk-form-item
                    v-else
                    :label="$t('environment.keyFile')"
                    :required="true"
                    property="keyFile"
                >
                    <bk-upload
                        ext-cls="upload-file-btn"
                        theme="button"
                        :tip="$t('environment.uploadTips')"
                        :custom-request="handleUpload"
                        url="/"
                        :limit="1"
                        :multiple="false"
                    >
                    </bk-upload>
                    <div v-if="formData.authType === 'PASSWORD'" class="keyFile-tips">{{ $t('environment.keyFileTips', [formData.osType === 'WINDOWS' ? 'Administrator' : 'root']) }}</div>
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.installationChannel')"
                    :required="true"
                    property="installChannelId"
                >
                    <bk-select
                        v-model="formData.installChannelId"
                        @change="handleChangeData"
                    >
                        <bk-option v-for="option in channelList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
            </bk-form>
        </div>
        <div class="sideslider-content" slot="content" v-else>
            <div class="install-status">
                <template v-if="['PENDING', 'RUNNING'].includes(installStatus)">
                    <Icon class="icon" name="loading" />
                    <span>{{ $t('environment.installingTips') }}</span>
                </template>
                <template v-else-if="installStatus === 'SUCCESS'">
                    <i class="bk-icon import-status-icon icon-check-1 success icon"></i>
                    <span>{{ $t('environment.installSuccessTips') }}</span>
                </template>
                <template v-else>
                    <i class="bk-icon import-status-icon icon-close error icon"></i>
                    <span>{{ $t('environment.installFailTips') }}</span>
                    <bk-button
                        text
                        class="ml10"
                        @click="handleRetryInstallAgent"
                    >
                        {{ $t('environment.retry') }}
                    </bk-button>
                </template>
            </div>
            <div ref="editor" class="log-wrapper">
                <div ref="executeScriptLog" v-once id="executeScriptLog" style="height: 100%;" />
            </div>
        </div>
        <div slot="footer">
            <template v-if="isEditing">
                <bk-button
                    class="ml20"
                    theme="primary"
                    :loading="isLoading"
                    @click="handleConFirm"
                >
                    {{ $t('environment.confirm') }}
                </bk-button>
                <bk-button
                    :loading="isLoading"
                    @click="handleCancel"
                >
                    {{ $t('environment.cancel') }}
                </bk-button>
            </template>
            <template v-else>
                <bk-button
                    class="ml20"
                    @click="handleCancel"
                >
                    {{ $t('environment.关闭') }}
                </bk-button>
            </template>
        </div>
        <section v-once id="executeScriptLog" style="height: 100%;" />
    </bk-sideslider>
</template>

<script>
    import ace from 'ace-builds/src-noconflict/ace'
    import 'ace-builds/src-noconflict/mode-text'
    import 'ace-builds/src-noconflict/theme-monokai'
    import 'ace-builds/src-noconflict/ext-searchbox'
    export default {
        props: {
            innerIp: {
                type: String,
                default: ''
            },
            hostId: {
                type: Number
            },
            osType: {
                type: String
            },
            taskId: {
                type: Number
            }
        },
        data () {
            const getDefaultFormData = () => {
                return {
                    osType: 'LINUX',
                    account: 'root',
                    authType: 'PASSWORD',
                    password: '',
                    installChannelId: 'auto',
                    innerIp: '',
                    bkHostId: null
                }
            }
            return {
                isZH: true,
                isShow: false,
                isLoading: false,
                labelWidth: 100,
                formData: getDefaultFormData(),
                isWillAutoScroll: true,
                getDefaultFormData,
                channelList: [],
                isEditing: true,
                jobId: 0,
                installStatus: 'PENDING',
                taskLog: '',
                keyFileFormData: new FormData()
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            rules () {
                return {
                    account: [
                        {
                            required: true,
                            message: this.$t('environment.请填写登录账号'),
                            trigger: 'blur'
                        }
                    ],
                    password: [
                        {
                            required: true,
                            message: this.$t('environment.请填写密码'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        watch: {
            isShow (val) {
                if (val) {
                    this.fetchChannelList()
                    this.formData.innerIp = this.innerIp
                    this.formData.bkHostId = this.hostId
                    if (['LINUX', 'AIX', 'SOLARIS', 'WINDOWS'].includes(this.osType)) {
                        this.formData.osType = this.osType
                    }
                } else {
                    if (this.jobId) {
                        this.$emit('install')
                    }
                    this.handleCancel()
                }
            },
            'formData.osType' (val) {
                if (['LINUX', 'AIX', 'SOLARIS'].includes(val)) {
                    this.formData.account = 'root'
                } else {
                    this.formData.account = 'Administrator'
                }
            },
            async taskId (val) {
                if (val) {
                    this.$nextTick(() => {
                        this.initEditor()
                    })
                    this.isEditing = false
                    this.jobId = val
                    await this.fetchInstallAgentStatus()
                    await this.fetchInstallAgentTaskLog()
                }
            }
        },
        created () {
            this.isZH = ['zh-CN', 'zh', 'zh_cn'].includes(document.documentElement.lang)
            this.labelWidth = this.isZH ? 100 : 130
        },
        methods: {
            initEditor () {
                const editor = ace.edit('executeScriptLog')
                editor.setTheme('ace/theme/monokai')
                editor.setOptions({
                    wrapBehavioursEnabled: true,
                    copyWithEmptySelection: true,
                    useElasticTabstops: true,
                    printMarginColumn: false,
                    printMargin: 80,
                    showPrintMargin: false,
                    scrollPastEnd: 0.05,
                    fixedWidthGutter: true
                })
                editor.$blockScrolling = Infinity
                editor.setReadOnly(true)
                const editorSession = editor.getSession()
                // 自动换行时不添加缩进
                editorSession.$indentedSoftWrap = false
                editorSession.setUseWrapMode(true)
                editorSession.on('changeScrollTop', (scrollTop) => {
                    const {
                        height,
                        maxHeight
                    } = editor.renderer.layerConfig
                    this.isWillAutoScroll = height + scrollTop + 30 >= maxHeight
                })
                this.editor = editor
                this.$once('hook:beforeDestroy', () => {
                    editor.destroy()
                    editor.container.remove()
                })
            },

            handleConFirm () {
                this.$refs.form.validate().then(async () => {
                    try {
                        this.isLoading = true
                        const params = { ...this.formData }
                        if (params.installChannelId === 'auto') {
                            params.isAutoChooseInstallChannelId = true
                            delete params.installChannelId
                        } else if (params.installChannelId === 'others') {
                            params.isAutoChooseInstallChannelId = false
                            delete params.installChannelId
                        } else {
                            params.isAutoChooseInstallChannelId = false
                        }
                        const hosts = []
                        hosts.push(params)
                        this.keyFileFormData.append('installAgentReq', JSON.stringify({ hosts }))
                        const res = await this.$store.dispatch('environment/installAgent', {
                            projectId: this.projectId,
                            data: this.keyFileFormData,
                            headers: {
                                'Content-Type': 'multipart/form-data'
                            }
                        })
                        this.jobId = res.jobId
                        this.isEditing = false
                        this.$nextTick(() => {
                            this.initEditor()
                        })
                        this.isLoading = false
                        setTimeout(async () => {
                            await this.fetchInstallAgentStatus()
                            await this.fetchInstallAgentTaskLog()
                        }, 1000)
                        this.$emit('install')
                    } catch (e) {
                        this.$bkMessage({
                            theme: 'error',
                            message: e.message || e
                        })
                        this.isLoading = false
                    }
                })
            },

            handleCancel () {
                this.isShow = false
                this.isLoading = false
                this.isEditing = true
                this.installStatus = 'PENDING'
                this.jobId = 0
                this.keyFile = null
                this.keyFileFormData = new FormData()
                this.formData = this.getDefaultFormData()
                this.$emit('update:taskId', 0)
                window.changeFlag = false
            },
            
            async fetchChannelList () {
                if (this.channelList.length) return
                const res = await this.$store.dispatch('environment/getChannelList', {
                    projectId: this.projectId
                })
                this.channelList = res.installChannelList
                this.channelList.unshift({
                    id: 'auto',
                    name: this.$t('environment.自动选择')
                })
                this.channelList.push({
                    id: 'others',
                    name: this.$t('environment.IDC直连区域')
                })
            },

            async fetchInstallAgentStatus () {
                if (!this.jobId) return
                const res = await this.$store.dispatch('environment/getAgentTaskStatus', {
                    projectId: this.projectId,
                    jobId: this.jobId,
                    params: {
                        page: 1,
                        pageSize: 20
                    }
                })
                this.installStatus = res.status
                this.instanceId = res.list.find(i => i.innerIp === this.innerIp).instanceId

                if (['PENDING', 'RUNNING'].includes(this.installStatus)) {
                    setTimeout(async () => {
                        await this.fetchInstallAgentStatus()
                        await this.fetchInstallAgentTaskLog()
                    }, 5000)
                } else {
                    this.$emit('install')
                }
            },

            async fetchInstallAgentTaskLog () {
                if (!this.jobId) return
                const res = await this.$store.dispatch('environment/getAgentTaskLog', {
                    projectId: this.projectId,
                    jobId: this.jobId,
                    instanceId: this.instanceId
                })
                const logList = []
                res.queryAgentTaskLogResult.forEach(i => {
                    if (i.log) logList.push(i.log)
                })
                this.taskLog = logList.join('\n')
                this.editor.getSession().setValue(this.taskLog)
                this.editor.scrollToLine(Infinity)
            },

            async handleRetryInstallAgent () {
                this.installStatus = 'PENDING'
                this.taskLog = ''
                this.editor.getSession().setValue('')
                this.handleConFirm()
            },

            handleUpload (option) {
                this.handleChangeData()
                this.keyFileFormData.append('keyFile', option.fileObj.origin)
            },

            handleChangeData () {
                window.changeFlag = true
            },

            handleBeforeClose () {
                if (window.changeFlag && !this.jobId) {
                    this.$bkInfo({
                        title: this.$t('environment.确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('environment.离开将会导致未保存信息丢失')),
                        okText: this.$t('environment.离开'),
                        confirmFn: () => {
                            this.isShow = false
                            return true
                        }
                    })
                } else {
                    this.isShow = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.install-agent-side {
    z-index: 2000;
    ::v-deep .bk-sideslider-content {
        height: calc(100% - 114px) !important;
        padding: 20px;
    }
    .sideslider-title {
        display: flex;
        align-items: center;
    }
    .sideslider-content {
        height: 100%;
    }
    .node-title {
        font-size: 12px;
        padding-left: 15px;
    }
    .password-tips,
    .keyFile-tips {
        font-size: 12px;
        color: #c7c9d0;
    }
    .install-status {
        display: flex;
        align-items: center;
        height: 80px;
        padding: 0 20px;
        background-color: #eef8ff;
        border: 1px solid #c5deff;
        .icon {
            margin-right: 20px;
        }
    }
    
}
</style>

<style lang="scss">
.import-status-icon {
    width: 38px;
    height: 38px;
    line-height: 38px;
    font-size: 36px;
    border-radius: 50%;
    &.success {
        background-color: #e5f6ea;
        color: #3fc06d;
    }
    &.error {
        background-color: #fdd;
        color: #ea3636;
    }
}
.log-wrapper {
    width: 100%;
    height: calc(100% - 90px);
    padding-top: 20px;
    /* stylelint-disable selector-class-pattern */
    .ace_editor {
        overflow: unset;
        line-height: 1.6;
        color: #c4c6cc;
        background: #1d1d1d;

        .ace_gutter {
            padding-top: 4px;
            margin-bottom: -4px;
            color: #63656e;
            background: #292929;
        }

        .ace_scroller {
            padding-top: 4px;
            margin-bottom: -4px;
            right: 0 !important;
        }

        .ace_hidden-cursors .ace_cursor {
            opacity: 0% !important;
        }

        .ace_selected-word {
            background: rgb(135 139 145 / 25%);
        }

        .ace_scrollbar-h {
            &::-webkit-scrollbar-thumb {
                background-color: #3b3c42;
                border: 1px solid #63656e;
            }

            &::-webkit-scrollbar-corner {
                background-color: transparent;
            }
        }

        .ace_scrollbar-v {
            overflow-x: hidden !important;
            overflow-y: hidden !important;
        }

        .ace_scrollbar-h {
            margin-bottom: -20px;

            &::-webkit-scrollbar {
                height: 14px;
            }
        }
    }
}
.upload-file-btn {
    input {
        font-size: 0;
    }
}
</style>
