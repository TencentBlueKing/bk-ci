<template>
    <div class="biz-container pipeline-subpages instance-manage-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <inner-header>
            <div class="instance-header" slot="left">
                <span class="inner-header-title" slot="left">实例管理</span>
            </div>
        </inner-header>
        <div class="sub-view-port" v-if="showContent && instanceList.length">
            <div class="instance-handle-row">
                <bk-button size="normal" class="batch-update" @click="handleBitch()"><span>批量更新</span></bk-button>
                <bk-button theme="primary" size="normal" @click="createInstance()"><span>创建新实例</span></bk-button>
            </div>
            <section class="instance-table">
                <bk-table
                    :data="instanceList"
                    size="small"
                    @select="selectItem"
                    @select-all="selectItem"
                >
                    <bk-table-column type="selection" width="60" align="center" :disalbed="false"></bk-table-column>
                    <bk-table-column label="流水线名称" prop="pipelineName">
                        <template slot-scope="props">
                            <span class="pipeline-name" @click="toPipelineHistory(props.row.pipelineId)">{{ props.row.pipelineName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="实例版本" prop="versionName"></bk-table-column>
                    <bk-table-column label="模板最新版本" :formatter="currentVersionFormatter">
                        <template>
                            <span>{{ currentVersionName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="状态" prop="updateTime">
                        <template slot-scope="props">
                            <div :class="{ &quot;status-card&quot;: true, &quot;need-update&quot;: isUpdate(props.row) }">
                                {{ isUpdate(props.row) ? '需更新' : '无需更新' }}
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="上次更新时间" prop="updateTime">
                        <template slot-scope="props">
                            <span>{{ localConvertTime(props.row.updateTime) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="操作" width="150">
                        <template slot-scope="props">
                            <bk-button theme="primary" text :disabled="!props.row.hasPermission" @click="updateInstance(props.row)">编辑</bk-button>
                            <bk-button theme="primary" text @click="toCompared(props.row)">差异对比</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </div>
        <empty-tips v-if="showContent && !instanceList.length"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>
        <instance-compared :show-compared-instance="showComparedInstance"
            :loading="dialogLoading"
            :instance-version="instanceVersion"
            :cur-version="currentVersion"
            :version-list="versionList"
            :cur-params-list="curParamsList"
            :cur-tpl-params-list="curTplParamsList"
            :target-params-list="targetParamsList"
            :target-tpl-params-list="targetTplParamsList"
            :cur-stages="curStages"
            :target-stages="targetStages"
            @comfire="comfireHandler"
            @cancel="cancelHandler"
            :selected-version="selectedVersion"></instance-compared>
    </div>
</template>

<script>
    import innerHeader from '@/components/devops/inner_header'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import instanceCompared from '@/components/template/instance-compared.vue'
    import { convertTime } from '@/utils/util'

    export default {
        components: {
            'inner-header': innerHeader,
            'empty-tips': emptyTips,
            'instance-compared': instanceCompared
        },
        data () {
            return {
                showContent: false,
                showComparedInstance: false,
                dialogLoading: false,
                isInit: false,
                instanceVersion: '',
                currentVersion: '',
                currentVersionId: '',
                currentVersionName: '',
                curComparedPipeline: '',
                selectItemList: [],
                instanceList: [],
                versionList: [],
                curParamsList: [],
                targetParamsList: [],
                curStages: [],
                curTplParamsList: [],
                targetStages: [],
                targetTplParamsList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyTipsConfig: {
                    title: '创建第一个实例',
                    desc: '创建自己的实例，快速生成你想要的流水线',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createInstance(),
                            text: '创建新实例'
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            templateId () {
                return this.$route.params.templateId
            }
        },
        watch: {
            instanceVersion (newVal) {
                if (newVal && !this.isInit) {
                    this.requestVersionCompare(newVal)
                }
            }
        },
        async mounted () {
            await this.requestInstanceList()
        },
        methods: {
            async requestInstanceList () {
                const { $store, loading } = this

                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestInstanceList', {
                        projectId: this.projectId,
                        templateId: this.templateId
                    })
                    this.currentVersionId = res.latestVersion.version
                    // this.versionList = res.versions
                    this.currentVersionName = res.latestVersion.versionName
                    this.instanceList = res.instances
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },
            localConvertTime (timestamp) {
                return convertTime(timestamp)
            },
            currentVersionFormatter () {
                return this.currentVersionName
            },
            createInstance (pipeline, type) {
                const route = {
                    name: 'createInstance',
                    params: {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        curVersionId: this.currentVersionId
                    }
                }
                if (pipeline) {
                    route.hash = type === 'single' ? `#${pipeline}` : `#${pipeline.join('&')}`
                }
                this.$router.push(route)
            },
            selectItem (items) {
                this.selectItemList = items
            },
            updateInstance (row) {
                if (row.hasPermission) {
                    const pipeline = row.pipelineId
                    this.createInstance(pipeline, 'single')
                }
            },
            handleBitch () {
                const targetList = []
                if (this.selectItemList && this.selectItemList.length) {
                    this.selectItemList.forEach(val => {
                        targetList.push(val.pipelineId)
                    })
                    this.createInstance(targetList, 'bitch')
                } else {
                    this.$showTips({
                        message: '请选择至少一条流水线',
                        theme: 'error'
                    })
                }
            },
            toCompared (row) {
                this.showComparedInstance = true
                this.isInit = true
                this.currentVersion = row.versionName
                this.curComparedPipeline = row.pipelineId
                this.requestVersionCompare(this.currentVersionId)
            },
            async requestVersionCompare (versionId) {
                this.dialogLoading = true

                try {
                    const res = await this.$store.dispatch('pipelines/requestVersionCompare', {
                        projectId: this.projectId,
                        templateId: this.templateId,
                        versionId: versionId,
                        pipelineId: this.curComparedPipeline
                    })

                    this.versionList = res.versions
                    const curVersion = this.versionList.filter(val => {
                        return val.version === parseInt(versionId)
                    })
                    this.instanceVersion = curVersion[0].version

                    const curData = res.origin
                    const targetData = res.target
                    const curContainer = curData.model.stages[0].containers[0]
                    const targetContainer = targetData.model.stages[0].containers[0]

                    this.curStages = curData.model.stages
                    this.targetStages = targetData.model.stages

                    this.curParamsList.splice(0, this.curParamsList.length)
                    this.targetParamsList.splice(0, this.targetParamsList.length)
                    this.curTplParamsList.splice(0, this.curTplParamsList.length)
                    this.targetTplParamsList.splice(0, this.targetTplParamsList.length)

                    curData.params && curData.params.map((item, index) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.curParamsList.push(temp)
                    })

                    targetData.params && targetData.params.map((item, index) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.targetParamsList.push(temp)
                    })

                    curContainer.templateParams && curContainer.templateParams.map((item, param) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.curTplParamsList.push(temp)
                    })

                    targetContainer.templateParams && targetContainer.templateParams.map((item, param) => {
                        const temp = {
                            key: item.id,
                            value: item.defaultValue
                        }
                        this.targetTplParamsList.push(temp)
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.dialogLoading = false
                }
            },
            isUpdate (row) {
                return row.version < this.currentVersionId
            },
            comfireHandler () {

            },
            cancelHandler () {
                this.showComparedInstance = false
            },
            selectedVersion (data) {
                this.isInit = false
                this.instanceVersion = data
            },
            toPipelineHistory (pipelineId) {
                const url = `${WEB_URL_PIRFIX}/pipeline/${this.projectId}/${pipelineId}/history`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipeline-subpages {
        min-height: 100%;
        .bk-exception {
            position: absolute;
        }
    }
    .instance-manage-wrapper {
        flex-direction: column;
        .instance-header {
            .bk-button-normal {
                margin-top: -6px;
                padding: 0 10px;
                font-size: 12px;
            }
        }
        .sub-view-port {
            padding: 20px;
            height: calc(100% - 60px);
            overflow: auto;
        }
        .instance-handle-row {
            display: flex;
            margin-bottom: 20px;
            button {
                margin-right: 4px;
                padding: 0 11px;
                font-size: 12px;
            }
        }
        .instance-table {
            .pipeline-name {
                color: $primaryColor;
                cursor: pointer;
            }
            .status-card {
                width: 68px;
                border: 1px solid #39E084;
                background-color: #CFFCE2;
                color: #39E084;
                font-size: 12px;
                text-align: center;
            }
            .need-update {
                border: none;
                background-color: #F6B026;
                border-radius: 10px;
                color: #fff;
            }
            .update-btn,
            .compared-btn {
                display: inline-block;
                margin-right: 20px;
                cursor: pointer;
            }
            .is-disabled {
                color: #ccc;
                cursor: not-allowed;
            }
        }
        .batch-update {
            padding: 0 11px;
            font-size: 12px;
        }
    }
</style>
