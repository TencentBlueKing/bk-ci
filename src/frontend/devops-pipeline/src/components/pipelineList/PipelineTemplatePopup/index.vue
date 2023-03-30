<template>
    <section>
        <bk-dialog
            v-model="isShow"
            ext-cls="create-pipeline-form"
            :has-header="false"
            :has-footer="false"
            :close-icon="false"
            :show-footer="false"
            :width="1054"
            :position="{ top: '100' }">
            <div v-bkloading="{ isLoading }" class="pipeline-template">
                <header>
                    {{ $t('newlist.addPipeline') }}
                    <i class="devops-icon icon-close" @click="toggleTemplatePopup(false)" />
                </header>
                <div class="temp-type-tab">
                    <span v-for="(type, index) in tempTypeList" :key="type" :class="{ &quot;active&quot;: tempTypeIndex === index }" @click="selectTempType(index)">
                        {{ type.categoryName }}
                        <i class="devops-icon icon-refresh" v-if="type.categoryCode === 'store' && tempTypeIndex === index" @click.stop="refreshStoreTEmp"></i>
                    </span>
                </div>
                <div class="template-content" :style="{ height: viewHeight }">
                    <div :class="{
                        'left-temp-list': true,
                        'left-temp-preivew': showPreview && tempPipeline
                    }">
                        <template v-if="tempList.length && !showPreview">
                            <div class="search-row-content" v-if="(tempTypeIndex === tempTypeList.length - 1)">
                                <div class="search-input-row">
                                    <input class="bk-form-input" type="text" :placeholder="$t('newlist.tempSearchTips')"
                                        name="searchInput"
                                        v-model="searchName"
                                        @keyup.enter="search()">
                                    <i class="devops-icon icon-search" @click="search()"></i>
                                </div>
                                <div class="search-category">
                                    <span v-for="category in categoryList"
                                        :key="category.id"
                                        :class="{ 'active': curCategory === category.categoryCode }"
                                        @click="selectCategory(category.categoryCode)">
                                        <img class="category-icon" :src="category.iconUrl" v-if="category.iconUrl">
                                        {{ category.categoryName }}
                                    </span>
                                </div>
                            </div>
                            <h2 v-if="(tempTypeIndex === tempTypeList.length - 1)">{{ $t('newlist.templateList') }}（{{ storeTemplateNum }}）</h2>
                            <h2 v-else>{{ $t('newlist.templateList') }}（{{ tempList.length }}）</h2>
                            <ul @scroll.passive="scrollLoadMore">
                                <li v-for="(item, index) in tempList"
                                    :class="{
                                        'temp-item': true,
                                        'active': activeTempIndex === index && !item.isInstall,
                                        'hover': item.isInstall,
                                        'permission': !item.isFlag
                                    }"
                                    :key="item.name"
                                    @click="selectTemp(index)"
                                >
                                    <span
                                        v-if="(tempTypeIndex !== tempTypeList.length - 1) && item.templateType.toLowerCase() === 'constraint'"
                                        class="temp-tip"
                                    >{{ $t('newlist.store') }}</span>
                                    <i v-if="activeTempIndex === index && !item.isInstall" class="devops-icon icon-check-circle-shape"></i>
                                    <p class="temp-logo">
                                        <logo size="50" :name="item.icon" v-if="item.icon"></logo>
                                        <img class="temp-img" :src="item.logoUrl" v-else-if="item.logoUrl">
                                        <i class="devops-icon icon-placeholder" v-else></i>
                                    </p>
                                    <p class="temp-title" :title="item.name">
                                        {{ item.name }}
                                    </p>
                                    <p class="install-btn" v-if="item.isInstall && item.isFlag " @click="installTemplate(item, index)" :title="item.name">{{ $t('editPage.install') }}</p>
                                    <p class="permission-tips" v-if="item.isInstall && !item.isFlag" :title="item.name">{{ $t('newlist.noInstallPerm') }}</p>
                                    <p class="permission-tips" v-if="!item.isInstall" :title="item.name">{{ $t('newlist.installed') }}</p>
                                </li>
                            </ul>
                        </template>
                        <bk-pipeline v-if="showPreview && tempPipeline" class="pipeline-preview" :pipeline="tempPipeline"></bk-pipeline>
                    </div>
                    <div class="right-temp-info">
                        <div class="temp-info-detail">
                            <template v-show="!isActiveTempEmpty">
                                <label class="info-label">{{ $t('pipelineName') }}：</label>
                                <div class="pipeline-input">
                                    <input type="text" ref="pipelineName" class="bk-form-input" :placeholder="$t('pipelineNameInputTips')" maxlength="40" name="newPipelineName" v-model.trim="newPipelineName" v-validate.initial="&quot;required&quot;" />
                                    <span class="border-effect" v-show="!errors.has(&quot;newPipelineName&quot;)"></span>
                                    <span v-show="errors.has(&quot;newPipelineName&quot;)" class="validate-fail-border-effect"></span>
                                </div>
                                <div class="detail-form-item">
                                    <label class="info-label">{{ $t('type') }}：</label>
                                    <div class="info-value template-type">
                                        <bk-radio-group v-model="templateType">
                                            <bk-popover placement="bottom" v-for="(entry, key) in tplTypes" :key="key">
                                                <bk-radio :value="entry.value" class="form-radio"><span class="radio-lable">{{ entry.label }}</span></bk-radio>
                                                <div slot="content" style="white-space: normal;">{{entry.tip}}</div>
                                            </bk-popover>
                                        </bk-radio-group>
                                    </div>
                                    <section class="new-pipeline-group-selector" v-if="activeTemp.templateType === 'PUBLIC'">
                                        <PipelineGroupSelector
                                            v-model="groupValue"
                                            :pipeline-name="newPipelineName"
                                            ref="pipelineGroupSelector"
                                            :has-manage-permission="isManage"
                                        />
                                    </section>
                                    <div v-else style="margin-bottom: 15px">
                                        <label class="bk-form-checkbox template-setting-checkbox">
                                            <bk-checkbox
                                                v-model="useTemplateSettings">
                                                {{ $t('template.applyTemplateSetting') }}
                                            </bk-checkbox>
                                            <bk-popover placement="top">
                                                <i class="bk-icon icon-info-circle"></i>
                                                <div slot="content" style="white-space: pre-wrap; min-width: 200px">
                                                    <div>{{ $t('template.applySettingTips') }}</div>
                                                </div>
                                            </bk-popover>
                                        </label>
                                    </div>
                                    <a class="view-pipeline" v-if="showPreview" @click="togglePreview(false)">{{ $t('newlist.closePreview') }}</a>
                                    <a class="view-pipeline" v-if="!showPreview && !activeTemp.isInstall && !isActiveTempEmpty" @click="togglePreview(true)">{{ $t('newlist.tempDetail') }}</a>
                                    <a class="view-pipeline disabled" v-if="!showPreview && (activeTemp.isInstall || isActiveTempEmpty)">{{ $t('newlist.tempDetail') }}</a>
                                </div></template>

                            <section v-show="isActiveTempEmpty" class="choose-tips">
                                <logo size="20" name="finger-left" style="fill:#3c96ff" />
                                <span>{{ $t('newlist.tempDetail') }}</span>
                            </section>
                        </div>
                        <div class="temp-operation-bar">
                            <bk-button theme="primary" size="small" :disabled="isConfirmDisable" @click="createNewPipeline">{{ $t('add') }}</bk-button>
                            <bk-button size="small" @click="toggleTemplatePopup(false)">{{ $t('cancel') }}</bk-button>
                        </div>
                    </div>
                </div>
            </div>
        </bk-dialog>
    </section>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import PipelineGroupSelector from '@/components/PipelineActionDialog/PipelineGroupSelector'
    import Logo from '@/components/Logo'

    export default {
        name: 'pipeline-template-popup',

        components: {
            Logo,
            PipelineGroupSelector
        },

        props: {
            togglePopup: {
                type: Function,
                required: true
            },
            isShow: {
                type: Boolean,
                required: true,
                default: false
            }
        },

        data () {
            return {
                isDisabled: false,
                activeTempIndex: -1,
                tempTypeIndex: 0,
                showPreview: false,
                useTemplateSettings: false,
                isLoading: !this.pipelineTemplate,
                headerHeight: 50,
                viewHeight: 0,
                newPipelineName: '',
                searchName: '',
                templateType: 'FREEDOM',
                curCategory: '',
                loadEnd: false,
                isLoadingMore: false,
                storeTemplate: [],
                storeTemplateNum: 0,
                page: 1,
                pageSize: 50,
                groupValue: {
                    labels: [],
                    staticViews: []
                }
            }
        },

        computed: {
            ...mapState('common', [
                'pipelineTemplate',
                'templateCategory'
            ]),
            ...mapState('pipelines', [
                'isManage'
            ]),
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),

            tplTypes () {
                const types = [{ label: this.$t('newlist.freedomMode'), value: 'FREEDOM', tip: this.$t('newlist.freedomModeTips') }]
                const currentType = this.activeTemp.templateType || ''
                if (currentType.toLowerCase() !== 'public') types.push({ label: this.$t('newlist.constraintMode'), value: 'CONSTRAINT', tip: this.$t('newlist.constraintModeTips') })
                else this.templateType = 'FREEDOM'
                return types
            },

            tempTypeList () {
                return this.templateCategory || []
            },
            categoryList () {
                const list = this.templateCategory || []
                return list.filter(item => !['custom', 'store'].includes(item.categoryCode))
            },
            hasSelectTemp () {
                return this.activeTempIndex !== -1 && this.activeTemp
            },
            tempList () {
                let list = []
                const { tempTypeIndex, pipelineTemplate, tempTypeList } = this
                const type = tempTypeList[tempTypeIndex] && tempTypeList[tempTypeIndex].categoryCode
                if (type === 'store') {
                    list = (this.storeTemplate || []).map(item => {
                        return {
                            ...item,
                            isInstall: !item.installed,
                            isFlag: item.flag,
                            stages: (pipelineTemplate[item.code] && pipelineTemplate[item.code].stages) || [],
                            templateId: (pipelineTemplate[item.code] && pipelineTemplate[item.code].templateId) || undefined,
                            version: (pipelineTemplate[item.code] && pipelineTemplate[item.code].version) || undefined
                        }
                    })
                } else {
                    Object.keys(pipelineTemplate || {}).forEach(item => {
                        const curItem = pipelineTemplate[item] || {}
                        if ((type === 'custom' && ['PUBLIC', 'CUSTOMIZE'].includes(curItem.templateType)) || curItem.category.includes(type)) {
                            list.push({
                                ...curItem,
                                isInstall: false,
                                isFlag: true
                            })
                        }
                    })
                }

                return list
            },
            activeTemp () {
                return this.tempList[this.activeTempIndex] || {}
            },
            tempPipeline () {
                return this.hasSelectTemp ? this.activeTemp : null
            },
            projectId () {
                return this.$route.params.projectId
            },
            isConfirmDisable () {
                const keys = Object.keys(this.activeTemp)
                return this.isDisabled || this.activeTemp.isInstall || keys.length <= 0
            },
            isActiveTempEmpty () {
                const keys = Object.keys(this.activeTemp)
                return keys.length <= 0
            }
        },

        watch: {
            pipelineTemplate: function (newVal, oldVal) {
                if (newVal) {
                    this.isLoading = false
                    this.selectTemp(0)
                }
            },
            isShow: function () {
                if (this.isShow) {
                    this.isLoading = true
                    this.requestCategory()
                    this.requestPipelineTemplate({
                        projectId: this.projectId
                    })
                    this.computPopupHeight()
                    window.addEventListener('resize', this.computPopupHeight)
                    this.timer = setTimeout(() => {
                        if (this.$refs.pipelineName) this.$refs.pipelineName.focus()
                    }, 0)
                } else {
                    clearTimeout(this.timer)
                    window.removeEventListener('resize', this.computPopupHeight)
                    this.reset()
                }
            }
        },

        methods: {
            ...mapActions('common', [
                'requestCategory',
                'requestPipelineTemplate',
                'requestStoreTemplate'
            ]),
            ...mapActions('atom', [
                'setPipeline'
            ]),
            ...mapActions('pipelines', [
                'requestInstallTemplate'
            ]),

            scrollLoadMore (event) {
                if (this.tempTypeIndex !== this.tempTypeList.length - 1) return
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 500 && !this.loadEnd && !this.isLoadingMore) this.requestMarkTemplates()
            },

            search () {
                this.selectTemp(0)
                this.requestMarkTemplates(true)
            },
            selectTemp (index) {
                const target = this.tempList.length && this.tempList[index]
                if (index !== this.activeTempIndex && !target.isInstall) {
                    this.activeTempIndex = index
                }
            },
            installTemplate (temp, index) {
                const postData = {
                    projectCodeList: [this.projectId],
                    templateCode: temp.code
                }
                this.isLoading = true
                this.requestInstallTemplate(postData).then((res) => {
                    return this.requestPipelineTemplate({
                        projectId: this.projectId
                    }).then(() => {
                        const currentStoreItem = this.storeTemplate.find(x => x.code === temp.code)
                        currentStoreItem.installed = true
                        this.selectTemp(index)
                    })
                }).catch((err) => {
                    this.$showTips({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            selectTempType (index) {
                if (index === this.tempTypeList.length - 1 && this.storeTemplate.length <= 0) {
                    this.requestMarkTemplates()
                }
                setTimeout(() => {
                    this.selectTemp(0)
                }, 100)
                this.tempTypeIndex = index
                this.togglePreview(false)
            },
            refreshStoreTEmp () {
                this.selectTemp(0)
                this.curCategory = ''
                this.searchName = ''
                this.requestPipelineTemplate({
                    projectId: this.projectId
                })
                this.requestMarkTemplates(true)
            },
            selectCategory (category) {
                if (this.curCategory === category) {
                    this.curCategory = ''
                } else {
                    this.curCategory = category
                }
                this.requestMarkTemplates(true)
            },
            toggleTemplatePopup (isShow) {
                this.togglePopup(isShow)
            },
            togglePreview (showPreview) {
                this.showPreview = showPreview
            },
            reset () {
                this.selectTemp(0)
                this.togglePreview(false)
                this.newPipelineName = ''
                this.groupValue = {
                    labels: [],
                    staticViews: []
                }
                this.$refs.pipelineGroupSelector?.reset?.()
                this.setPipeline(null)
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
                    keyword: this.searchName,
                    categoryCode: this.curCategory,
                    projectCode: this.projectId
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
            async createNewPipeline () {
                if (!this.newPipelineName.length) {
                    this.$showTips({ message: this.$t('pipelineNameTips'), theme: 'error' })
                    return false
                }

                const { icon, ...pipeline } = this.activeTemp
                Object.assign(pipeline, { name: this.newPipelineName })

                if (this.activeTemp.templateType === 'PUBLIC') {
                    Object.assign(pipeline, this.groupValue)
                }

                const keys = Object.keys(this.activeTemp)
                if (keys.length <= 0) {
                    this.$showTips({ message: this.$t('newlist.noTemplateTips'), theme: 'error' })
                    return
                }

                if (this.templateType === 'CONSTRAINT') {
                    const code = this.activeTemp.code || ''
                    const currentTemplate = this.pipelineTemplate[code] || this.activeTemp

                    this.$router.push({
                        name: 'createInstance',
                        params: {
                            templateId: currentTemplate.templateId,
                            curVersionId: currentTemplate.version,
                            pipelineName: pipeline.name
                        },
                        query: {
                            useTemplateSettings: this.useTemplateSettings.toString()
                        }
                    })
                    return
                }

                try {
                    this.isDisabled = true
                    const queryStr = this.activeTemp.templateType !== 'PUBLIC' ? `?useTemplateSettings=${this.useTemplateSettings}` : ''
                    const { data: { id } } = await this.$ajax.post(`/process/api/user/pipelines/${this.projectId}${queryStr}`, pipeline)
                    if (id) {
                        this.$showTips({ message: this.$t('addSuc'), theme: 'success' })

                        this.$router.push({
                            name: 'pipelinesEdit',
                            params: {
                                pipelineId: id
                            }
                        })
                    } else {
                        this.$showTips({
                            message: this.$t('addFail'),
                            theme: 'error'
                        })
                    }
                } catch (e) {
                    this.handleError(e, [{
                        actionId: this.$permissionActionMap.create,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [],
                        projectId: this.$route.params.projectId
                    }])
                } finally {
                    this.isDisabled = false
                }
            },
            computPopupHeight () {
                this.viewHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - this.headerHeight - 120 + 'px'
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf';
    @import '../../../scss/mixins/ellipsis';
    $tempTitleHeight: 36px;
    .create-pipeline-form {
        .bk-dialog-tool {
            display: none
        }
        .bk-dialog-body {
            padding: 0px;
        }
    }
    .new-pipeline-group-selector {
        margin-bottom: 27px;
    }
    .pipeline-template {
        display: flex;
        flex-direction: column;
        background-color: $bgHoverColor;
        > header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            height: 51px;
            padding:0 24px;
            font-size: 12px;
            line-height: 51px;
            background-color: #fff;
            .icon-close {
                cursor: pointer;
                z-index: 22;
            }
        }
        .temp-type-tab {
            position: absolute;
            display: flex;
            width: 100%;
            height: 51px;
            align-items: center;
            justify-content: center;
            border-bottom: 1px solid $borderWeightColor;
            box-shadow: 0px 2px 4px 0px rgba(51, 57, 72, 0.05);
            border-radius: 2px 2px 0px 0px;
            > span {
                position: relative;
                font-size: 14px;
                font-weight: bold;
                margin-right: 41px;
                line-height: 48px;
                border-bottom: 3px solid transparent;
                cursor: pointer;
                &.active {
                    border-bottom: 3px solid $primaryColor;
                    color: $primaryColor;
                }
            }
            .icon-refresh {
                position: absolute;
                top: 15px;
                padding-left: 10px;
                font-size: 18px;
            }
        }
        .template-content {
            flex: 1;
            display: flex;
            line-height: 1;
            transition: all .3s ease;
            max-height: 530px;
            min-height: 530px;
            .search-row-content {
                display: flex;
                margin: 0 20px 20px 0;
            }
            .search-input-row {
                padding: 0 10px;
                width: 233px;
                height: 36px;
                border: 1px solid #dde4eb;
                background-color: #fff;
                flex-shrink: 0;
                .bk-form-input {
                    padding: 0;
                    border: 0;
                    -webkit-box-shadow: border-box;
                    box-shadow: border-box;
                    outline: none;
                    width: 190px;
                    height: 32px;
                    margin-left: 0;
                }
                .icon-search {
                    float: right;
                    margin-top: 12px;
                    color: #c3cdd7;
                    cursor: pointer;
                }
            }
            .search-category {
                margin-left: 10px;
                display: flex;
                flex: 1;
                > span {
                    flex: 1;
                    display: inline-block;
                    height: 36px;
                    line-height: 36px;
                    border: 1px solid #DDE4EB;
                    text-align: center;
                    border-right: none;
                    color: #4A4A4A;
                    cursor: pointer;
                    &:last-child {
                        border-right: 1px solid #DDE4EB;
                    }
                    &.active {
                        border: 1px solid $primaryColor;
                        color: $primaryColor;
                        font-weight: bold;
                        > svg {
                            fill: $primaryColor;
                        }
                    }
                }
                .category-icon {
                    position: relative;
                    top: 2px;
                    width: 16px;
                    height: 16px;
                }
            }
            .pipeline-preview {
                pointer-events: none;
                transform: scale(.75);
                transform-origin: left top;
            }
            .left-temp-list {
                position: relative;
                display: flex;
                flex-direction: column;
                width: 666px;
                padding: 20px 0 20px 20px;
                &.left-temp-preivew {
                    overflow: auto;
                }
                > h2 {
                    font-size: 12px;
                    margin: 0 20px 18px 0;
                    color: $fontLighterColor;
                }
                > ul {
                    display: grid;
                    grid-gap: 20px;
                    grid-template-columns: repeat(3, 190px);
                    overflow: auto;
                    padding-right: 20px;
                    .temp-item {
                        position: relative;
                        height: 150px;
                        transition: all .3s ease;
                        border: 1px solid $borderWeightColor;
                        border-radius: 2px;
                        cursor: pointer;
                        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.16), 0 0 0 1px rgba(0,0,0,0.08);
                        overflow: hidden;
                        .temp-tip {
                            position: absolute;
                            top: -33px;
                            left: -33px;
                            height: 66px;
                            width: 66px;
                            background: $primaryColor;
                            color: #fff;
                            transform: rotate(-45deg);
                            text-align: center;
                            line-height: 106px;
                            font-size: 12px;
                        }
                        .temp-logo {
                            display: flex;
                            height: calc(100% - #{$tempTitleHeight});
                            align-items: center;
                            justify-content: center;
                            border: 1px solid transparent;
                            border-bottom-color: $borderWeightColor;
                            .icon-placeholder {
                                font-size: 50px;
                                color: #63656E;
                            }
                        }
                        .temp-img {
                            width: 50px;
                            height: 50px;
                        }
                        .temp-title,
                        .install-btn,
                        .permission-tips {
                            width: 100%;
                            margin: 0;
                            height: $tempTitleHeight;
                            line-height: $tempTitleHeight;
                            font-size: 14px;
                            color: $fontWeightColor;
                            padding: 0 22px;
                            @include ellipsis();
                        }
                        .install-btn,
                        .permission-tips {
                            display: none;
                            position: absolute;
                            top: 112px;
                            outline: 2px solid $primaryColor;
                            background-color: $primaryColor;
                            color: #FFF;
                        }
                        .permission-tips {
                            color: #cccccc;
                            background-color: #fafafa;
                            outline: 1px solid #e6e6e6;
                        }
                        &:hover {
                            box-shadow: 0 3px 8px 0 rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.08);
                        }

                        &.active {
                            position: relative;
                            .icon-check-circle-shape {
                                position: absolute;
                                color: $iconPrimaryColor;
                                top: 5px;
                                right: 7px;
                                font-size: 16px;
                            }
                            border: 1px solid $iconPrimaryColor;
                            .temp-logo {
                                border-color: $iconPrimaryColor;
                            }
                            .temp-title {
                                background-color: $iconPrimaryColor;
                                color: white;
                            }
                        }
                        &.hover:hover {
                            .install-btn {
                                display: block;
                            }
                        }
                        &.permission:hover {
                            cursor: default;
                            .permission-tips {
                                display: block;
                            }
                        }
                    }
                }
            }
            .right-temp-info {
                flex: 1;
                border-left: 1px solid $borderWeightColor;
                padding: 20px 0 20px 20px;
                display: flex;
                flex-direction: column;
                background-color: white;
                overflow: hidden;

                .temp-info-detail {
                    overflow: auto;
                    padding-right: 20px;
                    flex: 1;
                    .choose-tips {
                        padding: 20px 0;
                        display: flex;
                        align-items: center;
                        span {
                            margin-left: 10px;
                            font-size: 16px;
                        }
                    }
                    .info-label {
                        display: inline-block;
                        margin-bottom: 8px;
                    }
                    .template-type {
                        display: inline-block;
                        margin-bottom: 16px;
                        .form-radio {
                            margin-right: 30px;
                        }
                        .radio-lable {
                            border-bottom: 1px dotted $fontColor;
                        }
                    }
                    .pipeline-input {
                        position: relative;
                        margin-bottom: 16px;
                        font-size: 16px;
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
                    .from-group {
                        margin-bottom: 17px;
                        > label {
                            display: inline-block;
                            margin-bottom: 8px;
                            line-height: 19px;
                            font-size: 14px;
                            color: $fontWeightColor;
                        }
                    }
                    .bk-selector-wrapper {
                        .bk-selector-input.placeholder, .bk-selector-icon{
                            color: $fontLighterColor;
                        }
                        .bk-selector-input {
                            color: $fontWeightColor;
                        }
                    }
                    .view-pipeline {
                        display: inline-block;
                        color: $iconPrimaryColor;
                        cursor: pointer;
                        &.disabled {
                            color: #CCCCCC;
                        }
                    }
                }
                .temp-operation-bar {
                    padding-top: 20px;
                }
                .bk-button.bk-button-small {
                    padding: 0 15px;
                }
            }
        }
    }
</style>
