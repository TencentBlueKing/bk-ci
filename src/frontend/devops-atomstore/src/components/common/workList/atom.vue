<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <button class="bk-button bk-primary" @click="createNewAtom"> {{ $t('store.新增插件') }} </button>
            </div>
            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                <input class="g-input-border" type="text" :placeholder="$t('store.请输入关键字搜索')" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                <i class="bk-icon icon-search" v-if="!searchName"></i>
                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
            </section>
        </div>
        <bk-table style="margin-top: 15px;" :empty-text="$t('store.暂时没有插件')"
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
                    <span class="atom-status-icon bk-icon icon-initialize" v-if="props.row.atomStatus === 'INIT'"></span>
                    <span>{{ atomStatusList[props.row.atomStatus] }}</span>
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
                        v-if="props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED' || (props.row.atomStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                        @click="offline(props.row)"> {{ $t('store.下架') }} </span>
                    <span class="schedule-btn"
                        v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL'
                            || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING'"
                        @click="routerProgress(props.row)"> {{ $t('store.进度') }} </span>
                    <span class="delete-btn" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.atomStatus) && !props.row.version" @click="deleteAtom(props.row)"> {{ $t('store.删除') }} </span>
                </template>
            </bk-table-column>
        </bk-table>

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
                                        max: 20
                                    }"
                                    :class="{ 'is-danger': errors.has('atomName') }">
                                <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">{{ errors.first("atomName") }}</p>
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
                                    <i class="bk-icon icon-info-circle"></i>
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
                                    <big-select v-model="createAtomForm.projectCode" @selected="selectedProject" :searchable="true" @toggle="toggleProjectList" :options="projectList" setting-key="projectCode" display-key="projectName" :placeholder="$t('store.请选择调试项目')">
                                        <div slot="extension" style="cursor: pointer;">
                                            <a :href="itemUrl" target="_blank">
                                                <i class="bk-icon icon-plus-circle" />
                                                {{ itemText }}
                                            </a>
                                        </div>
                                    </big-select>
                                    <div v-if="atomErrors.projectError" class="error-tips"> {{ $t('store.项目不能为空') }} </div>
                                </div>
                                <bk-popover placement="right" width="400">
                                    <i class="bk-icon icon-info-circle"></i>
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
                        <template v-if="!isEnterprise">
                            <div class="bk-form-item is-required">
                                <label class="bk-label"> {{ $t('store.授权方式') }} </label>
                                <div class="bk-form-content atom-item-content">
                                    <bk-radio-group v-model="createAtomForm.authType">
                                        <bk-radio :value="entry.value" v-for="(entry, key) in authTypeList" :key="key">{{ entry.label }}</bk-radio>
                                    </bk-radio-group>
                                </div>
                            </div>
                            <div class="bk-form-item is-required">
                                <label class="bk-label"> {{ $t('store.是否开源') }} </label>
                                <div class="bk-form-content atom-item-content">
                                    <bk-radio-group v-model="createAtomForm.visibilityLevel">
                                        <bk-radio :value="entry.value" v-for="(entry, key) in isOpenSource" :key="key" @click.native="changeOpenSource">{{ entry.label }}</bk-radio>
                                    </bk-radio-group>
                                    <p v-if="atomErrors.openSourceError" class="error-tips"> {{ $t('store.是否开源不能为空') }} </p>
                                </div>
                            </div>
                            <div class="bk-form-item is-required" v-if="createAtomForm.visibilityLevel === 'PRIVATE'">
                                <label class="bk-label"> {{ $t('store.不开源原因') }} </label>
                                <div class="bk-form-content atom-item-content">
                                    <bk-input v-model="createAtomForm.privateReason" type="textarea" :placeholder="$t('store.请输入不开源原因')" @input="atomErrors.privateReasonError = false"></bk-input>
                                    <p v-if="atomErrors.privateReasonError" class="error-tips"> {{ $t('store.不开源原因不能为空') }} </p>
                                </div>
                            </div>
                            <form-tips :tips-content="createTips" class="atom-tip"></form-tips>
                        </template>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitCreateAtom()"> {{ $t('store.提交') }} </button>
                            <button class="bk-button bk-default" type="button" @click="cancelCreateAtom()"> {{ $t('store.取消') }} </button>
                        </div>
                    </form>
                    <div class="oauth-tips" v-else style="margin: 30px">
                        <button class="bk-button bk-primary" type="button" @click="openValidate"> {{ $t('store.OAUTH认证') }} </button>
                        <p class="prompt-oauth">
                            <i class="bk-icon icon-info-circle-shape"></i>
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
                            <label class="bk-label"> {{ $t('store.缓冲期') }} </label>
                            <div class="bk-form-content">
                                <bk-select v-model="buffer" searchable>
                                    <bk-option v-for="(option, index) in bufferLength"
                                        :key="index"
                                        :id="option.value"
                                        :name="option.label"
                                        @click.native="selectedBuffer"
                                        :placeholder="$t('store.请选择缓冲期')"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.bufferError" class="error-tips"> {{ $t('store.缓冲期不能为空') }} </div>
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
    </main>
</template>

<script>
    import formTips from '@/components/common/formTips/index'
    import { atomStatusMap } from '@/store/constants'

    export default {
        components: {
            formTips
        },

        data () {
            return {
                atomStatusList: atomStatusMap,
                isInputFocus: false,
                hasOauth: true,
                bufferError: false,
                buffer: '',
                searchName: '',
                gitOAuthUrl: '',
                itemUrl: '/console/pm',
                itemText: this.$t('store.新建项目'),
                offlineTips: this.$t('store.下架后：'),
                renderList: [],
                projectList: [],
                languageList: [],
                authTypeList: [
                    { label: this.$t('store.工蜂OAUTH'), value: 'OAUTH' }
                ],
                isOpenSource: [
                    { label: this.$t('store.是'), value: 'LOGIN_PUBLIC' },
                    { label: this.$t('store.否'), value: 'PRIVATE' }
                ],
                promptList: [
                    this.$t('store.1、插件市场不再展示插件'),
                    this.$t('store.2、已安装插件的项目不能再添加插件到流水线'),
                    this.$t('store.3、已使用插件的流水线可以继续使用，但有插件已下架标识')
                ],
                curHandlerAtom: {},
                bufferLength: [
                    { label: this.$t('store.0天'), value: '0' },
                    { label: this.$t('store.7天'), value: '7' },
                    { label: this.$t('store.15天'), value: '15' }
                ],
                createAtomForm: {
                    projectCode: '',
                    atomCode: '',
                    name: '',
                    language: '',
                    authType: 'OAUTH',
                    visibilityLevel: 'LOGIN_PUBLIC'
                },
                isLoading: false,
                atomErrors: {
                    projectError: false,
                    languageError: false,
                    bufferError: false,
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
                }
            }
        },

        computed: {
            createTips () {
                const host = location.host
                const innerHosts = ['dev.devops.oa.com', 'test.devops.oa.com']
                const index = innerHosts.findIndex(innerHost => innerHost === host)
                const group = index > -1 ? 'bkdevops-plugins-test' : 'bkdevops-plugins'
                return `${this.$t('store.提交后，系统将在工蜂自动创建插件代码库，地址示例')}：http://git.code.oa.com/${group}/${this.createAtomForm.atomCode}.git`
            },

            isEnterprise () {
                return VERSION_TYPE === 'ee'
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
            'offlinesideConfig.show' (val) {
                if (!val) {
                    this.atomErrors.bufferError = false
                    this.buffer = ''
                }
            }
        },

        created () {
            this.getLanguage()
            this.checkIsOAuth()
            this.requestList()
        },

        methods: {
            getLanguage () {
                this.$store.dispatch('store/getDevelopLanguage').then((res) => {
                    this.languageList = (res || []).map(({ language }) => ({ name: language, language }))
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' }))
            },

            async checkIsOAuth () {
                try {
                    const res = await this.$store.dispatch('store/checkIsOAuth', { type: 'ATOM_MARKET', atomCode: '' })
                    this.hasOauth = res.status === 200
                    this.gitOAuthUrl = res.url
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
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

            clearSearch () {
                this.searchName = ''
                this.requestList()
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

                if (this.isOpenSource.findIndex(x => x.value === this.createAtomForm.visibilityLevel) < 0) {
                    this.atomErrors.openSourceError = true
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
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.createAtomsideConfig.isLoading = false
                        this.requestList()
                        if (theme === 'success') {
                            this.cancelCreateAtom()
                        }
                    }
                }
            },

            async submitofflineAtom () {
                if (this.buffer === '') {
                    this.atomErrors.bufferError = true
                } else {
                    let message, theme
                    const params = {
                        bufferDay: this.buffer
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

            selectedBuffer () {
                this.atomErrors.bufferError = false
            },

            cancelCreateAtom () {
                this.createAtomsideConfig.show = false
            },

            routerAtoms (atomCode) {
                this.$router.push({
                    name: 'overview',
                    params: {
                        atomCode
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
                this.curHandlerAtom = form
            },

            installAHandle (code) {
                this.$router.push({
                    name: 'install',
                    query: {
                        code,
                        type: 'atom',
                        from: 'atomList'
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
                    await this.$store.dispatch('store/requestDeleteAtom', {
                        atomCode
                    })

                    message = this.$t('store.删除成功')
                    theme = 'success'
                    this.requestList()
                } catch (err) {
                    message = message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },

            deleteAtom (row) {
                const h = this.$createElement
                const subHeader = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, this.$t('store.确定删除插件', [row.name]))

                this.$bkInfo({
                    type: 'warning',
                    theme: 'warning',
                    subHeader,
                    confirmFn: async () => {
                        this.requestDeleteAtom(row.atomCode)
                    }
                })
            }
        }
    }
</script>
