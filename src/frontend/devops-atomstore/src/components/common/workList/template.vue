<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <button class="bk-button bk-primary" @click="relateTemplate()">
                    <span style="margin-left: 0;">{{ $t('store.list.relateTemplate') }}</span>
                </button>
            </div>
            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                <input class="g-input-border" type="text" :placeholder="$t('store.list.searchPlaceholder')" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                <i class="bk-icon icon-search" v-if="!searchName"></i>
                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
            </section>
        </div>
        <bk-table style="margin-top: 15px;"
            :empty-text="$t('store.list.emptyTemplate')"
            :data="renderList"
            :pagination="pagination"
            @page-change="pageChanged"
            @page-limit-change="pageCountChanged"
            v-bkloading="{ isLoading }"
        >
            <bk-table-column :label="$t('store.list.templateName')">
                <template slot-scope="props">
                    <span class="atom-name" :title="props.row.templateName" @click="routerAtoms(props.row.templateCode)">{{ props.row.templateName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('store.form.project')" prop="projectName"></bk-table-column>
            <bk-table-column :label="$t('store.list.status')">
                <template slot-scope="props">
                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                        v-if="props.row.templateStatus === 'AUDITING'">
                        <div class="rotate rotate1"></div>
                        <div class="rotate rotate2"></div>
                        <div class="rotate rotate3"></div>
                        <div class="rotate rotate4"></div>
                        <div class="rotate rotate5"></div>
                        <div class="rotate rotate6"></div>
                        <div class="rotate rotate7"></div>
                        <div class="rotate rotate8"></div>
                    </div>
                    <span class="atom-status-icon success" v-if="props.row.templateStatus === 'RELEASED'"></span>
                    <span class="atom-status-icon fail" v-if="props.row.templateStatus === 'GROUNDING_SUSPENSION'"></span>
                    <span class="atom-status-icon obtained" v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'UNDERCARRIAGED'"></span>
                    <span class="atom-status-icon bk-icon icon-initialize" v-if="props.row.templateStatus === 'INIT'"></span>
                    <span>{{ templateStatusMap[props.row.templateStatus] }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('store.list.modifier')" prop="modifier"></bk-table-column>
            <bk-table-column :label="$t('store.list.updateTime')" prop="updateTime" width="150" :formatter="timeFormatter"></bk-table-column>
            <bk-table-column :label="$t('store.list.operation')" width="250" class-name="handler-btn">
                <template slot-scope="props">
                    <span class="shelf-btn"
                        v-if="props.row.templateStatus === 'INIT' || props.row.templateStatus === 'UNDERCARRIAGED'
                            || props.row.templateStatus === 'GROUNDING_SUSPENSION' || props.row.templateStatus === 'AUDIT_REJECT'"
                        @click="editHandle(props.row.templateId)">{{ $t('store.list.shelf') }}
                    </span>
                    <span class="shelf-btn"
                        v-if="props.row.templateStatus === 'RELEASED'"
                        @click="editHandle(props.row.templateId)">{{ $t('store.list.upgrade') }}
                    </span>
                    <span class="shelf-btn"
                        v-if="props.row.templateStatus === 'RELEASED'"
                        @click="installAHandle(props.row.templateCode)">{{ $t('store.install') }}
                    </span>
                    <span class="schedule-btn"
                        v-if="props.row.templateStatus === 'AUDITING'"
                        @click="toTemplateProgress(props.row.templateId)">{{ $t('store.list.progress') }}
                    </span>
                    <span class="obtained-btn"
                        v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'RELEASED' || (props.row.templateStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                        @click="offline(props.row)"
                    >{{ $t('store.list.offline') }}</span>
                    <span @click="deleteTemplate(props.row)" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.templateStatus)">{{ $t('store.delete') }}</span>
                    <span style="margin-right:0">
                        <a target="_blank"
                            style="color:#3c96ff;"
                            :href="`/console/pipeline/${props.row.projectCode}/template/${props.row.templateCode}/edit`"
                        >{{ $t('store.list.sourceTemplate') }}</a>
                    </span>
                </template>
            </bk-table-column>
        </bk-table>

        <template v-if="templatesideConfig.show">
            <bk-sideslider
                class="create-atom-slider"
                :is-show.sync="templatesideConfig.show"
                :title="templatesideConfig.title"
                :quick-close="templatesideConfig.quickClose"
                :width="templatesideConfig.width">
                <template slot="content">
                    <form class="bk-form relate-template-form"
                        v-bkloading="{
                            isLoading: templatesideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.form.project') }}</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%">
                                    <big-select v-model="relateTemplateForm.projectCode" :searchable="true" @toggle="toggleProjectList" :options="projectList" setting-key="project_code" display-key="project_name" :placeholder="$t('store.list.projectPlaceholder')">
                                        <div slot="extension" style="cursor: pointer;">
                                            <a :href="itemUrl" target="_blank">
                                                <i class="bk-icon icon-plus-circle" />
                                                {{ itemText }}
                                            </a>
                                        </div>
                                    </big-select>
                                    <div v-if="templateErrors.projectError" class="error-tips">{{ $t('store.list.requiredProject') }}</div>
                                </div>
                                <bk-popover placement="right">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>{{ $t('store.list.sourceTemplateProject') }}</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.template') }}</label>
                            <div class="bk-form-content atom-item-content">
                                <bk-select v-model="relateTemplateForm.template" searchable>
                                    <bk-option v-for="(option, index) in templateList"
                                        :key="index"
                                        :id="option.templateId"
                                        :name="option.name"
                                        :placeholder="$t('store.list.templatePlaceholder')"
                                        @click.native="selectedTemplate"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="templateErrors.tplError" class="error-tips">{{ $t('store.list.templateRequired') }}</div>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">{{ $t('store.form.name') }}</label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" :placeholder="$t('store.list.tplNamePlaceholder')"
                                    name="templateName"
                                    v-model="relateTemplateForm.name"
                                    v-validate="{
                                        required: true,
                                        max: 20
                                    }"
                                    :class="{ 'is-danger': errors.has('templateName') }">
                                <p :class="errors.has('templateName') ? 'error-tips' : 'normal-tips'">{{ errors.first("templateName") }}</p>
                            </div>
                        </div>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitRelateTemplate()">{{ $t('store.submit') }}</button>
                            <button class="bk-button bk-default" type="button" @click="cancelRelateTemplate()">{{ $t('store.cancel') }}</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
        </template>

        <template v-if="offlineTempConfig.show">
            <bk-sideslider
                class="offline-atom-slider"
                :is-show.sync="offlineTempConfig.show"
                :title="offlineTempConfig.title"
                :quick-close="offlineTempConfig.quickClose"
                :width="offlineTempConfig.width">
                <template slot="content">
                    <form class="bk-form offline-atom-form"
                        v-bkloading="{
                            isLoading: offlineTempConfig.isLoading
                        }">
                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('store.form.name') }}</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerTemp.templateName }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label">{{ $t('store.list.sourceTemplate') }}</label>
                            <div class="bk-form-content">
                                <a target="_blank"
                                    style="color:#3c96ff;display:block;"
                                    :href="`/console/pipeline/${curHandlerTemp.projectCode}/template/${curHandlerTemp.templateCode}/edit`"
                                >{{ $t('store.list.view') }}</a>
                            </div>
                        </div>
                        <form-tips :tips-content="offlineTips" :prompt-list="tempPromptList"></form-tips>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitofflineTemp">{{ $t('store.submit') }}</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
        </template>
    </main>
</template>

<script>
    import { getQueryString } from '@/utils/index'
    import formTips from '@/components/common/formTips/index'
    import { templateStatusList } from '@/store/constants'

    export default {
        components: {
            formTips
        },

        data () {
            return {
                templateStatusMap: templateStatusList,
                isInputFocus: false,
                isSearch: false,
                searchName: '',
                itemUrl: '/console/pm',
                itemText: this.$t('newProject'),
                offlineTips: this.$t('store.list.afterOffline'),
                renderList: [],
                templateList: [],
                projectList: [],
                tempPromptList: [
                    `1、${this.$t('store.list.offlineTempalteDesc1')}`,
                    `2、${this.$t('store.list.offlineTempalteDesc2')}`
                ],
                curHandlerTemp: {},
                relateTemplateForm: {
                    projectCode: '',
                    template: '',
                    name: ''
                },
                isLoading: false,
                templateErrors: {
                    projectError: false,
                    tplError: false
                },
                offlineTempConfig: {
                    show: false,
                    isLoading: false,
                    title: this.$t('store.list.offlineTemplate'),
                    quickClose: true,
                    width: 565
                },
                templatesideConfig: {
                    show: false,
                    isLoading: false,
                    quickClose: true,
                    width: 565,
                    title: this.$t('store.list.linkTplToStore')
                },
                statusList: {
                    publish: this.$t('store.list.published'),
                    commiting: this.$t('store.list.commiting'),
                    fail: this.$t('store.list.shelffail'),
                    testing: this.$t('store.list.testing'),
                    auditing: this.$t('store.list.auditing'),
                    obtained: this.$t('store.list.obtained'),
                    draft: this.$t('store.list.draft')
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                }
            }
        },

        watch: {
            'relateTemplateForm.projectCode' (newVal, oldVal) {
                if (newVal) {
                    this.selectedTplProject()
                }
            }
        },

        mounted () {
            this.requestList()
            if (getQueryString('projectCode') && getQueryString('templateId')) {
                this.relateTemplateForm.projectCode = getQueryString('projectCode')
                this.relateTemplateForm.template = getQueryString('templateId')
                this.templatesideConfig.show = true
            }
        },

        methods: {
            async requestList () {
                this.isLoading = true
                const page = this.pagination.current
                const pageSize = this.pagination.limit

                try {
                    const res = await this.$store.dispatch('store/requestTemplateList', {
                        templateName: this.searchName,
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

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            deleteTemplate (row) {
                this.isLoading = true
                let message = this.$t('store.list.deleteSuccess')
                let theme = 'success'

                this.$store.dispatch('store/deleteTemplate', row.templateCode).then((res) => {
                    this.requestList()
                }).catch((err) => {
                    message = err.message || err
                    theme = 'error'
                }).finally(() => {
                    this.$bkMessage({ message, theme })
                    this.isLoading = false
                })
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

            async submitofflineTemp () {
                let message, theme

                this.offlineTempConfig.isLoading = true
                try {
                    await this.$store.dispatch('store/offlineTemplate', {
                        templateCode: this.curHandlerTemp.templateCode
                    })

                    message = this.$t('store.list.offineSuccess')
                    theme = 'success'
                    this.offlineTempConfig.show = false
                    this.requestList()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.offlineTempConfig.isLoading = false
                }
            },

            checkTplValid () {
                let errorCount = 0
                if (!this.relateTemplateForm.projectCode) {
                    this.templateErrors.projectError = true
                    errorCount++
                }
                if (!this.relateTemplateForm.template) {
                    this.templateErrors.tplError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },

            async submitRelateTemplate () {
                const isCheckValid = this.checkTplValid()
                const valid = await this.$validator.validate()
                if (isCheckValid && valid) {
                    let message, theme
                    const params = {
                        projectCode: this.relateTemplateForm.projectCode,
                        templateName: this.relateTemplateForm.name
                    }

                    this.templatesideConfig.isLoading = true
                    try {
                        await this.$store.dispatch('store/relateTemplate', {
                            templateCode: this.relateTemplateForm.template,
                            params
                        })

                        message = this.$t('store.list.relatedSuccess')
                        theme = 'success'
                        this.cancelRelateTemplate()
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.templatesideConfig.isLoading = false
                        this.requestList()
                    }
                }
            },

            cancelRelateTemplate () {
                this.templatesideConfig.show = false
                this.templateErrors.projectError = false
                this.templateErrors.tplError = false
                this.relateTemplateForm = {
                    projectCode: '',
                    template: '',
                    name: ''
                }
            },

            async toggleProjectList (isdropdown) {
                if (isdropdown) {
                    const res = await this.$store.dispatch('store/requestProjectList')
                    this.projectList.splice(0, this.projectList.length, ...res)
                }
            },

            async selectedTplProject () {
                this.templateErrors.projectError = false
                try {
                    const res = await this.$store.dispatch('store/requestPipelineTemplate', {
                        projectCode: this.relateTemplateForm.projectCode
                    })
                    this.templateList.splice(0, this.templateList.length, ...res.models || [])
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },

            selectedTemplate () {
                this.templateErrors.tplError = false
            },

            routerAtoms (templateCode) {
                this.$router.push({
                    name: 'tplOverview',
                    params: {
                        templateCode
                    }
                })
            },

            toTemplateProgress (id) {
                this.$router.push({
                    name: 'upgradeTemplate',
                    params: {
                        templateId: id
                    }
                })
            },

            relateTemplate () {
                this.templatesideConfig.show = true
            },

            offline (form) {
                this.offlineTempConfig.show = true
                this.curHandlerTemp = form
            },

            installAHandle (code) {
                this.$router.push({
                    name: 'install',
                    query: {
                        code,
                        type: 'template',
                        from: 'atomList'
                    }
                })
            },

            editHandle (templateId) {
                this.$router.push({
                    name: 'editTemplate',
                    params: { templateId }
                })
            }
        }
    }
</script>
