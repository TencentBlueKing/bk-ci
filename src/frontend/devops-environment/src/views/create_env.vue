<template>
    <div class="environment-create">
        <div class="env-header">
            <div class="title">
                <i class="bk-icon icon-arrows-left" @click="toEnvList"></i>
                <span class="header-text">新建环境</span>
            </div>
        </div>

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
                <devops-form-item label="名称" :required="true" :property="'name'" :is-error="errors.has('env_name')" :error-msg="errors.first('env_name')">
                    <bk-input
                        class="env-name-input"
                        name="env_name"
                        maxlength="30"
                        placeholder="请输入"
                        v-model="createEnvForm.name"
                        v-validate="'required'">
                    </bk-input>
                </devops-form-item>
                <bk-form-item label="环境描述" :property="'desc'">
                    <bk-input
                        class="env-desc-input"
                        placeholder="请输入"
                        :type="'textarea'"
                        :rows="3"
                        :maxlength="100"
                        v-model="createEnvForm.desc">
                    </bk-input>
                </bk-form-item>
                <bk-form-item label="环境类型" class="env-type-item" :required="true" :property="'envType'">
                    <bk-radio-group v-model="createEnvForm.envType">
                        <bk-radio :value="'BUILD'">构建环境</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item label="节点来源" :required="true" :property="'source'">
                    <div class="env-source-content">
                        <div class="source-type-radio">
                            <bk-radio-group v-model="createEnvForm.source">
                                <bk-radio :value="'EXISTING'">第三方构建机</bk-radio>
                            </bk-radio-group>
                            <span class="preview-node-btn"
                                v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length > 0"
                                @click="toShowNodeList">选取节点</span>
                        </div>
                        <div class="empty-node-selected" v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length === 0">
                            <p class="empty-prompt">暂未选取节点，
                                <span class="show-node-dialog" @click="toShowNodeList">点击选取节点</span>
                            </p>
                            <div v-if="errorHandler.nodeHashIds" class="error-tips">节点不能为空</div>
                        </div>
                        <div class="selected-node-Preview" v-if="createEnvForm.source === 'EXISTING' && previewNodeList.length > 0">
                            <div class="node-table-message">
                                <div class="table-node-head">
                                    <div class="table-node-item node-item-ip">IP</div>
                                    <div class="table-node-item node-item-name">主机名</div>
                                    <div class="table-node-item node-item-type">节点类型</div>
                                    <div class="table-node-item node-item-status">节点状态</div>
                                    <div class="table-node-item node-item-agstatus">Agent状态</div>
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
                                            <span class="node-type">{{ getNodeTypeMap[row.nodeType] }}</span>
                                        </div>
                                        <div class="table-node-item node-item-status">
                                            <span class="node-name">{{ row.nodeStatus }}</span>
                                        </div>
                                        <div class="table-node-item node-item-agstatus">
                                            <span class="node-status" v-if="row.nodeType === 'BCSVM'"
                                                :class="row.agentStatus ? 'normal-status-node' : 'refresh-status-node' ">{{ row.agentStatus ? '正常' : '刷新中' }}
                                            </span>
                                            <span class="node-status" v-else
                                                :class="row.agentStatus ? 'normal-status-node' : 'abnormal-status-node' ">{{ row.agentStatus ? '正常' : '刷新中' }}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" title="提交" @click.stop.prevent="submit">提交</bk-button>
                    <bk-button theme="default" title="取消" @click="toEnvList">取消</bk-button>
                </bk-form-item>
            </bk-form>
        </section>
        <node-select :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :change-created-user="changeCreatedUser"
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
    import { mapGetters } from 'vuex'
    import nodeSelect from '@/components/devops/environment/node-select-dialog'
    import emptyTips from '@/components/devops/emptyTips'

    export default {
        components: {
            'empty-tips': emptyTips,
            nodeSelect
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
                    title: '数据加载中，请稍候'
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
                    importText: '导入'
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
                            message: '必填项',
                            trigger: 'blur'
                        }
                    ]
                },
                // 权限配置
                emptyTipsConfig: {
                    title: '没有权限',
                    desc: `你在该项目【环境管理】下没有【创建】权限，请切换项目访问或申请`,
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: '去申请权限'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapGetters('environment', [
                'getNodeTypeMap',
                'getNodeStatusMap'
            ]),
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

                    this.nodeList.filter(item => {
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
                const url = `/backend/api/perm/apply/subsystem/?client_id=environment&project_code=${this.projectId}&service_code=environment&role_creator=environment`
                window.open(url, '_blank')
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
                                if (target[i] && str.indexOf(target[i]) > -1 && item.nodeType === 'THIRDPARTY' && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        } else {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str.indexOf(target[i]) > -1 && item.nodeType !== 'THIRDPARTY' && item.canUse) {
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
                    const message = '请选择节点来源'
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
                                'name': this.createEnvForm.name.trim(),
                                'desc': this.createEnvForm.desc,
                                'envType': this.createEnvForm.envType,
                                'source': this.createEnvForm.source,
                                'envVars': []
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

                                message = '新增成功'
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
                                        'name': 'envList'
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

                    res.map(item => {
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

                        this.previewNodeList.filter(vv => {
                            this.nodeList.filter(kk => {
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
            },
            /**
             * 构建机信息
             */
            async changeCreatedUser (id) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `是否修改主机责任人为当前用户？`)

                this.$bkInfo({
                    title: `修改导入人`,
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        const params = {}
                        try {
                            await this.$store.dispatch('environment/changeCreatedUser', {
                                projectId: this.projectId,
                                nodeHashId: id,
                                params
                            })

                            message = '修改成功'
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
                            this.requestNodeList()
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
        .env-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .header-text {
                font-size: 16px;
            }
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
            padding-left: 14px;
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
            flex: 5;
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
            float: right;
            padding-right: 20px;
            line-height: 42px;
            width: 82px;
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
