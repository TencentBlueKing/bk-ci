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
                    新T LAB
                    <i class="bk-icon icon-close" @click="toggleTemplatePopup(false)" />
                </header>
                <div class="template-content" :style="{ height: viewHeight }">
                    <div v-show="!showPreview" v-if="tempList" class="left-temp-list">
                        <h2>{{ $t('newlist.templateList') }}（{{ tempList.length }}）</h2>
                        <ul>
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
                                    v-if="item.templateType.toLowerCase() === 'constraint'"
                                    class="temp-tip"
                                >{{ $t('newlist.store') }}</span>
                                <i v-if="activeTempIndex === index && !item.isInstall" class="bk-icon icon-check-circle-shape"></i>
                                <p class="temp-logo">
                                    <logo size="50" :name="item.icon" v-if="item.icon"></logo>
                                    <img class="temp-img" :src="item.logoUrl" v-else-if="item.logoUrl">
                                    <i class="bk-icon icon-placeholder" v-else></i>
                                </p>
                                <p class="temp-title" :title="item.name">
                                    {{ item.name }}
                                </p>
                                <p class="permission-tips" v-if="item.isInstall && !item.isFlag" :title="item.name">{{ $t('newlist.noInstallPerm') }}</p>
                                <p class="permission-tips" v-if="!item.isInstall" :title="item.name">{{ $t('newlist.installed') }}</p>
                            </li>
                        </ul>
                    </div>
                    <div v-if="showPreview && tempPipeline" class="pipeline-detail-preview">
                        <bk-pipeline class="pipeline-preview" :pipeline="tempPipeline"></bk-pipeline>
                    </div>
                    <div class="right-temp-info">
                        <div class="temp-info-detail">
                            <template v-if="!isActiveTempEmpty">
                                <div class="pipeline-input">
                                    <input type="text" ref="pipelineName" class="bk-form-input" :placeholder="$t('pipelineNameInputTips')" maxlength="40" name="newPipelineName" v-model.trim="newPipelineName" v-validate.initial="'required'" />
                                    <span class="border-effect" v-show="!errors.has('newPipelineName')"></span>
                                    <span v-show="errors.has('newPipelineName')" class="validate-fail-border-effect"></span>
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
                                    <div class="from-group" v-for="(filter, index) in tagGroupList" :key="index">
                                        <label>{{filter.name}}</label>
                                        <bk-select
                                            v-model="filter.labelValue"
                                            multiple="true">
                                            <bk-option v-for="(option, oindex) in filter.labels" :key="oindex" :id="option.id" :name="option.name">
                                            </bk-option>
                                        </bk-select>
                                    </div>
                                    <a class="view-pipeline" v-if="showPreview" @click="togglePreview(false)">{{ $t('newlist.closePreview') }}</a>
                                    <a class="view-pipeline" v-if="!showPreview && !activeTemp.isInstall && !isActiveTempEmpty" @click="togglePreview(true)">{{ $t('newlist.tempDetail') }}</a>
                                    <a class="view-pipeline disabled" v-if="!showPreview && (activeTemp.isInstall || isActiveTempEmpty)">{{ $t('newlist.tempDetail') }}</a>
                                </div></template>

                            <section v-else class="choose-tips">
                                <logo size="20" name="finger-left" style="fill:#3c96ff" />
                                <span>{{ $t('newlist.tempDetail') }}</span>
                            </section>
                        </div>
                        <div class="temp-operation-bar">
                            <bk-button theme="primary" :disabled="isConfirmDisable" size="small" @click="createNewPipeline">{{ $t('add') }}</bk-button>
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
    import Logo from '@/components/Logo'

    export default {
        name: 'pipeline-template-popup',

        components: {
            Logo
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
                showPreview: false,
                isLoading: !this.pipelineTemplate,
                headerHeight: 50,
                viewHeight: 0,
                newPipelineName: '',
                searchName: '',
                templateType: 'CONSTRAINT',
                curCategory: ''
            }
        },

        computed: {
            ...mapState('common', [
                'pipelineTemplate'
            ]),
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),

            tplTypes () {
                return [{ label: this.$t('newlist.constraintMode'), value: 'CONSTRAINT', tip: this.$t('newlist.constraintModeTips') }]
            },
            hasSelectTemp () {
                return this.activeTempIndex !== -1 && this.activeTemp
            },
            tempList () {
                const list = []
                const { pipelineTemplate } = this
                Object.keys(pipelineTemplate || []).forEach(item => {
                    if (!pipelineTemplate[item].category.length && pipelineTemplate[item].name !== '空白流水线') { // custom类型category为空数组
                        list.push({
                            ...pipelineTemplate[item],
                            isInstall: false,
                            isFlag: true
                        })
                    }
                })

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
                return this.isDisabled || !this.newPipelineName || this.activeTemp.isInstall || keys.length <= 0
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
                    this.requestPipelineTemplate({
                        projectId: this.projectId
                    })

                    this.computPopupHeight()
                    window.addEventListener('resize', this.computPopupHeight)
                    this.$nextTick(() => {
                        if (this.$refs.pipelineName) this.$refs.pipelineName.focus()
                    })
                } else {
                    window.removeEventListener('resize', this.computPopupHeight)
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

            search () {
                this.selectTemp(0)
            },
            selectTemp (index) {
                const target = this.tempList.length && this.tempList[index]
                if (index !== this.activeTempIndex && !target.isInstall) {
                    this.activeTempIndex = index
                }
            },
            installTemplate (temp) {
                const postData = {
                    projectCodeList: [this.projectId],
                    templateCode: temp.code
                }
                this.isLoading = true
                this.requestInstallTemplate(postData).then((res) => {
                    this.requestPipelineTemplate({
                        projectId: this.projectId
                    })
                }).catch((err) => {
                    this.$showTips({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            toggleTemplatePopup (isShow) {
                this.togglePopup(isShow)
                this.selectTemp(0)
                this.togglePreview(false)
                this.newPipelineName = ''
                this.setPipeline(null)
            },
            togglePreview (showPreview) {
                this.showPreview = showPreview
            },
            async createNewPipeline () {
                const { icon, ...pipeline } = this.activeTemp
                let labels = []
                this.tagGroupList.forEach((item) => {
                    if (item.labelValue) labels = labels.concat(item.labelValue)
                })
                Object.assign(pipeline, { name: this.newPipelineName, labels })

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
                        }
                    })
                    return
                }

                try {
                    this.isDisabled = true
                    const { data: { id } } = await this.$ajax.post(`/process/api/user/pipelines/${this.projectId}`, pipeline)
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
                    if (e.code === 403) { // 没有权限创建
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId: this.$permissionActionMap.create,
                                resourceId: this.$permissionResourceMap.pipeline,
                                instanceId: [],
                                projectId: this.$route.params.projectId
                            }],
                            applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.$route.params.projectId}&service_code=pipeline&role_creator=pipeline`
                        })
                    } else {
                        this.$showTips({
                            message: e.message,
                            theme: 'error'
                        })
                    }
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
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';
    $tempTitleHeight: 36px;
    .create-pipeline-form {
        .bk-dialog-tool {
            display: none
        }
        .bk-dialog-body {
            padding: 0px;
        }
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
            .pipeline-detail-preview,
            .left-temp-list {
                width: 704px;
            }
            .search-row-content {
                display: flex;
                margin-top: 20px;
                margin-left: 35px;
            }
            .search-input-row {
                padding: 0 10px;
                width: 233px;
                height: 36px;
                border: 1px solid #dde4eb;
                background-color: #fff;
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
                > span {
                    width: 98px;
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
            .pipeline-detail-preview{
                padding: 40px;
                overflow: auto;
                .pipeline-preview {
                    pointer-events: none;
                    transform: scale(.75);
                    transform-origin: left top;
                }
            }
            .left-temp-list {
                position: relative;
                display: flex;
                flex-direction: column;
                > h2 {
                    font-size: 12px;
                    margin: 18px 0 10px 0;
                    color: $fontLighterColor;
                    padding: 0 40px;
                }
                > ul {
                    display: flex;
                    flex-wrap: wrap;
                    align-content: flex-start;
                    padding: 0 36px;
                    flex: 1;
                    overflow: auto;
                    .temp-item {
                        position: relative;
                        width: 190px;
                        height: 150px;
                        transition: all .3s ease;
                        box-shadow: 0px 2px 4px 0px rgba(51, 57, 72, 0.05);
                        border: 1px solid $borderWeightColor;
                        border-radius: 2px;
                        margin: 0 25px 25px 0;
                        cursor: pointer;
                        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.16), 0 0 0 1px rgba(0,0,0,0.08);
                        overflow: hidden;
                        &:nth-child(3n) {
                            margin-right: 0;
                        }
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
                background-color: white;
                width: 320px;
                border-left: 1px solid $borderWeightColor;
                padding: 20px 20px 20px 19px;
                display: flex;
                flex-direction: column;
                background-color: #fff;
                .temp-info-detail {
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
                    .template-type {
                        display: inline-block;
                        margin-bottom: 17px;
                        .form-radio {
                            margin-right: 30px;
                        }
                        .radio-lable {
                            border-bottom: 1px dotted $fontColor;
                        }
                    }
                    .pipeline-input {
                        position: relative;
                        margin-bottom: 27px;
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
                .bk-button.bk-button-small {
                    padding: 0 15px;
                }
            }
        }
    }
</style>
