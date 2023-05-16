<template>
    <section @click="toggleAtomSelectorPopup(false)" v-if="element" class="atom-property-panel">
        <div class="atom-main-content" v-bkloading="{ isLoading: fetchingAtmoModal }">
            <form-field v-if="atom && !isTriggerContainer(container)" :desc="$t('editPage.stepIdDesc')" label="Step ID" :is-error="errors.has('stepId')" :error-msg="errors.first('stepId')">
                <vuex-input :value="element.stepId" :clearable="false"
                    :placeholder="$t('editPage.stepIdPlaceholder')"
                    name="stepId"
                    :handle-change="handleUpdateAtom"
                    :disabled="!editable || showPanelType === 'PAUSE'"
                    style="width: 282px;margin-top: 6px;"
                    v-validate.initial="`varRule|unique:${allStepId}`"
                >
                </vuex-input>
            </form-field>
            <div class="atom-type-selector bk-form-row bk-form bk-form-vertical">
                <div :class="{ 'form-field': true, 'bk-form-inline-item': true, 'is-danger': errors.has('@type') }">
                    <label :title="$t('atom')" class="bk-label">
                        {{ $t('atom') }}：
                        <a v-if="atom && atom.docsLink" :href="atom.docsLink" class="atom-link" target="_blank">
                            {{ $t('editPage.atomHelpDoc') }}
                            <logo name="tiaozhuan" size="14" style="fill:#3c96ff;position:relative;top:2px;" />
                        </a>
                    </label>
                    <div class="bk-form-content">
                        <div class="atom-select-entry">
                            <template v-if="atom">
                                <span :title="atom.recommendFlag === false ? $t('editPage.notRecomendPlugin') : atom.name" :class="[{ 'not-recommend': atom.recommendFlag === false }, 'atom-selected-name']">{{ atom.name }}</span>
                                <bk-button theme="primary" class="atom-select-btn reselect-btn" :disabled="!editable || showPanelType === 'PAUSE'" @click.stop="toggleAtomSelectorPopup(true)">{{ $t('editPage.reSelect') }}</bk-button>
                            </template>
                            <template v-else-if="!atomCode">
                                <bk-button theme="primary" class="atom-select-btn" @click.stop="toggleAtomSelectorPopup(true)">{{ $t('editPage.selectAtomTips') }}</bk-button>
                            </template>
                            <template v-else>
                                <bk-button theme="primary" class="atom-select-btn" @click.stop="toggleAtomSelectorPopup(true)">{{ $t('editPage.reSelect') }}</bk-button>
                            </template>
                        </div>
                    </div>
                </div>
                <form-field v-if="hasVersionList" :desc="$t('editPage.atomVersionDesc')" :label="$t('version')">
                    <bk-select :value="element.version" :clearable="false"
                        :placeholder="$t('editPage.selectAtomVersion')"
                        name="version"
                        @selected="handleUpdateVersion"
                        :disabled="!editable || showPanelType === 'PAUSE'"
                    >
                        <bk-option v-for="v in computedAtomVersionList" :key="v.versionName" :id="v.versionValue" :name="v.versionName"></bk-option>
                    </bk-select>
                </form-field>
            </div>
            <div class="atom-form-content">
                <bk-alert class="atom-changed-prop" type="warning" :title="$t('editPage.atomPropChangedTip')" v-if="atomVersionChangedKeys.length"></bk-alert>

                <div class="no-atom-tips" v-if="!atom && atomCode">
                    <div class="no-atom-tips-icon">
                        <i class="bk-icon icon-info-circle-shape" size="14" />
                    </div>
                    <p>{{ $t('editPage.noAtomVersion') }}</p>
                </div>

                <div class="quality-setting-tips" v-if="showSetRuleTips">
                    <div class="quality-setting-desc">
                        {{ $t('details.quality.canSet') }}
                        <span class="quality-rule-link" @click="toSetRule()">{{ $t('details.quality.settingNow') }}
                            <logo name="tiaozhuan" size="14" style="fill:#3c96ff;position:relative;top:2px;" />
                        </span>
                    </div>
                    <div class="refresh-btn" v-if="isSetted && !refreshLoading" @click="refresh()">{{ $t('details.quality.reflashSetting') }}</div>
                    <i class="devops-icon icon-circle-2-1 executing-job" v-if="isSetted && refreshLoading"></i>
                </div>
                <qualitygate-tips v-if="showRuleList" :relative-rule-list="renderRelativeRuleList"></qualitygate-tips>

                <div v-if="atom" :class="{ 'atom-form-box': true, 'readonly': !editable && !isRemoteAtom }">
                    <!-- <div class='desc-tips' v-if="!isNewAtomTemplate(atom.htmlTemplateVersion) && atom.description"> <span>插件描述：</span> {{ atom.description }}</div> -->
                    <div
                        v-if="atom.atomModal"
                        :is="AtomComponent"
                        :atom="atom.atomModal"
                        :element-index="elementIndex"
                        :container-index="containerIndex"
                        :stage-index="stageIndex"
                        :element="element"
                        :container="container"
                        :stage="stage"
                        :atom-props-model="atom.atomModal.props"
                        :set-parent-validate="setAtomValidate"
                        :disabled="!editable"
                        class="atom-content">
                    </div>
                    <div class="atom-option">
                        <atom-option
                            v-if="element['@type'] !== 'manualTrigger'"
                            :element-index="elementIndex"
                            :container-index="containerIndex"
                            :stage-index="stageIndex"
                            :element="element"
                            :container="container"
                            :set-parent-validate="setAtomValidate"
                            :disabled="!editable"
                        >
                        </atom-option>
                    </div>
                </div>
            </div>
            <section class="atom-form-footer" v-if="showPanelType === 'PAUSE'">
                <bk-button @click="changePluginPause(true, 'isExeContinue')" theme="primary" :loading="isExeContinue" :disabled="isExeStop">{{ $t('resume') }}</bk-button>
                <bk-button @click="changePluginPause(false, 'isExeStop')" :loading="isExeStop" :disabled="isExeContinue">{{ $t('stop') }}</bk-button>
            </section>
        </div>
    </section>
</template>

<script>
    import AtomOption from './AtomOption'
    import { mapGetters, mapActions, mapState } from 'vuex'
    import RemoteAtom from './RemoteAtom'
    import QualitygateTips from '@/components/atomFormField/QualitygateTips'
    import BuildScript from './BuildScript'
    import Unity3dBuild from './Unity3dBuild'
    import NormalAtom from './NormalAtom'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from './FormField'
    import BuildArchiveGet from './BuildArchiveGet'
    import { isObject } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import TimerTrigger from './TimerTrigger'
    import CodePullGitX from './CodePullGitX'
    import CodePullSvn from './CodePullSvn'
    import IosCertInstall from './IosCertInstall'
    import CrossDistribute from './CrossDistribute'
    import SendWechatNotify from './SendWechatNotify'
    import CodeSvnWebHookTrigger from './CodeSvnWebHookTrigger'
    import ReportArchive from './ReportArchive'
    import PullGithub from './PullGithub'
    import CodeGithubWebHookTrigger from './CodeGithubWebHookTrigger'
    import PushImageToThirdRepo from './PushImageToThirdRepo'
    import ReferenceVariable from './ReferenceVariable'
    import NormalAtomV2 from './NormalAtomV2'
    import CodeGitWebHookTrigger from './CodeGitWebHookTrigger'
    import CodeGitlabWebHookTrigger from './CodeGitlabWebHookTrigger'
    import SubPipelineCall from './SubPipelineCall'
    import ManualReviewUserTask from './ManualReviewUserTask'
    import Logo from '@/components/Logo'

    export default {
        name: 'atom-content',
        components: {
            RemoteAtom,
            ReferenceVariable,
            AtomOption,
            VuexInput,
            Selector,
            FormField,
            BuildArchiveGet,
            CodePullGitX,
            CodePullSvn,
            IosCertInstall,
            CrossDistribute,
            SendWechatNotify,
            QualitygateTips,
            CodeGithubWebHookTrigger,
            ReportArchive,
            CodeSvnWebHookTrigger,
            PullGithub,
            NormalAtomV2,
            PushImageToThirdRepo,
            CodeGitWebHookTrigger,
            SubPipelineCall,
            ManualReviewUserTask,
            Logo
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
            containerGroupIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            isInstanceTemplate: Boolean
        },
        data () {
            return {
                nameEditing: false,
                isSetted: false,
                isSupportVersion: true,
                curVersionRelativeRules: [],
                ruleDetailMessage: {},
                isExeStop: false,
                isExeContinue: false
            }
        },
        computed: {
            ...mapState('common', [
                'ruleList',
                'qualityAtom',
                'templateRuleList',
                'refreshLoading'
            ]),
            ...mapGetters('atom', [
                'getAtomModal',
                'getAtomModalKey',
                'getDefaultVersion',
                'classifyCodeListByCategory',
                'getElement',
                'getContainer',
                'getContainers',
                'getStage',
                'isTriggerContainer',
                'isNewAtomTemplate',
                'atomVersionChangedKeys'
            ]),
            ...mapState('atom', [
                'globalEnvs',
                'atomCodeList',
                'atomClassifyCodeList',
                'atomMap',
                'atomModalMap',
                'fetchingAtmoModal',
                'atomVersionList',
                'isPropertyPanelVisible',
                'showPanelType',
                'editingElementPos'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            templateId () {
                return this.$route.params.templateId
            },
            isTemplatePanel () {
                return this.$route.path.indexOf('template') > 0
            },
            stage () {
                const { stageIndex, getStage, stages } = this
                return getStage(stages, stageIndex)
            },
            containers () {
                const { stage, getContainers } = this
                return getContainers(stage)
            },
            container () {
                const { containerIndex, containerGroupIndex, containers, getContainer } = this
                return getContainer(containers, containerIndex, containerGroupIndex)
            },
            element () {
                const { container, elementIndex, getElement } = this
                const element = getElement(container, elementIndex)
                return element
            },
            allStepId () {
                const stepIdList = []
                this.container.elements.forEach(ele => {
                    if (ele.stepId) {
                        stepIdList.push(ele.stepId)
                    }
                })
                return stepIdList
            },
            isIncludeRule () {
                return (this.checkAtomIsIncludeRule(this.ruleList) && !this.isTemplatePanel) || (this.isInstanceTemplate && this.checkAtomIsIncludeRule(this.templateRuleList))
            },
            isQualityAtom () {
                return this.qualityAtom.some(item => item.type === this.element.atomCode) && this.isSupportVersion && !this.isInstanceTemplate
            },
            showSetRuleTips () {
                return this.isQualityAtom && !this.isIncludeRule && !this.isIncludeTemplateRule && !this.curVersionRelativeRules.length
            },
            isIncludeTemplateRule () {
                return this.checkAtomIsIncludeRule(this.templateRuleList) && this.isTemplatePanel
            },
            showRuleList () {
                return this.isIncludeRule || this.isIncludeTemplateRule || this.curVersionRelativeRules.length
            },
            renderRelativeRuleList () {
                return this.isSupportVersion && this.curVersionRelativeRules.length ? this.curVersionRelativeRules : this.isTemplatePanel ? this.relativeTemplateRuleList : this.relativeRuleList
            },
            atomCode () {
                if (this.element) {
                    const isThrid = this.element.atomCode && this.element['@type'] !== this.element.atomCode
                    if (isThrid) {
                        return this.element.atomCode
                    } else {
                        return this.element['@type']
                    }
                }
                return ''
            },
            atomVersion () {
                return this.element.version || this.getDefaultVersion(this.atomCode)
            },
            atom () {
                const { atomMap, atomCode, element, getDefaultVersion, getAtomModal } = this
                const atom = atomMap[atomCode]
                const version = element.version || getDefaultVersion(atomCode)
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })
                console.log(atomMap, atomModal)
                switch (true) {
                    case !isObject(atom) && !isObject(atomModal):
                        return null
                    case !isObject(atom) && isObject(atomModal):
                        return {
                            ...atomModal,
                            atomModal
                        }
                    default:
                        return {
                            ...atom,
                            atomModal
                        }
                }
            },
            relativeRuleList () {
                if (!this.isTemplatePanel && this.isInstanceTemplate) {
                    return this.getRelativeRule(this.ruleList.concat(this.templateRuleList))
                } else {
                    return this.getRelativeRule(this.ruleList)
                }
            },
            relativeTemplateRuleList () {
                return this.getRelativeRule(this.templateRuleList)
            },
            hasVersionList () {
                return Array.isArray(this.computedAtomVersionList) && this.computedAtomVersionList.length > 0
            },
            computedAtomVersionList () {
                try {
                    if (typeof this.element.version === 'string') {
                        const versionValid = this.atomVersionList.find(v => v.versionValue === this.element.version)
                        if (typeof versionValid === 'undefined') {
                            return [
                                ...this.atomVersionList,
                                {
                                    versionValue: this.element.version,
                                    versionName: this.element.version.replace('.*', '.latest')
                                }
                            ]
                        }
                    }
                    return this.atomVersionList
                } catch (error) {
                    console.log(error)
                    return this.atomVersionList
                }
            },
            htmlTemplateVersion () {
                return (this.atom.atomModal && this.atom.atomModal.htmlTemplateVersion) || this.atom.htmlTemplateVersion
            },
            isRemoteAtom () {
                return this.htmlTemplateVersion === '1.2' || this.atomCode === 'CodeccCheckAtomDebug' || this.atomCode === 'CodeccCheckAtom'
            },
            AtomComponent () {
                if (this.isRemoteAtom) {
                    return RemoteAtom
                }
                if (this.isNewAtomTemplate(this.htmlTemplateVersion)) {
                    return NormalAtomV2
                }
                const atomMap = {
                    timerTrigger: TimerTrigger,
                    linuxScript: BuildScript,
                    windowsScript: BuildScript,
                    unity3dBuild: Unity3dBuild,
                    buildArchiveGet: BuildArchiveGet,
                    CODE_GIT: CodePullGitX,
                    CODE_GITLAB: CodePullGitX,
                    CODE_SVN: CodePullSvn,
                    iosCertInstall: IosCertInstall,
                    acrossProjectDistribution: CrossDistribute,
                    sendRTXNotify: SendWechatNotify,
                    reportArchive: ReportArchive,
                    reportArchiveService: ReportArchive,
                    codeGitWebHookTrigger: CodeGitWebHookTrigger,
                    codeGitlabWebHookTrigger: CodeGitlabWebHookTrigger,
                    codeSVNWebHookTrigger: CodeSvnWebHookTrigger,
                    GITHUB: PullGithub,
                    codeGithubWebHookTrigger: CodeGithubWebHookTrigger,
                    pushImageToThirdRepo: PushImageToThirdRepo,
                    subPipelineCall: SubPipelineCall,
                    manualReviewUserTask: ManualReviewUserTask
                }
                return atomMap[this.atomCode] || NormalAtom
            }
        },
        watch: {
            atomCode (atomCode) {
                this.errors.clear()
                this.$nextTick(() => {
                    bus.$emit('validate')
                })
                if (atomCode) {
                    const version = this.element.version ? this.element.version : this.getDefaultVersion(atomCode)
                    this.handleFetchAtomModal(atomCode, version)
                    this.fetchAtomVersionList({
                        projectCode: this.projectId,
                        atomCode
                    })
                }
            },
            atomVersion (newVal) {
                this.isSetted = false
                this.requestAtomVersionMatch(newVal)
            },
            'errors.items': {
                deep: true,
                handler: function (errorsItems) {
                    const isError = errorsItems.length > 0
                    this.handleUpdateAtom('isError', isError)
                }
            }
        },
        mounted () {
            const { projectId, element, globalEnvs, atomCode, requestGlobalEnvs, getDefaultVersion } = this
            const version = element.version ? element.version : getDefaultVersion(atomCode)
            this.handleFetchAtomModal(atomCode, version)
            this.fetchAtomVersionList({
                projectCode: projectId,
                atomCode
            })

            if (!globalEnvs) { // 获取环境变量列表
                requestGlobalEnvs()
            }
            this.toggleAtomSelectorPopup(!atomCode)
        },

        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'requestGlobalEnvs',
                'updateAtom',
                'updateAtomType',
                'fetchAtoms',
                'fetchAtomModal',
                'fetchAtomVersionList',
                'togglePropertyPanel',
                'pausePlugin',
                'requestPipelineExecDetail'
            ]),

            ...mapActions('common', [
                'updateRefreshQualityLoading'
            ]),

            changePluginPause (isContinue, loadingKey) {
                const postData = {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.$route.params.buildNo,
                    taskId: this.element.id,
                    isContinue,
                    stageId: this.stage.id,
                    containerId: this.container.id,
                    element: this.element
                }
                const editingElementPos = {
                    ...this.editingElementPos
                }
                this[loadingKey] = true
                this.togglePropertyPanel({
                    isShow: false,
                    showPanelType: ''
                })
                this.pausePlugin(postData).then(() => {
                    return this.requestPipelineExecDetail(this.$route.params)
                }).catch((err) => {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                    this.togglePropertyPanel({
                        isShow: true,
                        editingElementPos,
                        showPanelType: 'PAUSE'
                    })
                }).finally(() => {
                    this[loadingKey] = false
                })
            },

            toggleEditName (show) {
                this.nameEditing = show
            },
            handleEditName (e) {
                const { value } = e.target
                this.handleUpdateAtom('name', value)
            },
            setAtomValidate (addErrors, removeErrors) {
                if (addErrors && addErrors.length) {
                    addErrors.map(e => this.errors.add(e))
                }
                if (removeErrors && removeErrors.length) {
                    removeErrors.map(e => this.errors.remove(e.field))
                }
            },
            handleUpdateAtom (name, val) {
                this.updateAtom({
                    element: this.element,
                    newParam: {
                        [name]: val
                    }
                })
            },
            handleFetchAtomModal (atomCode, version) {
                const { atomModalMap, fetchAtomModal, getAtomModalKey } = this
                const atomModalKey = getAtomModalKey(atomCode, version)
                const atomModal = atomModalMap[atomModalKey]
                const queryOfflineFlag = !this.editable
                if (!atomModal && atomCode) { // 获取插件详情
                    fetchAtomModal({
                        projectCode: this.projectId,
                        atomCode,
                        version,
                        queryOfflineFlag
                    })
                }
            },
            handleUpdateVersion (id) {
                if (this.element && this.element.version !== id) {
                    this.updateAtomModal(this.atomCode, id)
                }
            },
            requestInterceptAtom () {
                this.$store.dispatch('common/requestInterceptAtom', {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId
                })
            },
            async requestAtomVersionMatch (version) {
                try {
                    let res
                    if (this.isTemplatePanel) {
                        res = await this.$store.dispatch('common/requestTemplateCheckVersion', {
                            projectId: this.projectId,
                            templateId: this.templateId,
                            atomCode: this.element.atomCode,
                            version
                        })
                    } else {
                        res = await this.$store.dispatch('common/requestPipelineCheckVersion', {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            atomCode: this.element.atomCode,
                            version
                        })
                    }
                    if (res) {
                        this.isSupportVersion = res.controlPoint
                        this.curVersionRelativeRules = []
                        if (res.ruleList && res.controlPoint) this.curVersionRelativeRules = res.ruleList
                    }
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            },
            updateAtomModal (atomCode, version) {
                const { elementIndex, container, updateAtomType, getAtomModal, fetchAtomModal } = this
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })

                const fn = atomModal ? updateAtomType : fetchAtomModal
                fn({
                    projectCode: this.projectId,
                    container,
                    version,
                    atomCode,
                    atomIndex: elementIndex
                })
            },
            checkAtomIsIncludeRule (ruleList) {
                const hasVaildRule = ruleList.some(item =>
                    item.taskId === this.element.atomCode
                    && (item.ruleList.every(rule => !rule.gatewayId)
                        || item.ruleList.some(rule => this.element.name.indexOf(rule.gatewayId) > -1))
                )
                return hasVaildRule
            },
            getRelativeRule (rules) {
                const result = []
                rules.map(rule => {
                    if (rule.taskId === this.element.atomCode && rule.ruleList.every(rule => !rule.gatewayId)) {
                        result.push(rule)
                    } else if (rule.taskId === this.element.atomCode
                        && rule.ruleList.some(val => this.element.name.indexOf(val.gatewayId) > -1)) {
                        const temp = {
                            ...rule,
                            ruleList: rule.ruleList.filter(item => this.element.name.indexOf(item.gatewayId) > -1)
                        }
                        return result.push(temp)
                    }
                    return false
                })

                return result
            },
            toSetRule () {
                this.isSetted = true
                const url = `${WEB_URL_PREFIX}/quality/${this.projectId}/createRule/?${this.isTemplatePanel ? 'templateId' : 'pipelineId'}=${this.isTemplatePanel ? this.templateId : this.pipelineId}&element=${this.element.atomCode}`
                window.open(url, '_blank')
            },
            requestMatchTemplateRules () {
                this.$store.dispatch('common/requestMatchTemplateRuleList', {
                    projectId: this.projectId,
                    templateId: this.templateId
                })
            },
            refresh () {
                this.updateRefreshQualityLoading(true)
                if (this.isTemplatePanel) {
                    this.requestMatchTemplateRules()
                } else {
                    this.requestInterceptAtom()
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './propertyPanel';
    .not-recommend {
        text-decoration: line-through;
    }
    .desc-tips {
        font-size: 14px;
        margin: 30px 0;
        span {
            font-weight: bold;
            color: $fontColor;
        }
    }
    .atom-form-footer {
        margin-top: 10px;
        button {
            margin-right: 6px;
        }
    }
    .atom-changed-prop {
        margin-bottom: 8px;
    }
    .no-atom-tips {
        display: flex;
        align-items: center;
        flex-direction: row;
        height: 50px;
        width: 100%;
        &-icon {
            background: $warningColor;
            border: 1px solid $warningColor;
            height: 100%;
            width: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
        }
        > p {
            line-height: 48px;
            border: 1px solid $borderColor;
            border-left-color: transparent;
            flex: 1;
            text-align: center;
        }
    }

    .atom-form-box.readonly {
        pointer-events: none;
    }

    .atom-main-content {
        font-size: 12px;
        .atom-link {
            color: $primaryColor;
        }
    }
    .atom-desc-content {
        padding: 12px;
        a {
            display: inline-block;
            margin-top: 16px;
            font-size: 12px;
            color: $primaryColor;
        }
    }
    .quality-setting-tips {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10px;
        align-items: center;
        .quality-setting-desc {
            flex: 3;
        }
        .quality-rule-link {
            // margin-left: -6px;
            color: $primaryColor;
            cursor: pointer;
        }
        .refresh-btn {
            color: $primaryColor;
            cursor: pointer;
            flex: 1;
        }
        .executing-job {
            position: relative;
            top: 2px;
            margin-right: 20px;
            flex: 1;
            &:before {
                display: inline-block;
                animation: rotating infinite .6s linear;
            }
        }
    }
    .atom-content {
        margin-bottom: 20px;
        .empty-tips {
            text-align: center;
            font-size: 14px;
            a {
                color: $primaryColor;
            }
        }
    }
    .property-panel-header {
        font-size: 14px;
        font-weight:normal;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 60px;
        width: calc(100% - 30px);
        border-bottom: 1px solid #e6e6e6;

        .atom-name-edit {
            display: flex;
            height: 36px;
            line-height: 36px;
            > p {
                max-width: 450px;
                @include ellipsis();
            }
            > .bk-form-input {
                width: 450px;
            }
            .icon-edit {
                cursor: pointer;
                margin-left: 12px;
                line-height: 36px;
                &.editing {
                    display: none;
                }
            }
        }
    }
    .atom-select-entry {
        display: flex;
        line-height: 30px;
        justify-content: space-between;
        font-size: 12px;
        .atom-selected-name {
            flex: 1;
            @include ellipsis();
            border: 1px solid $borderLightColor;
            border-right: 0;
            border-radius: 2px 0 0 2px;
            padding: 0 8px;
        }
        .atom-select-btn {
            padding: 0 12px;
            &.reselect-btn {
                border-radius: 0 2px 2px 0;
            }
        }
    }
    .pointer-events-auto {
        pointer-events: auto;
    }
</style>
