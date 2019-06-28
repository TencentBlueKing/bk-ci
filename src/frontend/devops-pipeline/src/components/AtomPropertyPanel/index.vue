<template>
    <section @click="toggleAtomSelectorPopup(false)" v-if="element" slot="content" :class="{ &quot;atom-property-panel&quot;: true }">
        <header class="property-panel-header">
            <div class="atom-name-edit">
                <input v-show="nameEditing" v-bk-focus="1" @blur="toggleEditName(false)" @keydown.enter="toggleEditName(false)" class="bk-form-input" name="name" v-validate.initial="&quot;required|max:30&quot;" @@keyup.enter="toggleEditName" @input="handleEditName" placeholder="请输入名称" :value="element.name" />
                <p v-if="!nameEditing">{{ atomCode ? element.name : '待选择插件' }}</p>
                <i v-if="atomCode && editable" @click="toggleEditName(true)" class="bk-icon icon-edit" :class="nameEditing ? 'editing' : ''" />
            </div>
            <reference-variable :global-envs="globalEnvs" :stages="stages" :container="container" />
        </header>
        <div class="atom-main-content" v-bkloading="{ isLoading: fetchingAtmoModal }">
            <div class="atom-type-selector bk-form-row bk-form bk-form-vertical">
                <form-field :inline="true" label="插件" :is-error="errors.has(&quot;@type&quot;)" :error-msg="errors.first(&quot;@type&quot;)">
                    <div class="atom-select-entry">
                        <template v-if="atom">
                            <span :title="atom.name" class="atom-selected-name">{{ atom.name }}</span>
                            <bk-button theme="primary" class="atom-select-btn reselect-btn" :disabled="!editable" @click.stop="toggleAtomSelectorPopup(true)">重选</bk-button>
                        </template>
                        <template v-else-if="!atomCode">
                            <bk-button theme="primary" class="atom-select-btn" @click.stop="toggleAtomSelectorPopup(true)">请在左侧选择一个插件</bk-button>
                        </template>
                        <template v-else>
                            <bk-button theme="primary" class="atom-select-btn" @click.stop="toggleAtomSelectorPopup(true)">重选</bk-button>
                        </template>
                    </div>
                </form-field>
                <form-field v-if="hasVersionList" :desc="`主版本号.latest：表示执行时使用对应\n 主版本号下最新版本的插件`" label="版本">
                    <bk-select :value="element.version" :clearable="false"
                        placeholder="请选择插件版本"
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
                    <p>当前插件没有可用版本。可能是插件已取消发布或者发布流程进行中。</p>
                </div>
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
                            v-if="element[&quot;@type&quot;] !== &quot;manualTrigger&quot;"
                            :element-index="elementIndex"
                            :container-index="containerIndex"
                            :stage-index="stageIndex"
                            :element="element"
                            :container="container"
                            :set-parent-validate="setAtomValidate"
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
    import BuildScript from './BuildScript'
    import Unity3dBuild from './Unity3dBuild'
    import NormalAtom from './NormalAtom'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import Cascader from '@/components/atomFormField/Cascader/index'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from './FormField'
    import BuildArchiveGet from './BuildArchiveGet'
    import { isObject } from '@/utils/util'
    import { bus } from '@/utils/bus'
    import TimerTrigger from './TimerTrigger'
    import CodePullGitX from './CodePullGitX'
    import CodePullSvn from './CodePullSvn'
    import IosCertInstall from './IosCertInstall'
    import CodeSvnWebHookTrigger from './CodeSvnWebHookTrigger'
    import ReportArchive from './ReportArchive'
    import PullGithub from './PullGithub'
    import CodeGithubWebHookTrigger from './CodeGithubWebHookTrigger'
    import PushImageToThirdRepo from './PushImageToThirdRepo'
    import ReferenceVariable from './ReferenceVariable'
    import NormalAtomV2 from './NormalAtomV2'
    import CodeGitWebHookTrigger from './CodeGitWebHookTrigger'
    import Logo from '@/components/Logo'

    export default {
        name: 'atom-property-panel',
        components: {
            ReferenceVariable,
            AtomOption,
            VuexInput,
            Cascader,
            Selector,
            FormField,
            BuildArchiveGet,
            CodePullGitX,
            CodePullSvn,
            IosCertInstall,
            CodeGithubWebHookTrigger,
            ReportArchive,
            CodeSvnWebHookTrigger,
            PullGithub,
            NormalAtomV2,
            PushImageToThirdRepo,
            CodeGitWebHookTrigger,
            Logo
        },
        props: {
            elementIndex: Number,
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean
        },
        data () {
            return {
                nameEditing: false,
                isSetted: false,
                ruleDetailMessage: {}
            }
        },
        computed: {
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
                'atomVersionList'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
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
            hasVersionList () {
                return Array.isArray(this.atomVersionList) && this.atomVersionList.length > 0
            },
            AtomComponent () {
                if (this.isNewAtomTemplate(this.atom.htmlTemplateVersion)) {
                    return NormalAtomV2
                }
                switch (this.atomCode) {
                    case 'timerTrigger':
                        return TimerTrigger
                    case 'linuxScript':
                    case 'windowsScript':
                        return BuildScript
                    case 'unity3dBuild':
                        return Unity3dBuild
                    case 'buildArchiveGet':
                        return BuildArchiveGet
                    case 'CODE_GIT':
                    case 'CODE_GITLAB':
                        return CodePullGitX
                    case 'CODE_SVN':
                        return CodePullSvn
                    case 'iosCertInstall':
                        return IosCertInstall
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
                'fetchAtomVersionList'
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
                console.lod(id)
                if (this.element && this.element.version !== id) {
                    this.updateAtomModal(this.atomCode, id)
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
            }
        }
    }
</script>

<style lang="scss">
    @import './propertyPanel';
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
    }
    .atom-content {
        margin-bottom: 20px;
        padding: 0 4px;
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
        position: absolute;
        left: 30px;
        top: 0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 60px;
        width: calc(100% - 30px);
        padding-left: 20px;
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
</style>
