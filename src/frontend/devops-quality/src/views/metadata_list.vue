<template>
    <div class="metadata-list-wrapper">
        <div class="metadata-list-header">
            <div class="title">{{$t('quality.指标列表')}}</div>
        </div>

        <section class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <template v-if="showContent">
                <div class="metadata-tabs-wrapper">
                    <ul class="metadata-tabs">
                        <li class="item-tab" :class="{ 'active-tab': currentTab === entry.value }"
                            v-for="(entry, index) in metaTypeList" :key="index"
                            @click="changeTab(entry.value)">{{entry.label}}
                        </li>
                    </ul>
                    <div class="search-input-row">
                        <input class="bk-form-input"
                            type="text"
                            :placeholder="$t('quality.请输入指标名称')"
                            v-model="searchName"
                            @keyup.enter="toSearch()">
                        <i class="devops-icon icon-search" @click="toSearch()"></i>
                    </div>
                </div>
                <div class="metadata-content">
                    <div class="metadata-item-wrapper">
                        <section v-for="(row, index) in renderList" :key="index">
                            <div class="info-title">
                                <label>{{row.label}}
                                    <bk-popover placement="right">
                                        <i class="devops-icon icon-info-circle" v-if="row.desc"></i>
                                        <template slot="content">
                                            <p style="width: 196px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">{{row.desc}}</p>
                                        </template>
                                    </bk-popover>
                                </label>
                                <div class="create-metadata" v-if="row.key === 'scriptIndicators'" @click="toCreateMeta()">
                                    <i class="devops-icon icon-plus-circle-shape"></i>{{$t('quality.自定义指标')}}
                                </div>
                            </div>
                            <bk-table
                                size="small"
                                class="metadata-list-table"
                                :data="row.records"
                                :empty-text="$t('quality.暂无数据')">
                                <bk-table-column :label="$t('quality.指标中文名')" prop="cnName">
                                    <template slot-scope="props">
                                        <span>{{props.row.cnName}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.指标英文名')" prop="name">
                                    <template slot-scope="props">
                                        <span>{{props.row.name}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.产出插件')" prop="elementName" min-width="80">
                                    <template slot-scope="props">
                                        <span>{{props.row.elementName}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.工具/插件子类')" prop="elementDetail" min-width="90">
                                    <template slot-scope="props">
                                        <span>{{props.row.elementDetail}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.可选操作')" prop="availableOperation">
                                    <template slot-scope="props">
                                        <span>{{getOperation(props.row.availableOperation)}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.数值类型')" prop="dataType">
                                    <template slot-scope="props">
                                        <span>{{props.row.dataType}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.默认阈值')" prop="threshold">
                                    <template slot-scope="props">
                                        <span>{{props.row.threshold}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.描述')" prop="desc">
                                    <template slot-scope="props">
                                        <span>{{props.row.desc}}</span>
                                    </template>
                                </bk-table-column>
                                <bk-table-column :label="$t('quality.操作')" width="150">
                                    <template slot-scope="props">
                                        <div class="handler-btn">
                                            <span v-if="row.key === 'scriptIndicators'" @click="editMeta(props.row.hashId)">{{$t('quality.编辑指标')}}</span>
                                            <span @click="toCreateRule(props.row.hashId)">{{$t('quality.创建规则')}}</span>
                                        </div>
                                    </template>
                                </bk-table-column>
                            </bk-table>
                        </section>
                    </div>
                </div>
            </template>
        </section>
    </div>
</template>

<script>

    export default {
        data () {
            return {
                showContent: false,
                currentTab: 'all',
                searchName: '',
                scriptTips: this.$t('quality.支持自定义指标，由脚本任务插件上报指标数值'),
                marketTips: this.$t('quality.添加研发商店的插件后将会在这里自动添加相关红线指标'),
                renderList: [],
                metaDataList: [],
                metaTypeList: [
                    { label: this.$t('quality.全部指标'), value: 'all' },
                    { label: this.$t('quality.脚本任务指标'), value: 'scriptIndicators' },
                    { label: this.$t('quality.研发商店指标'), value: 'marketIndicators' },
                    { label: this.$t('quality.系统指标'), value: 'systemIndicators' }
                ],
                operationMap: {
                    LT: '<',
                    LE: '<=',
                    GT: '>',
                    GE: '>=',
                    EQ: '='
                },
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId (val) {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        created () {
            this.requestList()
        },
        methods: {
            async requestList () {
                this.loading.isLoading = true
                this.loading.title = this.$t('quality.数据加载中，请稍候')

                const keyword = this.searchName.trim()

                try {
                    const res = await this.$store.dispatch('quality/requestIndicatorList', {
                        projectId: this.projectId,
                        keyword
                    })

                    this.renderList.splice(0, this.renderList.length)
                    this.metaDataList = this.metaTypeList.map(meta => {
                        return {
                            key: meta.value,
                            label: meta.label,
                            desc: meta.value === 'scriptIndicators' ? this.scriptTips : meta.value === 'marketIndicators' ? this.marketTips : '',
                            records: res[meta.value]
                        }
                    })
                    
                    if (this.currentTab === 'all') {
                        this.metaDataList.forEach(item => {
                            if (item.key !== 'all') {
                                this.renderList.push(item)
                            }
                        })
                    } else {
                        this.renderList = this.metaDataList.filter(item => {
                            return item.key === this.currentTab
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 100)
                }
            },
            getOperation (list) {
                const target = list.map(item => this.operationMap[item])
                return target.join(' ')
            },
            changeTab (val) {
                this.currentTab = val
                this.renderList.splice(0, this.renderList.length)
                if (val === 'all') {
                    this.metaDataList.forEach(item => {
                        if (item.key !== 'all') {
                            this.renderList.push(item)
                        }
                    })
                } else {
                    this.renderList = this.metaDataList.filter(item => {
                        return item.key === val
                    })
                }
            },
            toSearch () {
                this.requestList()
            },
            toCreateMeta () {
                this.$router.push({
                    name: 'createMeta',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            editMeta (id) {
                this.$router.push({
                    name: 'editMeta',
                    params: {
                        projectId: this.projectId,
                        metaId: id
                    }
                })
            },
            toCreateRule (id) {
                this.$router.push({
                    name: 'createRule',
                    params: {
                        projectId: this.projectId
                    },
                    query: {
                        indicator: id
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .metadata-list-wrapper {
        .metadata-list-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .sub-view-port {
            padding: 20px;
        }
        .metadata-tabs-wrapper {
            display: flex;
            justify-content: space-between;
            .item-tab {
                float: left;
                width: 124px;
                margin-right: -1px;
                padding: 12px 0;
                border: 1px solid #DDE4EB;
                background-color: #FFF;
                color: #737987;
                text-align: center;
                cursor: pointer;
                &:hover {
                    color: $primaryColor;
                }
            }
            .active-tab {
                background-color: #FFF;
                color: $primaryColor;
            }
        }
        .search-input-row {
            position: relative;
            padding: 0 10px;
            width: 279px;
            height: 36px;
            border: 1px solid #dde4eb;
            background-color: #fff;
            .bk-form-input {
                padding: 0;
                border: 0;
                -webkit-box-shadow: border-box;
                box-shadow: border-box;
                outline: none;
                width: 236px;
                height: 32px;
                margin-left: 0;
            }
            .icon-search {
                float: right;
                margin-top: 12px;
                color: #c3cdd7;
                cursor: pointer;
            }
        }
        .metadata-item-wrapper {
            .info-title {
                display: flex;
                justify-content: space-between;
                margin-top: 20px;
                color: #737987;
                font-size: 16px;
                font-weight: bold;
            }
            .icon-info-circle {
                position: relative;
                top: 1px;
                color: #C3CDD7;
            }
            .create-metadata {
                font-size: 14px;
                font-weight: normal;
                color: $primaryColor;
                cursor: pointer;
            }
            .icon-plus-circle-shape {
                margin-right: 4px;
                color: #699DF4;
            }
        }
        .metadata-list-table {
            min-width: 1086px;
            margin-top: 10px;
            td .cell {
                padding: 10px 15px;
                span {
                    display: inline-block;
                    overflow: hidden;
                }
                .handler-btn span {
                    display: inline;
                    margin-right: 12px;
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
        }
    }
</style>
