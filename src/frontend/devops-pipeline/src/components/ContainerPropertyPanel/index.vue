<template>
    <section v-if="container" slot="content" :class="{ &quot;readonly&quot;: !editable }" class="container-property-panel bk-form bk-form-vertical">
        <form-field :required="true" label="名称" :is-error="errors.has(&quot;name&quot;)" :error-msg="errors.first(&quot;name&quot;)">
            <vuex-input :disabled="!editable" input-type="text" placeholder="请输入名称" name="name" v-validate.initial="&quot;required&quot;" :value="container.name" :handle-change="handleContainerChange" />
        </form-field>

        <form v-if="isVmContainer(container)" v-bkloading="{ isLoading: !apps || !containerModalId }">
            <form-field label="构建资源类型">
                <selector
                    :disabled="!editable"
                    :handle-change="changeBuildResource"
                    :list="buildResourceTypeList"
                    :value="buildResourceType"
                    :clearable="false"
                    setting-key="type"
                    name="buildType"
                >
                    <template slot="props">
                        <div class="bk-selector-create-item" @click.stop.prevent="addThridSlave">
                            <i class="bk-icon icon-plus-circle"></i>
                            <i class="text">新增第三方构建机</i>
                        </div>
                    </template>
                </selector>
                <span class="bk-form-help" v-if="isPublicResourceType">由蓝盾官方提供的构建集群，你无需担心资源不足、安全等问题。<a target="_blank" :href="`${DOCS_URL_PREFIX}/所有服务/流水线/用户指南/publicBuild.html`">查看更多</a></span>
            </form-field>
            <form-field label="指定构建资源" v-if="!isPublicResourceType && containerModalId" :required="true" :is-error="errors.has(&quot;buildResource&quot;)" :error-msg="errors.first(&quot;buildResource&quot;)" :desc="buildResourceType === &quot;THIRD_PARTY_AGENT_ENV&quot; ? &quot;若环境下包含多个节点，则优先分配上次使用过的节点；若第一次使用，则随机分配&quot; : &quot;&quot;">
                <container-env-node :disabled="!editable"
                    :os="container.baseOS"
                    :container-id="containerModalId"
                    :build-resource-type="buildResourceType"
                    :toggle-visible="toggleVisible"
                    :handle-change="changeBuildResource"
                    :value="buildResource"
                    :has-error="errors.has(&quot;buildResource&quot;)"
                    v-validate.initial="&quot;required&quot;"
                    name="buildResource"
                />
            </form-field>
            <form-field label="指定工作空间" v-if="isThirdParty">
                <vuex-input :disabled="!editable" name="workspace" :value="container.dispatchType.workspace" :handle-change="changeBuildResource" placeholder="不填默认为 [Agent安装目录]/workspace/[流水线ID]/" />
            </form-field>
            <form-field class="container-app-field" v-if="showDependencies" label="依赖编译环境">
                <container-app-selector :disabled="!editable" class="app-selector-item" v-if="!hasBuildEnv" app="" version="" :handle-change="handleContainerAppChange" :apps="apps"></container-app-selector>
                <container-app-selector :disabled="!editable" v-else class="app-selector-item" v-for="(version, app) in container.buildEnv"
                    :key="app"
                    :app="app"
                    :version="version"
                    :handle-change="handleContainerAppChange"
                    :envs="container.buildEnv"
                    :apps="apps"
                    :remove-container-app="removeContainerApp"
                    :add-container-app="containerAppList.length > 0 ? addContainerApp : null"
                ></container-app-selector>
            </form-field>
            <div class="build-path-tips" v-if="hasBuildEnv">
                <div class="tips-icon"><i class="bk-icon icon-info-circle-shape"></i></div>
                <div class="tips-content">
                    <p class="tips-title">在选择了上述依赖环境后，我们将执行如下操作：</p>
                    <template v-for="(value, keys) in container.buildEnv">
                        <div class="tips-list" v-if="value" :key="keys">
                            <p class="tips-item">{{ appBinPath(value, keys) }}</p>
                            <p class="tips-item"
                                v-for="(env, envIndex) in appEnvs[keys]" :key="envIndex">{{ appEnvPath(value, keys, env) }}</p>
                        </div>
                    </template>
                </div>
            </div>
        </form>

        <section v-if="isTriggerContainer(container)">
            <version-config :disabled="!editable" :params="container.params" :build-no="container.buildNo" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></version-config>
            <build-params key="params" :disabled="!editable" :params="container.params" :addition-params="container.templateParams" setting-key="params" title="流水线变量" :build-no="container.buildNo" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></build-params>
            <build-params v-if="routeName === 'templateEdit'" key="templateParams" :disabled="!editable" :params="container.templateParams" :addition-params="container.params" setting-key="templateParams" title="模板常量" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></build-params>
        </section>

        <div>
            <div class="job-option">
                <job-option
                    v-if="!isTriggerContainer(container)"
                    :job-option="container.jobControlOption"
                    :update-container-params="handleContainerChange"
                    :set-parent-validate="setContainerValidate"
                    @setKeyValueValidate="setContainerValidate"
                    :disabled="!editable"
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

        <div v-if="!isTriggerContainer(container) && editable" class="control-bar">
            <bk-button theme="danger" @click="handleDelete">删除</bk-button>
        </div>
    </section>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import Vue from 'vue'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import ContainerAppSelector from './ContainerAppSelector'
    import ContainerEnvNode from './ContainerEnvNode'
    import BuildParams from './BuildParams'
    import VersionConfig from './VersionConfig'
    import JobOption from './JobOption'
    import JobMutual from './JobMutual'

    export default {
        name: 'container-property-panel',
        components: {
            VuexInput,
            FormField,
            ContainerAppSelector,
            BuildParams,
            VersionConfig,
            ContainerEnvNode,
            JobOption,
            JobMutual,
            Selector
        },
        props: {
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean
        },
        data () {
            return {
                DOCS_URL_PREFIX
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
                'execDetail',
                'getContainerModalId',
                'isThirdPartyContainer',
                'isPublicResource',
                'isDockerBuildResource'
            ]),
            ...mapState('soda', [
                'tstackWhiteList'
            ]),
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
            buildResource () {
                return this.container.dispatchType.value
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
            enableExt () {
                return this.$store.state.curProject.enableExternal || false
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
            if (this.isTriggerContainer(container) && this.container.templateParams === undefined) {
                Vue.set(container, 'templateParams', [])
            }
            if (this.isTriggerContainer(container) && this.container.buildNo === undefined) {
                Vue.set(container, 'buildNo', null)
            }
            if (!this.isTriggerContainer(container) && this.container.jobControlOption === undefined) {
                Vue.set(container, 'jobControlOption', {})
            }
            if (!this.isTriggerContainer(container) && this.container.mutexGroup === undefined) {
                Vue.set(container, 'mutexGroup', {})
            }
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'deleteContainer',
                'deleteStage'
            ]),
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
            handleDelete () {
                const { containerIndex, containers, stageIndex, stages } = this

                if (containers.length === 1) {
                    this.deleteStage({
                        stageIndex,
                        stages
                    })
                } else {
                    this.deleteContainer({
                        containerIndex,
                        containers
                    })
                }
            },
            changeBuildResource (name, value) {
                const emptyValueObj = name === 'buildType' ? { value: '' } : {}
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
    .container-property-panel {
        font-size: 14px;
        .control-bar {
            position: absolute;
            right: 12px;
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
            .bk-icon.icon-plus {
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
