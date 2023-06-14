<template>
    <div class="environment-create">
        <content-header class="env-header">
            <div slot="left" class="title">
                <i class="devops-icon icon-arrows-left" @click="toEnvList"></i>
                <span class="header-text">{{ `${$t('environment.createEnvTitle')}` }}</span>
            </div>
        </content-header>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">

            <empty-tips v-if="!hasPermission"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
            </empty-tips>

            <bk-form :label-width="100" class="create-env-form" :model="createEnvForm" v-if="hasPermission && !loading.isLoading">
                <devops-form-item :label="$t('environment.envInfo.name')" :required="true" :property="'name'" :is-error="errors.has('env_name')" :error-msg="errors.first('env_name')">
                    <bk-input
                        class="env-name-input"
                        name="env_name"
                        maxlength="30"
                        :placeholder="$t('environment.pleaseEnter')"
                        v-model="createEnvForm.name"
                        v-validate="'required'">
                    </bk-input>
                </devops-form-item>
                <bk-form-item :label="$t('environment.envInfo.envRemark')" :property="'desc'">
                    <bk-input
                        class="env-desc-input"
                        :placeholder="$t('environment.pleaseEnter')"
                        :type="'textarea'"
                        :rows="3"
                        :maxlength="100"
                        v-model="createEnvForm.desc">
                    </bk-input>
                </bk-form-item>
                <!-- <bk-form-item :label="$t('environment.envInfo.envType')" class="env-type-item" :required="true" :property="'envType'">
                    <bk-radio-group v-model="createEnvForm.envType">
                        <bk-radio :value="'BUILD'">{{ $t('environment.envInfo.buildEnvType') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item> -->
                <bk-form-item :label="$t('environment.nodeInfo.nodeSource')" :required="true" :property="'source'">
                    <div class="env-source-content">
                        <!-- <div class="source-type-radio">
                            <bk-radio-group v-model="createEnvForm.source">
                                <bk-radio :value="'EXISTING'">{{ $t('environment.thirdPartyBuildMachine') }}</bk-radio>
                            </bk-radio-group>
                            <span class="preview-node-btn"
                                v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length > 0"
                                @click="toShowNodeList"
                            >
                                {{ $t('environment.nodeInfo.selectNode') }}
                            </span>
                        </div> -->
                        <div class="empty-node-selected" v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length === 0">
                            <p class="empty-prompt">{{ $t('environment.nodeInfo.notyetNode') }}，
                                <span class="show-node-dialog" @click="toShowNodeList">{{ $t('environment.nodeInfo.clickSelectNode') }}</span>
                            </p>
                            <div v-if="errorHandler.nodeHashIds" class="error-tips">{{ $t('environment.nodeInfo.haveToNeedNode') }}</div>
                        </div>
                        <div class="selected-node-Preview" v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length > 0">
                            <div class="node-table-message">
                                <div class="table-node-head">
                                    <div class="table-node-item node-item-ip">IP</div>
                                    <div class="table-node-item node-item-name">{{ $t('environment.nodeInfo.cpuName') }}</div>
                                    <div class="table-node-item node-item-type">{{ $t('environment.nodeInfo.nodeType') }}</div>
                                    <div class="table-node-item node-item-status">{{ $t('environment.nodeInfo.nodeStatus') }}</div>
                                </div>
                                <div class="table-node-body">
                                    <div class="table-node-row" v-for="(row, index) of previewNodeList" :key="index">
                                        <div class="table-node-item node-item-ip">
                                            <span class="node-ip">{{ row.ip }}</span>
                                        </div>
                                        <div class="table-node-item node-item-name">
                                            <span class="node-name">{{ row.name }}</span>
                                        </div>
                                        <div class="table-node-item node-item-type">
                                            <span class="node-type">{{ $t('environment.nodeTypeMap')[row.nodeType] }}</span>
                                        </div>
                                        <div class="table-node-item node-item-status">
                                            <span class="node-name">{{ $t('environment.nodeStatusMap')[row.nodeStatus] }}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" :title="$t('environment.submit')" @click.stop.prevent="submit">{{ $t('environment.submit') }}</bk-button>
                    <bk-button theme="default" :title="$t('environment.cancel')" @click="toEnvList">{{ $t('environment.cancel') }}</bk-button>
                </bk-form-item>
            </bk-form>
        </section>
        <node-select :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :row-list="nodeList"
            :select-handlerc-conf="selectHandlercConf"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :confirm-fn="confirmFn"
            :cancel-fn="cancelFn"
            :query="query">
        </node-select>
    </div>
</template>

<script>
    import emptyTips from '@/components/devops/emptyTips'

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
                    curTotalCount: 0,
                    curDisplayCount: 0,
                    selectedNodeCount: 0,
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
                            handler: this.goToApplyPerm,
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
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            nodeList: {
                deep: true,
                handler: function (val) {
                    let curCount = 0
                    const isSelected = this.nodeList.some(item => {
                        return item.isChecked === true
                    })

                    if (isSelected) {
                        this.nodeSelectConf.unselected = false
                    } else {
                        this.nodeSelectConf.unselected = true
                    }

                    this.nodeList.forEach(item => {
                        if (item.isChecked) curCount++
                    })

                    this.selectHandlercConf.selectedNodeCount = curCount
                    this.decideToggle()
                }
            },
            'createEnvForm.envType' (val) {
                if (val === 'BUILD') {
                    this.createEnvForm.source = 'EXISTING'
                    this.previewNodeList = this.buildNodeList
                } else {
                    this.previewNodeList = this.devNodeList
                    this.createEnvForm.source = this.cacheNodeSource
                }
            },
            'createEnvForm.source' (val) {
                if (this.createEnvForm.envType !== 'BUILD') {
                    this.cacheNodeSource = val
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
            goToApplyPerm () {
                this.applyPermission(this.$permissionActionMap.create, this.$permissionResourceMap.environment, [{
                    id: this.projectId,
                    type: this.$permissionResourceTypeMap.PROJECT
                }])
                // const url = `/backend/api/perm/apply/subsystem/?client_id=environment&project_code=${this.projectId}&service_code=environment&role_creator=environment`
                // window.open(url, '_blank')
            },
            /**
             * 弹窗全选联动
             */
            decideToggle () {
                let curCount = 0
                let curCheckCount = 0

                this.nodeList.forEach(item => {
                    if (item.isDisplay) {
                        curCount++
                        if (item.isChecked) curCheckCount++
                    }
                })

                this.selectHandlercConf.curDisplayCount = curCount

                if (curCount === curCheckCount) {
                    this.selectHandlercConf.allNodeSelected = true
                } else {
                    this.selectHandlercConf.allNodeSelected = false
                }
            },
            /**
             * 节点全选
             */
            toggleAllSelect (data) {
                this.selectHandlercConf.allNodeSelected = data

                if (this.selectHandlercConf.allNodeSelected) {
                    this.nodeList.forEach(item => {
                        if (item.isDisplay) {
                            item.isChecked = true
                        }
                    })
                } else {
                    this.nodeList.forEach(item => {
                        if (item.isDisplay) {
                            item.isChecked = false
                        }
                    })
                }
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

                        if (this.createEnvForm.envType === 'BUILD') {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && item.nodeType === 'THIRDPARTY' && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        } else {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && item.nodeType !== 'THIRDPARTY' && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        }
                    })

                    const result = this.nodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlercConf.searchEmpty = false
                    } else {
                        this.selectHandlercConf.searchEmpty = true
                    }
                } else {
                    this.selectHandlercConf.searchEmpty = false

                    if (this.createEnvForm.envType === 'BUILD') {
                        this.nodeList.forEach(item => {
                            if (item.nodeType === 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    } else {
                        this.nodeList.forEach(item => {
                            if (item.nodeType !== 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    }
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
                        if (item.isChecked && !this.checkIsEixt(item.nodeHashId, curEnv) && item.nodeType === 'THIRDPARTY') {
                            this.buildNodeList.push(item)
                        }

                        if (!item.isChecked && this.checkIsEixt(item.nodeHashId, curEnv) && item.nodeType === 'THIRDPARTY') {
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
                        if (item.isChecked && !this.checkIsEixt(item.nodeHashId, curEnv) && item.nodeType !== 'THIRDPARTY') {
                            this.devNodeList.push(item)
                        }

                        if (!item.isChecked && this.checkIsEixt(item.nodeHashId, curEnv) && item.nodeType !== 'THIRDPARTY') {
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
                if (this.createEnvForm.source === 'CREATE') {
                    const message = this.$t('environment.nodeInfo.selectNodeSource')
                    const theme = 'warning'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } else {
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
                }
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
                        projectId: this.projectId
                    })

                    this.nodeList.splice(0, this.nodeList.length)

                    res.forEach(item => {
                        item.isChecked = false

                        if (this.createEnvForm.envType === 'BUILD') {
                            if (item.nodeType !== 'THIRDPARTY' || !item.canUse) {
                                item.isDisplay = false
                            } else {
                                item.isDisplay = true
                            }
                        } else {
                            if (item.nodeType === 'THIRDPARTY' || !item.canUse) {
                                item.isDisplay = false
                            } else {
                                item.isDisplay = true
                            }
                        }

                        this.nodeList.push(item)

                        this.previewNodeList.forEach(vv => {
                            this.nodeList.forEach(kk => {
                                if (vv.nodeHashId === kk.nodeHashId) {
                                    kk.isChecked = true
                                }
                            })
                        })
                    })

                    let curCount = 0

                    this.nodeList.forEach((item) => {
                        if (item.isDisplay) curCount++
                    })

                    this.selectHandlercConf.curTotalCount = curCount

                    const result = this.nodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlercConf.searchEmpty = false
                    } else {
                        this.selectHandlercConf.searchEmpty = true
                    }
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
        }

        .empty-node-selected {
            text-align: center;
        }

        .empty-prompt {
            display: inline-block;
            margin-top: 116px;
            color: $fontLigtherColor;
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

        .table-node-body {
            height: 258px;
            overflow: auto;
        }

        .table-node-head,
        .table-node-row {
            padding: 0 20px;
            @extend %flex;
            height: 43px;
            font-size: 14px;
            color: #333C48;
        }

        .table-node-row {
            border-top: 1px solid $borderWeightColor;
            color: $fontWeightColor;
            font-size: 12px;
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
