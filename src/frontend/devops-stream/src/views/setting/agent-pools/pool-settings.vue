<template>
    <article class="pool-setting-home" v-bkloading="{ isLoading }">
        <header class="pool-setting-head">
            <bk-breadcrumb separator-class="bk-icon icon-angle-right">
                <bk-breadcrumb-item v-for="(item,index) in navList" :key="index" :to="item.link">{{item.title}}</bk-breadcrumb-item>
            </bk-breadcrumb>
        </header>

        <main class="pool-setting-main" v-if="!isLoading">
            <section v-if="shareList.length">
                <div class="operate-agent">
                    <bk-button @click="toLinkShare('PROJECT')">{{$t('setting.agent.linkProject')}}</bk-button>
                    <bk-button @click="toLinkShare('GROUP')">{{$t('setting.agent.linkGroup')}}</bk-button>
                </div>
                <bk-table class="agent-table"
                    :data="shareList"
                    :outer-border="false"
                    :header-border="false"
                    :header-cell-style="{ background: '#fafbfd' }"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                >
                    <bk-table-column label="Id" prop="gitProjectId" min-width="150">
                        <template slot-scope="props">
                            {{ props.row.gitProjectId.replace('git_', '')}}
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('name')" prop="name" min-width="150"></bk-table-column>
                    <bk-table-column :label="$t('type')" prop="type" width="150"></bk-table-column>
                    <bk-table-column :label="$t('creator')" prop="creator" width="150"></bk-table-column>
                    <bk-table-column :label="$t('opeartion')" width="200" class-name="handler-btn">
                        <template slot-scope="props">
                            <span class="update-btn" @click="showDelete(props.row)">{{$t('remove')}}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
            <section v-else class="table-empty">
                <h3>{{$t('setting.agent.emptyLinkTitle')}}</h3>
                <h5>{{$t('setting.agent.emptyLinkTips')}}</h5>
                <div>
                    <bk-button class="import-agent" @click="toLinkShare('PROJECT')">{{$t('setting.agent.linkProject')}}</bk-button>
                    <bk-button @click="toLinkShare('GROUP')">{{$t('setting.agent.linkGroup')}}</bk-button>
                </div>
            </section>
        </main>

        <share-env 
            :select-type="selectType"
            :share-select-conf="shareSelectConf"
            :row-list="curPageList"
            :total-list="totalList"
            :share-handler-conf="shareHandlerConf"
            :confirm-fn="confirmFn"
            :toggle-all-select="toggleAllSelect"
            :loading="shareDialogLoading"
            :cancel-fn="cancelFn"
            :page-config="pageConfig"
            :update-page="updatePage"
            :query="query">
        </share-env>

        <bk-dialog v-model="isShowDelete"
            theme="danger"
            :mask-close="false"
            :loading="isDeleteing"
            @confirm="deleteShare"
            :title="$t('removeFromPool')">
            {{$t('deleteTips')}}【{{deleteRow.name}}】？
        </bk-dialog>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { setting } from '@/http'
    import shareEnv from '@/components/setting/share-env-dialog'
    import shareEnvMixin from '@/components/setting/share-env-mixin.js'

    export default {
        components: {
            shareEnv
        },

        mixins: [shareEnvMixin],

        data () {
            return {
                navList: [
                    { link: { name: 'agentPools' }, title: this.$t('setting.agent.agentPools') },
                    { link: { name: 'agentList' }, title: this.$route.params.poolName },
                    { link: '', title: this.$t('settings') }
                ],
                shareList: [],
                isLoading: false,
                isShowDelete: false,
                isDeleteing: false,
                deleteRow: {},
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 20
                }
            
            }
        },

        computed: {
            ...mapState(['appHeight', 'projectId']),

            envHashId () {
                return this.$route.params.poolId
            }
        },

        created () {
            this.pagination.current = 1
            this.getShareList()
        },

        methods: {
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.getShareList()
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.getShareList()
            },
            getShareList () {
                this.isLoading = true
                setting.getShareProjectList(this.projectId, this.$route.params.poolId, this.pagination.current, this.pagination.limit).then((res) => {
                    this.shareList = res.records || []
                    this.pagination.count = res.count || 0
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

            deleteShare () {
                this.isDeleteing = true
                setting.deleteShare(this.projectId, this.$route.params.poolId, this.deleteRow.gitProjectId).then(() => {
                    this.isShowDelete = false
                    this.$bkMessage({
                        theme: 'success',
                        message: 'Remove successfully'
                    })
                    this.pagination.current = 1
                    this.getShareList()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isDeleteing = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .pool-setting-home {
        overflow: auto;
    }
    .pool-setting-head {
        height: 49px;
        line-height: 49px;
        background: #fff;
        box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
        padding: 0 25.5px;
    }
    .pool-setting-main {
        padding: 16px;
        .operate-agent {
            margin-bottom: 20px;
            .bk-button {
                margin-right: 10px;
            }
        }
        .import-agent {
            margin-left: 10px;
            margin-right: 10px;
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
