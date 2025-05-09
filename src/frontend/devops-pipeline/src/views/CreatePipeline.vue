<template>
    <div class="create-pipeline-page-wrapper">
        <pipeline-header :title="$t('newlist.addPipeline')"></pipeline-header>
        <alert-tips
            v-if="enablePipelineNameTips"
            :title="$t('pipelineNameConventions')"
            :message="pipelineNameFormat"
        />
        <div
            v-bkloading="{ isLoading }"
            class="pipeline-template-box"
        >
            <aside class="pipeline-template-box-left-side">
                <bk-form form-type="vertical">
                    <bk-form-item :label="$t('pipelineName')">
                        <div class="pipeline-input">
                            <bk-input
                                ref="pipelineName"
                                :placeholder="$t('pipelineNameInputTips')"
                                maxlength="40"
                                name="newPipelineName"
                                v-model.trim="newPipelineName"
                                v-validate.initial="'required'"
                            />
                            <span
                                class="border-effect"
                                v-show="!errors.has('newPipelineName')"
                            ></span>
                            <span
                                v-show="errors.has('newPipelineName')"
                                class="validate-fail-border-effect"
                            ></span>
                        </div>
                    </bk-form-item>
                    <template v-if="!isActiveTempEmpty">
                        <bk-form-item :label="$t('type')">
                            <bk-radio-group
                                class="pipelinte-template-type-group"
                                v-model="templateType"
                            >
                                <bk-popover
                                    placement="bottom"
                                    v-for="(entry, key) in tplTypes"
                                    :key="key"
                                >
                                    <bk-radio
                                        :value="entry.value"
                                    >
                                        <span class="radio-label">{{ entry.label }}</span>
                                    </bk-radio>
                                    <div
                                        slot="content"
                                        style="white-space: normal;"
                                    >
                                        {{ entry.tip }}
                                    </div>
                                </bk-popover>
                            </bk-radio-group>
                        </bk-form-item>
                        <bk-form-item
                            :label="$t('copyTempConf')"
                            label-width="auto"
                        >
                            <bk-checkbox-group v-model="applySettings">
                                <div
                                    v-for="item in settingItems"
                                    :key="item.label"
                                    :class="['template-setting-apply-checkbox', {
                                        'disabled-setting': item.disabled
                                    }]"
                                >
                                    <bk-checkbox
                                        :value="item.value"
                                        :disabled="item.disabled || isConstrainMode"
                                    >
                                        <p class="template-apply-setting-checkbox-txt">
                                            <span class="template-apply-setting-checkbox-txt-label">{{ item.label }}</span>
                                        </p>
                                    </bk-checkbox>
                                    <em v-if="item.disabled">
                                        {{ $t('tempWithoutConf') }}
                                    </em>
                                    <bk-button
                                        text
                                        theme="primary"
                                        size="small"
                                        @click.stop="previewSetting(item.value)"
                                        v-else
                                    >
                                        {{ $t('pipelinesPreview') }}
                                    </bk-button>
                                </div>
                            </bk-checkbox-group>
                        </bk-form-item>
                    </template>
                    <bk-form-item>
                        <pipeline-label-selector
                            :value.sync="labelValues"
                            :disabled="isConstrainMode"
                        />
                    </bk-form-item>
                    <bk-form-item ext-cls="namingConvention">
                        <syntax-style-configuration
                            :disabled="isConstrainMode"
                            :inherited-dialect="inheritedDialect"
                            :pipeline-dialect="pipelineDialect"
                            @inherited-change="inheritedChange"
                            @pipeline-dialect-change="pipelineDialectChange"
                        />
                    </bk-form-item>
                </bk-form>
                <footer style="margin-top: 24px;">
                    <bk-button
                        theme="primary"
                        :disabled="isConfirmDisable"
                        @click="createNewPipeline"
                    >
                        {{ $t('create') }}
                    </bk-button>
                    <bk-button
                        @click="goList"
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                </footer>
            </aside>
            <div class="pipeline-template-box-right-side">
                <bk-tab
                    :active.sync="activePanel"
                    @tab-change="handleTemplateTypeChange"
                    type="unborder-card"
                >
                    <div
                        class="pipeline-template-searchbox"
                        slot="setting"
                    >
                        <bk-input
                            v-model.trim="searchName"
                            icon-right="bk-icon icon-search"
                            :placeholder="$t('searchPipelineTemplate')"
                        />
                    </div>
                    <bk-tab-panel
                        v-for="(panel, index) in panels"
                        v-bind="panel"
                        :key="index"
                    >
                    </bk-tab-panel>
                </bk-tab>
                <ul
                    class="create-pipeline-template-list"
                    v-if="tempList.length"
                    @scroll.passive="scrollLoadMore"
                >
                    <li
                        v-for="(temp, tIndex) in tempList"
                        :class="{
                            'active': activeTempIndex === tIndex,
                            'disabled': !temp.installed
                        }"
                        :key="temp.name"
                        @click="selectTemp(tIndex)"
                    >
                        <span
                            v-if="activeTempIndex === tIndex"
                            class="pipeline-template-corner"
                        >
                            <i class="bk-icon icon-check-1"></i>
                        </span>
                        <p class="pipeline-template-logo">
                            <img
                                :src="temp.logoUrl"
                                v-if="temp.logoUrl"
                            >
                            <logo
                                size="50"
                                :name="temp.icon || 'placeholder'"
                                v-else
                            ></logo>
                        </p>
                        <div class="pipeline-template-detail">
                            <p
                                class="pipeline-template-title"
                                :title="temp.name"
                            >
                                <span>{{ temp.name }}</span>
                                <logo
                                    v-if="temp.isStore"
                                    class="is-store-template"
                                    name="is-store"
                                    size="22"
                                />
                            </p>
                            <p
                                class="pipeline-template-desc"
                                :title="temp.desc"
                            >
                                {{ temp.desc || '--' }}
                            </p>
                        </div>
                        <div
                            v-if="tIndex > 0 || activePanel === 'store'"
                            class="pipeline-template-status"
                        >
                            <bk-button
                                v-if="temp.hasPermission"
                                text
                                size="small"
                                theme="primary"
                                v-perm="{
                                    hasPermission: temp.installed || temp.hasPermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: $route.params.projectId,
                                        resourceType: 'pipeline_template',
                                        resourceCode: $route.params.projectId,
                                        action: TEMPLATE_RESOURCE_ACTION.CREATE
                                    }
                                }"
                                @click.stop="handleTemp(temp, tIndex)"
                            >
                                {{ $t(temp.btnText) }}
                            </bk-button>
                            <span v-else>
                                {{ $t('newlist.noInstallPerm') }}
                            </span>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
        <pipeline-template-preview
            v-model="isShowPreview"
            :template-pipeline="activeTemp"
            :preview-setting-type="previewSettingType"
        />
    </div>
</template>

<script>
    import AlertTips from '@/components/AlertTips.vue'
    import Logo from '@/components/Logo'
    import PipelineLabelSelector from '@/components/PipelineLabelSelector/'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    import pipelineHeader from '@/components/devops/pipeline-header'
    import SyntaxStyleConfiguration from '@/components/syntaxStyleConfiguration'
    import { TEMPLATE_RESOURCE_ACTION } from '@/utils/permission'
    import { templateTypeEnum } from '@/utils/pipelineConst'
    import { getCacheViewId } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'

    export default {
        components: {
            pipelineHeader,
            PipelineTemplatePreview,
            Logo,
            SyntaxStyleConfiguration,
            AlertTips,
            PipelineLabelSelector
        },
        data () {
            return {
                TEMPLATE_RESOURCE_ACTION,
                activePanel: 'projected',
                isDisabled: false,
                activeTempIndex: 0,
                applySettings: [],
                inheritedDialect: true,
                pipelineDialect: 'CLASSIC',
                isLoading: false,
                newPipelineName: '',
                searchName: '',
                templateType: templateTypeEnum.FREEDOM,
                loadEnd: false,
                isLoadingMore: false,
                storeTemplate: [],
                storeTemplateNum: 0,
                page: 1,
                pageSize: 50,
                isShowPreview: false,
                previewSettingType: '',
                labelValues: []
            }
        },
        computed: {
            ...mapState('common', [
                'pipelineTemplateMap'
            ]),
            ...mapState('pipelines', [
                'isManage',
                'templateSetting'
            ]),
            curProject () {
                return this.$store.state.curProject
            },
            enablePipelineNameTips () {
                return this.curProject?.properties?.enablePipelineNameTips ?? false
            },
            pipelineNameFormat () {
                return this.curProject?.properties?.pipelineNameFormat ?? ''
            },
            defaultPipelineDialect () {
                return this.curProject?.properties?.pipelineDialect
            },
            pipelineListRoute () {
                return {
                    name: 'PipelineManageList',
                    params: {
                        viewId: this.viewId,
                        ...this.$route.params
                    }
                }
            },
            tplTypes () {
                const types = [{
                    label: this.$t('newlist.freedomMode'),
                    value: templateTypeEnum.FREEDOM,
                    tip: this.$t('newlist.freedomModeTips')
                }]
                const currentType = this.activeTemp.templateType || ''
                if (currentType !== templateTypeEnum.PUBLIC) {
                    types.push({
                        label: this.$t('newlist.constraintMode'),
                        value: templateTypeEnum.CONSTRAIN,
                        tip: this.$t('newlist.constraintModeTips')
                    })
                } else {
                    this.templateType = templateTypeEnum.FREEDOM
                }
                return types
            },
            viewId () {
                return getCacheViewId(this.$route.params.projectId)
            },
            panels () {
                return [
                    { name: 'projected', label: this.$t('storeMap.projectCustom') },
                    { name: 'store', label: this.$t('store') }
                ]
            },
            settingItems () {
                const cloneTemplateSettingExist = this.activeTemp?.cloneTemplateSettingExist
                return [
                    {
                        label: this.$t('template.notificationSetting'),
                        value: 'useSubscriptionSettings',
                        disabled: !cloneTemplateSettingExist?.notifySettingExist
                    },
                    {
                        label: this.$t('template.parallelSetting'),
                        value: 'useConcurrencyGroup',
                        disabled: !cloneTemplateSettingExist?.concurrencySettingExist
                    },
                    {
                        label: this.$t('template.labelSetting'),
                        value: 'useLabelSettings',
                        disabled: !cloneTemplateSettingExist?.labelSettingExist
                    }
                ]
            },
            activeTemp () {
                return this.tempList[this.activeTempIndex] ?? null
            },
            isActiveTempEmpty () {
                return !this.activeTemp
            },
            isConfirmDisable () {
                return this.isDisabled || !this.activeTemp?.installed || this.isActiveTempEmpty
            },
            projectedTemplateList () {
                if (!this.pipelineTemplateMap) return []
                return Object.values(this.pipelineTemplateMap).map(item => ({
                    ...item,
                    installed: true,
                    hasPermission: true,
                    btnText: 'pipelinesPreview'
                }))
            },
            tempList () {
                if (this.activePanel === 'projected') {
                    return this.projectedTemplateList.filter(item => item.name.toLowerCase().indexOf(this.searchName.toLowerCase()) > -1)
                } else {
                    return this.storeTemplate?.map(item => {
                        const temp = this.pipelineTemplateMap?.[item.code]
                        return {
                            ...item,
                            hasPermission: item.flag,
                            stages: temp?.stages ?? [],
                            templateId: temp?.templateId,
                            version: temp?.version,
                            cloneTemplateSettingExist: temp?.cloneTemplateSettingExist,
                            btnText: item.installed ? 'pipelinesPreview' : 'editPage.install'
                        }
                    }).filter(item => item.name.toLowerCase().indexOf(this.searchName.toLowerCase()) > -1) ?? []
                }
            },
            isConstrainMode () {
                return this.templateType === templateTypeEnum.CONSTRAIN
            }
        },
        watch: {
            settingItems (val) {
                if (val) {
                    this.applySettings = val.reduce((acc, item) => {
                        if (!item.disabled) {
                            acc.push(item.value)
                        }
                        return acc
                    }, [])
                }
            },
            searchName (val) {
                if (this.activePanel === 'store') {
                    this.requestMarkTemplates(true)
                }
            },
            defaultPipelineDialect (val) {
                this.pipelineDialect = val
            }
        },
        async created () {
            await this.$store.dispatch('requestProjectDetail', {
                projectId: this.$route.params.projectId
            })
            this.requestPipelineTemplate({
                projectId: this.$route.params.projectId
            })
        },
        mounted () {
            this.$nextTick(() => {
                this.$refs.pipelineName.focus()
            })
        },
        methods: {
            ...mapActions('common', [
                'requestPipelineTemplate',
                'requestStoreTemplate'
            ]),
            ...mapActions('pipelines', [
                'installPipelineTemplate',
                'createPipelineWithTemplate',
                'requestTemplateSetting'
            ]),
            goList () {
                this.$router.push(this.pipelineListRoute)
            },
            handleTemplateTypeChange (panel) {
                if (panel === 'store') {
                    this.requestMarkTemplates()
                }
            },
            scrollLoadMore (event) {
                if (this.activePanel === 'store') {
                    const target = event.target
                    const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                    if (bottomDis <= 500 && !this.loadEnd && !this.isLoadingMore) this.requestMarkTemplates()
                }
            },
            search () {
                this.selectTemp(0)
                this.requestMarkTemplates(true)
            },
            requestMarkTemplates (isReload) {
                this.isLoadingMore = true
                if (isReload) {
                    this.page = 1
                    this.storeTemplate = []
                }
                const param = {
                    page: this.page,
                    pageSize: this.pageSize,
                    projectCode: this.$route.params.projectId,
                    keyword: this.searchName
                }
                this.requestStoreTemplate(param).then((res) => {
                    this.page++
                    const data = res.data || {}
                    this.storeTemplateNum = data.count || 0
                    this.storeTemplate.push(...data.records)
                    this.loadEnd = data.count <= this.storeTemplate.length
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                    this.isLoadingMore = false
                })
            },
            async install (temp, index) {
                try {
                    const { projectId } = this.$route.params
                    const postData = {
                        projectCodeList: [projectId],
                        templateCode: temp.code
                    }
                    this.isLoading = true
                    await this.installPipelineTemplate(postData)
                    await this.requestPipelineTemplate({
                        projectId
                    })
                    this.storeTemplate = this.storeTemplate.map(x => {
                        if (x.code === temp.code) {
                            return {
                                ...x,
                                installed: true
                            }
                        }
                        return x
                    })
                    this.selectTemp(index)
                } catch (err) {
                    this.$showTips({ message: err.message || err, theme: 'error' })
                } finally {
                    this.isLoading = false
                }
            },
            async selectTemp (index) {
                const target = this.tempList.length && this.tempList[index]
                if (target?.templateType !== 'PUBLIC') {
                    await this.requestTemplateSetting({
                        projectId: this.$route.params.projectId,
                        templateId: target.templateId
                    })
                    this.labelValues = this.templateSetting.labels
                } else {
                    this.labelValues = []
                }
                if (index !== this.activeTempIndex && target.installed) {
                    this.activeTempIndex = index
                }
            },
            handleTemp (temp, index) {
                if (temp.installed) {
                    this.previewTemp(temp, index)
                } else {
                    this.install(temp, index)
                }
            },
            previewTemp (temp, index) {
                this.isShowPreview = true
                this.activeTempIndex = index
                this.previewSettingType = ''
            },
            previewSetting (setting) {
                this.isShowPreview = true
                this.previewSettingType = setting
            },
            async createNewPipeline () {
                try {
                    if (!this.newPipelineName) {
                        this.$showTips({ message: this.$t('pipelineNameTips'), theme: 'error' })
                        return false
                    }

                    if (this.isActiveTempEmpty) {
                        this.$showTips({ message: this.$t('newlist.noTemplateTips'), theme: 'error' })
                        return
                    }

                    const params = {
                        emptyTemplate: !this.activeTempIndex, // 0 为空模板
                        projectId: this.$route.params.projectId,
                        templateId: this.activeTemp.templateId,
                        templateVersion: this.activeTemp.version,
                        pipelineName: this.newPipelineName,
                        ...this.applySettings.reduce((result, item) => {
                            result[item] = true
                            return result
                        }, {}),
                        instanceType: this.templateType,
                        inheritedDialect: this.inheritedDialect,
                        pipelineDialect: this.pipelineDialect,
                        labels: this.labelValues
                    }

                    if (this.templateType === templateTypeEnum.CONSTRAIN) {
                        this.$router.push({
                            name: 'createInstance',
                            params: {
                                templateId: this.activeTemp.templateId,
                                curVersionId: this.activeTemp.version,
                                pipelineName: this.newPipelineName

                            },
                            query: {
                                useTemplateSettings: true
                            }
                        })
                        return
                    }

                    this.isDisabled = true
                    const { pipelineId, version } = await this.createPipelineWithTemplate(params)
                    if (pipelineId) {
                        this.$showTips({ message: this.$t('createPipelineSuc'), theme: 'success' })

                        this.$router.push({
                            name: 'pipelinesEdit',
                            params: {
                                ...this.$route.params,
                                version,
                                pipelineId
                            }
                        })
                    } else {
                        this.$showTips({
                            message: this.$t('addFail'),
                            theme: 'error'
                        })
                    }
                } catch (e) {
                    this.handleError(e, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.projectId,
                        action: this.$permissionResourceAction.CREATE
                    })
                } finally {
                    this.isDisabled = false
                }
            },
            inheritedChange (value) {
                this.inheritedDialect = value
                if (value) {
                    this.pipelineDialect = this.defaultPipelineDialect
                }
            },
            pipelineDialectChange (value) {
                this.pipelineDialect = value
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
    @import "@/scss/mixins/ellipsis";
    .create-pipeline-page-wrapper {
        height: 100%;
        display: flex;
        flex-direction: column;
        background: #F5F7FA;
    }
    .pipeline-template-box {
        display: flex;
        flex: 1;
        margin: 24px 24px 0 24px;
        grid-gap: 16px;
        overflow: hidden;
        &-left-side {
            width: 388px;
            box-shadow: 0 2px 2px 0 #00000026;
            flex-shrink: 0;
            padding: 24px;
            background: white;
            overflow: auto;

            .namingConvention {
                position: relative;
            }

            .pipeline-input {
                position: relative;
                margin-bottom: 16px;
                font-size: 12px;
                color: #333C48;
                input,
                input:focus {
                    border-color: transparent !important;
                    border-bottom-color: $borderWeightColor !important;
                }
                input:focus + .border-effect {
                    transform: scaleX(1) translateY(1px);
                    opacity: 1;
                }
                input:blur + .border-effect {
                    transform: scaleX(1) translateY(1px);
                    opacity: 1;
                }
                .border-effect {
                    position: absolute;
                    bottom: 0;
                    content: '';
                    height:2px;
                    width: 100%;
                    background: $primaryColor;
                    display: block;
                    transform: scaleX(0) translateY(1px);
                    transform-origin: 50%;
                    opacity: 0;
                    transition: all .2s ease-in-out;
                }
                .validate-fail-border-effect {
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    content: '';
                    height:2px;
                    width: 100%;
                    background: $failColor;
                }
            }
            .pipelinte-template-type-group {
                display: grid;
                grid-gap: 8px;
                height: 32px;
                grid-auto-flow: column;
                align-items: center;
                grid-template-columns: max-content;
            }
            .template-setting-apply-checkbox {
                border: 1px solid #3A84FF;
                border-radius: 2px;
                height: 40px;
                display: flex;
                align-items: center;
                padding: 0 20px;
                margin-bottom: 10px;
                .bk-form-checkbox {
                    display: flex;
                    align-items: center;
                    flex: 1;
                    .bk-checkbox-text {
                        flex: 1;
                    }
                }
                &.disabled-setting {
                    border: 1px solid #DCDEE5;
                    opacity: 0.5;
                    color: #C4C6CC;
                    em {
                        font-size: 12px;
                        font-style: normal;
                    }
                }
                .template-apply-setting-checkbox-txt {
                    width: 100%;
                    display: grid;
                    grid-gap: 10px;
                    grid-template-columns: 1fr max-content;
                    grid-auto-flow: column;
                    align-items: center;
                    &-label {
                        @include ellipsis();
                    }
                }
            }
            .pipeline-template-box-right-side-footer {
                margin-top: 20px;
            }
        }
        &-right-side {
            flex: 1;
            display: flex;
            flex-direction: column;
            box-shadow: 0 2px 2px 0 #00000026;
            overflow: hidden;
            background: white;
            .bk-tab.bk-tab-unborder-card .bk-tab-section {
                padding: 0;
            }
            .pipeline-template-searchbox {
                width: 600px;
                margin-right: 24px;
            }
            .create-pipeline-template-list {
                display: grid;
                grid-gap: 16px;
                margin: 16px 24px;
                grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
                grid-auto-rows: 72px;
                overflow: auto;
                flex: 1;
                > li {
                    position: relative;
                    padding: 12px 16px;
                    border: 1px solid #DCDEE5;
                    border-radius: 2px;
                    display: grid;
                    grid-auto-flow: column;
                    grid-template-columns: 50px 1fr;
                    grid-gap: 12px;
                    cursor: pointer;
                    transition: all .3s ease;
                    box-shadow: 0 2px 2px 0 rgba(0,0,0,0.16), 0 0 0 1px rgba(0,0,0,0.08);
                    overflow: hidden;

                    &:hover {
                        box-shadow: 0 3px 8px 0 rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.08);
                        .pipeline-template-status {
                            display: block;
                        }
                    }
                    &.disabled {
                        cursor: not-allowed;
                        opacity: .5;
                    }
                    &.active {
                        border: 1px solid #3A84FF;
                        box-shadow: 0;
                        .pipeline-template-corner {
                            position: absolute;
                            border-style: solid;
                            border-color: #3A84FF;
                            border-top-width: 12px;
                            border-bottom-width: 12px;
                            border-right-width: 10px;
                            border-left-width: 10px;
                            border-right-color: transparent;
                            border-bottom-color: transparent;
                            width: 0;
                            height: 0;
                            color: #fff;
                            > i {
                                font-size: 16px;
                                position: absolute;
                                top: -12px;
                                left: -12px;
                            }
                        }
                    }
                    .pipeline-template-logo {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #979BA5;
                        > img {
                            width: 100%;
                            height: 100%;
                        }
                    }
                    .pipeline-template-detail {
                        font-size: 14px;
                        overflow: hidden;
                        .pipeline-template-title {
                            display: flex;
                            align-items: center;
                            grid-gap: 8px;
                            > span {
                                @include ellipsis();
                            }
                            .is-store-template {
                                flex-shrink: 0;
                            }
                        }
                        .pipeline-template-desc {
                            font-size: 12px;
                            color: #979BA5;
                        }
                    }
                    .pipeline-template-status {
                        display: none;
                        font-size: 12px;
                        color: #979BA5;
                        align-self: center;
                    }
                }
            }
        }

    }
</style>
