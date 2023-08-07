<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button theme="primary" @click="openConvention"> {{ $t('store.新增插件') }} </bk-button>
            </div>
            <bk-input :placeholder="$t('store.请输入关键字搜索')"
                class="search-input"
                :clearable="true"
                :right-icon="'bk-icon icon-search'"
                v-model="searchName">
            </bk-input>
        </div>
        <main class="g-scroll-pagination-table">
            <bk-table style="margin-top: 15px;"
                :empty-text="$t('store.暂时没有插件')"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :data="renderList"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageCountChanged"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column :label="$t('store.插件名称')" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span class="atom-name" :title="props.row.name" @click="routerAtoms(props.row.atomCode)">{{ props.row.name }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.调试项目')" prop="projectName" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.开发语言')" prop="language" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.版本')" prop="version" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span
                            v-for="(prop, index) in [props.row, ...(props.row.processingVersionInfos || [])]"
                            :key="index"
                            class="mr15"
                            @click="handleVersionClick(prop)"
                        >
                            <status :status="calcStatus(prop.atomStatus)"></status>
                            <span
                                :class="{ 'g-text-link': ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING', 'CODECCING', 'CODECC_FAIL'].includes(prop.atomStatus) }"
                            >{{ prop.version }}</span>
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.修改人')" prop="modifier" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="150" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="240" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="upgrade-btn" v-if="['GROUNDING_SUSPENSION', 'AUDIT_REJECT', 'RELEASED'].includes(props.row.atomStatus) && (!props.row.processingVersionInfos || props.row.processingVersionInfos.length <= 0)"
                            @click="editHandle('upgradeAtom', props.row.atomId)"> {{ $t('store.升级') }} </span>
                        <span class="install-btn"
                            v-if="props.row.atomStatus === 'RELEASED'"
                            @click="installAHandle(props.row.atomCode)"> {{ $t('store.安装') }} </span>
                        <span class="shelf-btn"
                            v-if="['INIT', 'UNDERCARRIAGED'].includes(props.row.atomStatus) && (!props.row.processingVersionInfos || props.row.processingVersionInfos.length <= 0)"
                            @click="editHandle('shelfAtom', props.row.atomId)"> {{ $t('store.上架') }} </span>
                        <span class="obtained-btn"
                            v-if="['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'].includes(props.row.atomStatus) && props.row.releaseFlag"
                            @click="offline(props.row)"> {{ $t('store.下架') }} </span>
                        <span class="delete-btn" v-if="!props.row.releaseFlag" @click="deleteAtom(props.row)"> {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus :type="searchName ? 'search-empty' : 'empty'" @clear="searchName = ''" />
                </template>
            </bk-table>
        </main>
        <bk-sideslider
            class="create-atom-slider g-slide-radio"
            :is-show.sync="createAtomsideConfig.show"
            :title="createAtomsideConfig.title"
            :quick-close="createAtomsideConfig.quickClose"
            :width="createAtomsideConfig.width"
            :before-close="cancelCreateAtom">
            <template slot="content">
                <form class="bk-form create-atom-form" v-if="hasOauth"
                    v-bkloading="{
                        isLoading: createAtomsideConfig.isLoading
                    }">
                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.名称') }} </label>
                        <div class="bk-form-content atom-item-content is-tooltips">
                            <div style="min-width: 100%">
                                <bk-input class="atom-name-input" :placeholder="$t('store.请输入中英文名称，不超过40个字符')"
                                    name="atomName"
                                    v-model="createAtomForm.name"
                                    v-validate="{
                                        required: true,
                                        regex: '^[\u4e00-\u9fa5a-zA-Z0-9-_. ]{1,40}$'
                                    }"
                                    @change="handleChangeForm"
                                    :class="{ 'is-danger': errors.has('atomName') }"
                                />
                                <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">
                                    {{ errors.first("atomName") && errors.first("atomName").indexOf($t('store.正则')) > 0 ? $t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过40个字符') : errors.first("atomName") }}
                                </p>
                            </div>
                            <bk-popover class="info-circle-icon" placement="right" max-width="400">
                                <i class="devops-icon icon-info-circle"></i>
                                <template slot="content">
                                    <p> {{ $t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过40个字符') }} </p>
                                </template>
                            </bk-popover>
                        </div>
                    </div>

                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.标识') }} </label>
                        <div class="bk-form-content atom-item-content is-tooltips">
                            <div style="min-width: 100%;">
                                <bk-input class="atom-id-input" :placeholder="$t('store.请输入英文名称，不超过30个字符')"
                                    name="atomId"
                                    v-model="createAtomForm.atomCode"
                                    v-validate="{
                                        required: true,
                                        regex: '^[a-zA-Z][a-zA-Z0-9_-]{1,30}$'
                                    }"
                                    @change="handleChangeForm"
                                    :class="{ 'is-danger': errors.has('atomId') }"
                                />
                                <p :class="errors.has('atomId') ? 'error-tips' : 'normal-tips'">
                                    {{ errors.first("atomId") && errors.first("atomId").indexOf($t('store.正则')) > 0 ? $t('store.由英文字母、数字、连字符(-)或下划线(_)组成，以英文字母开头，不超过30个字符') : errors.first("atomId") }}
                                </p>
                            </div>
                            <bk-popover class="info-circle-icon" placement="right" max-width="400">
                                <i class="devops-icon icon-info-circle"></i>
                                <template slot="content">
                                    <p> {{ $t('store.唯一标识，创建后不能修改。将作为插件代码库路径。') }} </p>
                                </template>
                            </bk-popover>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.调试项目') }} </label>
                        <div class="bk-form-content atom-item-content is-tooltips">
                            <div style="min-width: 100%">
                                <bk-select v-model="createAtomForm.projectCode"
                                    @selected="selectedProject"
                                    @change="handleChangeForm"
                                    @toggle="toggleProjectList"
                                    searchable
                                    :placeholder="$t('store.请选择调试项目')"
                                    :enable-virtual-scroll="projectList && projectList.length > 3000"
                                    :list="projectList"
                                    id-key="projectCode"
                                    display-key="projectName"
                                >
                                    <bk-option
                                        v-for="item in projectList"
                                        :key="item.projectCode"
                                        :id="item.projectCode"
                                        :name="item.projectName"
                                    >
                                    </bk-option>
                                    <div slot="extension" style="cursor: pointer;">
                                        <a :href="itemUrl" target="_blank">
                                            <i class="devops-icon icon-plus-circle" />
                                            {{ itemText }}
                                        </a>
                                    </div>
                                </bk-select>
                                <div v-if="atomErrors.projectError" class="error-tips"> {{ $t('store.项目不能为空') }} </div>
                            </div>
                            <bk-popover class="info-circle-icon" placement="right" max-width="400">
                                <i class="devops-icon icon-info-circle"></i>
                                <template slot="content">
                                    <p> {{ $t('store.debugProjectTips') }} </p>
                                </template>
                            </bk-popover>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.开发语言') }} </label>
                        <div class="bk-form-content atom-item-content">
                            <bk-select
                                v-model="createAtomForm.language"
                                searchable
                                @change="handleChangeForm"
                            >
                                <bk-option v-for="(option, index) in languageList"
                                    :key="index"
                                    :id="option.language"
                                    :name="option.name"
                                    @click.native="selectedLanguage"
                                    :placeholder="$t('store.请选择开发语言')"
                                >
                                </bk-option>
                                <div slot="extension" style="cursor: pointer;">
                                    <a :href="itemUrl" target="_blank">
                                        <i class="devops-icon icon-plus-circle" />
                                        {{ itemText }}
                                    </a>
                                </div>
                            </bk-select>
                            <div v-if="atomErrors.languageError" class="error-tips"> {{ $t('store.开发语言不能为空') }} </div>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.自定义前端') }} </label>
                        <div class="bk-form-content atom-item-content">
                            <bk-radio-group
                                v-model="createAtomForm.frontendType"
                                @change="handleChangeForm"
                            >
                                <bk-radio :title="entry.title" :value="entry.value" v-for="(entry, key) in frontendTypeList" :key="key">{{ entry.label }}</bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                    <div class="form-footer">
                        <button class="bk-button bk-primary" type="button" @click="submitCreateAtom()"> {{ $t('store.提交') }} </button>
                        <button class="bk-button bk-default" type="button" @click="cancelCreateAtom()"> {{ $t('store.取消') }} </button>
                    </div>
                </form>
                <div class="oauth-tips" v-else style="margin: 30px">
                    <button class="bk-button bk-primary" type="button" @click="openValidate"> {{ $t('store.OAUTH认证') }} </button>
                    <p class="prompt-oauth">
                        <i class="devops-icon icon-info-circle-shape"></i>
                        <span> {{ $t('store.新增插件时将自动初始化插件代码库，请先进行工蜂OAUTH授权') }} </span>
                    </p>
                </div>
            </template>
        </bk-sideslider>
        
        <bk-sideslider
            class="offline-atom-slider"
            :is-show.sync="offlinesideConfig.show"
            :title="offlinesideConfig.title"
            :quick-close="offlinesideConfig.quickClose"
            :width="offlinesideConfig.width">
            <template slot="content">
                <form class="bk-form offline-atom-form" v-bkloading="{ isLoading: offlinesideConfig.isLoading }">
                    <div class="bk-form-item">
                        <label class="bk-label"> {{ $t('store.名称') }} </label>
                        <div class="bk-form-content">
                            <p class="content-value">{{ curHandlerAtom.name }}</p>
                        </div>
                    </div>
                    <div class="bk-form-item">
                        <label class="bk-label"> {{ $t('store.标识') }} </label>
                        <div class="bk-form-content">
                            <p class="content-value">{{ curHandlerAtom.atomCode }}</p>
                        </div>
                    </div>
                    <div class="bk-form-item is-required">
                        <label class="bk-label"> {{ $t('store.下架原因') }} </label>
                        <div class="bk-form-content">
                            <bk-input :placeholder="$t('store.请输入下架原因')"
                                name="reason"
                                @change="curHandlerAtom.error = curHandlerAtom.reason === ''"
                                type="textarea"
                                :rows="3"
                                v-model="curHandlerAtom.reason">
                            </bk-input>
                            <div v-if="curHandlerAtom.error" class="error-tips"> {{ $t('store.下架原因不能为空') }} </div>
                        </div>
                    </div>
                    <form-tips :tips-content="offlineTips" :prompt-list="promptList"></form-tips>
                    <div class="form-footer">
                        <button class="bk-button bk-primary" type="button" @click="submitofflineAtom()"> {{ $t('store.提交') }} </button>
                    </div>
                </form>
            </template>
        </bk-sideslider>
        
        <bk-dialog v-model="deleteObj.visible"
            render-directive="if"
            theme="primary"
            ext-cls="atom-dialog-wrapper"
            :title="$t('store.确定删除插件', [deleteObj.name])"
            width="500"
            footer-position="center"
            :mask-close="false"
            :auto-close="false"
        >
            <bk-form ref="deleteForm" class="delete-form" :label-width="0" :model="deleteObj.formData">
                <p class="dialog-tip">{{$t('store.删除时将清理数据。删除后不可恢复！')}}</p>
                <p class="dialog-tip">{{$t('store.deleteAtomTip', [deleteObj.atomCode])}}</p>
                <bk-form-item property="projectName">
                    <bk-input
                        maxlength="60"
                        v-model="deleteObj.formData.atomCode"
                        :placeholder="$t('store.请输入插件标识')">
                    </bk-input>
                </bk-form-item>
            </bk-form>
            <div class="dialog-footer" slot="footer">
                <bk-button
                    theme="danger"
                    :loading="deleteObj.loading"
                    :disabled="deleteObj.atomCode !== deleteObj.formData.atomCode"
                    @click="requestDeleteAtom(deleteObj.formData.atomCode)">{{ $t('store.删除') }}</bk-button>
                <bk-button @click="handleDeleteCancel" :disabled="deleteObj.loading">{{ $t('store.取消') }}</bk-button>
            </div>
        </bk-dialog>
        <bk-dialog v-model="showConvention"
            :close-icon="false"
            :show-footer="false"
            render-directive="if"
            theme="primary"
            ext-cls="atom-dialog-wrapper"
            :title="$t('store.插件开发公约')"
            width="700"
            footer-position="center"
            :mask-close="false"
            :auto-close="false"
            @cancel="cancelConvention">
            <bk-form ref="deleteForm" class="delete-form" :label-width="0" :model="deleteObj.formData">
                <p class="dialog-tip">{{$t('store.1、插件能获取到的所有内容（包括但不限于：代码、节点、凭证、项目信息）均属于项目资产，仅用于实现流水线编排设定好的功能。')}}</p>
                <p class="dialog-tip">
                    <font style="color: red;">{{$t('store.2、未经授权私自使用插件获取内容（包括但不限于：拉取或转移代码、泄露或滥用凭证等）属于违规行为。')}}</font>
                    {{$t('store.无论当事人是否在职，公司将对违规行为进行处理，并对情节严重者保留追究法律责任的权利。')}}
                </p>
                <p class="dialog-tip">
                    {{$t('store.3、插件发开者有义务按照')}}
                    <a :href="specificationDocUrl" class="text-link" target="_blank">{{$t('store.《插件开发规范》')}}</a>
                    {{$t('store.对插件进行升级维护，保证插件功能正常。')}}</p>
                <p class="dialog-tip">
                    {{$t('store.4、插件需提供详细的使用指引和执行日志、清晰明确的错误码信息和相关的修复指引（见')}}
                    <a :href="errorCodeDocUrl" class="text-link" target="_blank">{{$t('store.《插件错误码规范》')}}</a>
                    {{$t('store.），协助使用者快速定位和解决问题。')}}
                </p>
                <span class="delete-form-item">
                    <bk-checkbox v-model="agreeWithConvention" :disabled="conventionSecond > 0">
                        <span style="color: #3c96ff">{{$t('store.我已阅读并承诺遵守以上约定')}}</span>
                        <span v-if="conventionSecond > 0"> ({{ conventionSecond }}s)</span>
                    </bk-checkbox>
                    <bk-button theme="primary" style="width: 120px;" :disabled="!agreeWithConvention" @click="createNewAtom">{{ $t('store.确定') }}</bk-button>
                </span>
            </bk-form>
        </bk-dialog>
    </main>
</template>

<script>
    import { debounce } from '@/utils'
    import formTips from '@/components/common/formTips/index'
    import status from './status'
    import { atomStatusMap } from '@/store/constants'

    export default {
        components: {
            formTips,
            status
        },

        data () {
            return {
                atomStatusList: atomStatusMap,
                hasOauth: true,
                searchName: '',
                gitOAuthUrl: '',
                itemUrl: '/console/pm',
                itemText: this.$t('store.新建项目'),
                offlineTips: this.$t('store.下架后：'),
                specificationDocUrl: this.BKCI_DOCS.PLUGIN_SPECIFICATE_DOC,
                errorCodeDocUrl: this.BKCI_DOCS.PLUGIN_ERROR_CODE_DOC,
                renderList: [],
                projectList: [],
                languageList: [],
                frontendTypeList: [
                    { label: this.$t('store.是'), value: 'SPECIAL', title: this.$t('store.需自行开发插件输入页面,详见插件开发指引') },
                    { label: this.$t('store.否'), value: 'NORMAL', title: this.$t('store.仅需按照规范定义好输入字段，系统将自动渲染页面') }
                ],
                promptList: [
                    this.$t('store.1、插件市场不再展示插件'),
                    this.$t('store.2、已使用插件的流水线可以继续使用，但有插件已下架标识')
                ],
                curHandlerAtom: {
                    name: '',
                    atomCode: '',
                    reason: '',
                    error: false
                },
                createAtomForm: {
                    projectCode: '',
                    atomCode: '',
                    name: '',
                    language: '',
                    frontendType: 'NORMAL'
                },
                isLoading: false,
                atomErrors: {
                    projectError: false,
                    languageError: false,
                    openSourceError: false,
                    privateReasonError: false
                },
                createAtomsideConfig: {
                    show: false,
                    isLoading: false,
                    quickClose: true,
                    width: 720,
                    title: this.$t('store.新增插件')
                },
                offlinesideConfig: {
                    show: false,
                    isLoading: false,
                    title: this.$t('store.下架插件'),
                    quickClose: true,
                    width: 565
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                deleteObj: {
                    visible: false,
                    atomCode: '',
                    name: '',
                    formData: {
                        atomCode: ''
                    },
                    loading: false
                },
                showConvention: false,
                agreeWithConvention: false,
                conventionSecond: 5
            }
        },

        watch: {
            'createAtomsideConfig.show' (val) {
                if (!val) {
                    this.atomErrors.projectError = false
                    this.atomErrors.languageError = false
                    this.atomErrors.privateReasonError = false
                    this.createAtomForm = {
                        projectCode: '',
                        atomCode: '',
                        name: '',
                        language: ''
                    }
                }
            },
            searchName () {
                this.isLoading = true
                debounce(this.search)
            }
        },

        created () {
            this.getLanguage()
            this.requestList()
        },

        methods: {
            handleVersionClick (prop) {
                if (['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING', 'CODECCING', 'CODECC_FAIL'].includes(prop.atomStatus)) {
                    this.routerProgress(prop)
                }
            },
            calcStatus (status) {
                let icon = ''
                switch (status) {
                    case 'COMMITTING':
                    case 'BUILDING':
                    case 'TESTING':
                    case 'AUDITING':
                    case 'UNDERCARRIAGING':
                    case 'CODECCING':
                        icon = 'doing'
                        break
                    case 'RELEASED':
                        icon = 'success'
                        break
                    case 'GROUNDING_SUSPENSION':
                    case 'CODECC_FAIL':
                    case 'BUILD_FAIL':
                        icon = 'fail'
                        break
                    case 'AUDIT_REJECT':
                    case 'UNDERCARRIAGED':
                        icon = 'info'
                        break
                    case 'INIT':
                        icon = 'init'
                        break
                }
                return icon
            },
            openConvention () {
                this.showConvention = true
                this.agreeWithConvention = false
                this.conventionSecond = 5
                this.calcConventionSecond()
            },
            calcConventionSecond () {
                if (!this.conventionSecond || this.conventionSecond <= 0) return
                this.calcConventionSecond.id = setTimeout(() => {
                    this.conventionSecond--
                    this.calcConventionSecond()
                }, 1000)
            },
            cancelConvention () {
                this.showConvention = false
                clearTimeout(this.calcConventionSecond.id)
            },
            addImage (pos, file) {
                this.uploadimg(pos, file)
            },
            async uploadimg (pos, file) {
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

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            },
            getLanguage () {
                this.$store.dispatch('store/getDevelopLanguage').then((res) => {
                    this.languageList = (res || []).map(({ language }) => ({ name: language, language }))
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' }))
            },

            async requestList () {
                this.isLoading = true

                const page = this.pagination.current
                const pageSize = this.pagination.limit

                try {
                    const res = await this.$store.dispatch('store/requestAtomList', {
                        atomName: this.searchName,
                        page,
                        pageSize
                    })

                    this.renderList.splice(0, this.renderList.length, ...(res.records || []))
                    if (this.renderList.length) {
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                } finally {
                    this.isLoading = false
                }
            },

            changeOpenSource () {
                this.atomErrors.openSourceError = false
                this.createAtomForm.privateReason = ''
            },

            async pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                await this.requestList()
            },

            async pageChanged (page) {
                this.pagination.current = page
                await this.requestList()
            },

            search () {
                this.isSearch = true
                this.pagination.current = 1
                this.requestList()
            },

            checkValid () {
                let errorCount = 0
                if (!this.createAtomForm.projectCode) {
                    this.atomErrors.projectError = true
                    errorCount++
                }
                if (!this.createAtomForm.language) {
                    this.atomErrors.languageError = true
                    errorCount++
                }

                if (this.createAtomForm.visibilityLevel === 'PRIVATE' && !this.createAtomForm.privateReason) {
                    this.atomErrors.privateReasonError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },

            async submitCreateAtom () {
                const isCheckValid = this.checkValid()
                const valid = await this.$validator.validate()
                if (isCheckValid && valid) {
                    let message, theme
                    const params = Object.assign(this.createAtomForm, {})

                    this.createAtomsideConfig.isLoading = true
                    try {
                        await this.$store.dispatch('store/createNewAtom', {
                            params: params
                        })

                        message = this.$t('store.新增成功')
                        theme = 'success'

                        this.routerAtoms(this.createAtomForm.atomCode)
                        this.requestList()
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.createAtomForm = {
                            projectCode: '',
                            atomCode: '',
                            name: '',
                            language: '',
                            frontendType: 'NORMAL'
                        }
                        setTimeout(() => {
                            this.createAtomsideConfig.show = false
                        })
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.createAtomsideConfig.isLoading = false
                    }
                }
            },

            async submitofflineAtom () {
                if (this.curHandlerAtom.reason === '') {
                    this.curHandlerAtom.error = true
                    return
                }

                let message, theme
                const params = {
                    reason: this.curHandlerAtom.reason
                }

                this.offlinesideConfig.isLoading = true
                try {
                    await this.$store.dispatch('store/offlineAtom', {
                        atomCode: this.curHandlerAtom.atomCode,
                        params: params
                    })

                    message = this.$t('store.提交成功')
                    theme = 'success'
                    this.offlinesideConfig.show = false
                    this.requestList()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.offlinesideConfig.isLoading = false
                }
            },

            selectedProject (project) {
                this.atomErrors.projectError = !project
            },

            async toggleProjectList (isdropdown) {
                if (isdropdown) {
                    const res = await this.$store.dispatch('store/requestProjectList')
                    this.projectList.splice(0, this.projectList.length, ...res)
                }
            },

            selectedLanguage () {
                this.atomErrors.languageError = false
            },

            cancelCreateAtom () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            this.createAtomForm = {
                                projectCode: '',
                                atomCode: '',
                                name: '',
                                language: '',
                                frontendType: 'NORMAL'
                            }
                            setTimeout(() => {
                                this.createAtomsideConfig.show = false
                            })
                            return true
                        }
                    })
                } else {
                    this.createAtomsideConfig.show = false
                }
            },

            routerAtoms (code) {
                this.$router.push({
                    name: 'statisticData',
                    params: {
                        code,
                        type: 'atom'
                    }
                })
            },

            routerProgress (row) {
                let releaseType = 'upgrade'
                if (row.version === '1.0.0') releaseType = 'shelf'

                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType,
                        atomId: row.atomId
                    }
                })
            },

            openValidate () {
                window.open(this.gitOAuthUrl, '_self')
            },

            createNewAtom () {
                window.changeFlag = false
                this.showConvention = false
                this.createAtomsideConfig.show = true
            },

            offline (form) {
                this.offlinesideConfig.show = true
                this.curHandlerAtom.name = form.name
                this.curHandlerAtom.atomCode = form.atomCode
                this.curHandlerAtom.reason = ''
                this.curHandlerAtom.error = false
            },

            installAHandle (code) {
                this.$router.push({
                    name: 'install',
                    query: {
                        code,
                        type: 'atom',
                        from: 'atomWork'
                    }
                })
            },

            editHandle (routerName, atomId) {
                this.$router.push({
                    name: routerName,
                    params: { atomId }
                })
            },

            async requestDeleteAtom (atomCode) {
                let message, theme
                try {
                    this.deleteObj.loading = true
                    await this.$store.dispatch('store/requestDeleteAtom', {
                        atomCode
                    })

                    message = this.$t('store.删除成功')
                    theme = 'success'
                    this.requestList()
                    this.handleDeleteCancel()
                } catch (err) {
                    message = message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.deleteObj.loading = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },

            deleteAtom (row) {
                this.deleteObj.visible = true
                this.deleteObj.formData.atomCode = ''
                this.deleteObj.atomCode = row.atomCode
                this.deleteObj.name = row.name
            },

            handleDeleteCancel () {
                this.deleteObj.visible = false
                this.deleteObj.formData.atomCode = ''
                this.deleteObj.atomCode = ''
                this.deleteObj.name = ''
            },

            handleChangeForm () {
                window.changeFlag = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .info-circle-icon {
        display: flex;
        align-items: center;
        padding-left: 8px;
    }
    ::v-deep .atom-dialog-wrapper {
        .bk-form-item{
            .bk-label {
                padding: 0;
            }
        }
        .dialog-tip {
            margin-bottom: 10px;
            line-height: 25px;
            text-align: left;
            word-break: break-all;
            a {
                font-size: 14px;
            }
        }
        .bk-dialog-footer {
            text-align: center;
            padding: 0 65px 40px;
            background-color: #fff;
            border: none;
            border-radius: 0;
        }
        .dialog-footer {
            button {
                width: 86px;
            }
        }
        ::v-deep .bk-dialog-header {
            padding: 3px 24px 10px;
            border-bottom: 1px solid #e6e7ea;
        }
        ::v-deep .bk-dialog-body {
            padding: 10px 35px 26px;
        }
        .delete-form-item {
            margin-top: 50px;
            display: flex;
            justify-content: space-between;
        }
    }
</style>
