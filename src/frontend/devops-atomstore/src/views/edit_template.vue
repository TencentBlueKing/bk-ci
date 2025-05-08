<template>
    <div
        class="edit-template-wrapper"
        v-bkloading="{ isLoading: loading.isLoading, title: loading.title }"
    >
        <bread-crumbs
            :bread-crumbs="navList"
            type="template"
        >
            <a
                class="g-title-work"
                target="_blank"
                :href="docsLink"
            > {{ $t('store.模板指引') }} </a>
        </bread-crumbs>
        <div
            class="edit-template-content"
            v-if="showContent"
        >
            <form class="bk-form edit-template-form">
                <div
                    v-for="container in panels"
                    :key="container.name"
                >
                    <component
                        :ref="container.name"
                        :is="container.component"
                        v-bind="container.props"
                        @updateTemplateForm="updateTemplateForm"
                    />
                </div>
            </form>
        </div>
        <div class="form-footer">
            <div>
                <button
                    class="bk-button bk-primary"
                    type="button"
                    @click="submit()"
                >
                    {{ $t('store.提交上架') }}
                </button>
                <button
                    class="bk-button bk-default"
                    type="button"
                    @click="$router.back()"
                >
                    {{ $t('store.取消') }}
                </button>
            </div>
        </div>
    </div>
</template>

<script>
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import { PublishInfo, TemplateInfo } from '@/components/editContent'

    export default {
        components: {
            breadCrumbs,
            TemplateInfo,
            PublishInfo
        },
        data () {
            return {
                showContent: false,
                docsLink: this.BKCI_DOCS.TEMPLATE_GUIDE_DOC,
                loading: {
                    isLoading: false,
                    title: ''
                },
                templateForm: {
                    projectCode: '',
                    templateVersion: '',
                    publishStrategy: 'MANUAL',
                    fullScopeVisible: true,
                    templateName: '',
                    templateType: 'PIPELINE',
                    releaseFlag: false,
                    classifyCode: '',
                    categoryIdList: [],
                    labelIdList: [],
                    summary: '',
                    description: `#### ${this.$t('store.模板功能')}\n\n#### ${this.$t('store.适用场景')}\n\n#### ${this.$t('store["使用限制和受限解决方案[可选]"]')}\n\n#### ${this.$t('store.常见的失败原因和解决方案')}`,
                    publisher: '',
                    pubDescription: '',
                    logoUrl: '',
                    iconData: ''
                }
            }
        },
        computed: {
            type () {
                return this.$route.query.type
            },
            hasSourceInfo () {
                return this.$route.query.hasSourceInfo
            },
            templateCode () {
                return this.$route.params.templateCode
            },
            navList () {
                return [
                    { name: this.$t('store.工作台'), to: { name: 'templateWork' } },
                    { name: this.$t('store.上架模板') }
                ]
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            },
            panels () {
                return [{
                    name: 'TemplateInfo',
                    component: TemplateInfo,
                    props: {
                        templateForm: this.templateForm,
                        type: this.type,
                        isShowInfo: this.hasSourceInfo || this.type === 'edit'
                    }
                }, {
                    name: 'PublishInfo',
                    component: PublishInfo,
                    props: {
                        templateForm: this.templateForm
                    }
                }]
            }
        },
        async created () {
            this.init()
        },
        methods: {
            async init () {
                if (this.hasSourceInfo) {
                    const { projectCode, templateCode, templateName } = this.$route.query
                    Object.assign(this.templateForm, {
                        projectCode,
                        templateCode,
                        templateName
                    }, {})
                    this.showContent = true
                } else if (this.type === 'apply') {
                    this.showContent = true
                } else {
                    await this.requestTemplateDetail()
                }
            },
            async requestTemplateDetail () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestTemplateDetail', this.templateCode)
                    Object.assign(this.templateForm, res, {
                        fullScopeVisible: res.storeVisibleDept.fullScopeVisible
                    })
                    this.templateForm.categoryIdList = this.templateForm.categoryList.map(item => {
                        return item.id
                    })
                    this.templateForm.labelIdList = (this.templateForm.labelList || []).map(item => {
                        return item.id
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
                        this.showContent = true
                    }, 500)
                }
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            toPublishProgress (templateCode) {
                this.$router.push({
                    name: 'upgradeTemplate',
                    params: {
                        templateCode
                    }
                })
            },
            updateTemplateForm (updatedProps) {
                this.templateForm = {
                    ...this.templateForm,
                    ...updatedProps
                }
            },
            async isValid () {
                const TemplateInfoValid = await this.$refs.TemplateInfo[0].validate()
                const PublishInfo = await this.$refs.PublishInfo[0].validate()
                const isTemplateInfoCheckValid = this.$refs.TemplateInfo[0].checkValid()
                return TemplateInfoValid && PublishInfo && isTemplateInfoCheckValid
            },
            async submit () {
                const valid = await this.isValid()
                if (valid) {
                    let message, theme
                    
                    try {
                        this.loading.isLoading = true

                        const params = {
                            projectCode: this.templateForm.projectCode,
                            templateVersion: this.templateForm.templateVersion,
                            publishStrategy: this.templateForm.publishStrategy,
                            fullScopeVisible: this.templateForm.fullScopeVisible,
                            templateCode: this.templateForm.templateCode,
                            templateName: this.templateForm.templateName,
                            templateType: this.templateForm.templateType,
                            categoryIdList: this.templateForm.categoryIdList,
                            classifyCode: this.templateForm.classifyCode,
                            labelIdList: this.templateForm.labelIdList.filter(i => i !== 'null' && i !== ' ' && i),
                            publisher: this.templateForm.publisher,
                            logoUrl: this.templateForm.logoUrl || undefined,
                            iconData: this.templateForm.iconData || undefined,
                            summary: this.templateForm.summary || undefined,
                            description: this.templateForm.description || undefined,
                            pubDescription: this.templateForm.pubDescription || undefined
                        }

                        const res = await this.$store.dispatch('store/releaseTemplate', {
                            params: params
                        })

                        message = this.$t('store.提交成功')
                        theme = 'success'
                        if (res) {
                            this.toPublishProgress(res)
                        }
                    } catch (err) {
                        if (err.httpStatus === 200) {
                            const h = this.$createElement

                            this.$bkInfo({
                                type: 'error',
                                title: this.$t('store.提交失败'),
                                showFooter: false,
                                subHeader: h('p', {
                                    style: {
                                        textDecoration: 'none',
                                        cursor: 'pointer',
                                        whiteSpace: 'normal',
                                        textAlign: 'left'
                                    }
                                }, err.message ? err.message : err)
                            })
                        } else {
                            message = err.message ? err.message : err
                            theme = 'error'
                        }
                    } finally {
                        if (theme === 'error') {
                            this.$bkMessage({
                                message,
                                theme
                            })
                        }
                        
                        this.loading.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';

    .edit-template-wrapper {
        position: relative;
        height: 100%;
        .edit-template-content {
            margin: 20px 0;
            height: calc(100% - 5.6vh - 88px);
            overflow: auto;
            display: flex;
            justify-content: center;
            padding-bottom: 20px;
        }
        .edit-template-form {
            position: relative;
            margin: 0 auto;
            width: 1200px;

            .container {
                background-color: #fff;
                padding: 12px 24px 30px 24px;
                margin-bottom: 24px;
                box-shadow: 0 2px 4px 0 #1919290d;
                border-radius: 2px;
                
                .form-title {
                    margin-top: 20px;
                    margin-bottom: 25px;
                    font-weight: bold;
                    font-size: 16px;
                    color: #4d4f56;
                }

                .form-template {
                    margin-bottom: 25px;
                    padding-bottom: 25px;
                    border-bottom: 1px solid #e8e9ee;
                }

                .form-item-container {
                    width: 94%;
                }

                .logo-label {
                    position: absolute;
                    right: 120px;
                    top: 0;
                }

                .logo-label::after {
                    color: #ea3636;
                    content: "*";
                    display: inline-block;
                    font-size: 12px;
                    height: 8px;
                    line-height: 1;
                    position: absolute;
                    top: 50%;
                    transform: translate(3px, -50%);
                    vertical-align: middle;
                }
            }
            
            .strategy .bk-radio-text{
                border-bottom: 1px dashed #bbbec4;
            }
            
            .bk-label {
                width: 150px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 150px;
            }
            .bk-selector .bk-form-checkbox {
                display: block;
                padding: 12px 0;
            }
            .fixed-width {
                width: 72%;
            }
            .version {
                width: 25%;
            }
            .template-category-content {
                .bk-selector{
                    width: 40%;
                    .bk-form-checkbox {
                        display: block;
                        padding: 12px 0;
                    }
                }
            }
            .bk-form-checkbox {
                margin-right: 40px;
            }
            .form-tooltips {
                max-width: 210px;
                text-align: left;
                white-space: normal;
                word-break: break-all;
                font-weight: 400;
            }
            .introduction-form-item {
                .error-tips {
                    margin-left: 150px;
                }
            }
            .name-form-item,
            .introduction-form-item,
            .remark-form-item,
            .publish-form-item,
            .version-num-form-item {
                .bk-tooltip {
                    margin-top: 10px;
                    margin-left: 10px;
                    color: $fontLigtherColor;
                    p {
                        max-width: 400px;
                        text-align: left;
                        white-space: normal;
                        word-break: break-all;
                        font-weight: 400;
                    }
                }
            }
            .is-tooltips {
                display: flex;
                line-height: 0;
            }
            .introduction-form-item,
            .remark-form-item,
            .publish-form-item,
            .version-num-form-item  {
                .bk-tooltip {
                    left: 101%;
                }
            }
            .template-introduction-input,
            .template-remark-input {
                min-width: 100%;
                border: 1px solid #c4c6cc;
            }
            .version-num-form-item {
                .version-num-content {
                    position: relative;
                    top: 8px;
                    color: #333C48;
                }
                .version-prompt {
                    margin-left: 20px;
                    color: $fontWeightColor;
                }
            }
            .bk-radio-text {
                color: #333C48;
            }
            .tips-text {
                padding-bottom: 3px;
                border-bottom: 1px dashed #c3cdd7;
            }
            .env-checkbox {
                .devops-icon {
                    position: relative;
                    top: 3px;
                    font-size: 18px;
                    color: #979BA5;
                }
                .bk-checkbox-text {
                    color: #333C48;
                }
                >svg {
                    position: relative;
                    top: 5px;
                }
            }
            .template-remark-input {
                height: 263px;
                &.fullscreen {
                    height: auto;
                }
            }
            .version-msg {
                padding: 12px 0 12px 26px;
            }
            .cut-line {
                margin-top: 8px;
                height: 1px;
                border: none;
                background-color: #C3CDD7
            }
            .template-versionlog-input {
                padding: 10px;
                height: 80px;
            }
            .template-logo-box {
                position: absolute;
                top: 0;
                right: 0;
                width: 100px;
                height: 100px;
                background: #fff;
                border: 1px dashed $lineColor;
                text-align: center;
                cursor: pointer;
                .icon-plus {
                    display: inline-block;
                    margin-top: 30px;
                    font-size: 19px;
                    color: #979BA5;
                }
                p {
                    margin-top: 4px;
                    font-size: 12px;
                }
                img {
                    position: relative;
                    width: 100px;
                    height: 100px;
                    z-index: 99;
                    object-fit: cover;
                }
            }
            .no-img {
                border: none;
                background: transparent;
                cursor: pointer;
                &:hover {
                    &:after {
                        content: '\66F4\6362logo';
                        position: absolute;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        z-index: 100;
                        line-height: 25px;
                        text-align: center;
                        color: #fff;
                        background: black;
                        opacity: 0.7;
                    }
                }
            }
            .img-Error {
                border: 1px dashed $dangerColor;
                .error-msg {
                    color: $dangerColor;
                }
            }
            .category {
                margin-top: 5px;
            }
            .category-icon {
                position: absolute;
                top: 0;
                width: 18px;
                height: 18px;
            }
        }
        .auto-textarea-wrapper {
            min-height: 200px;
        }
        .form-footer {
            position: fixed;
            bottom: 0;
            left: 0;
            z-index: 9;
            background-color: #fafbfd;
            width: 100%;
            height: 48px;
            line-height: 48px;
            border-top: 1px solid #e5e7ec;

            div {
                margin: 0 auto;
            width: 1200px;
            }
        }
    }
    .error-commit {
        .bk-dialog-default-status {
            padding-top: 10px;
        }
    }
</style>
