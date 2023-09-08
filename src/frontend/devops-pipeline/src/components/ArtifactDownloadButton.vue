<template>
    <span v-if="hasPermission && artifactoryType !== 'IMAGE'" v-bk-tooltips="{
        content: $t('details.noDownloadPermTips'),
        disabled: hasPermission,
        allowHTML: false
    }">
        
        <i
            v-if="icon"
            class="devops-icon icon-download"
            @click.stop="downLoadFile"
        />
        <bk-button
            v-else
            text
            @click="downLoadFile"
            :disabled="!hasPermission"
                
        >
            {{ $t("download") }}
        </bk-button>
        
        <bk-dialog
            width="500"
            v-model="visible"
            theme="primary"
        >
            <b class="signing-tips">
                <i class="devops-icon icon-circle-2-1 spin-icon" />
                {{ $t('needSignTips', [name]) }}
            </b>
            <span class="signing-duration-tips">
                {{ $t('apkSignDurationTips') }}
            </span>
            <footer slot="footer">
                <bk-button
                    @click="cancelDownloading"
                >
                    {{ $t('稍后再下载') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    export default {
        emits: ['update:value'],
        props: {
            icon: Boolean,
            hasPermission: Boolean,
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
            value: Boolean
        },
        data () {
            return {
                visible: false,
                signingMap: new Map()
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
                    if (this.signingMap.get(this.path)) {
                        // this.apkSigningDialogVisible = true
                        this.setVisible(true)
                        return
                    }
                    const { url2 } = await this.$store
                        .dispatch('common/requestDownloadUrl', {
                            projectId: this.$route.params.projectId,
                            artifactoryType: this.artifactoryType,
                            path: this.path
                        })
                    
                    const result = await this.checkApkSigned(url2)
                    if (result) {
                        window.location.href = url2
                        return
                    } else {
                        this.signingMap.set(this.path, true)
                        this.setVisible(true)
                        const result = await this.pollingCheckSignedApk(url2)
                        if (result) {
                            window.location.href = url2
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
        > .devops-icon {
            color: $primaryColor;
        }
    }
    .signing-duration-tips {
        color: #979797;
        font-size: 12px;
    }
</style>
