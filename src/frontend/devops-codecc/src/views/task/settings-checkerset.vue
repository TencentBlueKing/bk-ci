<template>
    <div class="main-content-inner checker">
        <span v-if="taskDetail.createFrom === 'gongfeng_scan'" class="fs12 go-pipeline">{{$t('规则集配置由CodeCC开源扫描集群自动生成')}}</span>
        <span v-else-if="editDisabled" class="fs12 go-pipeline">{{$t('修改规则集配置，请前往流水线')}}
            <a class="ml15" @click="hanldeToPipeline" target="_blank" :href="`${DEVOPS_SITE_URL}/console/pipeline/${taskDetail.projectId}/${taskDetail.pipelineId}/edit#${taskDetail.atomCode}`">{{$t('立即前往>>')}}</a>
        </span>
        <div v-if="checkersetList.length">
            <p class="checker-title">
                {{$t('已启用')}}
                <span class="checker-title-num">{{enableList.length}}</span>
                <i class="codecc-icon icon-tips" v-bk-tooltips="{ content: $t('以下规则集适合于当前任务语言。新启用的规则集将在下次检查时生效') }"></i>
                <a class="fr fs12 cc-link" @click="goToCheckerset">{{$t('更多规则集')}}</a>
            </p>
            <card v-for="checkerset in list" :key="checkerset.checkerSetId"
                :checkerset="checkerset"
                :from="'task'"
                :has-ccn="hasCcnTips(checkerset)"
                :has-new="hasNewTips(checkerset)"
                :is-new-atom="editDisabled"
                :is-enable="checkerset.taskUsing"
                :handle-mannge="handleCheckerset">
            </card>
        </div>
        <div v-else-if="isFetched">
            <div class="codecc-table-empty-text">
                <img src="../../images/empty.png" class="empty-img">
                <div>{{$t('暂无数据')}}</div>
            </div>
        </div>
        <!-- <bk-dialog
            v-model="checkersetDialogVisiable"
            class="ckeckerset-dialog"
            :render-directive="'if'"
            :fullscreen="true"
            :draggable="false"
            :mask-close="false"
            :show-footer="false"
            :close-icon="true">
            <div class="checkerset-dialog-main">
                <checkerset-manage
                    :is-from-settings="true"
                    :checkerset-id="checkersetId"
                    :version="version"
                    :update-checker-list="updateCheckerList">
                </checkerset-manage>
            </div>
        </bk-dialog> -->
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import card from './../checkerset/card'
    // import checkersetManage from './../checkerset/manage'
    export default {
        components: {
            card
            // checkersetManage
        },
        data () {
            return {
                isFetched: false,
                checkersetList: [],
                checkersetDialogVisiable: false,
                checkersetId: '',
                version: ''
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            list () {
                const enableList = this.checkersetList.filter(item => item.taskUsing)
                const disableList = this.checkersetList.filter(item => !item.taskUsing)
                return enableList.concat(disableList)
            },
            enableList () {
                return this.checkersetList.filter(item => item.taskUsing) || []
            },
            editDisabled () {
                return (this.taskDetail.atomCode && this.taskDetail.createFrom === 'bs_pipeline') || this.taskDetail.createFrom === 'gongfeng_scan'
            }
        },
        created () {
            this.fetchList()
        },
        methods: {
            async fetchList () {
                this.$store.commit('setMainContentLoading', true)
                const { projectId, taskId } = this
                const params = { projectId, taskId, showLoading: true }
                const res = await this.$store.dispatch('checkerset/list', params)
                this.checkersetList = res
                this.isFetched = true
                this.$store.commit('setMainContentLoading', false)
            },
            handleCheckerset (checkerset) {
                // this.checkersetId = checkerset.checkerSetId
                // this.version = checkerset.version
                // this.checkersetDialogVisiable = true
                const href = this.$router.resolve({
                    name: 'checkerset-manage',
                    params: {
                        projectId: this.projectId,
                        checkersetId: checkerset.checkerSetId,
                        version: checkerset.version
                    }
                }).href
                window.open(`${window.JUMP_SITE_URL}${href}`)
            },
            goToCheckerset () {
                this.$router.push({
                    name: 'checkerset-list'
                })
            },
            hanldeToPipeline () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`, '_blank')
            },
            hasCcnTips (checkerset) {
                const isNotPipeline = this.taskDetail.createFrom !== 'bs_pipeline'
                const ownList = this.checkersetList.filter(item => !item.checkerSetSource)
                const isFromCcndupc = this.$route.query.from === 'ccndupc'
                return isFromCcndupc && isNotPipeline && ownList && ownList[0] === checkerset
            },
            hasNewTips (checkerset) {
                return this.checkersetList[0] === checkerset
            },
            updateCheckerList () {
                this.fetchList()
                this.checkersetDialogVisiable = false
            }
        }

    }
</script>

<style lang="postcss" scoped>
    .main-content-inner {
        padding: 0 20px;
    }
    .go-pipeline {
        display: block;
        padding-bottom: 15px;
        border-bottom: 1px solid #dcdee5;
        margin-bottom: 7px;
    }
    .checker-title {
        line-height: 30px;
        padding-bottom: 10px;
        font-size: 14px;
        .checker-title-num {
            /* padding: 0 10px; */
            color: #bbb;
        }
        .icon-tips {
            position: relative;
            top: -1px;
        }
    }
    .codecc-table-empty-text {
        text-align: center;
        padding-top: 200px;
    }
    .checkerset-dialog-main {
        height: 100%;
        padding-top: 30px;
        .checkerset-manage {
            height: 100%;
        }
    }
    .ckeckerset-dialog {
        >>>.bk-dialog-wrapper {
            .bk-dialog {
                /* width: calc(100% - 80px)!important;
                margin: 40px; */
            }
        }
    }
</style>
