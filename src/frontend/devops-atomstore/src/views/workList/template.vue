<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button theme="primary" @click="relateTemplate"> {{ $t('store.关联模板') }} </bk-button>
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
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :data="renderList"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageCountChanged"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column :label="$t('store.模板名称')" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span class="atom-name" :title="props.row.templateName" @click="routerAtoms(props.row.templateCode)">{{ props.row.templateName }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.所属项目')" prop="projectName" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.状态')" show-overflow-tooltip>
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
                        <span class="atom-status-icon devops-icon icon-initialize" v-if="props.row.templateStatus === 'INIT'"></span>
                        <span>{{ $t(templateStatusMap[props.row.templateStatus]) }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.修改人')" prop="modifier" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="150" :formatter="timeFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="250" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="shelf-btn"
                            v-if="props.row.templateStatus === 'INIT' || props.row.templateStatus === 'UNDERCARRIAGED'
                                || props.row.templateStatus === 'GROUNDING_SUSPENSION' || props.row.templateStatus === 'AUDIT_REJECT'"
                            @click="editHandle(props.row.templateId)"> {{ $t('store.上架') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.templateStatus === 'RELEASED'"
                            @click="editHandle(props.row.templateId)"> {{ $t('store.升级') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.templateStatus === 'RELEASED'"
                            @click="installAHandle(props.row.templateCode)"> {{ $t('store.安装') }} </span>
                        <span class="schedule-btn"
                            v-if="props.row.templateStatus === 'AUDITING'"
                            @click="toTemplateProgress(props.row.templateId)"> {{ $t('store.进度') }} </span>
                        <span class="obtained-btn"
                            v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'RELEASED' || (props.row.templateStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                            @click="offline(props.row)"
                        > {{ $t('store.下架') }} </span>
                        <span @click="deleteTemplate(props.row)" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.templateStatus)"> {{ $t('store.移除') }} </span>
                        <span style="margin-right:0">
                            <a target="_blank"
                                style="color:#3c96ff;"
                                :href="`/console/pipeline/${props.row.projectCode}/template/${props.row.templateCode}/edit`"
                            > {{ $t('store.源模板') }} </a>
                        </span>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus :type="searchName ? 'search-empty' : 'empty'" @clear="searchName = ''" />
                </template>
            </bk-table>
        </main>

        <template v-if="templatesideConfig.show">
            <bk-sideslider
                class="create-atom-slider"
                :is-show.sync="templatesideConfig.show"
                :title="templatesideConfig.title"
                :quick-close="templatesideConfig.quickClose"
                :width="templatesideConfig.width"
                :before-close="cancelRelateTemplate">
                <template slot="content">
                    <form class="bk-form relate-template-form"
                        v-bkloading="{
                            isLoading: templatesideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.所属项目') }} </label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%">
                                    <bk-select v-model="relateTemplateForm.projectCode"
                                        searchable
                                        @change="handleChangeProject"
                                        @toggle="toggleProjectList"
                                        :placeholder="$t('store.请选择项目')"
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
                                    <div v-if="templateErrors.projectError" class="error-tips"> {{ $t('store.项目不能为空') }} </div>
                                </div>
                                <bk-popover placement="right" class="bk-icon-tooltips">
                                    <i class="devops-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p> {{ $t('store.源模版所属项目') }} </p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.模板') }} </label>
                            <div class="bk-form-content atom-item-content">
                                <bk-select
                                    v-model="relateTemplateForm.template"
                                    searchable
                                    @change="handleChangeForm"
                                >
                                    <bk-option v-for="(option, index) in templateList"
                                        :key="index"
                                        :id="option.templateId"
                                        :name="option.name"
                                        :placeholder="$t('store.请选择模板')"
                                        @click.native="selectedTemplate"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="templateErrors.tplError" class="error-tips"> {{ $t('store.模板不能为空') }} </div>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label"> {{ $t('store.名称') }} </label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" :placeholder="$t('store.请输入发布到市场后的模板名称')"
                                    name="templateName"
                                    v-model="relateTemplateForm.name"
                                    v-validate="{
                                        required: true,
                                        max: 20
                                    }"
                                    @input="handleChangeForm"
                                    :class="{ 'is-danger': errors.has('templateName') }">
                                <p :class="errors.has('templateName') ? 'error-tips' : 'normal-tips'">{{ errors.first("templateName") }}</p>
                            </div>
                        </div>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitRelateTemplate()"> {{ $t('store.提交') }} </button>
                            <button class="bk-button bk-default" type="button" @click="cancelRelateTemplate()"> {{ $t('store.取消') }} </button>
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
                            <label class="bk-label"> {{ $t('store.名称') }} </label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerTemp.templateName }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label"> {{ $t('store.源模板') }} </label>
                            <div class="bk-form-content">
                                <a target="_blank"
                                    style="color:#3c96ff;display:block;"
                                    :href="`/console/pipeline/${curHandlerTemp.projectCode}/template/${curHandlerTemp.templateCode}/edit`"
                                > {{ $t('store.查看') }} </a>
                            </div>
                        </div>
                        <form-tips :tips-content="offlineTips" :prompt-list="tempPromptList"></form-tips>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitofflineTemp"> {{ $t('store.提交') }} </button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
        </template>
    </main>
</template>

<script>
    import { getQueryString, debounce } from '@/utils/index'
    import formTips from '@/components/common/formTips/index'
    import { templateStatusList } from '@/store/constants'

    export default {
        components: {
            formTips
        },

        data () {
            return {
                templateStatusMap: templateStatusList,
                isSearch: false,
                searchName: '',
                itemUrl: '/console/pm',
                itemText: this.$t('store.新建项目'),
                offlineTips: this.$t('store.下架后：'),
                renderList: [],
                templateList: [],
                projectList: [],
                tempPromptList: [
                    this.$t('store.1、不再在模版市场中展示'),
                    this.$t('store.2、已使用模版的流水线可以继续使用，但有模版已下架标识')
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
                    title: this.$t('store.下架模板'),
                    quickClose: true,
                    width: 565
                },
                templatesideConfig: {
                    show: false,
                    isLoading: false,
                    quickClose: true,
                    width: 565,
                    title: this.$t('store.关联模板到Store')
                },
                statusList: {
                    publish: this.$t('store.已发布'),
                    commiting: this.$t('store.提交中'),
                    fail: this.$t('store.上架失败'),
                    testing: this.$t('store.测试中'),
                    auditing: this.$t('store.审核中'),
                    obtained: this.$t('store.已下架'),
                    draft: this.$t('store.草稿')
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
            },
            searchName () {
                this.isLoading = true
                debounce(this.search)
            }
        },

        mounted () {
            this.requestList()
            if (getQueryString('projectCode') && getQueryString('templateId')) {
                this.relateTemplateForm.projectCode = getQueryString('projectCode')
                this.relateTemplateForm.template = getQueryString('templateId')
                this.templatesideConfig.show = true
                this.toggleProjectList(true)
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

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            deleteTemplate (row) {
                this.isLoading = true
                let message = this.$t('store.移除成功')
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

                    message = this.$t('store.下架成功')
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

                        message = this.$t('store.关联成功')
                        theme = 'success'
                        
                        this.templateErrors.projectError = false
                        this.templateErrors.tplError = false
                        this.relateTemplateForm = {
                            projectCode: '',
                            template: '',
                            name: ''
                        }

                        setTimeout(() => {
                            this.templatesideConfig.show = false
                        })
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
                            this.relateTemplateForm = {
                                projectCode: '',
                                template: '',
                                name: ''
                            }
                            setTimeout(() => {
                                this.templatesideConfig.show = false
                                this.templateErrors.projectError = false
                                this.templateErrors.tplError = false
                            })
                            return true
                        }
                    })
                } else {
                    this.templatesideConfig.show = false
                    this.templateErrors.projectError = false
                    this.templateErrors.tplError = false
                    this.relateTemplateForm = {
                        projectCode: '',
                        template: '',
                        name: ''
                    }
                }
            },

            async toggleProjectList (isdropdown) {
                if (isdropdown) {
                    const res = await this.$store.dispatch('store/requestProjectList')
                    this.projectList.splice(0, this.projectList.length, ...res)
                }
            },

            /**
             * 切换所属项目，清空模板数据
             */
            handleChangeProject () {
                this.handleChangeForm()
                this.relateTemplateForm.template = ''
            },

            handleChangeForm () {
                window.changeFlag = true
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

            routerAtoms (code) {
                this.$router.push({
                    name: 'setting',
                    params: {
                        code,
                        type: 'template'
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
                window.changeFlag = false
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
                        from: 'templateWork'
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
<style lang="scss">
    .bk-icon-tooltips {
        padding-top: 3px;
        padding-left: 10px;
    }
    
</style>
