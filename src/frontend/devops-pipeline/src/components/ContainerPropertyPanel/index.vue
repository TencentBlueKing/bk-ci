<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="container-panel-header" slot="header">
            {{ title }}
            <div v-if="showDebugDockerBtn" :class="!editable ? 'control-bar' : 'debug-btn'">
                <bk-button theme="warning" @click="startDebug('docker')">{{ $t('editPage.docker.debugConsole') }}</bk-button>
            </div>
        </header>
        <container-content v-bind="$props" slot="content" ref="container"></container-content>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import ContainerContent from './ContainerContent'

    export default {
        name: 'container-property-panel',
        components: {
            ContainerContent
        },
        props: {
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            title: String
        },

        computed: {
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            imageTypeList () {
                return [
                    { label: this.$t('editPage.fromList'), value: 'BKSTORE' },
                    { label: this.$t('editPage.fromHand'), value: 'THIRD', hidden: this.buildResourceType === 'PUBLIC_DEVCLOUD' }
                ]
            },
            appEnvs () {
                return this.getAppEnvs(this.container.baseOS)
            },
            routeName () {
                return this.$route.name
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
                const { containers, containerIndex } = this
                return this.getContainer(containers, containerIndex)
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
                const { container: { baseOS }, getContainerModalId } = this
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
            xcodeVersion () {
                return this.container.dispatchType.xcodeVersion
            },
            systemVersion () {
                return this.container.dispatchType.systemVersion
            },
            buildResource () {
                return this.container.dispatchType.value
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
            buildImageCreId () {
                return this.container.dispatchType.credentialId || ''
            },
            buildAgentType () {
                return this.container.dispatchType.agentType || 'ID'
            },
            apps () {
                const { container: { baseOS }, getContainerApps } = this
                return getContainerApps(baseOS)
            },
            showDependencies () {
                const buildType = this.buildResourceTypeList.find(bt => bt.type === this.buildResourceType)
                return this.apps && buildType && buildType.enableApp
            },
            buildResourceTypeList () {
                const { container: { baseOS }, getBuildResourceTypeList } = this
                return getBuildResourceTypeList(baseOS)
            },
            containerAppList () {
                const { apps, container: { buildEnv } } = this
                const selectedApps = Object.keys(buildEnv)
                return Object.keys(apps).filter(app => !selectedApps.includes(app))
            },
            showDebugDockerBtn () {
                return this.routeName !== 'templateEdit' && this.container.baseOS === 'LINUX' && this.isDocker && this.buildResource && (this.routeName === 'pipelinesEdit' || this.container.status === 'RUNNING' || (this.routeName === 'pipelinesDetail' && this.execDetail && this.execDetail.buildNum === this.execDetail.latestBuildNum && this.execDetail.curVersion === this.execDetail.latestVersion))
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
            }
        },

        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'togglePropertyPanel',
                'getMacSysVersion',
                'getMacXcodeVersion'
            ]),
            ...mapActions('soda', [
                'startDebugDocker',
                'getContainerInfoByBuildId',
                'startDebugTstack'
            ]),
            ...mapActions('pipelines', [
                'requestImageVersionlist',
                'requestImageHistory',
                'requestImageDetail'
            ]),

            changeResourceType (name, val) {
                this.imageRecommend = true
                const defaultAgentType = (name === 'buildType' && ['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV'].includes(val) && !this.agentType) ? { agentType: 'ID' } : {}
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    ...defaultAgentType,
                    imageVersion: '',
                    value: '',
                    imageCode: '',
                    imageName: '',
                    [name]: val
                }))
                if (val === 'MACOS') this.getMacOsData()
            },

            changeThirdImage (val) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    value: val
                }))
            },

            changeImageVersion (value) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    imageVersion: value,
                    value: this.buildImageCode
                }))
            },

            choose (card) {
                this.imageRecommend = card.recommendFlag
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    imageCode: card.code,
                    imageName: card.name
                }))
                return this.getVersionList(card.code).then(() => {
                    let chooseVersion = this.versionList[0] || {}
                    if (card.historyVersion) chooseVersion = this.versionList.find(x => x.versionValue === card.historyVersion) || {}
                    this.handleContainerChange('dispatchType', Object.assign({
                        ...this.container.dispatchType,
                        imageVersion: chooseVersion.versionValue,
                        value: card.code
                    }))
                })
            },

            getVersionList (imageCode) {
                this.isVersionLoading = true
                const data = {
                    projectCode: this.projectId,
                    imageCode
                }
                return this.requestImageVersionlist(data).then((res) => {
                    this.versionList = res.data || []
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => {
                    this.isVersionLoading = false
                })
            },

            chooseImage (event) {
                event.preventDefault()
                this.showImageSelector = !this.showImageSelector
            },

            getMacOsData () {
                this.isLoadingMac = true
                Promise.all([this.getMacSysVersion(), this.getMacXcodeVersion()]).then(([sysVersion, xcodeVersion]) => {
                    this.xcodeVersionList = xcodeVersion.data || []
                    this.systemVersionList = sysVersion.data || []
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => (this.isLoadingMac = false))
            },
            chooseMacSystem (item) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    systemVersion: item,
                    value: `${this.systemVersion}:${this.xcodeVersion}`
                }))
            },
            chooseXcode (item) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    xcodeVersion: item,
                    value: `${this.systemVersion}:${this.xcodeVersion}`
                }))
            },
            setContainerValidate (addErrors, removeErrors) {
                const { errors } = this

                if (addErrors && addErrors.length) {
                    addErrors.map(e => {
                        if (errors && errors.items.every(err => err.field !== e.field)) {
                            errors.add({
                                field: e.field,
                                msg: e.msg
                            })
                        }
                    })
                }
                if (removeErrors && removeErrors.length) {
                    removeErrors.map(e => errors.remove(e.field))
                }
                const isError = !!errors.items.length
                this.handleContainerChange('isError', isError)
            },

            changeBuildResource (name, value) {
                const emptyValueObj = (name === 'imageType' || name === 'agentType') ? { value: '' } : {}
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    [name]: value
                }, emptyValueObj))
                this.handleContainerChange('buildEnv', {}) // 清空依赖编译环境
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
                const { container, containerAppList } = this
                const newAppName = containerAppList[0]
                this.handleContainerChange('buildEnv', {
                    ...container.buildEnv,
                    [newAppName]: ''
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
                const { container: { buildEnv }, apps } = this
                if (preApp !== curApp && buildEnv.hasOwnProperty(preApp)) {
                    delete buildEnv[preApp]
                }
                buildEnv[curApp] = version || apps[curApp][0]
                this.handleContainerChange('buildEnv', {
                    ...buildEnv
                })
            },
            async startDebug (type) {
                const vmSeqId = this.getRealSeqId()
                let url = ''
                const tab = window.open('about:blank')
                try {
                    if (type === 'docker') {
                        // docker 分根据buildId获取容器信息和新启动一个容器
                        if (this.routeName === 'pipelinesDetail' && this.container.status === 'RUNNING') {
                            const res = await this.getContainerInfoByBuildId({
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                buildId: this.buildId,
                                vmSeqId
                            })
                            if (res.containerId && res.address) {
                                url = `${WEB_URL_PIRFIX}/pipeline/${this.projectId}/dockerConsole/?pipelineId=${this.pipelineId}&containerId=${res.containerId}&targetIp=${res.address}`
                            }
                        } else {
                            const res = await this.startDebugDocker({
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                vmSeqId,
                                imageCode: this.buildImageCode,
                                imageVersion: this.buildImageVersion,
                                imageName: this.buildResource,
                                buildEnv: this.container.buildEnv,
                                imageType: this.buildImageType,
                                credentialId: this.buildImageCreId
                            })
                            if (res === true) {
                                url = `${WEB_URL_PIRFIX}/pipeline/${this.projectId}/dockerConsole/?pipelineId=${this.pipelineId}&vmSeqId=${vmSeqId}`
                            }
                        }
                    }
                    tab.location = url
                } catch (err) {
                    tab.close()
                    if (err.code === 403) {
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: this.$t('pipeline'),
                                option: this.$t('edit')
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_manager=pipeline:${this.pipelineId}`
                        })
                    } else {
                        this.$showTips({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
                }
            },
            getRealSeqId () {
                let i = 0
                let seqId = 0
                this.stages && this.stages.map((stage, sIndex) => {
                    stage.containers.map((container, cIndex) => {
                        if (sIndex === this.stageIndex && cIndex === this.containerIndex) {
                            seqId = i
                        }
                        i++
                    })
                })
                return seqId
            },
            appBinPath (value, key) {
                const { container: { baseOS }, apps } = this
                const app = apps[key]
                if (app) {
                    return baseOS === 'LINUX' ? `export PATH=/data/bkdevops/apps/${key}/${value}/${app.binPath}:$PATH` : `export PATH=/data/soda/apps/${key}/${value}/${app.binPath}:$PATH`
                } else {
                    return ''
                }
            },
            appEnvPath (value, key, env) {
                const { container: { baseOS } } = this
                return baseOS === 'LINUX' ? `export ${env.name}=/data/bkdevops/apps/${key}/${value}/${env.path}` : `export ${env.name}=/data/soda/apps/${key}/${value}/${env.path}`
            },
            addThridSlave () {
                const url = `${WEB_URL_PIRFIX}/environment/${this.projectId}/nodeList?type=${this.container.baseOS}`
                window.open(url, '_blank')
            },
            addDockerImage () {
                const url = `${WEB_URL_PIRFIX}/artifactory/${this.projectId}/depot/project-image`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import '../AtomPropertyPanel/propertyPanel';
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
                width: 44%;
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
                width: 44%;
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
