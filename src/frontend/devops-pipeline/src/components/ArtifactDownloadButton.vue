<template>
    <div v-if="artifactoryType !== 'IMAGE'">
        <bk-popover
            :disabled="!btnDisabled || isLoading"
        >
            <i
                v-if="isLoading"
                class="devops-icon icon-circle-2-1 spin-icon"
                @click.stop=""
            />
            <i
                v-else-if="downloadIcon"
                :class="['devops-icon icon-download', {
                    'artifactory-download-icon-disabled': btnDisabled
                }]"
                @click.stop="downLoadFile"
            />
            <bk-button
                v-else
                text
                @click="downLoadFile"
                :disabled="btnDisabled"
            >
                {{ $t("download") }}
            </bk-button>
            <template slot="content">
                <p>{{ disabled ? $t('downloadDisabledTips') : $t('details.noDownloadPermTips') }}</p>
            </template>
        </bk-popover>
        <bk-dialog
            width="500"
            v-model="visible"
            theme="primary"
        >
            <b class="signing-tips">
                <i class="devops-icon icon-circle-2-1 spin-icon" />
                {{ $t('needSignTips', [name]) }}
            </b>
            <pre
                class="signing-duration-tips"
                v-if="signingDurationTips"
            >{{ signingDurationTips }}</pre>
            <footer slot="footer">
                <bk-button
                    @click="cancelDownloading"
                >
                    {{ $t('downloadLater') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import request from '@/utils/request'
    export default {
        props: {
            downloadIcon: Boolean,
            hasPermission: Boolean,
            disabledTips: {
                type: String,
                default: ''
            },
            artifactoryType: {
                type: String
            },
            name: {
                type: String,
                required: true
            },
            path: {
                type: String,
                required: true
            },
            output: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                visible: false,
                signingMap: new Map(),
                isLoading: false,
                signingDurationTips: ''
            }
        },
        computed: {
            disabled () {
                // 目录超10Gb 禁用状态
                if (this.output?.folder) {
                    const size = this.getFolderSize(this.output)
                    return size >= 10 * 1024 * 1024 * 1024
                }
                return false
            },
            btnDisabled () {
                return !this.hasPermission || this.disabled || this.isLoading
            }
        },
        beforeDestroy () {
            this.cancelDownloading()
        },
        methods: {
            setVisible (visible) {
                this.visible = visible
            },
            async downLoadFile () {
                try {
                    if (this.btnDisabled) return
                    if (this.signingMap.get(this.path)) {
                        // this.apkSigningDialogVisible = true
                        this.setVisible(true)
                        return
                    }
                    this.isLoading = true
                    const [isDevnet, res] = await Promise.all([
                        this.$store.dispatch('common/requestDevnetGateway'),
                        this.$store.dispatch('common/requestDownloadUrl', {
                            projectId: this.$route.params.projectId,
                            artifactoryType: this.artifactoryType,
                            path: this.path
                        })
                    ])
                    const url = isDevnet ? res.url : res.url2
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
                                extCls: 'apk-sign',
                                message: this.$t('apkSignSuccess', [this.name])
                            })

                            window.location.href = url
                        }
                        this.signingMap.delete(this.path)
                    }
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err, extCls: 'apk-sign' })
                } finally {
                    this.isLoading = false
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
                    await request.get(url, { headers: { 'Range': 'bytes=0-1' } })
                    this.signingDurationTips = ''
                    return true
                } catch (err) {
                    if (err.httpStatus === 451) {
                        this.signingDurationTips = err.message || ''
                    } else {
                        this.signingDurationTips = ''
                    }
                    return err.httpStatus !== 451
                }
            },
            cancelDownloading () {
                clearTimeout(this.timer)
                this.resolve?.(false)
                this.resolve = null
                this.setVisible(false)
                this.signingDurationTips = ''
            },
            getFolderSize (payload) {
                if (!payload.folder) return '0'
                return this.getValuesByKey(payload.properties, 'size')
            },
            getValuesByKey (data, key) {
                for (const item of data) {
                    if (key.includes(item.key)) {
                        return item.value
                    }
                }
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
        font-family: inherit;
        white-space: pre-wrap;
        margin: 0;
        padding: 0;
        color: #979ba5;
        font-size: 12px;
    }
    .apk-sign {
        z-index: 6666 !important;
    }
</style>
