<template>
    <section
        v-if="container"
        :class="{ readonly: !editable }"
        class="container-property-panel bk-form bk-form-vertical"
    >
        <form-field
            label="Job ID"
            :is-error="errors.has('jobId')"
            :error-msg="errors.first('jobId')"
            :desc="$t('jobIdTips')"
        >
            <div class="container-resource-name">
                <vuex-input
                    :disabled="!editable"
                    input-type="text"
                    :placeholder="$t('jobIdTips')"
                    name="jobId"
                    v-validate.initial="`paramsRule|unique:${allJobId}`"
                    :value="container.jobId"
                    :handle-change="handleContainerChange"
                />
                <atom-checkbox
                    v-if="isVmContainer(container)"
                    class="show-build-resource"
                    :value="container.showBuildResource"
                    :text="$t('editPage.showAliasName')"
                    name="showBuildResource"
                    :handle-change="handleContainerChange"
                    :disabled="!editable"
                >
                </atom-checkbox>
            </div>
        </form-field>
        <form
            v-if="isVmContainer(container)"
            v-bkloading="{ isLoading: !apps || !containerModalId }"
        >
            <form-field :label="$t('editPage.resourceType')">
                <selector
                    :disabled="!editable"
                    :handle-change="changeResourceType"
                    :list="buildResourceTypeList"
                    :value="buildResourceType"
                    :clearable="false"
                    setting-key="type"
                    name="buildType"
                >
                    <template>
                        <div
                            class="bk-selector-create-item cursor-pointer"
                            @click.stop.prevent="addThridSlave"
                        >
                            <i class="devops-icon icon-plus-circle"></i>
                            <span class="text">{{ $t("editPage.addThirdSlave") }}</span>
                        </div>
                    </template>
                </selector>
                <span class="bk-form-help" v-if="isPublicResourceType">{{
                    $t("editPage.publicResTips")
                }}</span>
            </form-field>

            <form-field
                :label="$t('editPage.image')"
                v-if="showImagePublicTypeList.includes(buildResourceType)"
                :required="true"
                :is-error="errors.has('buildImageVersion') || errors.has('buildResource')"
                :error-msg="$t('editPage.imageErrMgs')"
            >
                <enum-input
                    name="imageType"
                    :list="imageTypeList"
                    :disabled="!editable"
                    :handle-change="changeBuildResource"
                    :value="buildImageType"
                >
                </enum-input>

                <section v-if="buildImageType === 'BKSTORE'" class="bk-image">
                    <section class="image-name">
                        <span
                            :class="[
                                { disable: !editable },
                                { 'not-recommend': buildImageRecommendFlag === false },
                                'image-named'
                            ]"
                            :title="
                                buildImageRecommendFlag === false
                                    ? $t('editPage.notRecomendImage')
                                    : buildImageName
                            "
                        >{{ buildImageName || $t("editPage.chooseImage") }}</span
                        >
                        <bk-button theme="primary" @click.stop="chooseImage" :disabled="!editable">{{
                            buildImageCode ? $t("editPage.reElection") : $t("editPage.select")
                        }}</bk-button>
                    </section>
                    <bk-select
                        @change="changeImageVersion"
                        :value="buildImageVersion"
                        searchable
                        class="image-tag"
                        :loading="isVersionLoading"
                        :disabled="!editable"
                        v-validate.initial="'required'"
                        name="buildImageVersion"
                    >
                        <bk-option
                            v-for="option in versionList"
                            :key="option.versionValue"
                            :id="option.versionValue"
                            :name="option.versionName"
                        >
                        </bk-option>
                    </bk-select>
                </section>

                <bk-input
                    v-else
                    @change="changeThirdImage"
                    :value="buildResource"
                    :disabled="!editable"
                    class="bk-image"
                    :placeholder="$t('editPage.thirdImageHolder')"
                    v-validate.initial="'required'"
                    name="buildResource"
                ></bk-input>
            </form-field>
            <form-field
                :label="$t('editPage.assignResource')"
                v-if="
                    buildResourceType !== 'MACOS' &&
                        buildResourceType !== 'WINDOWS' &&
                        !isPublicResourceType &&
                        containerModalId &&
                        !showImagePublicTypeList.includes(buildResourceType)
                "
                :is-error="errors.has('buildResource')"
                :error-msg="errors.first('buildResource')"
                :desc="
                    buildResourceType === 'THIRD_PARTY_AGENT_ENV'
                        ? this.$t('editPage.thirdSlaveTips')
                        : ''
                "
            >
                <container-env-node
                    :required="true"
                    :disabled="!editable"
                    :os="container.baseOS"
                    :container-id="containerModalId"
                    :build-resource-type="buildResourceType"
                    :build-image-type="buildImageType"
                    :agent-type="buildAgentType"
                    :toggle-visible="toggleVisible"
                    :handle-change="changeBuildResource"
                    :add-thrid-slave="addThridSlave"
                    :value="buildResource"
                    :env-project-id="buildResourceProj"
                    :has-error="errors.has('buildResource')"
                    v-validate.initial="'required'"
                    name="buildResource"
                />
            </form-field>

            <!-- windows公共构建机类型 -->
            <template v-if="buildResourceType === 'WINDOWS'">
                <form-field
                    :label="$t('editPage.winSystemVersion')"
                    :required="true"
                    :is-error="errors.has('systemVersion')"
                    :error-msg="errors.first(`systemVersion`)"
                >
                    <bk-select
                        @change="changeWindowSystem"
                        :disabled="!editable"
                        :value="systemVersion"
                        searchable
                        :loading="isLoadingWin"
                        name="systemVersion"
                        v-validate.initial="'required'"
                    >
                        <bk-option
                            v-for="item in windowsVersionList"
                            :key="item.name"
                            :id="item.name"
                            :name="item.systemVersion"
                        >
                        </bk-option>
                    </bk-select>
                </form-field>
            </template>

            <template v-if="buildResourceType === 'MACOS'">
                <form-field
                    :label="$t('editPage.macSystemVersion')"
                    :required="true"
                    :is-error="errors.has('systemVersion')"
                    :error-msg="errors.first(`systemVersion`)"
                >
                    <bk-select
                        :disabled="!editable"
                        :value="systemVersion"
                        searchable
                        :loading="isLoadingMac"
                        name="systemVersion"
                        v-validate.initial="'required'"
                    >
                        <bk-option
                            v-for="item in systemVersionList"
                            :key="item"
                            :id="item"
                            :name="item"
                            @click.native="chooseMacSystem(item)"
                        >
                        </bk-option>
                    </bk-select>
                </form-field>
                <form-field
                    :label="$t('editPage.xcodeVersion')"
                    :required="true"
                    :is-error="errors.has('xcodeVersion')"
                    :error-msg="errors.first(`xcodeVersion`)"
                >
                    <bk-select
                        :disabled="!editable"
                        :value="xcodeVersion"
                        searchable
                        :loading="isLoadingMac"
                        name="xcodeVersion"
                        v-validate.initial="'required'"
                        @toggle="toggleXcode"
                    >
                        <bk-option
                            v-for="item in xcodeVersionList"
                            :key="item"
                            :id="item"
                            :name="item"
                            @click.native="chooseXcode(item)"
                        >
                        </bk-option>
                    </bk-select>
                </form-field>
            </template>

            <form-field
                :label="$t('editPage.imageTicket')"
                v-if="
                    showImagePublicTypeList.includes(buildResourceType) &&
                        buildImageType === 'THIRD'
                "
            >
                <select-input
                    v-bind="imageCredentialOption"
                    :disabled="!editable"
                    name="credentialId"
                    :value="buildImageCreId"
                    :handle-change="changeBuildResource"
                ></select-input>
            </form-field>

            <section v-if="buildResourceType === 'DOCKER'">
                <form-field :label="$t('editPage.performance')" v-show="isShowPerformance">
                    <devcloud-option
                        :disabled="!editable"
                        :value="container.dispatchType.performanceConfigId"
                        :build-type="buildResourceType"
                        :handle-change="changeBuildResourceWithoutEnv"
                        :change-show-performance="changeShowPerformance"
                    >
                    </devcloud-option>
                </form-field>
            </section>

            <form-field :label="$t('editPage.workspace')" v-if="isThirdParty">
                <vuex-input
                    :disabled="!editable"
                    name="workspace"
                    :value="container.dispatchType.workspace"
                    :handle-change="changeBuildResource"
                    :placeholder="$t('editPage.workspaceTips')"
                />
            </form-field>
            <form-field class="container-app-field" v-if="isShowNFSDependencies">
                <atom-checkbox
                    :value="nfsSwitch"
                    :text="$t('editPage.envDependency')"
                    name="nfsSwitch"
                    :handle-change="handleNfsSwitchChange"
                    :disabled="!editable"
                >
                </atom-checkbox>
                <template v-if="nfsSwitch && apps">
                    <container-app-selector
                        :disabled="!editable"
                        class="app-selector-item"
                        v-if="!hasBuildEnv"
                        app=""
                        version=""
                        :handle-change="handleContainerAppChange"
                        :apps="apps"
                        :remove-container-app="removeContainerApp"
                        :add-container-app="containerAppList.length > 0 ? addContainerApp : null"
                    ></container-app-selector>
                    <container-app-selector
                        :disabled="!editable"
                        v-else
                        class="app-selector-item"
                        v-for="(version, app) in container.buildEnv"
                        :key="app"
                        :app="app"
                        :version="version"
                        :handle-change="handleContainerAppChange"
                        :envs="container.buildEnv"
                        :apps="apps"
                        :remove-container-app="removeContainerApp"
                        :add-container-app="containerAppList.length > 0 ? addContainerApp : null"
                    ></container-app-selector>
                </template>
            </form-field>

            <div class="build-path-tips" v-if="hasBuildEnv">
                <div class="tips-icon"><i class="bk-icon icon-info-circle-shape"></i></div>
                <div class="tips-content">
                    <p class="tips-title">{{ $t("editPage.envDependencyTips") }}：</p>
                    <template v-for="(value, keys) in container.buildEnv">
                        <div class="tips-list" v-if="value" :key="keys">
                            <p class="tips-item">{{ appBinPath(value, keys) }}</p>
                            <p
                                class="tips-item"
                                v-for="(env, envIndex) in appEnvs[keys]"
                                :key="envIndex"
                            >
                                {{ appEnvPath(value, keys, env) }}
                            </p>
                        </div>
                    </template>
                </div>
            </div>
        </form>

        <section v-if="isTriggerContainer(container)">
            <version-config
                :disabled="!editable"
                :params="container.params"
                :build-no="container.buildNo"
                :set-parent-validate="setContainerValidate"
                :update-container-params="handleContainerChange"
            ></version-config>
            <build-params
                key="params"
                :disabled="!editable"
                :params="container.params"
                :addition-params="container.templateParams"
                setting-key="params"
                :title="$t('template.pipelineVar')"
                :build-no="container.buildNo"
                :set-parent-validate="setContainerValidate"
                :update-container-params="handleContainerChange"
            ></build-params>
            <build-params
                v-if="routeName === 'templateEdit'"
                key="templateParams"
                :disabled="!editable"
                :params="container.templateParams"
                :addition-params="container.params"
                setting-key="templateParams"
                :title="$t('template.templateConst')"
                :set-parent-validate="setContainerValidate"
                :update-container-params="handleContainerChange"
            ></build-params>
        </section>

        <div>
            <div class="job-matrix">
                <job-matrix
                    v-if="!isTriggerContainer(container)"
                    :enable-matrix="container.matrixGroupFlag || false"
                    :matrix-control-option="container.matrixControlOption"
                    :update-container-params="handleContainerChange"
                    :set-parent-validate="setContainerValidate"
                    :disabled="!editable"
                >
                </job-matrix>
            </div>
            <div class="job-option">
                <job-option
                    v-if="!isTriggerContainer(container)"
                    :job-option="container.jobControlOption"
                    :update-container-params="handleContainerChange"
                    :set-parent-validate="setContainerValidate"
                    @setKeyValueValidate="setContainerValidate"
                    :disabled="!editable"
                    :stage="stage"
                    :stage-index="stageIndex"
                    :container-index="containerIndex"
                >
                </job-option>
            </div>
            <div class="job-mutual">
                <job-mutual
                    v-if="!isTriggerContainer(container)"
                    :mutex-group="container.mutexGroup"
                    :update-container-params="handleContainerChange"
                    :set-parent-validate="setContainerValidate"
                    :disabled="!editable"
                >
                </job-mutual>
            </div>
        </div>

        <image-selector
            :is-show.sync="showImageSelector"
            v-if="showImagePublicTypeList.includes(buildResourceType)"
            :code="buildImageCode"
            :build-resource-type="buildResourceType"
            @choose="choose"
        ></image-selector>
    </section>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import Vue from 'vue'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import ContainerAppSelector from './ContainerAppSelector'
    import ContainerEnvNode from './ContainerEnvNode'
    import DevcloudOption from './DevcloudOption'
    import BuildParams from './BuildParams'
    import VersionConfig from './VersionConfig'
    import JobOption from './JobOption'
    import JobMutual from './JobMutual'
    import JobMatrix from './JobMatrix'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import ImageSelector from '@/components/AtomSelector/imageSelector'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'

    export default {
        name: 'container-content',
        components: {
            EnumInput,
            VuexInput,
            FormField,
            ContainerAppSelector,
            BuildParams,
            VersionConfig,
            DevcloudOption,
            ContainerEnvNode,
            JobOption,
            JobMutual,
            JobMatrix,
            Selector,
            AtomCheckbox,
            ImageSelector,
            SelectInput
        },
        props: {
            containerIndex: Number,
            containerGroupIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            title: String
        },
        data () {
            return {
                showImagePublicTypeList: [
                    'DOCKER',
                    'IDC',
                    'PUBLIC_DEVCLOUD',
                    'KUBERNETES',
                    'PUBLIC_BCS'
                ],
                showImageSelector: false,
                isVersionLoading: false,
                isLoadingMac: false,
                xcodeVersionList: [],
                systemVersionList: [],
                isLoadingWin: false,
                windowsVersionList: [],
                isShowPerformance: false
            }
        },
        computed: {
            ...mapGetters('atom', [
                'getAppEnvs',
                'getContainerApps',
                'getContainer',
                'getContainers',
                'getStage',
                'isVmContainer',
                'isTriggerContainer',
                'isNormalContainer',
                'getBuildResourceTypeList',
                'getContainerModalId',
                'isThirdPartyContainer',
                'isPublicResource',
                'isDockerBuildResource',
                'isPublicDevCloudContainer'
            ]),
            imageTypeList () {
                return [
                    { label: this.$t('editPage.fromList'), value: 'BKSTORE' },
                    {
                        label: this.$t('editPage.fromHand'),
                        value: 'THIRD',
                        hidden: this.buildResourceType === 'PUBLIC_DEVCLOUD'
                    }
                ]
            },
            appEnvs () {
                return this.getAppEnvs(this.container.baseOS)
            },
            routeName () {
                return this.$route.name
            },
            isDetailPage () {
                return this.$route.name === 'pipelinesDetail'
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            buildId () {
                return this.$route.params.buildNo
            },
            stage () {
                const { stageIndex, stages } = this
                return this.getStage(stages, stageIndex)
            },
            containers () {
                const { stage, getContainers } = this
                return getContainers(stage)
            },
            container () {
                const { containers, containerIndex, containerGroupIndex } = this
                return this.getContainer(containers, containerIndex, containerGroupIndex)
            },
            isPublicResourceType () {
                return this.isPublicResource(this.container)
            },
            isThirdParty () {
                return this.isThirdPartyContainer(this.container)
            },
            isDocker () {
                return this.isDockerBuildResource(this.container)
            },
            containerModalId () {
                const {
                    container: { baseOS },
                    getContainerModalId
                } = this
                return getContainerModalId(baseOS)
            },
            hasBuildEnv () {
                return Object.keys(this.container.buildEnv).length > 0
            },
            buildResourceType () {
                try {
                    return this.container.dispatchType.buildType
                } catch (e) {
                    return ''
                }
            },
            isPublicDevCloud () {
                return this.isPublicDevCloudContainer(this.container)
            },
            xcodeVersion () {
                return this.container.dispatchType.xcodeVersion
            },
            systemVersion () {
                return this.container.dispatchType.systemVersion
            },
            buildResource () {
                return this.container.dispatchType.value
            },
            buildResourceProj () {
                return this.container.dispatchType.envProjectId
            },
            buildImageType () {
                return this.container.dispatchType.imageType
            },
            buildImageCode () {
                return this.container.dispatchType && this.container.dispatchType.imageCode
            },
            buildImageVersion () {
                return this.container.dispatchType.imageVersion
            },
            buildImageName () {
                return this.container.dispatchType && this.container.dispatchType.imageName
            },
            buildImageRecommendFlag () {
                return this.container.dispatchType && this.container.dispatchType.recommendFlag
            },
            buildImageCreId () {
                return this.container.dispatchType.credentialId || ''
            },
            buildAgentType () {
                return this.container.dispatchType.agentType || 'ID'
            },
            apps () {
                const {
                    container: { baseOS },
                    getContainerApps
                } = this
                return getContainerApps(baseOS)
            },
            nfsSwitch () {
                return (
                    !Object.prototype.hasOwnProperty.call(this.container, 'nfsSwitch')
                    || this.container.nfsSwitch
                )
            },
            isShowNFSDependencies () {
                if (this.buildResourceType === 'MACOS') return false
                const buildType = this.buildResourceTypeList.find(
                    (bt) => bt.type === this.buildResourceType
                )
                return buildType && buildType.enableApp
            },
            buildResourceTypeList () {
                const {
                    container: { baseOS },
                    getBuildResourceTypeList
                } = this
                return getBuildResourceTypeList(baseOS)
            },
            containerAppList () {
                const {
                    apps,
                    container: { buildEnv }
                } = this
                const selectedApps = Object.keys(buildEnv)
                return Object.keys(apps).filter((app) => !selectedApps.includes(app))
            },
            imageCredentialOption () {
                return {
                    optionsConf: {
                        paramId: 'credentialId',
                        paramName: 'credentialId',
                        url: `/ticket/api/user/credentials/${this.projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000&credentialTypes=USERNAME_PASSWORD`,
                        hasAddItem: true,
                        itemText: this.$t('editPage.addCredentials'),
                        itemTargetUrl: `/ticket/${this.projectId}/createCredential/USERNAME_PASSWORD/true`
                    }
                }
            },
            allJobId () {
                const jobIdList = []
                this.stages.forEach((stage) => {
                    stage.containers.forEach((container) => {
                        if (container.jobId) {
                            jobIdList.push(container.jobId)
                        }
                    })
                })
                return jobIdList
            }
        },
        watch: {
            errors: {
                deep: true,
                handler: function (errors, old) {
                    // this.setContainerValidate()
                    const isError = errors.any()
                    this.handleContainerChange('isError', isError)
                }
            }
        },
        created () {
            const { container } = this
            if (
                this.isTriggerContainer(container)
                && this.container.templateParams === undefined
            ) {
                Vue.set(container, 'templateParams', [])
            }
            if (this.isTriggerContainer(container) && this.container.buildNo === undefined) {
                Vue.set(container, 'buildNo', null)
            }
            if (
                !this.isTriggerContainer(container)
                && this.container.jobControlOption === undefined
            ) {
                Vue.set(container, 'jobControlOption', {})
            }
            if (!this.isTriggerContainer(container) && this.container.mutexGroup === undefined) {
                Vue.set(container, 'mutexGroup', {})
            }
            if (!this.isTriggerContainer(container) && this.container.jobId === undefined) {
                Vue.set(container, 'jobId', '')
            }
            if (
                !this.isTriggerContainer(container)
                && this.container.matrixGroupFlag === undefined
            ) {
                Vue.set(container, 'matrixGroupFlag', false)
            }
            if (
                this.buildResourceType === 'THIRD_PARTY_AGENT_ID'
                && !this.container.dispatchType.agentType
            ) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        agentType: 'ID'
                    })
                )
            }
            if (this.container.dispatchType && this.container.dispatchType.imageCode) {
                this.getVersionList(this.container.dispatchType.imageCode)
            }
            if (this.buildResourceType === 'MACOS') this.getMacOsData()
            if (this.buildResourceType === 'WINDOWS') this.getWinData()
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'getMacSysVersion',
                'getMacXcodeVersion',
                'getWinVersion'
            ]),
            ...mapActions('pipelines', ['requestImageVersionlist']),

            changeResourceType (name, val) {
                const currentType
                    = this.buildResourceTypeList.find((buildType) => buildType.type === val) || {}
                const defaultBuildResource = currentType.defaultBuildResource || {}
                const defaultAgentType
                    = name === 'buildType'
                        && ['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV'].includes(val)
                        && !this.agentType
                        ? { agentType: 'ID' }
                        : {}
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        ...defaultAgentType,
                        imageVersion: defaultBuildResource.version || '',
                        value: defaultBuildResource.value || '',
                        imageCode: defaultBuildResource.code || '',
                        imageName: defaultBuildResource.name || '',
                        imageType: defaultBuildResource.imageType || '',
                        recommendFlag: defaultBuildResource.recommendFlag,
                        [name]: val
                    })
                )
                if (val === 'MACOS') this.getMacOsData()
                if (val === 'WINDOWS') this.getWinData()
                if (this.container.dispatchType && this.container.dispatchType.imageCode) {
                    this.getVersionList(this.container.dispatchType.imageCode)
                }
            },

            changeThirdImage (val) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        value: val
                    })
                )
            },

            changeImageVersion (value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        imageVersion: value,
                        value: this.buildImageCode
                    })
                )
            },

            choose (card) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        imageCode: card.code,
                        imageName: card.name,
                        recommendFlag: card.recommendFlag
                    })
                )
                return this.getVersionList(card.code).then(() => {
                    let chooseVersion = this.versionList[0] || {}
                    if (card.historyVersion) {
                        chooseVersion
                            = this.versionList.find((x) => x.versionValue === card.historyVersion) || {}
                    }
                    this.handleContainerChange(
                        'dispatchType',
                        Object.assign({
                            ...this.container.dispatchType,
                            imageVersion: chooseVersion.versionValue,
                            value: card.code
                        })
                    )
                })
            },

            getVersionList (imageCode) {
                this.isVersionLoading = true
                const data = {
                    projectCode: this.projectId,
                    imageCode
                }
                return this.requestImageVersionlist(data)
                    .then((res) => {
                        this.versionList = res.data || []
                    })
                    .catch((err) => this.$showTips({ theme: 'error', message: err.message || err }))
                    .finally(() => {
                        this.isVersionLoading = false
                    })
            },

            chooseImage (event) {
                event.preventDefault()
                this.showImageSelector = !this.showImageSelector
            },

            getMacOsData () {
                this.isLoadingMac = true
                Promise.all([this.getMacSysVersion(), this.getMacXcodeVersion(this.systemVersion)])
                    .then(([sysVersion, xcodeVersion]) => {
                        this.xcodeVersionList = xcodeVersion.data?.versionList || []
                        this.systemVersionList = sysVersion.data?.versionList || []
                        if (
            this.container.dispatchType?.systemVersion === undefined
            && this.container.dispatchType?.xcodeVersion === undefined
                        ) {
                            this.chooseMacSystem(sysVersion.data?.defaultVersion)
                            this.chooseXcode(xcodeVersion.data?.defaultVersion)
                        }
                    })
                    .catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    })
                    .finally(() => (this.isLoadingMac = false))
            },
            async toggleXcode (show) {
                if (show) {
                    const res = await this.getMacXcodeVersion(this.systemVersion)
                    this.xcodeVersionList = res.data?.versionList || []
                }
            },
            chooseMacSystem (item) {
                if (item !== this.systemVersion) {
                    this.handleContainerChange(
                        'dispatchType',
                        Object.assign({
                            ...this.container.dispatchType,
                            systemVersion: item,
                            xcodeVersion: '',
                            value: `${item}:''`
                        })
                    )
                }
            },
            chooseXcode (item) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        xcodeVersion: item,
                        value: `${this.systemVersion}:${item}`
                    })
                )
            },
            setContainerValidate (addErrors, removeErrors) {
                const { errors } = this

                if (addErrors && addErrors.length) {
                    addErrors.forEach((e) => {
                        if (errors && errors.items.every((err) => err.field !== e.field)) {
                            errors.add({
                                field: e.field,
                                msg: e.msg
                            })
                        }
                    })
                }
                if (removeErrors && removeErrors.length) {
                    removeErrors.map((e) => errors.remove(e.field))
                }
                const isError = !!errors.items.length
                this.handleContainerChange('isError', isError)
            },

            changeBuildResource (name, value, envProjectId) {
                console.log(name, value, envProjectId)
                const emptyValueObj
                    = name === 'imageType' || name === 'agentType'
                        ? { value: '', envProjectId: '' }
                        : {}
                if (name === 'value' && envProjectId) {
                    emptyValueObj.envProjectId = envProjectId
                }
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign(
                        {
                            ...this.container.dispatchType,
                            [name]: value
                        },
                        emptyValueObj
                    )
                )
                this.handleContainerChange('buildEnv', {}) // 清空依赖编译环境
            },
            changeBuildResourceWithoutEnv (name, value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        [name]: value
                    })
                )
            },
            changeWindowSystem (value) {
                this.handleContainerChange(
                    'dispatchType',
                    Object.assign({
                        ...this.container.dispatchType,
                        systemVersion: value,
                        value
                    })
                )
            },
            async getWinData () {
                this.isLoadingWin = true
                this.windowsVersionList = await this.getWinVersion()
                this.isLoadingWin = false
            },
            handleContainerChange (name, value) {
                this.updateContainer({
                    container: this.container,
                    newParam: {
                        [name]: value
                    }
                })
            },
            addContainerApp () {
                const { container, containerAppList, getAppDefaultVersion } = this
                const newAppName = containerAppList[0]
                this.handleContainerChange('buildEnv', {
                    ...container.buildEnv,
                    [newAppName]: getAppDefaultVersion(newAppName)
                })
            },
            removeContainerApp (app) {
                const { container } = this
                delete container.buildEnv[app]
                this.handleContainerChange('buildEnv', {
                    ...container.buildEnv
                })
            },
            handleContainerAppChange (preApp, curApp, version = '') {
                const {
                    container: { buildEnv },
                    getAppDefaultVersion
                } = this
                if (preApp !== curApp && Object.prototype.hasOwnProperty.call(buildEnv, preApp)) {
                    delete buildEnv[preApp]
                }
                const defaultAppVer = getAppDefaultVersion(curApp)
                buildEnv[curApp] = version || defaultAppVer
                this.handleContainerChange('buildEnv', {
                    ...buildEnv
                })
            },
            async startDebug () {
                const vmSeqId = this.container.id
                const tab = window.open('about:blank')
                const buildIdStr = this.buildId ? `&buildId=${this.buildId}` : ''
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/dockerConsole/?pipelineId=${this.pipelineId}&dispatchType=${this.buildResourceType}&vmSeqId=${vmSeqId}${buildIdStr}`
                tab.location = url
            },
            handleNfsSwitchChange (name, value) {
                if (!value) {
                    this.handleContainerChange('buildEnv', {})
                }
                this.handleContainerChange(name, value)
            },
            getAppDefaultVersion (app) {
                const { apps } = this
                return apps
                    && apps[app]
                    && Array.isArray(apps[app].versions)
                    && apps[app].versions.length > 0
                    ? apps[app].versions[0]
                    : ''
            },
            appBinPath (value, key) {
                const {
                    container: { baseOS },
                    apps
                } = this
                const app = apps[key]
                if (app) {
                    return baseOS === 'LINUX'
                        ? `export PATH=/data/bkdevops/apps/${key}/${value}/${app.binPath}:$PATH`
                        : `export PATH=/data/soda/apps/${key}/${value}/${app.binPath}:$PATH`
                } else {
                    return ''
                }
            },
            appEnvPath (value, key, env) {
                const {
                    container: { baseOS }
                } = this
                return baseOS === 'LINUX'
                    ? `export ${env.name}=/data/bkdevops/apps/${key}/${value}/${env.path}`
                    : `export ${env.name}=/data/soda/apps/${key}/${value}/${env.path}`
            },
            addThridSlave () {
                const url = `${WEB_URL_PREFIX}/environment/${this.projectId}/nodeList?type=${this.container.baseOS}`
                window.open(url, '_blank')
            },
            changeShowPerformance (isShow = false) {
                this.isShowPerformance = isShow
            }
        }
    }
</script>

<style lang="scss">
@import "../AtomPropertyPanel/propertyPanel";
.container-panel-header {
  display: flex;
  margin-right: 20px;
  justify-content: space-between;
}
.container-property-panel {
  font-size: 14px;
  .bk-image {
    display: flex;
    align-items: center;
    margin-top: 15px;
    .image-name {
      width: 50%;
      display: flex;
      align-items: center;
      .not-recommend {
        text-decoration: line-through;
      }
      .image-named {
        border: 1px solid #c4c6cc;
        flex: 1;
        height: 32px;
        line-height: 32px;
        font-size: 12px;
        color: $fontWeightColor;
        line-height: 32px;
        padding-left: 10px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        &.disable {
          color: #c4c6cc;
          cursor: not-allowed;
        }
      }
    }
    .container-property-panel {
      font-size: 14px;
      .bk-image {
        display: flex;
        align-items: center;
        margin-top: 15px;
        .image-name {
          width: 50%;
          display: flex;
          align-items: center;
          .not-recommend {
            text-decoration: line-through;
          }
          .image-named {
            border: 1px solid #c4c6cc;
            flex: 1;
            height: 32px;
            line-height: 32px;
            font-size: 12px;
            color: $fontWeightColor;
            line-height: 32px;
            padding-left: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            &.disable {
              color: #c4c6cc;
              cursor: not-allowed;
            }
          }
        }
        .image-tag {
          width: 50%;
          margin-left: 10px;
        }
      }
      .container-resource-name {
        display: flex;
        align-items: center;
        > input {
          flex: 1;
        }
        .show-build-resource {
          margin-left: 10px;
        }
      }
      .control-bar {
        position: absolute;
        right: 34px;
        top: 12px;
      }
      .debug-btn {
        position: absolute;
        right: 34px;
        top: 12px;
      }
      .accordion-checkbox {
        margin-left: auto;
      }
      .bk-form-content span.bk-form-help {
        padding-top: 5px;
        display: inline-block;
        a {
          color: #3c96ff;
          &:hover {
            color: #3c96ff;
          }
        }
      }
      form .bk-form-item {
        margin-top: 8px;
      }
    }
  }
  .container-resource-name {
    display: flex;
    align-items: center;
    > input {
      flex: 1;
    }
    .show-build-resource {
      margin-left: 10px;
    }
  }
  .control-bar {
    position: absolute;
    right: 34px;
    top: 12px;
  }
  .accordion-checkbox {
    margin-left: auto;
  }
  .bk-form-content span.bk-form-help {
    padding-top: 5px;
    display: inline-block;
    a {
      color: #3c96ff;
      &:hover {
        color: #3c96ff;
      }
    }
  }
  form .bk-form-item {
    margin-top: 8px;
  }
}
.app-selector-item {
  margin: 10px 0;
  &:last-child {
    .devops-icon.icon-plus {
      display: block;
    }
  }
}
.build-path-tips {
  display: flex;
  min-height: 60px;
  margin-top: 8px;
  .tips-icon {
    display: flex;
    width: 44px;
    align-items: center;
    text-align: center;
    border: 1px solid #ffb400;
    background-color: #ffb400;
    i {
      display: inline-block;
      font-size: 18px;
      color: #fff;
      margin: 21px 13px;
    }
  }
  .tips-content {
    flex: 1;
    padding: 0 20px;
    border: 1px solid #e6e6e6;
    border-left: none;
    .tips-title {
      margin: 15px 0;
      font-weight: 600;
    }
    .tips-list {
      margin-bottom: 10px;
    }
  }
}
</style>
