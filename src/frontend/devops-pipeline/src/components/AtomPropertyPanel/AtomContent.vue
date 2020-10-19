<template>
    <section @click="toggleAtomSelectorPopup(false)" v-if="element" class="atom-property-panel">
        <div class="atom-main-content" v-bkloading="{ isLoading: fetchingAtmoModal }">
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
                                <bk-button theme="primary" class="atom-select-btn reselect-btn" :disabled="!editable" @click.stop="toggleAtomSelectorPopup(true)">{{ $t('editPage.reSelect') }}</bk-button>
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
                        :disabled="!editable"
                    >
                        <bk-option v-for="v in atomVersionList" :key="v.versionName" :id="v.versionValue" :name="v.versionName"></bk-option>
                    </bk-select>
                </form-field>
            </div>
            <div class="atom-form-content">
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

                <div v-if="atom" :class="{ 'atom-form-box': true, 'readonly': !editable }">
                    <!-- <div class='desc-tips' v-if="!isNewAtomTemplate(atom.htmlTemplateVersion) && atom.description"> <span>插件描述：</span> {{ atom.description }}</div> -->
                    <div
                        v-if="atom.atomModal"
                        :is="AtomComponent"
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
    import SubPipelineCall from './SubPipelineCall'
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
            Logo
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
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
                ruleDetailMessage: {}
            }
        },
        computed: {
            ...mapState('soda', [
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
                'isNewAtomTemplate'
            ]),
            ...mapState('atom', [
                'globalEnvs',
                'atomCodeList',
                'atomClassifyCodeList',
                'atomMap',
                'atomModalMap',
                'fetchingAtmoModal',
                'atomVersionList',
                'isPropertyPanelVisible'
            ]),
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.toggleAtomSelectorPopup(value)
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
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
                const { containerIndex, containers, getContainer } = this
                return getContainer(containers, containerIndex)
            },
            element () {
                const { container, elementIndex, getElement } = this
                const element = getElement(container, elementIndex)
                return element
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
                return Array.isArray(this.atomVersionList) && this.atomVersionList.length > 0
            },
            AtomComponent () {
                if (this.atomCode === 'ddtestatomdev' || this.atomCode === 'CodeccCheckAtom') {
                    return RemoteAtom
                }
                if (this.isNewAtomTemplate(this.atom.htmlTemplateVersion)) {
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
                    codeSVNWebHookTrigger: CodeSvnWebHookTrigger,
                    GITHUB: PullGithub,
                    codeGithubWebHookTrigger: CodeGithubWebHookTrigger,
                    pushImageToThirdRepo: PushImageToThirdRepo,
                    subPipelineCall: SubPipelineCall
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
                'togglePropertyPanel'
            ]),
            ...mapActions('soda', [
                'updateRefreshQualityLoading'
            ]),
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
                if (!atomModal && atomCode) { // 获取插件详情
                    fetchAtomModal({
                        projectCode: this.projectId,
                        atomCode,
                        version
                    })
                }
            },
            handleUpdateVersion (id) {
                if (this.element && this.element.version !== id) {
                    this.updateAtomModal(this.atomCode, id)
                }
            },
            requestInterceptAtom () {
                this.$store.dispatch('soda/requestInterceptAtom', {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId
                })
            },
            async requestAtomVersionMatch (version) {
                try {
                    let res
                    if (this.isTemplatePanel) {
                        res = await this.$store.dispatch('soda/requestTemplateCheckVersion', {
                            projectId: this.projectId,
                            templateId: this.templateId,
                            atomCode: this.element.atomCode,
                            version
                        })
                    } else {
                        res = await this.$store.dispatch('soda/requestPipelineCheckVersion', {
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
                const url = `${WEB_URL_PIRFIX}/quality/${this.projectId}/createRule/?${this.isTemplatePanel ? 'templateId' : 'pipelineId'}=${this.isTemplatePanel ? this.templateId : this.pipelineId}&element=${this.element.atomCode}`
                window.open(url, '_blank')
            },
            requestMatchTemplateRules () {
                this.$store.dispatch('soda/requestMatchTemplateRuleList', {
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
    .atom-option {
        margin-bottom: 50px;
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
