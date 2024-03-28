<template>
    <article class="agent-list-home" v-bkloading="{ isLoading }">
        <header class="agent-list-head">
            <bk-breadcrumb separator-class="bk-icon icon-angle-right">
                <bk-breadcrumb-item v-for="(item,index) in navList" :key="index" :to="item.link">{{item.title}}</bk-breadcrumb-item>
            </bk-breadcrumb>
        </header>

        <main class="agent-list-main" v-if="!isLoading">
            <section v-if="agentList.length">
                <div class="operate-agent">
                    <bk-button theme="primary" @click="goToAddAgent">{{$t('setting.agent.addAgent')}}</bk-button>
                    <bk-button theme="primary" class="import-agent" @click="importNewNode">{{$t('setting.agent.importAgent')}}</bk-button>
                </div>
                <bk-table class="agent-table"
                    :data="agentList"
                    :outer-border="false"
                    :header-border="false"
                    :header-cell-style="{ background: '#fafbfd' }"
                >
                    <bk-table-column :label="$t('displayName')" prop="displayName">
                        <template slot-scope="props">
                            <div class="bk-form-content node-item-content" v-if="props.row.nodeHashId === curEditAgentId">
                                <div class="edit-content">
                                    <input type="text" class="bk-form-input env-name-input"
                                        maxlength="30"
                                        name="nodeName"
                                        v-validate="'required'"
                                        v-model="curEditDisplayName"
                                        :class="{ 'is-danger': errors.has('nodeName') }">
                                    <div class="handler-btn">
                                        <span class="edit-base save" @click="saveEdit(props.row)">{{$t('save')}}</span>
                                        <span class="edit-base cancel" @click="cancelEdit(props.row.nodeHashId)">{{$t('cancel')}}</span>
                                    </div>
                                </div>
                            </div>
                            <div class="table-node-item node-item-id" v-else>
                                <span class="update-btn node-name"
                                    :title="props.row.displayName"
                                    @click="goToAgentDetail(props.row.nodeHashId)"
                                >{{ props.row.displayName || '-' }}</span>
                                <i class="bk-icon icon-edit" v-if="!isEditNodeStatus" @click="editNodeName(props.row)"></i>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('hostName')" prop="name"></bk-table-column>
                    <bk-table-column label="Ip" prop="ip"></bk-table-column>
                    <bk-table-column label="OS" prop="osName"></bk-table-column>
                    <bk-table-column :label="$t('status')" prop="nodeStatus"></bk-table-column>
                    <bk-table-column :label="$t('operation')" width="200" class-name="handler-btn">
                        <template slot-scope="props">
                            <span class="update-btn" @click="showDelete(props.row)">{{$t('setting.agent.removeAgentTips')}}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
            <section v-else class="table-empty">
                <h3>{{$t('setting.agent.emptyAgentTitle')}}</h3>
                <h5>{{$t('setting.agent.emptyAgentTips')}}</h5>
                <div>
                    <bk-button theme="primary" @click="goToAddAgent">{{$t('setting.agent.addSelfAgent')}}</bk-button>
                    <bk-button theme="primary" class="import-agent" @click="importNewNode">{{$t('setting.agent.importSelfAgent')}}</bk-button>
                </div>
            </section>
        </main>

        <node-select :node-select-conf="nodeSelectConf"
            :row-list="importNodeList"
            :select-handlerc-conf="selectHandlercConf"
            :confirm-fn="confirmFn"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :cancel-fn="cancelFn"
            :query="query">
        </node-select>

        <bk-dialog v-model="isShowDelete"
            theme="danger"
            :mask-close="false"
            :loading="isDeleteing"
            @confirm="deleteAgent"
            :title="$t('setting.agent.removeAgentTips')">
            {{$t('deleteTips')}} 【{{deleteRow.displayName}}】？
        </bk-dialog>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { setting } from '@/http'
    import nodeSelect from '@/components/setting/node-select-dialog'
    import nodeSelectMixin from '@/components/setting/node-select-mixin.js'

    export default {
        components: {
            nodeSelect
        },

        mixins: [nodeSelectMixin],

        data () {
            return {
                navList: [
                    { link: { name: 'agentPools' }, title: this.$t('setting.agent.agentPools') },
                    { link: '', title: this.$route.params.poolName }
                ],
                agentList: [],
                isLoading: false,
                isShowDelete: false,
                isDeleteing: false,
                deleteRow: {},
                isEditNodeStatus: false,
                curEditAgentId: '',
                curEditNodeDisplayName: ''
            }
        },

        computed: {
            ...mapState(['projectId']),

            envHashId () {
                return this.$route.params.poolId
            }
        },

        created () {
            this.getNodeList()
        },

        methods: {
            getNodeList () {
                this.isLoading = true
                setting.getNodeList(this.projectId, this.$route.params.poolId).then((res) => {
                    this.agentList = res
                    this.nodeList = res
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            showDelete (row) {
                this.isShowDelete = true
                this.deleteRow = row
            },

            deleteAgent () {
                this.isDeleteing = true
                setting.deleteEnvNode(this.projectId, this.$route.params.poolId, [this.deleteRow.nodeHashId]).then(() => {
                    this.isShowDelete = false
                    this.getNodeList()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isDeleteing = false
                })
            },

            goToAddAgent () {
                this.$router.push({
                    name: 'addAgent'
                })
            },

            goToAgentDetail (id) {
                this.$router.push({
                    name: 'agentDetail',
                    params: {
                        agentId: id
                    }
                })
            },

            editNodeName (node) {
                this.curEditDisplayName = node.displayName
                this.isEditNodeStatus = true
                this.curEditAgentId = node.nodeHashId
            },

            async saveEdit (node) {
                const valid = await this.$validator.validate()
                const displayName = this.curEditDisplayName.trim()
                if (valid) {
                    let message, theme
                    const params = {
                        displayName
                    }

                    setting.updateDisplayName(this.projectId, node.nodeHashId, params).then(() => {
                        message = 'Update successfully'
                        theme = 'success'
                    }).catch((err) => {
                        message = err.message ? err.message : err
                        theme = 'error'
                    }).finally(() => {
                        message && this.$bkMessage({
                            message,
                            theme
                        })
                        this.isEditNodeStatus = false
                        this.curEditAgentId = ''
                        this.curEditDisplayName = ''
                        this.getNodeList()
                    })
                }
            },

            cancelEdit () {
                this.isEditNodeStatus = false
                this.curEditAgentId = ''
                this.curEditNodeDisplayName = ''
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .agent-list-head {
        height: 49px;
        line-height: 49px;
        background: #fff;
        box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
        padding: 0 25.5px;
    }
    .agent-list-main {
        padding: 16px;
        height: calc(100% - 49px);
        overflow-y: auto;
        .operate-agent {
            margin-bottom: 20px;
        }
        .import-agent {
            margin-left: 10px;
        }
    }
    .agent-table {
        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;
            cursor: pointer;
            .bk-icon {
                margin-right: 6px;
            }
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

        .node-item-content {
            position: absolute;
            top: 6px;
            display: flex;
            width: 90%;
            min-width: 280px;
            margin-right: 12px;
            z-index: 2;
            .edit-content {
                display: flex;
                width: 100%;
            }
            .bk-form-input {
                height: 30px;
                font-size: 12px;
                min-width: 280px;
                padding-right: 74px;
            }
            .error-tips {
                font-size: 12px;
            }
            .handler-btn {
                display: flex;
                align-items: center;
                margin-left: 10px;
                position: absolute;
                color: #3a84ff;
                right: 11px;
                top: 8px;
                .edit-base {
                    cursor: pointer;
                }
                .save {
                    margin-right: 8px;
                }
            }
            .is-danger {
                border-color: #ff5656;
                background-color: #fff4f4;
            }
        }

        .node-item-id {
            display: flex;
        }

        td:first-child {
            .node-name {
                line-height: 14px;
                display: inline-block;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .icon-edit {
                position: relative;
                left: 4px;
                /* color: $fontColor; */
                cursor: pointer;
                display: none;
            }
            &:hover {
                .icon-edit {
                    display: inline-block;
                }
            }
        }
        
    }
</style>
