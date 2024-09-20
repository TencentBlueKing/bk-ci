<template>
    <div class="environment-create">
        <content-header class="env-header">
            <div
                slot="left"
                class="title"
            >
                <i
                    class="devops-icon icon-arrows-left"
                    @click="toEnvList"
                ></i>
                <span class="header-text">{{ $t('environment.createEnvTitle') }}</span>
            </div>
        </content-header>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <empty-tips
                v-if="!hasPermission"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns"
            >
            </empty-tips>

            <bk-form
                :label-width="100"
                class="create-env-form"
                :model="createEnvForm"
                v-if="hasPermission && !loading.isLoading"
            >
                <devops-form-item
                    :label="$t('environment.envInfo.name')"
                    :required="true"
                    :property="'name'"
                    :is-error="errors.has('env_name')"
                    :error-msg="errors.first('env_name')"
                >
                    <bk-input
                        class="env-name-input"
                        name="env_name"
                        maxlength="30"
                        :placeholder="$t('environment.pleaseEnter')"
                        v-model="createEnvForm.name"
                        v-validate="'required'"
                    >
                    </bk-input>
                </devops-form-item>
                <bk-form-item
                    :label="$t('environment.envInfo.envRemark')"
                    :property="'desc'"
                >
                    <bk-input
                        class="env-desc-input"
                        :placeholder="$t('environment.pleaseEnter')"
                        :type="'textarea'"
                        :rows="3"
                        :maxlength="100"
                        v-model="createEnvForm.desc"
                    >
                    </bk-input>
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.envInfo.envType')"
                    class="env-type-item"
                    :required="true"
                    :property="'envType'"
                >
                    <bk-radio-group v-model="createEnvForm.envType">
                        <bk-radio
                            v-for="envRadio in envTypeEnums"
                            :key="envRadio.key"
                            :value="envRadio.key"
                        >
                            {{ $t(`environment.envInfo.${envRadio.label}`) }}
                        </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.nodeInfo.nodeSource')"
                    :required="true"
                    :property="'source'"
                >
                    <div
                        v-if="previewNodeList.length > 0"
                        class="source-type-radio"
                    >
                        <!-- <bk-radio-group v-model="createEnvForm.source">
                                <bk-radio :value="'EXISTING'" v-if="createEnvForm.envType !== 'BUILD'">{{ $t('environment.envInfo.existingNode') }}</bk-radio>
                                <bk-radio :value="'EXISTING'" v-else>{{ $t('environment.thirdPartyBuildMachine') }}</bk-radio>
                            </bk-radio-group> -->
                        <span
                            class="preview-node-btn"
                            @click="toShowNodeList"
                        >
                            {{ $t('environment.nodeInfo.selectNode') }}
                        </span>
                    </div>
                    <bk-table
                        :data="previewTableData"
                    >
                        <bk-table-column
                            label="IP"
                            prop="ip"
                        />
                        <bk-table-column
                            :label="$t('environment.nodeInfo.hostName')"
                            prop="name"
                        />
                        <bk-table-column
                            :label="$t('environment.nodeInfo.nodeType')"
                            prop="nodeTypeLabel"
                        />
                        <bk-table-column
                            :label="$t('environment.nodeInfo.nodeStatus')"
                            prop="nodeStatusLabel"
                        />
                        <template #empty>
                            <p class="empty-prompt">
                                {{ $t('environment.nodeInfo.notyetNode') }}，
                                <span
                                    class="show-node-dialog"
                                    @click="toShowNodeList"
                                >{{ $t('environment.nodeInfo.clickSelectNode') }}</span>
                            </p>
                            <div
                                v-if="errorHandler.nodeHashIds"
                                class="error-tips"
                            >
                                {{ $t('environment.nodeInfo.haveToNeedNode') }}
                            </div>
                        </template>
                    </bk-table>
                </bk-form-item>
                <bk-form-item>
                    <bk-button
                        theme="primary"
                        :title="$t('environment.submit')"
                        @click.stop.prevent="submit"
                    >
                        {{ $t('environment.submit') }}
                    </bk-button>
                    <bk-button
                        theme="default"
                        :title="$t('environment.cancel')"
                        @click="toEnvList"
                    >
                        {{ $t('environment.cancel') }}
                    </bk-button>
                </bk-form-item>
            </bk-form>
        </section>
        <node-select
            :title="nodeSelectTitle"
            :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :change-created-user="changeCreatedUser"
            :row-list="nodeList"
            :select-handler-conf="selectHandlercConf"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :confirm-fn="confirmFn"
            :cancel-fn="cancelFn"
            :query="query"
        >
        </node-select>
    </div>
</template>

<script>
    import emptyTips from '@/components/devops/emptyTips'
    import { ENV_RESOURCE_ACTION, ENV_RESOURCE_TYPE } from '../utils/permission'

    import nodeSelect from '@/components/devops/environment/node-select-dialog'
    export default {
        components: {
            nodeSelect,
            'empty-tips': emptyTips
        },
        data () {
            return {
                cacheNodeSource: 'EXISTING',
                hasPermission: true, // 创建权限
                nodeList: [], // 节点列表
                buildNodeList: [], // 已选构建节点
                devNodeList: [], // 已选非构建节点列表
                previewNodeList: [], // 选中节点预览
                // 表单校验
                errorHandler: {
                    clusterId: false,
                    instanceCount: false,
                    nodeHashIds: false,
                    exceedCount: false
                },
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                },
                nodeDialogLoading: {
                    isLoading: false,
                    title: ''
                },
                // 节点选择配置
                selectHandlercConf: {
                    allNodeSelected: false,
                    searchEmpty: false
                },
                // 节点弹窗
                nodeSelectConf: {
                    isShow: false,
                    quickClose: false,
                    unselected: true,
                    importText: this.$t('environment.import')
                },
                // 搜索节点
                searchInfo: {
                    search: ''
                },
                // 创建表单
                createEnvForm: {
                    name: '',
                    desc: '',
                    envType: 'BUILD',
                    source: 'EXISTING',
                    bcsVmParam: {
                        clusterId: '',
                        vmModelId: '',
                        imageId: '',
                        instanceCount: 0,
                        validity: 1
                    }
                },
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('environment.requiredField'),
                            trigger: 'blur'
                        }
                    ]
                },
                // 权限配置
                emptyTipsConfig: {
                    title: this.$t('environment.noPermission'),
                    desc: this.$t('environment.envInfo.noCreateEnvPermissionTips'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('environment.switchProject')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.applyPermission,
                            text: this.$t('environment.applyPermission')
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            curUserInfo () {
                return window.userInfo
            },
            envTypeEnums () {
                return [
                    {
                        key: 'BUILD',
                        label: 'buildEnvType'
                    },
                    ...(this.isExtendTx
                        ? [
                            {
                                key: 'DEV',
                                label: 'devEnvType'
                            },
                            {
                                key: 'PROD',
                                label: 'testEnvType'
                            },
                            {
                                key: 'DEVX',
                                label: 'cloudDesktopType'
                            }
                        ]
                        : [])
                ]
            },
            previewTableData () {
                return this.previewNodeList.map(item => ({
                    ...item,
                    nodeTypeLabel: this.$t('environment.nodeTypeMap')[item.nodeType],
                    nodeStatusLabel: this.$t('environment.nodeStatusMap')[item.nodeStatus]
                }))
            },
            nodeSelectTitle () {
                const typeLabel = `environment.envInfo.${this.createEnvForm.envType === 'DEVX' ? 'DEVX' : 'buildEnvType'}`
                
                return `${this.createEnvForm?.name || this.$t('environment.createEnvTitle')}-导入${this.$t(typeLabel)}`
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            nodeList: {
                deep: true,
                handler: function (val) {
                    const isSelected = this.nodeList.some(item => item.isChecked)

                    this.nodeSelectConf.unselected = !isSelected

                    this.decideToggle()
                }
            },
            'createEnvForm.envType' (val) {
                if (val === 'BUILD') {
                    this.previewNodeList = this.buildNodeList
                } else {
                    this.previewNodeList = this.devNodeList
                }
            }
            
        },
        async created () {
            await this.requestPermission()
        },
        methods: {
            toEnvList () {
                this.$router.push({ name: 'envList' })
            },
            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },
            applyPermission () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: ENV_RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: ENV_RESOURCE_ACTION.CREATE
                })
            },
            /**
             * 弹窗全选联动
             */
            decideToggle () {
                this.selectHandlercConf.allNodeSelected = this.nodeList.every(item => item.isChecked)
            },
            /**
             * 节点全选
             */
            toggleAllSelect (data) {
                this.selectHandlercConf.allNodeSelected = data
                this.nodeList.forEach(item => {
                    if (item.isDisplay) {
                        item.isChecked = this.selectHandlercConf.allNodeSelected
                    }
                })
            },
            detectNodeType (nodeType) {
                return ['THIRDPARTY', 'DEVCLOUD'].includes(nodeType)
            },
            /**
             * 搜索节点
             */
            query (target) {
                if (target.length) {
                    target.filter(item => {
                        return item && item.length
                    })
                    this.nodeList.forEach(item => {
                        const str = item.ip
                        const nodeTypeFlag = this.detectNodeType(item.nodeType)
                        if (this.createEnvForm.envType === 'BUILD') {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && nodeTypeFlag && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        } else {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && !(nodeTypeFlag) && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        }
                    })

                    this.selectHandlercConf.searchEmpty = !this.nodeList.some(element => element.isDisplay)
                } else {
                    this.selectHandlercConf.searchEmpty = false
                    
                    this.nodeList.forEach(item => {
                        const nodeTypeFlag = this.detectNodeType(item.nodeType)
                        if (this.createEnvForm.envType === 'BUILD') {
                            item.isDisplay = nodeTypeFlag && item.canUse
                        } else {
                            item.isDisplay = !nodeTypeFlag && item.canUse
                        }
                    })
                }
                this.decideToggle()
            },
            /**
             * 判断节点是否已选
             */
            checkIsEixt (id, type) {
                let results

                for (let i = 0; i < this.previewNodeList.length; i++) {
                    if (type === 'BUILD') {
                        if (this.buildNodeList[i].nodeHashId === id) {
                            results = true
                            break
                        }
                    } else {
                        if (this.devNodeList[i].nodeHashId === id) {
                            results = true
                            break
                        }
                    }
                }
                return results
            },
            /**
             * 确认选中导入节点
             */
            confirmFn () {
                const curEnv = this.createEnvForm.envType
                
                if (curEnv === 'BUILD') {
                    this.nodeList.forEach(item => {
                        const nodeTypeFlag = this.detectNodeType(item.nodeType)
                        if (item.isChecked && !this.checkIsEixt(item.nodeHashId, curEnv) && nodeTypeFlag) {
                            this.buildNodeList.push(item)
                        }

                        if (!item.isChecked && this.checkIsEixt(item.nodeHashId, curEnv) && nodeTypeFlag) {
                            for (let i = this.buildNodeList.length - 1; i >= 0; i--) {
                                if (this.buildNodeList[i].nodeHashId === item.nodeHashId) {
                                    this.buildNodeList.splice(i, 1)
                                }
                            }
                        }
                    })

                    this.previewNodeList = this.buildNodeList
                } else {
                    this.nodeList.forEach(item => {
                        const nodeTypeFlag = this.detectNodeType(item.nodeType)
                        if (item.isChecked && !this.checkIsEixt(item.nodeHashId, curEnv) && !nodeTypeFlag) {
                            this.devNodeList.push(item)
                        }

                        if (!item.isChecked && this.checkIsEixt(item.nodeHashId, curEnv) && !nodeTypeFlag) {
                            for (let i = this.devNodeList.length - 1; i >= 0; i--) {
                                if (this.devNodeList[i].nodeHashId === item.nodeHashId) {
                                    this.devNodeList.splice(i, 1)
                                }
                            }
                        }
                    })

                    this.previewNodeList = this.devNodeList
                }

                this.nodeSelectConf.isShow = false
            },
            cancelFn () {
                this.nodeSelectConf.isShow = false
                this.selectHandlercConf.searchEmpty = false
            },
            toShowNodeList () {
                this.searchInfo = {
                    search: ''
                }
                this.nodeSelectConf.isShow = true
                this.requestNodeList()
            },
            /**
             * 表单校验
             */
            validate () {
                let errorCount = 0

                if (!this.previewNodeList.length) {
                    this.errorHandler.nodeHashIds = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }
                return true
            },
            /**
             * 提交表单
             */
            submit () {
                const isValid = this.validate()

                this.$validator.validateAll().then(async (result) => {
                    if (isValid && result) {
                        let message, theme
                        const createEnv = {
                            name: this.createEnvForm.name.trim(),
                            desc: this.createEnvForm.desc,
                            envType: this.createEnvForm.envType,
                            source: this.createEnvForm.source,
                            envVars: []
                        }

                        if (this.createEnvForm.source === 'CREATE') {
                            createEnv.bcsVmParam = this.createEnvForm.bcsVmParam
                        } else {
                            const nodeHashIds = []

                            this.previewNodeList.forEach(item => {
                                nodeHashIds.push(item.nodeHashId)
                            })

                            createEnv.nodeHashIds = nodeHashIds
                        }

                        this.loading.isLoading = true

                        try {
                            await this.$store.dispatch('environment/createNewEnv', {
                                projectId: this.projectId,
                                params: createEnv
                            })

                            message = this.$t('environment.successfullyAdded')
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })

                            this.loading.isLoading = false

                            if (theme === 'success') {
                                this.$router.push({
                                    name: 'envList'
                                })
                            }
                        }
                    }
                })
            },
            /**
             * 是否拥有创建环境权限
             */
            async requestPermission () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestPermission', {
                        projectId: this.projectId
                    })

                    this.hasPermission = res
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取节点
             */
            async requestNodeList () {
                this.nodeDialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId,
                        params: {
                            page: -1
                        }
                    })
                    const selectedNodesMap = this.previewNodeList.reduce((acc, item) => {
                        acc[item.nodeHashId] = 1
                        return acc
                    }, {})

                    this.nodeList = res.records.map(item => {
                        item.isChecked = !!selectedNodesMap[item.nodeHashId]
                        const nodeTypeFlag = this.detectNodeType(item.nodeType)

                        if (this.createEnvForm.envType === 'BUILD') {
                            item.isDisplay = nodeTypeFlag && item.canUse
                        } else {
                            item.isDisplay = !nodeTypeFlag && item.canUse
                        }
                        return item
                    })

                    this.selectHandlercConf.searchEmpty = !this.nodeList.some(element => element.isDisplay)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.nodeDialogLoading.isLoading = false
                }
            },
            async changeCreatedUser (id) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('environment.nodeInfo.modifyOperatorTips')}`)

                this.$bkInfo({
                    title: this.$t('environment.nodeInfo.modifyImporter'),
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        
                        try {
                            await this.$store.dispatch('environment/changeCreatedUser', {
                                projectId: this.projectId,
                                nodeHashId: id
                            })

                            message = this.$t('environment.successfullyModified')
                            theme = 'success'
                        } catch (err) {
                            const message = err.message ? err.message : err
                            const theme = 'error'

                            this.$bkMessage({
                                message,
                                theme
                            })
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestList()
                        }
                    }
                })
            }
        }
    }
</script>

<style lang='scss'>
    @import './../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }
    .environment-create {
        height: 100%;
        overflow: hidden;
        .title {
            .icon-arrows-left {
                margin-right: 4px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
        }
        .create-env-form {
            padding: 40px 20px;
            .bk-form-radio {
                margin-right: 20px;
            }
        }

        .env-name-input,
        .env-desc-input {
            width: 540px;
        }

        .env-type-item {
            line-height: 28px;
        }

        .env-source-content {
            width: 926px;
            height: 346px;
            border: 1px solid $borderWeightColor;
        }

        .source-type-radio {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 20px 0 14px;
            height: 42px;
            line-height: 38px;
            border-bottom: 1px solid $borderWeightColor;

            .bk-form-radio {
                line-height: 36px;
            }
        }

        .empty-node-selected {
            text-align: center;
        }

        .empty-prompt {
            display: inline-block;
            color: $fontLighterColor;
        }

        .show-node-dialog {
            cursor: pointer;
            color: $primaryColor;
        }

        .handler-result-step {
            margin: 20px 0 26px 116px;

            .bk-button {
                width: 90px;
            }
        }

        .node-item-name {
            flex: 3;
        }

        .node-item-ip,
        .node-item-status,
        .node-item-type,
        .node-item-agstatus {
            flex: 2;
        }

        .node-item-agstatus {
            text-align: right;
        }

        .preview-node-btn {
            flex-shrink: 0;
            cursor: pointer;
            color: $primaryColor;
            font-size: 14px;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .refresh-status-node {
            color: $primaryColor;
        }

        .unable-import {
            border-color: #e6e6e6;
            background: #fafafa;
            color: #ccc;
        }
    }
</style>
