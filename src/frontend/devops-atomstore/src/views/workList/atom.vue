<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button theme="primary" @click="createNewAtom"> {{ $t('store.新增插件') }} </bk-button>
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
                <bk-table-column :label="$t('store.插件名称')">
                    <template slot-scope="props">
                        <span class="atom-name" :title="props.row.name" @click="routerAtoms(props.row.atomCode)">{{ props.row.name }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.调试项目')" prop="projectName"></bk-table-column>
                <bk-table-column :label="$t('store.开发语言')" prop="language"></bk-table-column>
                <bk-table-column :label="$t('store.版本')" prop="version"></bk-table-column>
                <bk-table-column :label="$t('store.状态')">
                    <template slot-scope="props">
                        <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL' || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING' || props.row.atomStatus === 'UNDERCARRIAGING'">
                            <div class="rotate rotate1"></div>
                            <div class="rotate rotate2"></div>
                            <div class="rotate rotate3"></div>
                            <div class="rotate rotate4"></div>
                            <div class="rotate rotate5"></div>
                            <div class="rotate rotate6"></div>
                            <div class="rotate rotate7"></div>
                            <div class="rotate rotate8"></div>
                        </div>
                        <span class="atom-status-icon success" v-if="props.row.atomStatus === 'RELEASED'"></span>
                        <span class="atom-status-icon fail" v-if="props.row.atomStatus === 'GROUNDING_SUSPENSION'"></span>
                        <span class="atom-status-icon obtained" v-if="props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'UNDERCARRIAGED'"></span>
                        <span class="atom-status-icon devops-icon icon-initialize" v-if="props.row.atomStatus === 'INIT'"></span>
                        <span>{{ $t(atomStatusList[props.row.atomStatus]) }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.修改人')" prop="modifier"></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="150"></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="240" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="upgrade-btn"
                            v-if="props.row.atomStatus === 'GROUNDING_SUSPENSION' || props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED'"
                            @click="editHandle('upgradeAtom', props.row.atomId)"> {{ $t('store.升级') }} </span>
                        <span class="install-btn"
                            v-if="props.row.atomStatus === 'RELEASED'"
                            @click="installAHandle(props.row.atomCode)"> {{ $t('store.安装') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.atomStatus === 'INIT' || props.row.atomStatus === 'UNDERCARRIAGED'"
                            @click="editHandle('shelfAtom', props.row.atomId)"> {{ $t('store.上架') }} </span>
                        <span class="obtained-btn"
                            v-if="['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'].includes(props.row.atomStatus) && props.row.releaseFlag"
                            @click="offline(props.row)"> {{ $t('store.下架') }} </span>
                        <span class="schedule-btn"
                            v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL'
                                || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING'"
                            @click="routerProgress(props.row)"> {{ $t('store.进度') }} </span>
                        <span class="delete-btn" v-if="!props.row.releaseFlag" @click="deleteAtom(props.row)"> {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>
        </main>
        <template v-if="createAtomsideConfig.show">
            <bk-sideslider
                class="create-atom-slider g-slide-radio"
                :is-show.sync="createAtomsideConfig.show"
                :title="createAtomsideConfig.title"
                :quick-close="createAtomsideConfig.quickClose"
                :width="createAtomsideConfig.width">
                <template slot="content">
                    <form class="bk-form create-atom-form" v-if="hasOauth"
                        v-bkloading="{
                            isLoading: createAtomsideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.名称') }} </label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" :placeholder="$t('store.请输入中英文名称，不超过20个字符')"
                                    name="atomName"
                                    v-model="createAtomForm.name"
                                    v-validate="{
                                        required: true,
                                        max: 20,
                                        regex: '^[\u4e00-\u9fa5a-zA-Z0-9-_]+$'
                                    }"
                                    :class="{ 'is-danger': errors.has('atomName') }">
                                <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">
                                    {{ errors.first("atomName") && errors.first("atomName").indexOf($t('store.正则')) > 0 ? $t('store.由汉字、英文字母、数字、连字符(-)和下划线组成，长度小于20个字符') : errors.first("atomName") }}
                                </p>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.标识') }} </label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%;">
                                    <input type="text" class="bk-form-input atom-id-input" :placeholder="$t('store.请输入英文名称，不超过30个字符')"
                                        name="atomId"
                                        v-model="createAtomForm.atomCode"
                                        v-validate="{
                                            required: true,
                                            max: 30,
                                            regex: '^[a-zA-Z]+$'
                                        }"
                                        :class="{ 'is-danger': errors.has('atomId') }">
                                    <p :class="errors.has('atomId') ? 'error-tips' : 'normal-tips'">
                                        {{ errors.first("atomId") && errors.first("atomId").indexOf($t('store.正则')) > 0 ? $t('store.只能输入英文') : errors.first("atomId") }}
                                    </p>
                                </div>
                                <bk-popover placement="right">
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
                                <bk-popover placement="right" width="400">
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
                                <bk-select v-model="createAtomForm.language" searchable>
                                    <bk-option v-for="(option, index) in languageList"
                                        :key="index"
                                        :id="option.language"
                                        :name="option.name"
                                        @click.native="selectedLanguage"
                                        :placeholder="$t('store.请选择开发语言')"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.languageError" class="error-tips"> {{ $t('store.开发语言不能为空') }} </div>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.自定义前端') }} </label>
                            <div class="bk-form-content atom-item-content">
                                <bk-radio-group v-model="createAtomForm.frontendType">
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
        </template>
        <template v-if="offlinesideConfig.show">
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
        </template>
        <bk-dialog v-model="deleteObj.visible"
            render-directive="if"
            theme="primary"
            ext-cls="delete-dialog-wrapper"
            :title="$t('store.确定删除插件', [deleteObj.name])"
            width="500"
            footer-position="center"
            :mask-close="false"
            :auto-close="false"
        >
            <bk-form ref="deleteForm" class="delete-form" :label-width="0" :model="deleteObj.formData">
                <p>{{$t('store.删除时将清理数据，包括工蜂代码库。删除后不可恢复！')}}</p>
                <p>{{$t('store.deleteAtomTip', [deleteObj.atomCode])}}</p>
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
    </main>
</template>

<script>
    import { debounce } from '@/utils'
    import formTips from '@/components/common/formTips/index'
    import { atomStatusMap } from '@/store/constants'

    export default {
        components: {
            formTips
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
                    width: 565,
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
                }
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
                debounce(this.search)
            }
        },

        created () {
            this.getLanguage()
            this.requestList()
        },

        methods: {
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
                        this.cancelCreateAtom()
                        this.routerAtoms(this.createAtomForm.atomCode)
                        this.requestList()
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
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
                this.createAtomsideConfig.show = false
            },

            routerAtoms (code) {
                this.$router.push({
                    name: 'overView',
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    /deep/ .delete-dialog-wrapper {
        .bk-form-item{
            .bk-label {
                padding: 0;
            }
        }
        p {
            margin-bottom: 15px;
            text-align: left;
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
    }
</style>
