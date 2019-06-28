<template>

    <section class="certificate-list">
        <inner-header>
            <template slot="left">
                <span class="inner-header-title">新增凭据</span>
            </template>
        </inner-header>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">

            <empty-tips
                v-if="!hasPermission && showContent"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
            </empty-tips>

            <div class="bk-form credential-setting">
                <div v-if="showContent && hasPermission" class="bk-form-wrapper">
                    <!-- 凭据类型 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">类型：</label>
                        <div class="bk-form-content credential-type-content">
                            <selector
                                :list="ticketType"
                                :value="localConfig.credentialType"
                                class="credential-type-selector"
                                :disabled="nameReadOnly || typeReadOnly"
                                :item-selected="changeTicketType"
                            >
                            </selector>
                            <bk-popover placement="right">
                                <i class="bk-icon icon-info-circle"></i>
                                <div slot="content" style="white-space: normal;">
                                    <div> {{ getTypeDesc(localConfig.credentialType) }}<a style="color:#3c96ff" target="_blank" :href="`${DOCS_URL_PREFIX}/所有服务/凭证管理/summary.html`">了解更多。</a> </div>
                                </div>
                            </bk-popover>

                        </div>
                    </div>
                    <!-- 凭据类型 end -->

                    <!-- 凭据名称 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">名称：</label>
                        <div class="bk-form-content">
                            <input type="text" name="credentialId" v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\_]{1,30}$/ }" class="bk-form-input" placeholder="凭据名称不能为空，且只支持英文大小写、数字、下划线和英文句号"
                                v-model="localConfig.credentialId"
                                :class="{
                                    'is-danger': errors.has('credentialId')
                                }"
                                :disabled="nameReadOnly"
                            >
                            <p class="error-tips"
                                v-show="errors.has('credentialId')">
                                凭据名称不能为空，且只支持英文大小写、数字、下划线和英文句号，长度不能超过30个字符
                            </p>
                        </div>
                    </div>
                    <!-- 凭据名称 end -->

                    <!-- 凭据内容 start -->
                    <div v-for="(obj, key) in newModel" :key="key" :class="{ &quot;bk-form-item&quot;: true, &quot;is-required&quot;: obj.rules }">
                        <label v-if="obj.label" class="bk-label">{{ obj.label }}：</label>
                        <div class="bk-form-content">
                            <a v-if="obj.type === 'password' && localConfig.credential[obj.modelName] !== '******'" href="javascript:;" @click="toggleShowPwdCon(obj.modelName)"><i :class="showPwdCon[obj.modelName] ? 'bk-icon icon-hide' : 'bk-icon icon-eye'"></i></a>
                            <component v-validate="(obj.label === 'ssh私钥' && localConfig.credential[obj.modelName] === '******') ? {} : obj.rule" v-if="obj.type !== 'password' || !showPwdCon[obj.modelName]" :is="obj.component" :name="key" :handle-change="updateElement" v-model="localConfig.credential[obj.modelName]" v-bind="obj" :class="{ 'is-danger': errors.has(key) }"></component>
                            <component v-validate="obj.rule" v-if="obj.type === 'password' && showPwdCon[obj.modelName]" :is="obj.component" :name="key" :handle-change="updateElement" v-model="localConfig.credential[obj.modelName]" type="text" v-bind="obj" :class="{ 'is-danger': errors.has(key) }"></component>
                            <p class="error-tips"
                                v-show="errors.has(key)">
                                {{obj.errorMsg}}
                            </p>
                        </div>
                    </div>
                    <!-- 凭据内容 end -->

                    <!-- 凭据描述 start -->
                    <div class="bk-form-item cre-content">
                        <label class="bk-label">描述：</label>
                        <div class="bk-form-content">
                            <textarea class="bk-form-textarea" placeholder="请输入凭据描述" name="credentialDesc" v-validate="{ required: false, max: 50 }"
                                :class="{
                                    'is-danger': errors.has('credentialDesc')
                                }"
                                v-model="localConfig.credentialRemark"
                            ></textarea>
                            <p class="error-tips"
                                v-show="errors.has('credentialDesc')">
                                凭据描述长度不能超过50个字符
                            </p>
                        </div>
                    </div>
                    <!-- 凭据描述 end -->

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
    import empty from '@/components/common/empty'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import Selector from '@/components/atomFormField/Selector'
    import emptyTips from '@/components/devops/emptyTips'
    import { mapState, mapGetters } from 'vuex'

    export default {
        components: {
            'vue-input': VuexInput,
            'vue-textarea': VuexTextarea,
            'bk-empty': empty,
            innerHeader,
            emptyTips,
            Selector
        },
        data () {
            return {
                DOCS_URL_PREFIX: DOCS_URL_PREFIX,
                showContent: false,
                hasPermission: true,
                newModel: {},
                pageType: 'create',
                localConfig: {},
                nameReadOnly: false,
                typeReadOnly: false,
                showPwdCon: {
                    v1: false,
                    v2: false,
                    v3: false,
                    v4: false
                },
                loading: {
                    isLoading: true,
                    title: '数据加载中，请稍候'
                },
                editInitCredential: {
                    v1: '******',
                    v2: '******',
                    v3: '******',
                    v4: '******'
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
            ...mapState('ticket', [
                'ticket',
                'ticketType'
            ]),
            ...mapGetters('ticket', [
                'getTicketByType',
                'getDefaultCredential'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            creId () {
                return this.$route.params.credentialId
            },
            type () {
                return this.$route.params.type ? this.$route.params.type : 'PASSWORD'
            },
            routeName () {
                return this.$route.name
            }
        },
        watch: {
            routeName: async function () {
                await this.handleCreate()
                if (this.$route.params.locked) {
                    this.typeReadOnly = true
                } else {
                    this.typeReadOnly = false
                }
            },
            projectId: async function () {
                await this.handleCreate()
                if (this.$route.params.locked) {
                    this.typeReadOnly = true
                } else {
                    this.typeReadOnly = false
                }
            }
        },
        async created () {
            await this.handleCreate()
            if (this.$route.params.locked) {
                this.typeReadOnly = true
            }
        },
        methods: {
            updateElement () {

            },
            toggleShowPwdCon (modelName) {
                this.showPwdCon[modelName] = !this.showPwdCon[modelName]
            },
            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },
            goToApplyPerm () {
                const url = `/backend/api/perm/apply/subsystem/?client_id=ticket&project_code=${this.projectId}&service_code=ticket&role_creator=credential`
                window.open(url, '_blank')
            },
            cancel () {
                this.$router.push({
                    'name': 'credentialList'
                })
            },
            getTypeDesc (type) {
                const curType = this.ticketType.find(item => item.id === type)
                return curType.desc ? curType.desc : type
            },
            async submit () {
                this.$validator.validateAll().then(async (result) => {
                    if (result) {
                        const credential = {
                            'credentialId': this.localConfig.credentialId,
                            'credentialRemark': this.localConfig.credentialRemark,
                            'credentialType': this.localConfig.credentialType
                        }
                        for (const key in this.newModel) {
                            Object.assign(credential, { [key]: this.localConfig.credential[key] })
                        }
                        let message, theme
                        try {
                            if (this.pageType === 'create') {
                                await this.$store.dispatch('ticket/createCredential', {
                                    projectId: this.projectId,
                                    credential
                                })
                            } else {
                                await this.$store.dispatch('ticket/editCredential', {
                                    projectId: this.projectId,
                                    creId: this.localConfig.credentialId,
                                    credential
                                })
                            }
                            message = '保存凭据成功'
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            if (theme === 'success') {
                                this.$router.push({
                                    'name': 'credentialList'
                                })
                            }
                        }
                    }
                })
            },
            changeTicketType (id, data) {
                this.$router.push({
                    name: 'createCredentialWithType',
                    params: {
                        'type': id
                    }
                })
                this.newModel = this.getTicketByType(id)
                this.localConfig.credential = {
                    v1: '',
                    v2: '',
                    v3: '',
                    v4: ''
                }
                this.showPwdCon = {
                    v1: false,
                    v2: false,
                    v3: false,
                    v4: false
                }
            },
            async handleCreate () {
                this.localConfig = {
                    credentialId: '',
                    credentialType: 'PASSWORD',
                    credentialRemark: '',
                    credential: {
                        v1: '',
                        v2: '',
                        v3: '',
                        v4: ''
                    }
                }
                if (this.$route.name === 'editCredential') {
                    this.pageType = 'edit'
                    this.nameReadOnly = true
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('ticket/requestCredentialDetail', {
                            projectId: this.projectId,
                            creId: this.creId
                        })
                        const data = res
                        Object.assign(this.localConfig, {
                            credentialId: data.credentialId,
                            credentialRemark: data.credentialRemark,
                            credentialType: data.credentialType,
                            credential: this.editInitCredential
                        })
                        this.newModel = this.getTicketByType(data.credentialType)
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'
                        this.$bkMessage({
                            message,
                            theme
                        })
                    } finally {
                        this.loading.isLoading = false
                    }
                } else {
                    this.pageType = 'create'
                    this.nameReadOnly = false
                    this.localConfig.credentialType = this.type
                    this.newModel = this.getTicketByType(this.type)
                    this.loading.isLoading = true
                    try {
                        const res = await this.$store.dispatch('ticket/requestCredentialPermission', {
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
                    }
                }
                this.showContent = true
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .operate-btn {
        margin: 30px 0 0 140px
    }
    .credential-setting {
        width: 100%;
        max-width: initial;
        padding: 42px 0 0 37px;
        .bk-form-wrapper {
            max-width: 750px;
            margin-top: -15px
        }
        .bk-label {
            margin-top: 13px;
            min-width: 120px;
            width: auto;
        }
        .bk-form-content {
            margin-left: 140px;
            margin-top: 10px;
            .error-tips {
                max-width: 550px;
                font-size: 12px;
                padding-top: 6px;
            }
        }
        .bk-form-input, .bk-form-password, .bk-selector, .bk-form-textarea {
            width: 90%
        }
        .credential-type-content {
            display: flex;
            align-items: center;
        }
        .credential-type-selector {
            display: inline-block;
            width: 550px;
        }
        .icon-hide,.icon-eye {
            right: 4%;
            position: absolute;
            padding: 10px;
            color: #808080;
        }
        .icon-info-circle {
            padding-left: 8px;
            color: #C3CDD7;
            font-size: 14px;
        }
        .link-tips {
            margin-left: 28px;
        }
    }
</style>
