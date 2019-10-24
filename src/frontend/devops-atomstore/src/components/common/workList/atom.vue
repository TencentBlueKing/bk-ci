<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button class="bk-button bk-primary" @click.native="createNewAtom">{{ $t('store.list.createAtom') }}</bk-button>
            </div>
            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                <input class="g-input-border" type="text" :placeholder="$t('store.list.searchPlaceholder')" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                <i class="bk-icon icon-search" v-if="!searchName"></i>
                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
            </section>
        </div>
        <bk-table style="margin-top: 15px;"
            :empty-text="$t('store.list.emptyAtom')"
            :data="renderList"
            :pagination="pagination"
            @page-change="pageChanged"
            @page-limit-change="pageCountChanged"
            v-bkloading="{ isLoading }"
        >
            <bk-table-column :label="$t('store.list.atomName')">
                <template slot-scope="props">
                    <span class="atom-name" :title="props.row.name" @click="routerAtoms(props.row.atomCode)">{{ props.row.name }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('store.form.project')" prop="projectName"></bk-table-column>
            <bk-table-column :label="$t('store.list.language')" prop="language"></bk-table-column>
            <bk-table-column :label="$t('store.form.version')" prop="version"></bk-table-column>
            <bk-table-column :label="$t('store.list.status')">
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
            <bk-table-column :label="$t('store.list.modifier')" prop="modifier"></bk-table-column>
            <bk-table-column :label="$t('store.list.updateTime')" prop="updateTime" width="150"></bk-table-column>
            <bk-table-column :label="$t('store.list.operation')" width="200" class-name="handler-btn">
                <template slot-scope="props">
                    <span class="upgrade-btn"
                        v-if="props.row.atomStatus === 'GROUNDING_SUSPENSION' || props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED'"
                        @click="editHandle('upgradeAtom', props.row.atomId)">{{ $t('store.list.upgrade') }}</span>
                    <span class="install-btn"
                        v-if="props.row.atomStatus === 'RELEASED'"
                        @click="installAHandle(props.row.atomCode)">{{ $t('store.install') }}</span>
                    <span class="shelf-btn"
                        v-if="props.row.atomStatus === 'INIT' || props.row.atomStatus === 'UNDERCARRIAGED'"
                        @click="editHandle('shelfAtom', props.row.atomId)">{{ $t('store.list.shelf') }}</span>
                    <span class="obtained-btn"
                        v-if="props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED' || (props.row.atomStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                        @click="offline(props.row)">{{ $t('store.list.offline') }}</span>
                    <span class="schedule-btn"
                        v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL'
                            || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING'"
                        @click="routerProgress(props.row.atomId)">{{ $t('store.list.progress') }}</span>
                    <span class="delete-btn" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.atomStatus) && !props.row.version" @click="deleteAtom(props.row)">{{ $t('store.delete') }}</span>
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
                    <form class="bk-form create-atom-form"
                        v-bkloading="{
                            isLoading: createAtomsideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.list.name') }}</label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" :placeholder="$t('store.list.namePlaceholder')"
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
                            <label class="bk-label">{{ $t('store.list.mark') }}</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%;">
                                    <input type="text" class="bk-form-input atom-id-input" :placeholder="$t('store.list.markPlaceholder')"
                                        name="atomId"
                                        v-model="createAtomForm.atomCode"
                                        v-validate="{
                                            required: true,
                                            max: 30,
                                            regex: '^[a-zA-Z]+$'
                                        }"
                                        :class="{ 'is-danger': errors.has('atomId') }">
                                    <p :class="errors.has('atomId') ? 'error-tips' : 'normal-tips'">
                                        {{ errors.first("atomId") && errors.first("atomId").indexOf($t('store.list.regular')) > 0 ? $t('store.list.requiredEn') : errors.first("atomId") }}
                                    </p>
                                </div>
                                <bk-popover placement="right">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>{{ $t('store.list.markDesc') }}</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.form.project') }}</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%">
                                    <big-select v-model="createAtomForm.projectCode" @selected="selectedProject" :searchable="true" @toggle="toggleProjectList" :options="projectList" setting-key="project_code" display-key="project_name" :placeholder="$t('store.list.projectPlaceholder')">
                                        <div slot="extension" style="cursor: pointer;">
                                            <a :href="itemUrl" target="_blank">
                                                <i class="bk-icon icon-plus-circle" />
                                                {{ itemText }}
                                            </a>
                                        </div>
                                    </big-select>
                                    <div v-if="atomErrors.projectError" class="error-tips">{{ $t('store.lsit.requiredProject') }}</div>
                                </div>
                                <bk-popover placement="right" width="400">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>{{ $t('store.lsit.projectDesc') }}</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.lsit.language') }}</label>
                            <div class="bk-form-content atom-item-content">
                                <bk-select v-model="createAtomForm.language" searchable>
                                    <bk-option v-for="(option, index) in languageList"
                                        :key="index"
                                        :id="option.language"
                                        :name="option.name"
                                        @click.native="selectedLanguage"
                                        :placeholder="$t('store.lsit.languagePlaceholder')"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.languageError" class="error-tips">{{ $t('store.lsit.languageRequired') }}</div>
                            </div>
                        </div>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitCreateAtom()">{{ $t('store.submit') }}</button>
                            <button class="bk-button bk-default" type="button" @click="cancelCreateAtom()">{{ $t('store.cancel') }}</button>
                        </div>
                    </form>
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
                            <label class="bk-label">{{ $t('store.list.name') }}</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerAtom.name }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('store.list.mark') }}</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerAtom.atomCode }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.list.bufferTime') }}</label>
                            <div class="bk-form-content">
                                <bk-select v-model="buffer" searchable>
                                    <bk-option v-for="(option, index) in bufferLength"
                                        :key="index"
                                        :id="option.value"
                                        :name="option.label"
                                        @click.native="selectedBuffer"
                                        :placeholder="$t('store.list.bufferTimePlaceholder')"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.bufferError" class="error-tips">{{ $t('store.list.bufferTimeRequired') }}</div>
                            </div>
                        </div>
                        <form-tips :tips-content="offlineTips" :prompt-list="promptList"></form-tips>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitofflineAtom()">{{ $t('store.submit') }}</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
        </template>
    </main>
</template>

<script>
    import { atomStatusMap } from '@/store/constants'

    export default {

        data () {
            return {
                atomStatusList: atomStatusMap,
                isInputFocus: false,
                bufferError: false,
                buffer: '',
                searchName: '',
                gitOAuthUrl: '',
                itemUrl: '/console/pm',
                itemText: this.$t('newProject'),
                offlineTips: this.$t('store.list.afterOffline'),
                renderList: [],
                projectList: [],
                languageList: [],
                promptList: [
                    `1、${this.$t('store.list.offlineAtomDesc1')}`,
                    `2、${this.$t('store.list.offlineAtomDesc2')}`,
                    `3、${this.$t('store.list.offlineAtomDesc3')}`
                ],
                curHandlerAtom: {},
                bufferLength: [
                    { label: `0${this.$t('store.list.days')}`, value: '0' },
                    { label: `7${this.$t('store.list.days')}`, value: '7' },
                    { label: `15${this.$t('store.list.days')}`, value: '15' }
                ],
                createAtomForm: {
                    projectCode: '',
                    atomCode: '',
                    name: '',
                    language: ''
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
                    title: this.$t('store.list.createAtom')
                },
                offlinesideConfig: {
                    show: false,
                    isLoading: false,
                    title: this.$t('store.list.offineAtom'),
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
            this.requestList()
        },

        methods: {
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

                        message = this.$t('store.list.newSuccess')
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

                        message = this.$t('store.list.submitSuccess')
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

            routerProgress (id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: 'shelf',
                        atomId: id
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

                    message = this.$t('store.list.deleteSuccess')
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
                }, `${this.$t('store.list.comfireDeleteAtom')}(${row.name})？`)

                this.$bkInfo({
                    title: this.$t('store.delete'),
                    subHeader,
                    confirmFn: async () => {
                        this.requestDeleteAtom(row.atomCode)
                    }
                })
            }
        }
    }
</script>
