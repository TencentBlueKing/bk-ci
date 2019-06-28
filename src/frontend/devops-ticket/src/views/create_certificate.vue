<template>
    <section class="certificate-list">
        <inner-header>
            <template slot="left">
                <span class="inner-header-title">新增证书</span>
            </template>
        </inner-header>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <empty-tips v-if="!hasPermission && showContent"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns"
            >
            </empty-tips>
            <div class="bk-form cert-setting" v-if="certType">
                <div class="bk-form-wrapper" v-if="hasPermission && showContent">
                    <!-- 证书类型 start -->
                    <div class="bk-form-item is-required cert-input-item">
                        <label class="bk-label">证书类型：</label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="certType" @change="changeType">
                                <bk-radio v-for="(item, index) in certTypeList" :key="index" :value="item.value" :disabled="isEdit">
                                    <i class="bk-icon" :class="item.icon"></i>
                                    {{ item.label }}
                                </bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>

                    <transition name="fade">
                        <ios v-if="certType === 'ios'"
                            @requestCertDetail="requestCertDetail"
                            :is-edit="isEdit"
                            :apply-cre-url="applyCreUrl"
                            :cert-data="certData"
                            ref="ios"
                        >
                        </ios>
                    </transition>
                    <transition name="fade">
                        <android v-if="certType === 'android'"
                            @requestCertDetail="requestCertDetail"
                            :is-edit="isEdit"
                            :apply-cre-url="applyCreUrl"
                            :cert-data="certData"
                            ref="android"
                        >
                        </android>
                    </transition>
                    <transition name="fade">
                        <ssl v-if="certType === 'tls'"
                            @requestCertDetail="requestCertDetail"
                            :is-edit="isEdit"
                            :cert-data="certData"
                            ref="tls"
                        >
                        </ssl>
                    </transition>
                    <transition name="fade">
                        <enterprise v-if="certType === 'enterprise'"
                            @requestCertDetail="requestCertDetail"
                            :is-edit="isEdit"
                            :cert-data="certData"
                            ref="enterprise"
                        >
                        </enterprise>
                    </transition>

                    <div class="operate-btn">
                        <bk-button theme="primary" @click="submit">确定</bk-button>
                        <bk-button @click="cancel">取消</bk-button>
                    </div>
                </div>
            </div>
        </section>
    </section>
</template>

<script>
    import innerHeader from '@/components/devops/inner_header'
    import emptyTips from '@/components/devops/emptyTips'
    import ios from '../components/centificate/ios'
    import android from '../components/centificate/android'
    import ssl from '../components/centificate/ssl'
    import enterprise from '../components/centificate/enterprise'

    export default {
        components: {
            innerHeader,
            emptyTips,
            ios,
            android,
            ssl,
            enterprise
        },

        data () {
            return {
                showContent: false,
                isEdit: false,
                credentialList: [],
                isCredentialLoading: false,
                hasPermission: true,
                certType: '',
                certData: {},
                certTypeList: [
                    {
                        label: 'iOS证书',
                        value: 'ios',
                        icon: 'icon-macos'
                    },
                    {
                        label: 'Android证书',
                        value: 'android',
                        icon: 'icon-android-shape'
                    },
                    {
                        label: 'SSL/TLS证书',
                        value: 'tls',
                        icon: 'icon-personal-cert'
                    },
                    {
                        label: 'iOS企业签名证书',
                        value: 'enterprise',
                        icon: 'icon-macos'
                    }
                ],
                loading: {
                    isLoading: true,
                    title: '数据加载中，请稍候'
                },
                emptyTipsConfig: {
                    title: '没有权限',
                    desc: `你在该项目[凭证管理]下没有[创建]权限，请切换项目访问或申请`,
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: '去申请权限'
                        }
                    ]
                }
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            },

            certId () {
                return this.$route.parmas.certId
            },

            applyCreUrl () {
                return `/console/ticket/${this.projectId}/createCredential/PASSWORD/true`
            }
        },
        
        watch: {
            projectId: async function () {
                await this.requestPermission()
            }
        },

        created () {
            this.init()
        },

        methods: {
            init () {
                const params = this.$route.params || {}
                this.certType = params.certType || 'ios'
                this.isEdit = this.$route.name === 'editCert'

                if (this.isEdit) {
                    this.requestCertDetail()
                } else {
                    this.requestPermission()
                }
            },

            cancel () {
                this.$router.push({ name: 'certList' })
            },

            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },

            goToApplyPerm () {
                const url = `/backend/api/perm/apply/subsystem/?client_id=ticket&project_code=${this.projectId}&service_code=ticket&role_creator=cert`
                window.open(url, '_blank')
            },

            async requestCertDetail (callBack) {
                this.loading.isLoading = true
                const certType = this.$route.params.certType
                const certId = this.$route.params.certId
                try {
                    this.certData = await this.$store.dispatch('ticket/requestCertDetail', {
                        projectId: this.projectId,
                        certType,
                        certId
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({ message, theme })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },

            changeType () {
                const ref = this.$refs[this.certType]
                if (ref) ref.$validator.reset()
            },

            async submit () {
                // 先检验数据合法性
                const validResult = await this.$refs[this.certType].validCertForm()
                if (validResult) return
                const url = `certs/${this.projectId}/${this.certType}`
                const formData = this.$refs[this.certType].postData
                const config = { headers: { } }
                let message = ''
                let theme = 'success'

                try {
                    if (this.isEdit) {
                        await this.$store.dispatch('ticket/editCert', { url, formData, config })
                        message = '编辑证书成功'
                    } else {
                        await this.$store.dispatch('ticket/createCert', { url, formData, config })
                        message = '新增证书成功'
                    }
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({ message, theme })
                    if (theme === 'success') this.$router.push({ 'name': 'certList' })
                }
            },

            async requestPermission () {
                this.loading.isLoading = true
                try {
                    const res = await this.$store.dispatch('ticket/requestCertsPermission', {
                        projectId: this.projectId
                    })
                    this.hasPermission = res
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },

            refreshTicket (val) {
                return new Promise(async (resolve, reject) => {
                    if (!val) reject(new Error('val is null'))

                    try {
                        const credentialRes = await this.$store.dispatch('ticket/requestCreditByPermission', {
                            projectId: this.projectId,
                            permission: 'USE',
                            creTypes: 'PASSWORD'
                        })
                        resolve(credentialRes.records)
                    } catch (err) {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                        reject(err)
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .operate-btn {
        margin: 30px 0 0 147px
    }
    .cert-setting {
        width: 100%;
        max-width: initial;
        padding: 42px 0 0 37px;
        .bk-form-wrapper {
            margin-top: -30px;
        }
    }
    .cert-textarea-item {
        .bk-label {
            margin-top: 3px
        }
        .bk-form-content {
            width: 454px;
            textarea {
                height: 72px
            }
        }
    }
    .fade-leave-active {
        display: none;
    }
</style>
