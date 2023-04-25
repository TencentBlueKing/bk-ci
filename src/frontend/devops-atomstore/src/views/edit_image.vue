<template>
    <article class="edit-image-home">
        <bread-crumbs :bread-crumbs="navList" type="image">
            <a class="g-title-work" target="_blank" :href="docsLink"> {{ $t('store.镜像指引') }} </a>
        </bread-crumbs>
        <main v-bkloading="{ isLoading }" class="edit-content">
            <bk-form ref="imageForm" class="edit-image" label-width="150" :model="form" v-show="!isLoading">
                <bk-form-item class="wt660"
                    :label="$t('store.镜像名称')"
                    :required="true"
                    property="imageName"
                    :rules="[requireRule]"
                    ref="imageName"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.imageName" :placeholder="$t('store.请输入镜像名称')"></bk-input>
                </bk-form-item>
                <bk-form-item class="wt660"
                    :label="$t('store.范畴')"
                    property="category"
                    :required="true"
                    :rules="[requireRule]"
                    ref="category"
                    error-display-type="normal"
                >
                    <bk-select v-model="form.category" searchable>
                        <bk-option v-for="(option, index) in categoryList"
                            :key="index"
                            :id="option.categoryCode"
                            :name="option.categoryName"
                            :placeholder="$t('store.请选择范畴')"
                            @click.native="changeShowAgentType(option)"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item class="wt660"
                    :label="$t('store.分类')"
                    :required="true"
                    property="classifyCode"
                    :rules="[requireRule]"
                    ref="classifyCode"
                    error-display-type="normal"
                >
                    <bk-select v-model="form.classifyCode" searchable>
                        <bk-option v-for="(option, index) in classifys"
                            :key="index"
                            :id="option.classifyCode"
                            :name="option.classifyName"
                            :placeholder="$t('store.请选择分类')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('store.标签')" property="labelIdList">
                    <bk-tag-input v-model="form.labelIdList" :list="labelList" display-key="labelName" search-key="labelName" trigger="focus" :placeholder="$t('store.请选择标签')"></bk-tag-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.适用机器')"
                    property="agentTypeScope"
                    :required="true"
                    :rules="[requireRule]"
                    ref="agentTypeScope"
                    v-if="needAgentType"
                    error-display-type="normal"
                >
                    <bk-select
                        v-model="form.agentTypeScope"
                        searchable
                        multiple
                        show-select-all>
                        <bk-option v-for="(option, index) in agentTypes"
                            :key="index"
                            :id="option.code"
                            :name="option.name"
                            :placeholder="$t('store.请选择适用机器')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('store.简介')"
                    property="summary"
                    :required="true"
                    :rules="[requireRule]"
                    ref="summary"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.summary" :placeholder="$t('store.请输入简介')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.描述')" property="description">
                    <mavon-editor class="image-remark-input"
                        ref="mdHook"
                        preview-background="#fff"
                        v-model="form.description"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        :language="mavenLang"
                        @imgAdd="uploadimg('mdHook', ...arguments)"
                    />
                </bk-form-item>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('store.镜像信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('store.镜像源')"
                    :required="true"
                    property="imageSourceType"
                    class="h32"
                    :rules="[requireRule]"
                    ref="imageSourceType"
                    error-display-type="normal"
                >
                    <bk-radio-group v-model="form.imageSourceType" @change="clearRepo">
                        <bk-radio value="THIRD"> {{ $t('store.第三方源') }} </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item :label="$t('store.源镜像库地址')" property="imageRepoUrl" :desc="$t('store.请输入源镜像库地址。若源为 docker hub，可留空不填')">
                    <bk-input v-model="form.imageRepoUrl" :placeholder="$t('store.imageRepoUrl')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.源镜像名称')"
                    property="imageRepoName"
                    :required="true"
                    :rules="[requireRule]"
                    ref="imageRepoName"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.imageRepoName" :placeholder="$t('store.请输入源镜像名称，如 XXX/XXXX')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.源镜像Tag')"
                    property="imageTag"
                    :desc="$t('store.不建议使用可变功能的Tag（如latest），避免镜像变更导致关联流水线不能正常执行')"
                    :required="true"
                    :rules="[requireRule]"
                    ref="imageTag"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.imageTag" :placeholder="$t('store.imageTag')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.凭证')" property="ticketId" :desc="$t('store.若为私有镜像，请提供凭证，用于流水线执行时拉取镜像')">
                    <bk-select v-model="form.ticketId" searchable :placeholder="$t('store.请选择凭证')">
                        <bk-option v-for="option in ticketList"
                            :key="option.credentialId"
                            :id="option.credentialId"
                            :name="option.credentialId">
                        </bk-option>
                        <a v-if="form.projectCode" :href="`/console/ticket/${form.projectCode}/createCredential/USERNAME_PASSWORD/true`" slot="extension" target="_blank"> {{ $t('store.新增凭证') }} </a>
                    </bk-select>
                </bk-form-item>
                <bk-form-item label="Dockerfile Type"
                    :required="true"
                    property="dockerFileType"
                    class="h32"
                    :rules="[requireRule]"
                    ref="dockerFileType"
                    error-display-type="normal"
                >
                    <bk-radio-group v-model="form.dockerFileType" @change="form.dockerFileContent = ''">
                        <bk-radio value="INPUT" class="mr12"> {{ $t('store.手动录入') }} </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item label="Dockerfile" property="dockerFileContent" ref="dockerFileContent">
                    <code-section :code="form.dockerFileContent" :cursor-blink-rate="530" :read-only="false" ref="codeEditor" />
                </bk-form-item>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('store.版本信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('store.发布类型')"
                    :required="true"
                    property="releaseType"
                    class="h32"
                    :rules="[requireRule]"
                    ref="releaseType"
                    v-if="form.releaseType !== 'CANCEL_RE_RELEASE'"
                    error-display-type="normal"
                >
                    <bk-radio-group v-model="form.releaseType">
                        <bk-radio value="NEW" class="mr12" v-if="form.imageStatus === 'INIT'"> {{ $t('store.新上架') }} </bk-radio>
                        <template v-else>
                            <bk-radio value="INCOMPATIBILITY_UPGRADE" class="mr12"> {{ $t('store.非兼容升级') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_UPGRADE" class="mr12"> {{ $t('store.兼容式功能更新') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_FIX"> {{ $t('store.兼容式问题修正') }} </bk-radio>
                        </template>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item :label="$t('store.版本号')" property="version" class="lh30" :required="true">
                    <span>{{$t('store.semverType', [form.version])}}</span>
                    <span class="version-modify" @click="form.releaseType = 'COMPATIBILITY_FIX'" v-if="form.releaseType === 'CANCEL_RE_RELEASE'"> {{ $t('store.修改') }} </span>
                </bk-form-item>
                <bk-form-item :label="$t('store.发布者')"
                    :required="true"
                    property="publisher"
                    :rules="[requireRule]"
                    ref="publisher"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.publisher" :placeholder="$t('store.请输入发布者')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.版本日志')"
                    :required="true"
                    property="versionContent"
                    :rules="[requireRule]"
                    ref="versionContent"
                    error-display-type="normal"
                >
                    <mavon-editor class="image-remark-input"
                        :placeholder="$t('store.请输入版本日志')"
                        ref="versionMd"
                        preview-background="#fff"
                        v-model="form.versionContent"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        :language="mavenLang"
                        @imgAdd="uploadimg('versionMd', ...arguments)"
                    />
                </bk-form-item>
                <select-logo ref="selectLogo" label="Logo" :form="form" type="IMAGE" :is-err="logoErr" right="25"></select-logo>
            </bk-form>
            <section class="edit-image button-padding" v-show="!isLoading">
                <bk-button theme="primary" @click="submitImage"> {{ $t('store.提交') }} </bk-button>
                <bk-button @click="$router.back()"> {{ $t('store.取消') }} </bk-button>
            </section>
        </main>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'
    import codeSection from '@/components/common/detailTab/codeSection'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    export default {
        components: {
            selectLogo,
            codeSection,
            breadCrumbs
        },

        data () {
            return {
                form: {
                    imageId: '',
                    imageName: '',
                    imageCode: '',
                    classifyCode: '',
                    labelIdList: [],
                    labelList: [],
                    summary: '',
                    description: '',
                    logoUrl: '',
                    iconData: '',
                    imageSourceType: 'THIRD',
                    dockerFileType: 'INPUT',
                    dockerFileContent: '',
                    imageRepoUrl: '',
                    imageRepoName: '',
                    imageTag: '',
                    releaseType: '',
                    version: '1.0.0',
                    publisher: '',
                    versionContent: '',
                    ticketId: '',
                    projectCode: '',
                    category: '',
                    agentTypeScope: []
                },
                docsLink: this.BKCI_DOCS.IMAGE_GUIDE_DOC,
                ticketList: [],
                classifys: [],
                labelList: [],
                categoryList: [],
                agentTypes: [],
                imageList: [],
                imageVersionList: [],
                isLoading: false,
                isLoadingTag: false,
                needAgentType: false,
                originVersion: '',
                requireRule: {
                    required: true,
                    message: this.$t('store.必填项'),
                    trigger: 'blur'
                },
                logoErr: false,
                toolbars
            }
        },

        computed: {
            navList () {
                return [
                    { name: this.$t('store.工作台'), to: { name: 'imageWork' } },
                    { name: `${this.$t('store.上架/升级镜像')}（${this.form.imageCode}）` }
                ]
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },

        watch: {
            'form.releaseType': {
                handler (val) {
                    switch (val) {
                        case 'NEW':
                            this.form.version = '1.0.0'
                            break
                        case 'INCOMPATIBILITY_UPGRADE':
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${+b + 1}.0.0`))
                            break
                        case 'COMPATIBILITY_UPGRADE':
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${b}.${+c + 1}.0`))
                            break
                        case 'COMPATIBILITY_FIX':
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${b}.${c}.${+d + 1}`))
                            break
                        default:
                            break
                    }
                },
                immediate: true
            }
        },

        mounted () {
            this.getImageDetail()
        },

        methods: {
            ...mapActions('store', [
                'requestImageDetail',
                'requestImageClassifys',
                'requestImageLabel',
                'requestImageCategorys',
                'requestImageList',
                'requestImageTagList',
                'requestTicketList',
                'requestReleaseImage',
                'fetchAgentTypes'
            ]),

            changeShowAgentType (option) {
                const settings = option.settings || {}
                this.needAgentType = settings.needAgentType === 'NEED_AGENT_TYPE_TRUE'
            },

            submitImage () {
                if (this.form.dockerFileType === 'INPUT') this.form.dockerFileContent = this.$refs.codeEditor.getValue()
                this.$refs.imageForm.validate().then(() => {
                    if (!this.form.logoUrl && !this.form.iconData) {
                        this.logoErr = true
                        const err = { field: 'selectLogo' }
                        throw err
                    }
                    this.isLoading = true
                    this.requestReleaseImage(this.form).then((imageId) => {
                        this.$bkMessage({ message: this.$t('store.提交成功'), theme: 'success' })
                        this.$router.push({ name: 'imageProgress', params: { imageId } })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isLoading = false
                    })
                }).catch((validate) => {
                    const field = validate.field
                    const label = this.$refs[field].label
                    this.$bkMessage({ message: `${label + this.$t('store.输入不正确，请确认修改后再试')}`, theme: 'error' })
                })
            },

            clearRepo () {
                this.form.imageRepoUrl = ''
                this.form.imageRepoName = ''
                this.form.imageTag = ''
                this.form.ticketId = ''
            },

            getImageTagList (option) {
                this.form.imageRepoUrl = option.repoUrl
                this.form.imageTag = ''
                this.isLoadingTag = true

                const imageRepo = option.repo
                const imageId = this.form.imageId
                this.requestImageTagList({ imageRepo, imageId }).then((res) => {
                    this.imageVersionList = res.tags || []
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoadingTag = false
                })
            },

            getImageDetail () {
                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                this.isLoading = true
                this.requestImageDetail(imageId).then((res) => {
                    Object.assign(this.form, res)
                    if (res.imageStatus === 'RELEASED') this.form.imageTag = ''
                    this.form.description = this.form.description || this.$t('store.imageMdDesc')
                    this.originVersion = res.version
                    this.form.labelIdList = res.labelList.map(x => x.id)

                    switch (res.imageStatus) {
                        case 'INIT':
                            this.form.releaseType = 'NEW'
                            break
                        case 'GROUNDING_SUSPENSION':
                            this.form.releaseType = 'CANCEL_RE_RELEASE'
                            break
                        default:
                            this.form.releaseType = 'COMPATIBILITY_FIX'
                            break
                    }

                    return Promise.all([
                        this.requestImageClassifys(),
                        this.requestImageLabel(),
                        this.requestTicketList({ projectCode: res.projectCode }),
                        this.fetchAgentTypes(),
                        this.requestImageCategorys()]).then(([classifys, labels, ticket, agents, categorys]) => {
                            this.classifys = classifys
                            this.labelList = labels
                            this.categoryList = categorys
                            this.agentTypes = agents
                            this.ticketList = ticket.records || []
                            const currentCategory = categorys.find((category) => (res.category === category.categoryCode)) || {}
                            const settings = currentCategory.settings || {}
                            this.needAgentType = settings.needAgentType === 'NEED_AGENT_TYPE_TRUE'
                            
                            if (this.form.imageRepoName && this.form.imageSourceType === 'BKDEVOPS') {
                                const imageRepo = this.form.imageRepoName
                                const imageId = this.form.imageId
                                return Promise.all([this.requestImageList(res.projectCode), this.requestImageTagList({ imageRepo, imageId })]).then(([imageList, res]) => {
                                    this.imageList = imageList.imageList
                                    const resData = res || {}
                                    this.form.imageRepoUrl = resData.repoUrl
                                    this.imageVersionList = resData.tags || []
                                })
                            }
                        })
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            getImageList (isExpand) {
                if (!isExpand) return
                const code = this.form.projectCode
                this.requestImageList(code).then((imageList) => {
                    this.imageList = imageList.imageList
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' }))
            },

            async uploadimg (ref, pos, file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs[ref].$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs[ref].$refs.toolbar_left.$imgDel(pos)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .edit-image-home {
        height: 100%;
        overflow: hidden;
    }

    .dockerfile {
        height: 400px;
        overflow: auto;
        background: black;
        ::v-deep .CodeMirror {
            font-family: Consolas, "Courier New", monospace;
            line-height: 1.5;
            padding: 10px;
            height: auto;
        }
    }

    .button-padding {
        padding-left: 150px;
    }

    .version-msg {
        margin: 30px 0 20px;
    }

    .mr12 {
        margin-right: 12px;
    }

    .lh30 {
        line-height: 30px;
    }

    .mt10 {
        margin-top: 10px;
    }

    .edit-content {
        height: calc(100% - 5.6vh);
        overflow: auto;
    }

    .edit-image {
        width: 1200px;
        margin: 20px auto;
        position: relative;
        .image-remark-input {
            border: 1px solid #c4c6cc;
            height: 263px;
            &.fullscreen {
                height: auto;
            }
        }
        .bk-form-control {
            vertical-align: baseline;
        }
    }

    .version-modify {
        cursor: pointer;
        color: $primaryColor;
        margin-left: 3px;
    }

    .bk-form-item {
        padding-right: 25px;
        &.is-error .bk-select {
            border-color: $dangerColor;
        }
    }

    .wt660 {
        width: 660px;
    }
</style>
