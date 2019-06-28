<template>
    <div class="env-detail-wrapper">
        <div class="env-detail-header">
            <div class="title">
                <i class="bk-icon icon-arrows-left" @click="toEnvList"></i>
                <span class="header-text">{{ curEnvDetail.name }}</span>
            </div>
        </div>

        <div class="env-detail-container"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="env-parameter-tab">
                <div class="env-detail-nav">
                    <div class="tab-nav-item node-list" :class="{ activeItem: curItemTab === 'node' }"
                        @click="changeTab('node')">节点</div>
                    <div class="tab-nav-item config-item" :class="{ activeItem: curItemTab === 'config' }"
                        @click="changeTab('config')">配置项</div>
                    <div class="tab-nav-item base-item" :class="{ activeItem: curItemTab === 'base' }"
                        @click="changeTab('base')">基本信息</div>
                </div>

                <div class="node-content-wrapper" v-if="curItemTab === 'node'">
                    <div class="node-content-header">
                        <bk-button theme="primary" @click="importNewNode">导入</bk-button>
                    </div>
                    <div class="node-table" v-if="showContent && nodeList.length">
                        <div class="table-head">
                            <div class="table-node-item node-item-ip">名称</div>
                            <div class="table-node-item node-item-name">IP</div>
                            <div class="table-node-item node-item-type">来源/导入人</div>
                            <div class="table-node-item node-item-os">操作系统</div>
                            <div class="table-node-item node-item-area">区域</div>
                            <div class="table-node-item node-item-status">主机状态</div>
                            <div class="table-node-item node-item-handler node-header-head">操作</div>
                        </div>
                        <div class="table-node-body" ref="scrollBox">
                            <div class="table-row" v-for="(row, index) of nodeList" :key="index">
                                <div class="table-node-item node-item-ip">
                                    <span class="node-ip">{{ row.displayName }}</span>
                                </div>
                                <div class="table-node-item node-item-name">
                                    <span class="node-name">{{ row.ip }}</span>
                                </div>
                                <div class="table-node-item node-item-type">
                                    <div>
                                        <span class="node-name">{{ row.nodeType }}</span>
                                        <span>({{ row.createdUser }})</span>
                                    </div>
                                </div>
                                <div class="table-node-item node-item-os">
                                    <span class="node-type">{{ row.osName }}</span>
                                </div>
                                <div class="table-node-item node-item-area">
                                    <span v-if="row.gateway">{{ row.gateway }}</span>
                                    <span v-else>--</span>
                                </div>
                                <div class="table-node-item node-item-status">
                                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                                        v-if="row.nodeStatus === '正在创建中'">
                                        <div class="rotate rotate1"></div>
                                        <div class="rotate rotate2"></div>
                                        <div class="rotate rotate3"></div>
                                        <div class="rotate rotate4"></div>
                                        <div class="rotate rotate5"></div>
                                        <div class="rotate rotate6"></div>
                                        <div class="rotate rotate7"></div>
                                        <div class="rotate rotate8"></div>
                                    </div>
                                    <span class="node-status-icon normal-stutus-icon" v-if="row.nodeStatus === '正常'"></span>
                                    <span class="node-status-icon abnormal-stutus-icon"
                                        v-if="row.nodeStatus === '异常' || row.nodeStatus === '未知' || row.nodeStatus === '已删除' || row.nodeStatus === '失联'">
                                    </span>

                                    <span class="node-status">{{ row.nodeStatus }}</span>
                                </div>
                                <div class="table-node-item node-item-handler">
                                    <span class="node-delete delete-node-text" @click.stop="confirmDelete(row, index)">移除</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <bk-empty v-if="showContent && !nodeList.length"></bk-empty>
                </div>

                <div class="config-content-wrapper" v-if="curItemTab === 'config'">
                    <div class="config-content-header">
                        <bk-button theme="primary" :disabled="lastselectConfIndex > -1"
                            @click="createConfigItem">新增配置项</bk-button>
                    </div>
                    <div class="config-table" v-if="showContent && configList.length">
                        <div class="table-head config-head">
                            <div class="table-config-item config-item-key">键</div>
                            <div class="table-config-item config-item-value">值</div>
                            <div class="table-config-item config-item-type">类型</div>
                            <div class="table-config-item config-item-handler">操作</div>
                        </div>
                        <div class="table-config-body">
                            <div class="table-row config-row" v-for="(row, index) of configList" :key="index">
                                <div class="table-config-item config-item-key">
                                    <input type="text" class="bk-form-input config-input config-key-input" placeholder="请输入"
                                        v-if="row.isCreateItem || row.isEditItem"
                                        v-model="row.name"
                                        name="confName"
                                        @input="errorHandler.nameError = false"
                                        :class="{ 'is-danger': errorHandler.nameError }">
                                    <span class="config-name" v-else>{{ row.name }}</span>
                                </div>
                                <div class="table-config-item config-item-value">
                                    <input type="password" class="bk-form-input config-input config-value-input" placeholder="请输入"
                                        v-if="(!curIsPlaintext && (row.isCreateItem || row.isEditItem) && (row.isSecure === 'ciphertext'))"
                                        v-model="row.value"
                                        name="confvalue"
                                        @input="errorHandler.valueError = false"
                                        :class="{ 'is-danger': errorHandler.valueError }">
                                    <input type="text" class="bk-form-input config-input config-value-input" placeholder="请输入"
                                        v-if="(curIsPlaintext || row.isSecure === 'plaintext') && (row.isCreateItem || row.isEditItem)"
                                        v-model="row.value"
                                        name="confvalue"
                                        @input="errorHandler.valueError = false"
                                        :class="{ 'is-danger': errorHandler.valueError }">
                                    <i class="bk-icon" :class="curIsPlaintext ? 'icon-eye' : 'icon-hide'"
                                        v-if="(row.isCreateItem || row.isEditItem) && row.isSecure === 'ciphertext'"
                                        @click="curIsPlaintext = !curIsPlaintext"></i>
                                    <span class="config-name"
                                        v-if="(!row.isCreateItem && !row.isEditItem)">{{ row.secure ? '******' : row.value }}</span>
                                </div>
                                <div class="table-config-item config-item-type">
                                    <bk-select v-if="row.isCreateItem"
                                        class="config-text-type"
                                        popover-min-width="120"
                                        v-model="row.isSecure"
                                        @item-selected="secureSelected">
                                        <bk-option v-for="(option, cindex) in confTextType"
                                            :key="cindex"
                                            :id="option.label"
                                            :name="option.name">
                                        </bk-option>
                                    </bk-select>
                                    <span class="config-type" v-else>{{ row.secure ? '密文' : '明文' }}</span>
                                </div>
                                <div class="table-config-item config-item-handler">
                                    <div class="editing-handler" v-if="(row.isCreateItem || row.isEditItem)">
                                        <span class="config-edit" @click="saveEditConfig(row, index)">保存</span>
                                        <span class="text-type" @click="cancelEdit(row, index)">取消</span>
                                    </div>
                                    <div class="preview-handler" v-else>
                                        <span class="config-edit" @click="changeConfig(row, index)">编辑</span>
                                        <span class="config-edit" @click="deleteConfig(row, index)">删除</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <bk-empty v-if="showContent && !configList.length"></bk-empty>
                </div>
                <div class="base-message-wrapper" v-if="curItemTab === 'base'">
                    <form class="bk-form base-env-form" ref="modifyEnv">
                        <div class="bk-form-item">
                            <label class="bk-label env-item-label">名称</label>
                            <div class="bk-form-content env-item-content">
                                <div class="edit-content" v-if="isEditingName">
                                    <input type="text" class="bk-form-input env-name-input" placeholder="请输入"
                                        maxlength="30"
                                        name="envName"
                                        v-model="editEnvForm.name"
                                        v-validate="'required'"
                                        :class="{ 'is-danger': errors.has('envName') }">
                                </div>
                                <p v-else class="env-base cur-env-name"><span class="env-name-content">{{ curEnvDetail.name }}</span></p>
                                <div class="handler-btn">
                                    <i class="bk-icon icon-edit" v-if="!isEditingName" @click="toEditBaseForm('name')"></i>
                                    <span class="edit-base" v-if="isEditingName" @click="saveEnvDetail('name')">保存</span>
                                    <span class="edit-base" v-if="isEditingName" @click="cancelEnvDetail('name')">取消</span>
                                </div>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label env-item-label env-desc-label">环境描述</label>
                            <div class="bk-form-content env-item-content">
                                <div class="edit-content" v-if="isEditingDesc">
                                    <textarea class="bk-form-input env-desc-input" placeholder="请输入" name="envDesc" v-if="isEditingDesc"
                                        maxlength="100"
                                        v-model="editEnvForm.desc">
                                    </textarea>
                                </div>
                                <p v-else class="env-base cur-env-desc">
                                    <span v-if="curEnvDetail.desc" class="env-desc-content">{{ curEnvDetail.desc }}</span>
                                    <span v-else>--</span>
                                </p>
                                <div class="handler-btn">
                                    <i class="bk-icon icon-edit" v-if="!isEditingDesc" @click="toEditBaseForm('desc')"></i>
                                    <span class="edit-base" v-if="isEditingDesc" @click="saveEnvDetail('desc')">保存</span>
                                    <span class="edit-base" v-if="isEditingDesc" @click="cancelEnvDetail('desc')">取消</span>
                                </div>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label env-item-label env-desc-label">环境类型</label>
                            <div class="bk-form-content env-item-content">
                                <div class="edit-content" v-if="isEditingType">
                                    <bk-radio-group v-model="editEnvForm.type">
                                        <bk-radio :value="'DEV'" class="env-type-radio">部署-研发/测试环境</bk-radio>
                                        <bk-radio :value="'PROD'" class="env-type-radio">部署-生产环境</bk-radio>
                                    </bk-radio-group>
                                </div>
                                <p class="env-base type-content" v-else>
                                    <span v-if="curEnvDetail.envType === 'DEV'">部署-研发/测试环境</span>
                                    <span v-if="curEnvDetail.envType === 'PROD'">部署-生产环境</span>
                                    <span v-if="curEnvDetail.envType === 'BUILD'">构建环境</span>
                                </p>
                                <div class="handler-btn" v-if="curEnvDetail.envType !== 'BUILD'">
                                    <i class="bk-icon icon-edit" v-if="!isEditingType" @click="toEditBaseForm('type')"></i>
                                    <span class="edit-base" v-if="isEditingType" @click="saveEnvDetail('type')">保存</span>
                                    <span class="edit-base" v-if="isEditingType" @click="cancelEnvDetail('type')">取消</span>
                                </div>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label env-item-label env-desc-label">节点数</label>
                            <div class="bk-form-content env-item-content">
                                <p class="env-base">{{ curEnvDetail.nodeCount }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label env-item-label env-desc-label">创建时间</label>
                            <div class="bk-form-content env-item-content">
                                <p class="env-base">{{ localConvertTime(curEnvDetail.createdTime) }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item create-user-item">
                            <label class="bk-label env-item-label env-desc-label">创建人</label>
                            <div class="bk-form-content env-item-content">
                                <p class="env-base">{{ curEnvDetail.createdUser }}</p>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <node-select :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :change-created-user="changeCreatedUser"
            :row-list="importNodeList"
            :select-handlerc-conf="selectHandlercConf"
            :confirm-fn="confirmFn"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :cancel-fn="cancelFn"
            :query="query">
        </node-select>
    </div>
</template>

<script>
    import nodeSelect from '@/components/devops/environment/node-select-dialog'
    import empty from '@/components/common/empty'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            nodeSelect,
            'bk-empty': empty
        },
        data () {
            return {
                timer: -1,
                isOverflow: false,
                isEditingName: false,
                isEditingDesc: false,
                isEditingType: false,
                curIsPlaintext: false, // 明文/密文
                showContent: false, // 显示内容
                lastselectConfIndex: -1, // 最后选中的配置项索引
                curItemTab: 'node', // 当前tab(节点/配置项)
                importNodeList: [], // 导入的节点
                nodeList: [], // 环境的节点
                configList: [], // 配置项
                curEnvDetail: {}, // 当前环境信息
                editEnvForm: {
                    name: '',
                    desc: '',
                    type: ''
                },
                lastSelectConfig: {}, // 最后选中配置型obj
                loading: {
                    isLoading: false,
                    title: ''
                },
                nodeDialogLoading: {
                    isLoading: false,
                    title: ''
                },
                errorHandler: {
                    nameError: false,
                    valueError: false
                },
                // 节点选择弹窗
                nodeSelectConf: {
                    isShow: false,
                    quickClose: false,
                    hasHeader: false,
                    unselected: true,
                    importText: '导入'
                },
                // 搜索节点
                searchInfo: {
                    search: ''
                },
                // 选择节点
                selectHandlercConf: {
                    curTotalCount: 0,
                    curDisplayCount: 0,
                    selectedNodeCount: 0,
                    allNodeSelected: false,
                    searchEmpty: false
                },
                confTextType: [
                    { label: 'plaintext', name: '明文' },
                    { label: 'ciphertext', name: '密文' }
                ]
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            envHashId () {
                return this.$route.params.envId
            },
            curUserInfo () {
                return window.userInfo
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            importNodeList: {
                deep: true,
                handler: function (val) {
                    let curCount = 0
                    const isSelected = this.importNodeList.some(item => {
                        return item.isChecked === true && !item.isEixtEnvNode
                    })

                    if (isSelected) {
                        this.nodeSelectConf.unselected = false
                    } else {
                        this.nodeSelectConf.unselected = true
                    }

                    this.importNodeList.filter(item => {
                        if (item.isChecked && !item.isEixtEnvNode) curCount++
                    })

                    this.selectHandlercConf.selectedNodeCount = curCount
                    this.decideToggle()
                }
            }
        },
        async created () {
            // 获取环境详情
            await this.requestEnvDetail()
        },
        async mounted () {
            await this.init()
        },
        beforeDestroy () {
            clearTimeout(this.timer)
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取环境节点列表
             */
            async requestList () {
                try {
                    const res = await this.$store.dispatch('environment/requestEnvNodeList', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })

                    this.nodeList.splice(0, this.nodeList.length)
                    res.map(item => {
                        this.nodeList.push(item)
                    })

                    if (this.importNodeList.length) {
                        this.nodeList.filter(vv => {
                            this.importNodeList.filter(kk => {
                                if (vv.nodeHashId === kk.nodeHashId) {
                                    kk.isChecked = true
                                    kk.isEixtEnvNode = true
                                }
                            })
                        })
                    }

                    if (this.nodeList.length) {
                        this.loopCheck()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
                this.showContent = true
            },
            /**
             *  轮询环境的节点列表状态
             */
            async loopCheck () {
                const {
                    timer
                } = this
                let res

                clearTimeout(timer)

                for (let i = 0; i < this.nodeList.length; i++) {
                    const target = this.nodeList[i]
                    if (target.nodeType === 'BCS虚拟机' && (target.nodeStatus === '异常'
                        || target.nodeStatus === '正在创建中'
                        || target.nodeStatus === '未知'
                        || !target.agentStatus)) {
                        res = true
                        break
                    } else {
                        res = false
                    }
                }

                if (res) {
                    this.timer = setTimeout(async () => {
                        await this.requestList()
                    }, 10000)
                }
            },
            /**
             * 获取环境详情
             */
            async requestEnvDetail () {
                try {
                    const res = await this.$store.dispatch('environment/requestEnvDetail', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })

                    this.curEnvDetail = res
                    this.configList = res.envVars

                    this.configList.forEach(item => {
                        item.isCreateItem = false
                        item.isEditItem = false

                        if (item.secure) {
                            item.isSecure = 'ciphertext'
                        } else {
                            item.isSecure = 'plaintext'
                        }
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            /**
             * 获取弹窗节点列表
             */
            async requestNodeList () {
                this.nodeDialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })

                    this.importNodeList.splice(0, this.importNodeList.length)

                    res.map(item => {
                        item.isChecked = false
                        item.isDisplay = true
                        this.importNodeList.push(item)
                    })

                    this.importNodeList.filter(kk => {
                        this.nodeList.filter(vv => {
                            if (vv.nodeHashId === kk.nodeHashId) {
                                kk.isChecked = true
                                kk.isEixtEnvNode = true
                            }
                        })

                        if (this.curEnvDetail.envType === 'BUILD') {
                            if (kk.nodeType !== 'THIRDPARTY' || !kk.canUse) {
                                kk.isDisplay = false
                            }
                        } else {
                            if (kk.nodeType === 'THIRDPARTY' || !kk.canUse) {
                                kk.isDisplay = false
                            }
                        }
                    })

                    let curCount = 0

                    this.importNodeList.forEach(item => {
                        if (item.isDisplay) curCount++
                    })

                    this.selectHandlercConf.curTotalCount = curCount

                    const result = this.importNodeList.some(element => {
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
            toEnvList () {
                this.$router.push({ name: 'envList' })
            },
            /**
             * 删除节点
             */
            async confirmDelete (row) {
                const params = []
                const id = row.nodeHashId

                params.push(id)

                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `确定移除节点(${row.nodeId})？`)

                this.$bkInfo({
                    title: `移除`,
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteEnvNode', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: params
                            })

                            message = '删除成功'
                            theme = 'success'
                        } catch (err) {
                            message = err.data ? err.data.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 构建机信息
             */
            async changeCreatedUser (id, type) {
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
                            if (type) {
                                this.requestNodeList()
                            } else {
                                this.requestList()
                            }
                        }
                    }
                })
            },
            /**
             * 切换节点/配置项
             */
            changeTab (curTab) {
                this.curItemTab = curTab

                this.$nextTick(() => {
                    const obj = this.$el.querySelector('.config-item')

                    if (curTab === 'config') {
                        obj.className += ' ' + 'config-active'
                    } else {
                        obj.classList.remove('.config-active')
                    }
                })
            },
            /**
             * 明文/密文切换
             */
            secureSelected (val) {
                this.curIsPlaintext = val === 'plaintext'
            },
            validate (row) {
                let errorCount = 0
                if (!row.name) {
                    this.errorHandler.nameError = true
                    errorCount++
                }
                if (!row.value) {
                    this.errorHandler.valueError = true
                    errorCount++
                }
                if (errorCount > 0) {
                    return false
                }
                return true
            },
            /**
             * 保存编辑的配置项
             */
            async saveEditConfig (row, index) {
                const isValid = this.validate(row)

                if (!isValid) {
                    return
                }

                let message, theme
                const modifyEenv = {
                    name: this.curEnvDetail.name,
                    desc: this.curEnvDetail.desc,
                    envType: this.curEnvDetail.envType,
                    envVars: []
                }

                this.configList.forEach(item => {
                    const temp = {}
                    temp.name = item.name
                    temp.value = item.value
                    temp.secure = item.isSecure !== 'plaintext'
                    modifyEenv.envVars.push(temp)
                })

                try {
                    await this.$store.dispatch('environment/toModifyEnv', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: modifyEenv
                    })

                    message = '保存成功'
                    theme = 'success'
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.lastselectConfIndex = -1
                    this.requestEnvDetail()
                }
            },
            /**
             * 删除环境变量
             */
            async deleteConfig (row, index) {
                if (this.lastselectConfIndex === -1) {
                    const h = this.$createElement
                    const content = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, `确认删除该配置项?`)

                    this.$bkInfo({
                        title: `确认`,
                        subHeader: content,
                        confirmFn: async () => {
                            let message, theme
                            const modifyEenv = {
                                name: this.curEnvDetail.name,
                                desc: this.curEnvDetail.desc,
                                envType: this.curEnvDetail.envType,
                                envVars: []
                            }

                            const resArr = this.configList.slice(0)

                            resArr.splice(index, 1)

                            resArr.forEach(item => {
                                const temp = {}
                                temp.name = item.name
                                temp.value = item.value
                                temp.secure = item.isSecure !== 'plaintext'
                                modifyEenv.envVars.push(temp)
                            })

                            try {
                                await this.$store.dispatch('environment/toModifyEnv', {
                                    projectId: this.projectId,
                                    envHashId: this.envHashId,
                                    params: modifyEenv
                                })

                                message = '删除成功'
                                theme = 'success'
                            } catch (err) {
                                message = err.message ? err.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })

                                this.lastselectConfIndex = -1
                                this.requestEnvDetail()
                            }
                        }
                    })
                }
            },
            /**
             * 导入节点
             */
            async importEnvNode (nodeArr) {
                let message, theme
                const params = []

                this.nodeDialogLoading.isLoading = true
                this.nodeSelectConf.importText = '导入中...'

                nodeArr.map(item => {
                    params.push(item)
                })

                try {
                    await this.$store.dispatch('environment/importEnvNode', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: params
                    })

                    message = '导入成功'
                    theme = 'success'
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.nodeSelectConf.isShow = false
                    this.nodeDialogLoading.isLoading = false
                    this.nodeSelectConf.importText = '导入'
                    this.requestList()
                    this.requestEnvDetail()
                }
            },
            /**
             * 弹窗全选联动
             */
            decideToggle () {
                let curCount = 0
                let curCheckCount = 0

                this.importNodeList.forEach(item => {
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
            toggleAllSelect () {
                this.selectHandlercConf.allNodeSelected = !this.selectHandlercConf.allNodeSelected

                if (this.selectHandlercConf.allNodeSelected) {
                    this.importNodeList.forEach(item => {
                        if (item.isDisplay && !item.isEixtEnvNode) {
                            item.isChecked = true
                        }
                    })
                } else {
                    this.importNodeList.forEach(item => {
                        if (item.isDisplay && !item.isEixtEnvNode) {
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
                    this.importNodeList.forEach(item => {
                        const str = item.ip

                        if (this.curEnvDetail.envType === 'BUILD') {
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

                    const result = this.importNodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlercConf.searchEmpty = false
                    } else {
                        this.selectHandlercConf.searchEmpty = true
                    }
                } else {
                    this.selectHandlercConf.searchEmpty = false

                    if (this.curEnvDetail.envType === 'BUILD') {
                        this.importNodeList.forEach(item => {
                            if (item.nodeType === 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    } else {
                        this.importNodeList.forEach(item => {
                            if (item.nodeType !== 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    }
                }

                this.decideToggle()
            },
            toEditBaseForm (type) {
                if (type === 'name') {
                    this.isEditingName = true
                    this.editEnvForm.name = this.curEnvDetail.name
                } else if (type === 'desc') {
                    this.isEditingDesc = true
                    this.editEnvForm.desc = this.curEnvDetail.desc
                } else {
                    this.isEditingType = true
                    this.editEnvForm.type = this.curEnvDetail.envType
                }
            },
            async saveEnvDetail (type) {
                const valid = await this.$validator.validate()
                if ((type === 'name' && valid) || type !== 'name') {
                    let message, theme
                    const modifyEenv = {
                        envVars: []
                    }

                    this.configList.forEach(item => {
                        const temp = {}
                        temp.name = item.name
                        temp.value = item.value
                        temp.secure = item.isSecure !== 'plaintext'
                        modifyEenv.envVars.push(temp)
                    })

                    try {
                        if (type === 'name') {
                            if (this.editEnvForm.name) {
                                modifyEenv.name = this.editEnvForm.name
                                modifyEenv.desc = this.curEnvDetail.desc
                                modifyEenv.envType = this.curEnvDetail.envType

                                await this.$store.dispatch('environment/toModifyEnv', {
                                    projectId: this.projectId,
                                    envHashId: this.envHashId,
                                    params: modifyEenv
                                })

                                message = '保存成功'
                                theme = 'success'
                            }
                        } else if (type === 'desc') {
                            modifyEenv.name = this.curEnvDetail.name
                            modifyEenv.desc = this.editEnvForm.desc
                            modifyEenv.envType = this.curEnvDetail.envType

                            await this.$store.dispatch('environment/toModifyEnv', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: modifyEenv
                            })

                            message = '保存成功'
                            theme = 'success'
                        } else {
                            modifyEenv.name = this.curEnvDetail.name
                            modifyEenv.desc = this.curEnvDetail.desc
                            modifyEenv.envType = this.editEnvForm.type

                            await this.$store.dispatch('environment/toModifyEnv', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: modifyEenv
                            })

                            message = '保存成功'
                            theme = 'success'
                        }
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })

                        if (theme === 'success') {
                            this.requestEnvDetail()
                            if (type === 'name') {
                                this.curEnvDetail.name = modifyEenv.name
                                this.isEditingName = false
                            } else if (type === 'desc') {
                                this.curEnvDetail.desc = modifyEenv.desc
                                this.isEditingDesc = false
                            } else {
                                this.curEnvDetail.envType = modifyEenv.envType
                                this.isEditingType = false
                            }
                        }
                    }
                }
            },
            cancelEnvDetail (type) {
                if (type === 'name') {
                    this.isEditingName = false
                } else if (type === 'desc') {
                    this.isEditingDesc = false
                } else {
                    this.isEditingType = false
                }
            },
            /**
             * 编辑配置项
             */
            changeConfig (row, index) {
                if (this.lastselectConfIndex === -1) {
                    this.lastselectConfIndex = index
                    this.lastSelectConfig = row

                    this.configList.forEach((item, index) => {
                        if (item.name === row.name) {
                            this.curIsPlaintext = !item.secure
                            item.isEditItem = true
                        }
                    })

                    this.lastSelectConfig = JSON.parse(JSON.stringify(row))
                    this.configList = this.configList.concat([])
                }
            },
            /**
             * 取消编辑配置项
             */
            cancelEdit (row, index) {
                const target = this.lastSelectConfig

                if (target.isEditItem) {
                    target.isEditItem = false
                    this.configList.splice(index, 1, target)
                } else {
                    this.configList.shift()
                }

                this.errorHandler.nameError = false
                this.errorHandler.valueError = false
                this.lastselectConfIndex = -1
            },
            /**
             * 新增配置项
             */
            createConfigItem () {
                const newItem = {
                    name: '',
                    value: '',
                    isSecure: 'plaintext',
                    secure: false,
                    isCreateItem: true,
                    isEditItem: false
                }

                this.lastselectConfIndex = 0
                this.lastSelectConfig = newItem
                this.configList.unshift(newItem)
            },
            toInstall () {
                const url = `${DOCS_URL_PREFIX}/%E6%89%80%E6%9C%89%E6%9C%8D%E5%8A%A1/%E7%8E%AF%E5%A2%83%E7%AE%A1%E7%90%86/installGseAgentGuide.html`
                window.open(url, '_blank')
            },
            importNewNode () {
                this.searchInfo = {
                    search: ''
                }
                this.nodeSelectConf.isShow = true
                this.requestNodeList()
            },
            confirmFn () {
                if (!this.nodeDialogLoading.isLoading) {
                    const nodeArr = []

                    this.importNodeList.forEach(item => {
                        if (item.isChecked && !item.isEixtEnvNode) {
                            nodeArr.push(item.nodeHashId)
                        }
                    })

                    this.importEnvNode(nodeArr)
                }
            },
            cancelFn () {
                if (!this.nodeDialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
                    this.selectHandlercConf.searchEmpty = false
                    this.nodeSelectConf.importText = '导入'
                }
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
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

    .env-detail-wrapper {
        height: 100%;
        overflow: hidden;

        .env-detail-header {
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

        .env-detail-container {
            padding: 20px;
            height: 92%;
            overflow: auto;
        }

        .env-parameter-tab {
            border: 1px solid $borderWeightColor;
            height: 100%;
            overflow: hidden;
            background-color: #fff;

            .paas-ci-empty {
                height: 94%;
            }
        }

        .env-detail-nav {
            margin-bottom: 18px;
            height: 42px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: rgb(250, 251, 253);
        }

        .tab-nav-item {
            float: left;
            width: 100px;
            height: 100%;
            line-height: 42px;
            text-align: center;
            background-color: rgb(250, 251, 253);
            font-size: 14px;
            color: $fontWeightColor;
            cursor: pointer;
        }

        .activeItem {
            height: 42px;
            background: #fff;
            color: $primaryColor;
        }

        .node-list {
            border-right: 1px solid $borderWeightColor;
        }

        .base-item {
            border-left: 1px solid $borderWeightColor;
            border-right: 1px solid $borderWeightColor;
        }

        .config-active {
            border-right: none;
        }

        .config-content-wrapper,
        .node-content-wrapper {
            height: 94%;
            overflow: hidden;
        }

        .node-content-header,
        .config-content-header {
            padding: 0 20px;

            .bk-button {
                width: 76px;
                height: 32px;
                line-height: 30px;
            }
        }

        .config-content-header {

            .bk-button {
                padding: 0 8px;
                width: 90px;
            }
        }

        .node-table {
            height: calc(95% - 32px);
            overflow: auto;
        }

        // 表格滚动
        // .node-table {
        //     height: 94%;
        //     overflow: hidden;
        // }
        //
        // .table-node-body {
        //     height: 92%;
        //     overflow: auto;
        //     position: relative;
        // }

        .table-head,
        .table-row {
            padding: 0 20px;
            @extend %flex;
            height: 43px;
            font-size: 12px;
            color: #333C48;
        }

        .table-row {
            border-top: 1px solid $borderWeightColor;
            color: $fontWeightColor;
            font-size: 12px;

            &:last-child {
                border-bottom: 1px solid $borderWeightColor;
            }
        }

        .config-table {
            height: calc(95% - 32px);
            overflow: auto;
        }

        .config-row,
        .config-head {
            padding-right: 0
        }

        .node-item-os {
            flex: 3;
        }

        .node-item-ip,
        .node-item-name,
        .node-item-type,
        .node-item-agstatus {
            flex: 2;
        }

        .node-item-area,
        .node-item-status {
            flex: 1;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .bk-icon {
                margin-right: 6px;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        .node-item-handler {
            flex: 1;
            padding-right: 20px;
            text-align: right;
        }

        .node-header-head {
            position: relative;
            right: 16px;
            left: 2px;
        }

        .table-config-item {
            padding-right: 20px;
        }

        .config-item-key,
        .config-item-value,
        .config-item-type {
            flex: 5
        }

        .config-item-value {
            position: relative;

            .bk-icon {
                position: absolute;
                top: 9px;
                right: 30px;
                font-size: 16px;
                cursor: pointer;
            }
        }

        .config-item-handler {
            flex: 1;
            min-width: 88px;
        }

        .item-handler-head {
            position: relative;
            right: 34px;
            text-align: right;
        }

        .node-status-icon {
            display: inline-block;
            margin-left: 2px;
            width: 10px;
            height: 10px;
            border: 2px solid #30D878;
            border-radius: 50%;
            -webkit-border-radius: 50%;
        }

        .abnormal-stutus-icon {
            border-color: $failColor;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .node-delete,
        .config-edit,
        .text-type {
            color: $primaryColor;
            cursor: pointer;
        }

        .install-btn {
            color: $primaryColor;
            cursor: pointer;
        }

        .install-btn {
            margin-left: 4px;
        }

        .config-edit {
            margin-right: 10px;
        }

        .config-input {
            width: 100%;
            height: 32px;
            border-color: $lineColor;
            font-size: 12px;
        }

        .bk-selector,
        .bk-selector-input {
            width: 120px;
            height: 32px;
            font-size: 12px;
        }

        .conf-error-tips {
            position: absolute;
        }

        .config-text-type {
            width: 120px;
        }

        .is-danger {
            border-color: #ff5656;
            background-color: #fff4f4;
            color: #ff5656;
        }

        .base-message-wrapper {
            .base-env-form {
                margin: 0 20px;
                border: 1px solid $borderWeightColor;
            }
            .bk-form-item {
                margin-top: 6px;
                padding-bottom: 6px;
                padding-left: 20px;
                border-bottom: 1px solid $borderWeightColor;
                &:last-child {
                    border: none;
                }
            }
            .bk-label {
                width: 90px;
                font-weight: normal;
                &:after {
                    position: absolute;
                    content: '';
                    top: -6px;
                    left: 110px;
                    height: 84px;
                    width: 1px;
                    background-color: $borderWeightColor;
                }
            }
            .create-user-item {
                .bk-label:after {
                    height: 45px;
                }
            }
            .bk-form-content {
                display: flex;
                margin-left: 110px;
            }
            .env-name-input {
                width: 320px;
            }
            .env-desc-input {
                padding: 10px;
                width: 540px;
                min-height: 60px;
                line-height: 20px;
            }
            .env-base,
            .handler-btn {
                line-height: 32px;
                font-size: 14px;
            }
            .env-type-radio {
                position: relative;
                top: 6px;
                margin-right: 10px;
            }
            .handler-btn {
                display: flex;
                align-items: center;
                margin-left: 20px;
                .icon-edit {
                    &:hover {
                        color: $primaryColor;
                        cursor: pointer;
                    }
                }
            }
            .edit-base {
                margin-right: 8px;
                color: $primaryColor;
                cursor: pointer;
            }
            .env-desc-content,
            .env-name-content {
                display: inline-block;
                line-height: 18px;
                margin-top: 9px;
                max-width: 540px;
                word-break: break-all;
            }
        }
    }
</style>
