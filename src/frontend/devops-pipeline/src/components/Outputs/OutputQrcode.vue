<template>
    <div class="output-qrcode-box" @click.stop="">
        <bk-popover
            theme="light"
            placement="bottom-end"
            trigger="click"
            :on-show="getArtifactQrcodeUrl"
        >
            <i class="devops-icon icon-qrcode"></i>
            <div slot="content" class="output-qrcode-popup" v-bkloading="{ isLoading: gettingQrcode }">
                <qrcode :text="qrcodeUrl" :size="100" />
            </div>
        </bk-popover>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import qrcode from '@/components/devops/qrcode'
    export default {
        components: {
            qrcode
        },
        props: {
            output: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                qrcodeUrl: '',
                gettingQrcode: false
            }
        },
        methods: {
            ...mapActions('common', [
                'requestExternalUrl'
            ]),
            async getArtifactQrcodeUrl () {
                try {
                    const { $route, output } = this
                    this.gettingQrcode = true
                    const params = {
                        projectId: $route.params.projectId,
                        type: output.artifactoryType,
                        path: output.fullPath
                    }
                    const external = await this.requestExternalUrl(params)
                    this.qrcodeUrl = external.url
                } catch (err) {
                    console.error(err)
                } finally {
                    this.gettingQrcode = false
                }
            }
        }
    }
</script>

<style lang="scss">
    .output-qrcode-box {
        display: inline-flex;
        align-items: center;
        .icon-qrcode {
            color: #979ba5;
            cursor: pointer;
        }
    }
    .output-qrcode-popup {
        width: 100px;
        height: 100px;
    }
</style>
