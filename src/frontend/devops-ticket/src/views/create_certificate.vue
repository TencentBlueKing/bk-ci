<template>
    <section class="credential-certificate-content">
        <content-header>
            <template slot="left">
                <span class="inner-header-title">{{ $t('ticket.createCert') }}</span>
            </template>
        </content-header>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <empty-tips v-if="!hasPermission && showContent"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns"
            >
            </empty-tips>
            <!-- <div class="bk-form cert-setting" v-if="certType">
                <div class="bk-form-wrapper" v-if="hasPermission && showContent"> -->
            <div class="cert-setting" v-if="certType">
                <div class="bk-form" v-if="hasPermission && showContent">
                    <!-- 证书类型 start -->
                    <div class="bk-form-item is-required cert-input-item">
                        <label class="bk-label">{{ $t('ticket.cert.certType') }}：</label>
                        <div class="bk-form-content">
                            <!-- <bk-radio-group v-model="certType" @change="changeType">
                                <bk-radio v-for="(item, index) in certTypeList" :key="index" :value="item.value" :disabled="isEdit"> -->
                            <bk-radio-group class="cert-type-group" v-model="certType" @change="changeType">
                                <bk-radio class="cert-type-group-item" v-for="(item, index) in certTypeList" :key="index" :value="item.value" :disabled="isEdit">
                                    <i class="devops-icon" :class="item.icon"></i>
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
                        <bk-button theme="primary" @click="submit">{{ $t('ticket.comfirm') }}</bk-button>
                        <bk-button @click="cancel">{{ $t('ticket.cancel') }}</bk-button>
                    </div>
                </div>
            </div>
        </section>
    </section>
</template>

<script>
    import emptyTips from '@/components/devops/emptyTips'
    import ios from '../components/centificate/ios'
    import android from '../components/centificate/android'
    import ssl from '../components/centificate/ssl'
    import enterprise from '../components/centificate/enterprise'
    import { CERT_RESOURCE_ACTION, CERT_RESOURCE_TYPE } from '../utils/permission'

    export default {
        components: {
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
                    // {
                    //     label: this.$t('ticket.cert.iosCert'),
                    //     value: 'ios',
                    //     icon: 'icon-macos'
                    // },
                    // {
                    //     label: this.$t('ticket.cert.androidCert'),
                    //     value: 'android',
                    //     icon: 'icon-android-shape'
                    // },
                    // {
                    //     label: this.$t('ticket.cert.sslOrTlsCert'),
                    //     value: 'tls',
                    //     icon: 'icon-personal-cert'
                    // },
                    {
                        label: this.$t('ticket.cert.iosCorporatesignCert'),
                        value: 'enterprise',
                        icon: 'icon-macos'
                    }
                ],
                loading: {
                    isLoading: true,
                    title: this.$t('ticket.loadingTitle')
                },
                emptyTipsConfig: {
                    title: this.$t('ticket.noPermission'),
                    desc: this.$t('ticket.credential.noCreateCredPermissionTips'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('ticket.switchProject')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: this.$t('ticket.applyPermission')
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
                this.certType = params.certType ? params.certType.toLowerCase() : 'enterprise'
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

            applyPermission () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: CERT_RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: CERT_RESOURCE_ACTION.CREATE
                })
            },

            async requestCertDetail (callBack) {
                this.loading.isLoading = true
                const certType = this.$route.params.certType.toLowerCase()
                const certId = this.$route.params.certId
                try {
                    this.certData = await this.$store.dispatch('ticket/requestCertDetail', {
                        projectId: this.projectId,
                        certType,
                        certId
                    })
                } catch (e) {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: CERT_RESOURCE_TYPE,
                            resourceCode: certId,
                            action: CERT_RESOURCE_ACTION.VIEW
                        }
                    )
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
                const url = `certs/projects/${this.projectId}/types/${this.certType}`
                const formData = this.$refs[this.certType].postData
                const config = { headers: { } }
                let message = ''
                let theme = 'success'

                try {
                    if (this.isEdit) {
                        await this.$store.dispatch('ticket/editCert', { url, formData, config })
                        message = this.$t('ticket.cert.successfullyEditedCert')
                    } else {
                        await this.$store.dispatch('ticket/createCert', { url, formData, config })
                        message = this.$t('ticket.cert.successfullyCreatedCert')
                    }
                } catch (err) {
                    if (err.code === 403) {
                        const actionId = this.isEdit ? this.$permissionActionMap.edit : this.$permissionActionMap.create
                        const instanceId = this.isEdit
                            ? [{
                                id: formData.certId,
                                type: this.$permissionResourceTypeMap.TICKET_CERT
                            }]
                            : []
                        this.applyPermission(actionId, this.$permissionResourceMap.cert, [{
                            id: this.projectId,
                            type: this.$permissionResourceTypeMap.PROJECT
                        }, ...instanceId])
                    }
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({ message, theme })
                    if (theme === 'success') this.$router.push({ name: 'certList' })
                }
            },

            async requestPermission () {
                this.loading.isLoading = true
                try {
                    const res = await this.$store.dispatch('ticket/requestCertsPermission', {
                        projectId: this.projectId
                    })
                    this.hasPermission = res
                } catch (e) {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: CERT_RESOURCE_TYPE,
                            resourceCode: this.projectId,
                            action: CERT_RESOURCE_ACTION.CREATE
                        }
                    )
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },

            async refreshTicket (val) {
                if (!val) throw Error('val is null')

                try {
                    const credentialRes = await this.$store.dispatch('ticket/requestCreditByPermission', {
                        projectId: this.projectId,
                        permission: 'USE',
                        creTypes: 'PASSWORD'
                    })
                    return credentialRes.records
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                    throw err
                }
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
        .cert-input-item {
            .bk-form-content .bk-form-input {
                width: 350px;
            }
        }
        .file-input {
            border: 1px solid #ddd;
            background: #fff;
            color: #000;
            display: inline-block;
            text-decoration: none;
            cursor: pointer;
            white-space: nowrap;
            height: 32px;
            line-height: 32px;
            box-sizing: border-box;
            font-size: 12px;
            vertical-align: middle;
            margin-left: 10px;
            .file-input-wrap {
                width: 100%;
                position: relative;
                opacity: 1;
                height: 32px;
                overflow: hidden;
                span {
                    padding: 6px 24px
                }
                .file-input-btn {
                    position: absolute;
                    right: 0;
                    opacity: 0;
                    left: 0;
                    width: 100%;
                    height: 32px;
                }
            }
            &:hover {
                border: 1px solid $primaryColor;
            }
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
     .cert-type-group {
        height: 32px;
        line-height: 32px;
        .cert-type-group-item {
            margin-right: 20px;
        }
    }
    .cert-input-item {
        margin-bottom: 20px;
    }
</style>
