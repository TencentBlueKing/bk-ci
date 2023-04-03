<template>
    <div class="edit-atom-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <bread-crumbs :bread-crumbs="navList" type="atom">
            <a class="g-title-work" target="_blank" :href="docsLink"> {{ $t('store.插件指引') }} </a>
        </bread-crumbs>

        <div class="edit-atom-content" v-if="showContent">
            <div class="bk-form edit-atom-form g-form-radio">
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label"> {{ $t('store.名称') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <div style="width: 40%;">
                            <input type="text" class="bk-form-input atom-name-input" :placeholder="$t('store.请输入中英文名称')"
                                ref="atomName"
                                name="atomName"
                                v-model="atomForm.name"
                                v-validate="{
                                    required: true,
                                    max: 40
                                }"
                                :class="{ 'is-danger': errors.has('atomName') }">
                            <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">{{ errors.first("atomName") }}</p>
                        </div>
                        <bk-popover placement="right">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.插件名称不超过40个字符') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="bk-form-item is-required" ref="categoryError">
                    <label class="bk-label category-label"> {{ $t('store.范畴') }} </label>
                    <div class="bk-form-content atom-item-content">
                        <bk-radio-group v-model="atomForm.category" class="radio-group">
                            <bk-radio :value="entry.value" v-for="(entry, key) in categoryList" :key="key" @click.native="formErrors.categoryError = false">{{entry.label}}</bk-radio>
                        </bk-radio-group>
                        <div v-if="formErrors.categoryError" class="error-tips"> {{ $t('store.字段有误，请重新选择') }} </div>
                    </div>
                </div>
                <div class="bk-form-item  is-required" ref="sortError">
                    <label class="bk-label"> {{ $t('store.分类') }} </label>
                    <div class="bk-form-content atom-item-content atom-classify-content">
                        <bk-select v-model="atomForm.classifyCode" @selected="changeClassify" style="width: 40%;" searchable :clearable="false">
                            <bk-option v-for="(option, index) in sortList"
                                :key="index"
                                :id="option.classifyCode"
                                :name="option.classifyName">
                            </bk-option>
                        </bk-select>
                        <div v-if="formErrors.sortError" class="error-tips"> {{ $t('store.分类不能为空') }} </div>
                    </div>
                </div>
                <div class="bk-form-item is-required" ref="jobError">
                    <label class="bk-label env-label"> {{ $t('store.适用Job类型') }} </label>
                    <div class="bk-form-content atom-item-content">
                        <bk-radio-group v-model="atomForm.jobType" class="radio-group">
                            <span v-for="(entry, key) in jobTypeList" :key="key">
                                <bk-radio v-show="entry.isShow" :value="entry.value" @click.native="changeJobType">{{entry.label}}</bk-radio>
                            </span>
                        </bk-radio-group>
                        <div v-if="formErrors.jobError" class="error-tips"> {{ $t('store.字段有误，请重新选择') }} </div>
                    </div>
                </div>
                <bk-checkbox-group v-model="atomForm.os" v-if="atomForm.jobType === 'AGENT'" class="bk-form-content atom-os" ref="envError">
                    <bk-checkbox :value="entry.value" v-for="(entry, key) in envList" :key="key" @click.native="changeOs(entry.value)">
                        <p class="os-checkbox-label">
                            <i :class="{ 'devops-icon': true, [`icon-${entry.icon}`]: true }"></i>
                            <span class="bk-checkbox-text">{{ entry.label }}</span>
                        </p>
                    </bk-checkbox>
                </bk-checkbox-group>
                <div v-if="formErrors.envError" class="error-tips env-error"> {{ $t('store.字段有误，请重新选择') }} </div>
                <div class="bk-form-item">
                    <label class="bk-label"> {{ $t('store.功能标签') }} </label>
                    <div class="bk-form-content template-item-content">
                        <bk-select :placeholder="$t('store.请选择功能标签')"
                            v-model="atomForm.labelIdList"
                            @selected="changeClassify"
                            show-select-all
                            searchable
                            multiple
                        >
                            <bk-option v-for="(option, index) in labelList"
                                :key="index"
                                :id="option.id"
                                :name="option.labelName">
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
                <div class="bk-form-item introduction-form-item is-required">
                    <label class="bk-label"> {{ $t('store.简介') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <input type="text" class="bk-form-input atom-introduction-input" :placeholder="$t('store.插件一句话简介，不超过256个字符')"
                            name="introduction"
                            maxlength="256"
                            v-model="atomForm.summary"
                            v-validate="{
                                required: true,
                                max: 256
                            }"
                            :class="{ 'is-danger': errors.has('introduction') }">
                        <bk-popover placement="left">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.插件一句话简介，不超过256个字符。') }} </p>
                                <p> {{ $t('store.展示在插件市场以及流水线选择插件页面。') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                    <p :class="errors.has('introduction') ? 'error-tips' : 'normal-tips'">{{ errors.first("introduction") }}</p>
                </div>
                <div class="bk-form-item remark-form-item">
                    <label class="bk-label"> {{ $t('store.详细描述') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <mavon-editor class="atom-remark-input" :placeholder="descTemplate"
                            ref="mdHook"
                            v-model="atomForm.description"
                            :toolbars="toolbarOptions"
                            :external-link="false"
                            :box-shadow="false"
                            preview-background="#fff"
                            :language="mavenLang"
                            @imgAdd="addImage('mdHook', ...arguments)"
                            @imgDel="delImage"
                            @change="changeData"
                        />
                        <bk-popover placement="left">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.atomRemark') }} </p>
                                <p> {{ $t('store.展示在插件市场查看插件详情界面，帮助用户快速了解插件和解决遇到的问题。') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <section>
                    <div class="version-msg">
                        <p class="form-title"> {{ $t('store.配置') }} </p>
                        <hr class="cut-line">
                    </div>
                    <div class="bk-form-item is-required" ref="categoryError">
                        <label class="bk-label category-label"> {{ $t('store.自定义前端') }} </label>
                        <div class="bk-form-content atom-item-content">
                            <bk-radio-group v-model="atomForm.frontendType" class="radio-group">
                                <bk-radio :value="entry.value" :title="entry.title" v-for="(entry, key) in frontendTypeList" :key="key">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                </section>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('store.版本信息') }} </p>
                    <hr class="cut-line">
                </div>
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label"> {{ $t('store.发布者') }} </label>
                    <div class="bk-form-content atom-item-content">
                        <bk-select v-model="atomForm.publisher">
                            <bk-option v-for="publisher in publishersList" :key="publisher.id" :id="publisher.publisherCode" :name="publisher.publisherName"></bk-option>
                        </bk-select>
                    </div>
                </div>
                <div class="bk-form-item publish-form-item is-required" ref="releaseTypeError" v-if="atomForm.releaseType !== 'CANCEL_RE_RELEASE'">
                    <label class="bk-label publish-type-label"> {{ $t('store.发布类型') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips radio-flex">
                        <section v-if="atomForm.releaseType !== 'NEW'" style="min-width: 100%;">
                            <bk-radio-group v-model="atomForm.releaseType" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in publishTypeList" :key="key" @click.native="formErrors.releaseTypeError = false">
                                    <bk-popover placement="top" :delay="800" style="margin-top:0;margin-left:0;">
                                        <span class="bk-radio-text">{{entry.label}}</span>
                                        <template slot="content">
                                            <p>{{ entry.desc }}</p>
                                        </template>
                                    </bk-popover>
                                </bk-radio>
                            </bk-radio-group>
                            <div v-if="formErrors.releaseTypeError" class="error-tips"> {{ $t('store.发布类型不能为空') }} </div>
                        </section>
                        <section v-else style="min-width: 100%;">
                            <bk-radio-group v-model="atomForm.releaseType" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in publishShelf" :key="key" @click.native="formErrors.releaseTypeError = false">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                            <div v-if="formErrors.releaseTypeError" class="error-tips"> {{ $t('store.发布类型不能为空') }} </div>
                        </section>
                    </div>
                </div>
                <bk-alert
                    v-if="atomForm.releaseType === 'HIS_VERSION_UPGRADE'"
                    class="history-version-tip"
                    type="warning"
                    :title="$t('store.hisUpgradeTips')"
                ></bk-alert>
                <div class="bk-form-item version-num-form-item is-required" style="margin-top: 10px">
                    <label class="bk-label"> {{ $t('store.版本号') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <bk-input v-model="curVersion" v-if="atomForm.releaseType === 'HIS_VERSION_UPGRADE'"></bk-input>
                        <template v-else>
                            <p class="version-num-content" style="min-width: 100%;">
                                <span class="version-prompt"> {{ $t('store.semverType', [curVersion]) }} </span>
                                <span
                                    class="version-modify"
                                    @click="atomForm.releaseType = 'COMPATIBILITY_FIX'"
                                    v-if="atomForm.releaseType === 'CANCEL_RE_RELEASE'"
                                > {{ $t('store.修改') }} </span>
                            </p>
                            <bk-popover placement="left">
                                <i class="devops-icon icon-info-circle"></i>
                                <template slot="content">
                                    <p> {{ $t('store.根据发布类型自动生成') }} </p>
                                </template>
                            </bk-popover>
                        </template>
                    </div>
                </div>
                <div class="bk-form-item version-num-form-item is-required" style="margin-top: 10px" v-if="atomForm.releaseType === 'HIS_VERSION_UPGRADE'">
                    <label class="bk-label"> {{ $t('store.分支') }} </label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <bk-input v-model="atomForm.branch"></bk-input>
                    </div>
                </div>
                <div class="bk-form-item release-package-form-item is-required">
                    <label class="bk-label"> {{ $t('store.发布包') }} </label>
                    <div class="bk-form-content atom-item-content">
                        <bk-file-upload
                            :post-url="releasePackageUrl"
                            :os="atomForm.os"
                            :job-type="atomForm.jobType"
                            :tip="$t('store.只允许上传 zip 格式的文件')"
                            accept="application/zip"
                            @uploadSuccess="uploadPackageSuccess"
                            @uploadFail="uploadPackageErr"
                        ></bk-file-upload>
                        <div v-if="formErrors.releasePackageError" class="error-tips"> {{ $t('store.发布包不能为空') }} </div>
                    </div>
                </div>
                <div class="bk-form-item versionlog-form-item is-required">
                    <label class="bk-label"> {{ $t('store.版本日志') }} </label>
                    <div class="bk-form-content atom-item-content">
                        <mavon-editor
                            :class="{ 'is-danger': errors.has('versionContent'), 'atom-remark-input': true }"
                            ref="versionMd"
                            v-model="atomForm.versionContent"
                            :toolbars="toolbarOptions"
                            :external-link="false"
                            :box-shadow="false"
                            preview-background="#fff"
                            name="versionContent"
                            v-validate="{ required: true }"
                            :language="mavenLang"
                            @imgAdd="addImage('versionMd', ...arguments)"
                            @imgDel="delImage"
                            @change="changeData"
                        />
                        <p :class="errors.has('versionContent') ? 'error-tips' : 'normal-tips'">{{ errors.first("versionContent") }}</p>
                    </div>
                </div>
                <div class="form-footer">
                    <bk-button theme="primary" @click="submit()"> {{ $t('store.提交') }} </bk-button>
                    <bk-button @click="$router.back()"> {{ $t('store.取消') }} </bk-button>
                </div>
                <select-logo :form="atomForm" type="ATOM" :is-err="formErrors.logoUrlError" ref="logoUrlError"></select-logo>
            </div>
        </div>
    </div>
</template>

<script>
    import selectLogo from '@/components/common/selectLogo'
    import { toolbars } from '@/utils/editor-options'
    import bkFileUpload from '@/components/common/file-upload'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import api from '@/api'
    
    export default {
        components: {
            selectLogo,
            bkFileUpload,
            breadCrumbs
        },
        data () {
            return {
                curVersion: '',
                atomName: 'landun-atom-codecc',
                initJobType: '',
                initReleaseType: '',
                descTemplate: '',
                docsLink: this.BKCI_DOCS.PLUGIN_GUIDE_DOC,
                showContent: false,
                isUploading: false,
                initOs: [],
                categoryList: [
                    { label: this.$t('store.流水线插件'), value: 'TASK' }
                    // { label: '流水线触发器', value: 'TRIGGER' }
                ],
                jobTypeList: [
                    { label: this.$t('store.编译环境'), value: 'AGENT', isShow: true },
                    { label: this.$t('store.无编译环境'), value: 'AGENT_LESS', isShow: true }
                ],
                envList: [
                    { label: 'Linux', value: 'LINUX', icon: 'linux-view' },
                    { label: 'Windows', value: 'WINDOWS', icon: 'windows' },
                    { label: 'macOS', value: 'MACOS', icon: 'macos' }
                ],
                publishShelf: [
                    { label: this.$t('store.新上架'), value: 'NEW' }
                ],
                publishTypeList: [
                    {
                        label: this.$t('store.非兼容式升级'),
                        value: 'INCOMPATIBILITY_UPGRADE',
                        desc: this.$t('store.当新版本输入输出不兼容旧版本时，使用非兼容式升级方式，发布后用户需修改流水线中的插件版本号才能使用新版本。')
                    },
                    {
                        label: this.$t('store.兼容式功能更新'),
                        value: 'COMPATIBILITY_UPGRADE',
                        desc: this.$t('store.当新版本输入输出兼容旧版本，仅更新功能时，使用兼容式升级方式，发布后用户无需修改流水线中的插件版本号，默认使用最新版本。')
                    },
                    {
                        label: this.$t('store.兼容式问题修正'),
                        value: 'COMPATIBILITY_FIX',
                        desc: this.$t('store.当新版本为bug fix时，使用兼容式问题修正方式，发布后用户无需修改流水线中的插件版本号，默认使用最新版本。')
                    },
                    {
                        label: this.$t('store.历史大版本问题修复'),
                        value: 'HIS_VERSION_UPGRADE',
                        desc: this.$t('store.当历史大版本下发现 bug 时，使用此方式进行 fix。不建议在此场景下新增/删除入参。')
                    }
                ],
                frontendTypeList: [
                    { label: this.$t('store.是'), value: 'SPECIAL', title: this.$t('store.需自行开发插件输入页面,详见插件开发指引') },
                    { label: this.$t('store.否'), value: 'NORMAL', title: this.$t('store.仅需按照规范定义好输入字段，系统将自动渲染页面') }
                ],
                sortList: [],
                labelList: [],
                img_file: {},
                loading: {
                    isLoading: false,
                    title: ''
                },
                atomForm: {
                    name: '',
                    atomCode: '',
                    logoUrl: '',
                    category: 'TASK',
                    classifyCode: '',
                    classifyName: '',
                    jobType: 'AGENT',
                    os: [],
                    labelIdList: [],
                    summary: '',
                    description: `#### ${this.$t('store.插件功能')}\n\n#### ${this.$t('store.适用场景')}\n\n#### ${this.$t('store["使用限制和受限解决方案[可选]"]')}\n\n#### ${this.$t('store.常见的失败原因和解决方案')}`,
                    publisher: '',
                    frontendType: 'NORMAL',
                    version: '1.0.0',
                    releaseType: 'NEW',
                    versionContent: '',
                    branch: ''
                },
                formErrors: {
                    categoryError: false,
                    jobError: false,
                    openSourceError: false,
                    logoUrlError: false,
                    sortError: false,
                    envError: false,
                    releaseTypeError: false
                },
                versionMap: {},
                publishersList: [],
                containerList: []
            }
        },
        computed: {
            atomId () {
                return this.$route.params.atomId
            },
            curTitle () {
                return this.$route.name === 'shelfAtom' ? this.$t('store.上架插件') : this.$t('store.升级插件')
            },
            toolbarOptions () {
                return toolbars
            },
            releasePackageUrl () {
                return `${API_URL_PREFIX}/artifactory/api/user/artifactories/projects/${this.atomForm.projectCode}/ids/${this.atomForm.atomId}/codes/${this.atomForm.atomCode}/versions/${this.curVersion || '1.0.0'}/types/${this.atomForm.releaseType}/archive`
            },
            navList () {
                const name = `${this.curTitle}（${this.atomForm.atomCode}）`
                return [
                    { name: this.$t('store.工作台'), to: { name: 'atomWork' } },
                    { name }
                ]
            },
            userName () {
                return this.$store.state.user.username
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },
        watch: {
            'atomForm.jobType' (val) {
                if (this.$route.name === 'upgradeAtom' && this.atomForm.releaseType !== 'CANCEL_RE_RELEASE') {
                    const isEqualType = val === this.initJobType
                    const isEqualOs = this.initOs.every(item => this.atomForm.os.indexOf(item) > -1)
                    if (this.initJobType === 'AGENT') {
                        this.atomForm.releaseType = isEqualType && isEqualOs ? this.initReleaseType : 'INCOMPATIBILITY_UPGRADE'
                    } else {
                        this.atomForm.releaseType = isEqualType ? this.initReleaseType : 'INCOMPATIBILITY_UPGRADE'
                    }
                }
            },

            'atomForm.os' (val) {
                if (this.$route.name === 'upgradeAtom' && this.initJobType === 'AGENT' && this.atomForm.releaseType !== 'CANCEL_RE_RELEASE') {
                    const isEqualOs = this.initOs.every(item => this.atomForm.os.indexOf(item) > -1)
                    this.atomForm.releaseType = isEqualOs ? this.initReleaseType : 'INCOMPATIBILITY_UPGRADE'
                }
            },

            'atomForm.releaseType' (val) {
                this.curVersion = this.versionMap[val] || ''
                this.formErrors.releaseTypeError = false
            }
        },
        async created () {
            await this.fetchContainerList()
            await this.requestAtomlabels()
            await this.requestAtomDetail(this.$route.params.atomId)
            await this.fetchPublishersList(this.atomForm.atomCode)
            this.requestAtomClassify()
        },
        methods: {
            fetchContainerList () {
                this.$store.dispatch('store/getContainerList').then(res => {
                    this.containerList = res
                    this.jobTypeList[1].isShow = !!this.containerList.find(i => i.type === 'normal')
                })
            },
            toPublishProgress (type, id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: type,
                        atomId: id
                    }
                })
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            fetchPublishersList (atomCode) {
                this.$store.dispatch('store/getPublishersList', { atomCode }).then(res => {
                    this.publishersList = res
                    const result = this.publishersList.find(i => i.publisherCode === this.userName)
                    if (!result) {
                        this.publishersList.push({
                            publisherCode: this.userName,
                            publisherName: this.userName
                        })
                    }
                }).catch(() => [])
            },
            async requestAtomlabels () {
                try {
                    const res = await this.$store.dispatch('store/requestAtomLables')
                    this.labelList = res || []
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestAtomDetail (atomId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestAtomDetail', {
                        atomId: atomId
                    })
                    const { showVersionList } = await api.requestAtomVersionDetail(res.atomCode)
                    if (res) {
                        Object.assign(this.atomForm, res, {})
                        this.atomForm.jobType = !this.atomForm.jobType ? 'AGENT' : this.atomForm.jobType
                        this.initJobType = this.atomForm.jobType
                        this.atomForm.labelIdList = (this.atomForm.labelList || []).map(item => {
                            return item.id
                        })
                        this.initOs = JSON.parse(JSON.stringify(this.atomForm.os))
                        // init version
                        showVersionList.forEach((versionInfo) => {
                            this.versionMap[versionInfo.releaseType] = versionInfo.version
                            if (versionInfo.defaultFlag) {
                                this.curVersion = versionInfo.version
                                this.atomForm.releaseType = versionInfo.releaseType
                                this.initReleaseType = versionInfo.releaseType
                            }
                        })
                    }
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
                        this.autoFocus()
                    }, 500)
                }
            },
            async requestAtomClassify () {
                try {
                    const res = await this.$store.dispatch('store/requestAtomClassify')
                    this.sortList = res
                    this.sortList = this.sortList.filter(item => item.classifyCode !== 'trigger')
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            changeClassify () {
                this.formErrors.sortError = false
            },
            changeOs (data) {
                this.formErrors.envError = false
            },

            changeJobType () {
                this.formErrors.envError = false
                this.formErrors.jobError = false
                this.atomForm.os = []
            },
            autoFocus () {
                this.$nextTick(() => {
                    this.$refs.atomName.focus()
                })
            },

            addImage (ref, pos, file) {
                this.uploadimg(ref, pos, file)
            },
            delImage (pos) {

            },
            changeData (value, render) {
                // console.log(value, render)
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
            },

            uploadPackageSuccess (data) {
                if (data.atomEnvRequests && data.atomEnvRequests.length) {
                    this.formErrors.releasePackageError = false
                }
            },

            uploadPackageErr (message) {
                if (message) {
                    this.$bkMessage({
                        message: message,
                        theme: 'error'
                    })
                }
            },

            checkValid () {
                let errorCount = 0
                let ref = ''

                if (!this.atomForm.logoUrl && !this.atomForm.iconData) {
                    this.formErrors.logoUrlError = true
                    ref = ref || 'logoUrlError'
                    errorCount++
                }

                if (this.categoryList.findIndex(x => x.value === this.atomForm.category) < 0) {
                    this.formErrors.categoryError = true
                    ref = ref || 'categoryError'
                    errorCount++
                }

                if (!this.atomForm.classifyCode) {
                    this.formErrors.sortError = true
                    ref = ref || 'sortError'
                    errorCount++
                }

                if (this.jobTypeList.findIndex(x => x.value === this.atomForm.jobType) < 0) {
                    this.formErrors.jobError = true
                    ref = ref || 'jobError'
                    errorCount++
                }

                if (this.atomForm.jobType === 'AGENT' && !this.atomForm.os.length) {
                    this.formErrors.envError = true
                    ref = ref || 'envError'
                    errorCount++
                }

                if (!this.atomForm.releaseType) {
                    this.formErrors.releaseTypeError = true
                    ref = ref || 'releaseTypeError'
                    errorCount++
                }

                if (errorCount > 0) {
                    const errorEle = this.$refs[ref]
                    if (errorEle) errorEle.$el.scrollIntoView()
                    return false
                }

                return true
            },

            checkJobType () {
                let message = ''
                const isEqualOs = this.initOs.every(item => this.atomForm.os.indexOf(item) > -1)
                if (this.atomForm.releaseType !== 'INCOMPATIBILITY_UPGRADE' && this.atomForm.jobType !== this.initJobType && this.$route.name === 'upgradeAtom') {
                    message = this.$t('store.适用Job类型发生变更，发布类型请选择非兼容式升级，避免影响已有流水线的使用。')
                } else if (this.atomForm.releaseType !== 'INCOMPATIBILITY_UPGRADE' && this.$route.name === 'upgradeAtom' && this.atomForm.jobType === 'AGENT' && this.initJobType === 'AGENT' && !isEqualOs) {
                    message = this.$t('store.操作系统发生变更，发布类型请选择非兼容式升级，避免影响已有流水线的使用。')
                }
                return message
            },

            validate () {
                return new Promise((resolve, reject) => {
                    const isCheckValid = this.checkValid()
                    const message = this.checkJobType()
                    this.$validator.validate().then(valid => {
                        if (isCheckValid && !message && valid) resolve()
                        else reject(new Error(message || this.$t('store.校验不通过，请修改后再试')))
                    })
                })
            },

            submit (fieldCheckConfirmFlag) {
                this.validate().then(() => {
                    this.loading.isLoading = true
                    const params = {
                        atomCode: this.atomForm.atomCode,
                        name: this.atomForm.name,
                        category: this.atomForm.category,
                        classifyCode: this.atomForm.classifyCode,
                        version: this.curVersion,
                        releaseType: this.atomForm.releaseType,
                        jobType: this.atomForm.jobType,
                        os: this.atomForm.jobType === 'AGENT' ? this.atomForm.os : [],
                        labelIdList: this.atomForm.labelIdList.filter(i => i !== 'null' && i !== ' ' && i),
                        publisher: this.atomForm.publisher,
                        versionContent: this.atomForm.versionContent,
                        logoUrl: this.atomForm.logoUrl || undefined,
                        iconData: this.atomForm.iconData || undefined,
                        summary: this.atomForm.summary || undefined,
                        description: this.atomForm.description || undefined,
                        visibilityLevel: this.atomForm.visibilityLevel,
                        frontendType: this.atomForm.frontendType,
                        fieldCheckConfirmFlag,
                        branch: this.atomForm.branch
                    }

                    return this.$store.dispatch('store/editAtom', {
                        projectCode: this.atomForm.projectCode,
                        params: params,
                        initProject: this.atomForm.initProjectCode
                    }).then((res) => {
                        this.$bkMessage({ message: this.$t('store.提交成功'), theme: 'success' })
                        if (res) this.toPublishProgress(this.$route.name === 'shelfAtom' ? 'shelf' : 'upgrade', res)
                    })
                }).catch((err) => {
                    if (err.httpStatus === 200) {
                        const h = this.$createElement
                        const subHeader = h('p', {
                            style: {
                                textDecoration: 'none',
                                cursor: 'pointer',
                                whiteSpace: 'normal',
                                textAlign: 'left',
                                lineHeight: '24px'
                            }
                        }, err.message || err)
                        if ([2120030, 2120031].includes(err.code)) {
                            const confirmFn = () => this.submit(true)
                            this.$bkInfo({
                                type: 'warning',
                                subHeader,
                                width: 440,
                                okText: this.$t('store.已确认兼容新增参数，继续'),
                                confirmFn
                            })
                        } else {
                            this.$bkInfo({
                                type: 'error',
                                title: this.$t('store.提交失败'),
                                showFooter: false,
                                subHeader
                            })
                        }
                    } else if (err) {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }
                }).finally(() => {
                    this.loading.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .atom-item-content {
        max-width: calc(100% - 110px);
    }

    .history-version-tip {
        margin: 5px 0 0 110px;
    }

    .edit-atom-wrapper {
        height: 100%;
        .edit-atom-content {
            padding: 20px 0 40px;
            height: calc(100% - 5.6vh);
            overflow: auto;
            display: flex;
            justify-content: center;
        }
        .edit-atom-form {
            position: relative;
            width: 1200px;
            .bk-label {
                width: 110px;
                font-weight: normal;
                padding: 0 20px 0 0;
            }
            .bk-form-content {
                margin-left: 0;
                .bk-form-checkbox {
                    margin: 10px 21px 20px 0;
                }
            }
            .atom-os .bk-form-checkbox:first-child {
                margin-left: 110px;
            }
            .env-error {
                margin: -10px 0 20px 110px;
            }
            .introduction-form-item {
                display: block;
                .error-tips {
                    margin-left: 110px;
                }
            }
            .bk-selector .bk-form-checkbox {
                display: block;
                padding: 12px 0;
            }
            .atom-classify-content {
                .bk-selector {
                    width: 40%;
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
            }
            .introduction-form-item,
            .remark-form-item,
            .publish-form-item,
            .version-num-form-item {
                .bk-tooltip {
                    left: 101%;
                }
            }
            .atom-introduction-input,
            .atom-remark-input {
                min-width: 100%;
                max-width: 100%;
                border: 1px solid #c4c6cc;
            }
            .version-num-form-item {
                .version-num-content {
                    top: 8px;
                    color: #333C48;
                    display: flex;
                    align-items: center;
                }
                .version-prompt {
                    color: $fontWeightColor;
                }
                .version-modify {
                    margin-left: 10px;
                    cursor: pointer;
                    color: $primaryColor;
                }
            }
            .bk-radio-text {
                color: #333C48;
            }
            .env-checkbox {
                .devops-icon {
                    position: relative;
                    top: 3px;
                    font-size: 16px;
                    color: $fontWeightColor;
                }
                .bk-checkbox-text {
                    color: #333C48;
                }
            }
            .os-checkbox-label {
                display: flex;
                align-items: center;
            }
            .atom-remark-input {
                height: 263px;
                &.fullscreen {
                    height: auto;
                }
            }
            .version-msg {
                padding: 12px 0 12px 26px;
            }
            .form-title {
                margin-top: 20px;
                font-weight: bold
            }
            .cut-line {
                margin-top: 8px;
                height: 1px;
                border: none;
                background-color: #C3CDD7
            }
            .atom-versionlog-input {
                padding: 10px;
                height: 80px;
            }
            .form-footer {
                margin: 26px 0 30px 107px;
            }
            .atom-logo-box {
                position: absolute;
                top: 0;
                right: 0;
                width: 100px;
                height: 100px;
                border: 1px dashed $lineColor;
                background: #fff;
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
                    // border-radius: 50%;
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
        }
        .op-image {
            .dropdown-item:first-child,
            .dropdown-images {
                display: none;
            }
        }
        .auto-textarea-wrapper {
            min-height: 200px;
        }
    }
    .error-commit {
        .bk-dialog-default-status {
            padding-top: 10px;
        }
    }
</style>
