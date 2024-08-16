<template>
    <span
        v-bk-tooltips="{
            content: disabledTips || '你没有该流水线的下载构件权限，无法下载',
            disabled: !disabled,
            allowHTML: false
        }">
        <bk-button
            text
            @click="downLoadFile"
            :disabled="disabled"
                        
        >
            <slot></slot>
        </bk-button>
        <bk-dialog
            width="500"
            v-model="visible"
            theme="primary"
        >
            <b class="signing-tips">
                <i class="devops-icon icon-circle-2-1 spin-icon" />
                {{ name }}为内部测试版本，正在准备下载，请稍等
            </b>
            <span class="signing-duration-tips">
                仅首次下载需等待，准备时长和包大小有关，500MB的包大概需要1分钟
            </span>
            <footer slot="footer">
                <bk-button
                    @click="cancelDownloading"
                >
                    稍后再下载
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        props: {
            disabled: Boolean,
            disabledTips: {
                type: String,
                default: ''
            },
            name: {
                type: String,
                required: true
            },
            experienceId: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                visible: false,
                signingMap: new Map()
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        beforeDestroy () {
            this.cancelDownloading()
        },
        methods: {
            ...mapActions('experience', [
                'downloadInstallation'
            ]),
            setVisible (visible) {
                this.visible = visible
            },
            async downLoadFile () {
                try {
                    if (this.btnDisabled) return
                    if (this.signingMap.get(this.path)) {
                        this.setVisible(true)
                        return
                    }
                    
                    const { url } = await this.downloadInstallation({
                        projectId: this.projectId,
                        experienceHashId: this.experienceId
                    })
                    
                    const result = await this.checkApkSigned(url)
                    if (result) {
                        window.location.href = url
                        return
                    } else {
                        this.signingMap.set(this.path, true)
                        this.setVisible(true)
                        const result = await this.pollingCheckSignedApk(url)
                        if (result) {
                            this.setVisible(false)
                            this.$bkMessage({
                                theme: 'success',
                                message: `${this.name}已经准备完成，开始下载`
                            })
                            
                            window.location.href = url
                        }
                        this.signingMap.delete(this.path)
                    }
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }
            },
            pollingCheckSignedApk (url) {
                clearTimeout(this.timer)
                // eslint-disable-next-line no-async-promise-executor
                return new Promise(async (resolve) => {
                    const result = await this.checkApkSigned(url)
                    console.log('result', result, resolve)
                    this.resolve = resolve
                    if (!result) {
                        this.timer = setTimeout(() => {
                            resolve(this.pollingCheckSignedApk(url))
                        }, 5000)
                        return
                    }
                    clearTimeout(this.timer)
                    resolve(result)
                })
            },
            async checkApkSigned (url) {
                try {
                    await this.$ajax.head(url)
                    return true
                } catch (err) {
                    console.log(err)
                    return err.httpStatus !== 451
                }
            },
            cancelDownloading () {
                clearTimeout(this.timer)
                this.resolve?.(false)
                this.resolve = null
                this.setVisible(false)
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/conf";
    .signing-tips {
        display: flex;
        flex-direction: row;
        align-items: center;
        grid-gap: 6px;
        font-weight: bold;
        margin-bottom: 12px;
        word-break: break-all;
        > .devops-icon {
            color: $primaryColor;
        }
    }
    .artifactory-download-icon-disabled {
        color: #979ba5;
        cursor: not-allowed;
    }
    .signing-duration-tips {
        color: #979ba5;
        font-size: 12px;
    }
</style>
