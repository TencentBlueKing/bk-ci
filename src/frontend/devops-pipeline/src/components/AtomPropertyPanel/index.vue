<template>
    <bk-sideslider class="bkci-property-panel" width="640" :quick-close="true" :is-show.sync="visible">
        <header class="property-panel-header" slot="header">
            <div class="atom-name-edit">
                <input v-if="nameEditing" v-bk-focus="1" @blur="toggleEditName(false)" @keydown.enter="toggleEditName(false)" class="bk-form-input" name="name" v-validate.initial="'required|max:30'" @@keyup.enter="toggleEditName" @input="handleEditName" :placeholder="$t('nameInputTips')" :value="element.name" />
                <p v-if="!nameEditing">{{ atomCode ? element.name : this.$t('editPage.pendingAtom') }}</p>
                <i v-if="atomCode && editable" @click="toggleEditName(true)" class="devops-icon icon-edit" :class="nameEditing ? 'editing' : ''" />
            </div>
            <reference-variable :global-envs="globalEnvs" :stages="stages" :container="container" />
        </header>
        <atom-content v-bind="$props" slot="content"></atom-content>
    </bk-sideslider>
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
    import Gcloud from './Gcloud'
    import JobExecuteTaskExt from './JobExecuteTaskExt'
    import FormField from './FormField'
    import BuildArchiveGet from './BuildArchiveGet'
    import Codecc from './Codecc'
    import { isObject } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import JobDevopsFastExecuteScript from './JobDevopsFastExecuteScript'
    import Tcls from './Tcls'
    import JobDevOpsFastPushFile from './JobDevOpsFastPushFile'
    import JobDevopsExecuteTaskExt from './JobDevopsExecuteTaskExt'
    import ZhiyunInstanceMaintenance from './ZhiyunInstanceMaintenance'
    import TimerTrigger from './TimerTrigger'
    import Tcm from './Tcm'
    import CodePullGitX from './CodePullGitX'
    import CodePullSvn from './CodePullSvn'
    import ZhiyunUpdateAsyncEX from './ZhiyunUpdateAsyncEX'
    import IosCertInstall from './IosCertInstall'
    import BcsContainerOp from './BcsContainerOp'
    import NewBcsContainerOp from './NewBcsContainerOp'
    import CrossDistribute from './CrossDistribute'
    import VersionExperience from './VersionExperience'
    import SendWechatNotify from './SendWechatNotify'
    import WeTest from './WeTest'
    import jobCloudsFastPush from './jobCloudsFastPush'
    import jobCloudsFastExecuteScript from './jobCloudsFastExecuteScript'
    import CodeSvnWebHookTrigger from './CodeSvnWebHookTrigger'
    import ReportArchive from './ReportArchive'
    import PullGithub from './PullGithub'
    import CodeGithubWebHookTrigger from './CodeGithubWebHookTrigger'
    import PushImageToThirdRepo from './PushImageToThirdRepo'
    import ReferenceVariable from './ReferenceVariable'
    import AtomFormWithAppID from './AtomFormWithAppID'
    import NormalAtomV2 from './NormalAtomV2'
    import CodeGitWebHookTrigger from './CodeGitWebHookTrigger'
    import SubPipelineCall from './SubPipelineCall'
    import Logo from '@/components/Logo'

    export default {
        name: 'atom-property-panel',
        components: {
            ReferenceVariable,
            AtomOption,
            VuexInput,
            Selector,
            Gcloud,
            JobExecuteTaskExt,
            FormField,
            BuildArchiveGet,
            Codecc,
            JobDevopsFastExecuteScript,
            Tcls,
            JobDevOpsFastPushFile,
            JobDevopsExecuteTaskExt,
            ZhiyunInstanceMaintenance,
            CodePullGitX,
            CodePullSvn,
            ZhiyunUpdateAsyncEX,
            Tcm,
            IosCertInstall,
            BcsContainerOp,
            NewBcsContainerOp,
            CrossDistribute,
            VersionExperience,
            SendWechatNotify,
            QualitygateTips,
            WeTest,
            CodeGithubWebHookTrigger,
            jobCloudsFastPush,
            jobCloudsFastExecuteScript,
            ReportArchive,
            CodeSvnWebHookTrigger,
            PullGithub,
            AtomFormWithAppID,
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
                nameEditing: false
            }
        },
        computed: {
            ...mapState('atom', [
                'globalEnvs',
                'isPropertyPanelVisible'
            ]),
            ...mapGetters('atom', [
                'getElement',
                'getContainer',
                'getContainers',
                'getStage'
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
                switch (this.atomCode) {
                    case 'comDistribution':
                    case 'cloudStone':
                    case 'openStatePushFile':
                    case 'gseKitProcRunCmdDev':
                    case 'gseKitProcRunCmdProd':
                        return AtomFormWithAppID
                    case 'timerTrigger':
                        return TimerTrigger
                    case 'linuxScript':
                    case 'windowsScript':
                        return BuildScript
                    case 'linuxPaasCodeCCScript':
                        return Codecc
                    case 'unity3dBuild':
                        return Unity3dBuild
                    case 'gcloud':
                        return Gcloud
                    case 'jobExecuteTaskExt':
                        return JobExecuteTaskExt
                    case 'buildArchiveGet':
                        return BuildArchiveGet
                    case 'jobDevOpsFastExecuteScript':
                        return JobDevopsFastExecuteScript
                    case 'tclsAddVersion':
                        return Tcls
                    case 'jobDevOpsFastPushFile':
                        return JobDevOpsFastPushFile
                    case 'jobDevOpsExecuteTaskExt':
                        return JobDevopsExecuteTaskExt
                    case 'zhiyunInstanceMaintenance':
                        return ZhiyunInstanceMaintenance
                    case 'CODE_GIT':
                    case 'CODE_GITLAB':
                        return CodePullGitX
                    case 'CODE_SVN':
                        return CodePullSvn
                    case 'zhiyunUpdateAsyncEX':
                        return ZhiyunUpdateAsyncEX
                    case 'tcmElement':
                        return Tcm
                    case 'iosCertInstall':
                        return IosCertInstall
                    case 'bcsContainerOp':
                        return BcsContainerOp
                    case 'bcsContainerOpByName':
                        return NewBcsContainerOp
                    case 'acrossProjectDistribution':
                        return CrossDistribute
                    case 'experience':
                        return VersionExperience
                    case 'wetestElement':
                        return WeTest
                    case 'jobCloudsFastPush':
                        return jobCloudsFastPush
                    case 'jobCloudsFastExecuteScript':
                        return jobCloudsFastExecuteScript
                    case 'sendRTXNotify':
                        return SendWechatNotify
                    case 'reportArchive':
                    case 'reportArchiveService':
                        return ReportArchive
                    case 'codeGitWebHookTrigger':
                        return CodeGitWebHookTrigger
                    case 'codeSVNWebHookTrigger':
                        return CodeSvnWebHookTrigger
                    case 'GITHUB':
                        return PullGithub
                    case 'codeGithubWebHookTrigger':
                        return CodeGithubWebHookTrigger
                    case 'pushImageToThirdRepo':
                        return PushImageToThirdRepo
                    case 'subPipelineCall':
                        return SubPipelineCall
                    default:
                        return NormalAtom
                }
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
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'updateAtom',
                'togglePropertyPanel'
            ]),

            toggleEditName (show) {
                this.nameEditing = show
            },

            handleEditName (e) {
                const { value } = e.target
                this.handleUpdateAtom('name', value)
            },

            handleUpdateAtom (name, val) {
                this.updateAtom({
                    element: this.element,
                    newParam: {
                        [name]: val
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './propertyPanel';

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
</style>
