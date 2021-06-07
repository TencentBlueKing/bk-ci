<template>
    <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
        <div class="cloc-list" v-if="taskDetail.enableToolList.find(item => item.toolName === 'CLOC')">
            <div class="breadcrumb">
                <div class="breadcrumb-name">
                    <bk-tab :active.sync="active" @tab-change="handleTableChange" type="unborder-card">
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index">
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            </div>
            <div class="main-container" ref="mainContainer">
                <div class="main-content-inner main-content-list">
                    <div class="catalog" v-show="isFetched">
                        <span class="mr10">
                            <i class="codecc-icon icon-branchs mr5"></i>Branch: {{branch}}
                        </span>
                        <template v-for="(item, index) in pathList">
                            <template v-if="item.isRoot">
                                <bk-popover placement="top" :key="index" v-if="codeRepo.length > 1">
                                    <span v-if="item.isPath" class="mr5 cc-link-primary project-name" @click="handleHref(item.href)">
                                        {{item.name}}<i class="bk-icon icon-plus f16"></i>
                                    </span>
                                    <span v-else class="mr5 project-name">
                                        {{item.name}}<i class="bk-icon icon-plus f16"></i>
                                    </span>
                                    <div slot="content">
                                        <div v-for="repo in codeRepo" :key="repo.name" class="pb5">
                                            {{repo.name}}@{{repo.branch}}
                                        </div>
                                    </div>
                                </bk-popover>
                                <span v-else :key="index">
                                    <span v-if="item.isPath" class="mr5 cc-link-primary project-name" @click="handleHref(item.href)">
                                        {{item.name}}
                                    </span>
                                    <span v-else class="mr5 project-name">
                                        {{item.name}}
                                    </span>
                                </span>
                            </template>
                            <template v-else>
                                <span v-if="item.isPath" class="mr5 cc-link-primary" :key="index" @click="handleHref(item.href)">{{item.name}}</span>
                                <span v-else class="mr5" :key="index">{{item.name}}</span>
                            </template>
                        </template>
                    </div>
                    <bk-table class="cloc-list-table"
                        :data="clocList"
                        v-show="isFetched"
                        v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }">
                        <bk-table-column class-name="name" min-width="150" :label="$t('文件名')" prop="name">
                            <template slot-scope="props">
                                <i class="codecc-icon mr5 f14" :class="[props.row.clocChildren ? 'icon-folders' : (props.row.type !== 'sum' ? 'icon-file' : '')]"></i>
                                <span :class="{ 'cloc-link': props.row.clocChildren }" @click="openFolder(props.row.name, !!props.row.clocChildren)">
                                    {{props.row.name}}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('总行数')" align="right" class-name="pr40" prop="totalLines"></bk-table-column>
                        <bk-table-column :label="$t('代码行')" align="right" class-name="pr40" prop="codeLines">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('代码行比率') + handleRate(props.row.codeLines, props.row.totalLines) }">
                                    {{props.row.codeLines}}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('注释行')" align="right" class-name="pr40" prop="commentLines">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('注释率') + handleRate(props.row.commentLines, props.row.totalLines) }">
                                    {{props.row.commentLines}}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('空白行')" align="right" class-name="pr40" prop="blankLines">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('空白率') + handleRate(props.row.blankLines, props.row.totalLines) }">
                                    {{props.row.blankLines}}
                                </span>
                            </template>
                        </bk-table-column>
                        <div slot="empty">
                            <div class="codecc-table-empty-text">
                                <img src="../../images/empty.png" class="empty-img">
                                <div>{{$t('没有查询到数据')}}</div>
                            </div>
                        </div>
                    </bk-table>
                </div>
            </div>
        </div>
        <div class="cloc-list" v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="$t('CodeCC集成了代码统计工具，可以检测代码中所包含的语言种类，以及代码行、注释行、空白行等占比')">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool({ from: 'cloc' })">{{$t('配置规则集')}}</bk-button>
                        </template>
                    </empty>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Empty from '@/components/empty'
    export default {
        components: {
            Empty
        },
        data () {
            return {
                contentLoading: false,
                fileLoading: false,
                isFetched: false,
                panels: [
                    { name: 'list', label: this.$t('按目录') },
                    { name: 'lang', label: this.$t('按语言') }
                ],
                active: 'list',
                projectName: '',
                clocTree: {},
                clocData: {},
                path: this.$route.params.path,
                branch: '',
                codeRepo: []
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            ...mapState('defect', {
                defaultCheckset: 'defaultCheckset'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            pathArr () {
                return this.path ? this.path.split('/') : []
            },
            pathList () {
                const path = this.path
                if (!path) return [{ name: this.projectName, isRoot: true }]
                const list = path.split('/')
                const length = list.length
                const pathList = [
                    {
                        isPath: true,
                        isRoot: true,
                        name: this.projectName,
                        href: ''
                    }
                ]
                const divider = {
                    isPath: false,
                    name: '/'
                }
                const hrefList = []
                for (let i = 0; i < length; i++) {
                    pathList.push(divider)
                    hrefList.push(list[i])
                    const item = {
                        isPath: i !== length - 1,
                        name: list[i],
                        href: hrefList.join('/')
                    }
                    pathList.push(item)
                }
                return pathList
            },
            clocList () {
                if (!this.clocData.clocChildren) return []
                const clocChildren = this.clocData.clocChildren.slice() || []
                const { codeLines, totalLines, commentLines, blankLines } = this.clocData
                const summary = {
                    name: this.$t('总计'),
                    type: 'sum',
                    codeLines,
                    totalLines,
                    commentLines,
                    blankLines
                }
                clocChildren.unshift(summary)
                return clocChildren
            }
        },
        watch: {
            path () {
                this.handlePath()
            }
        },
        created () {
            if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName === 'CLOC')) {
                this.init(true)
            }
        },
        methods: {
            async init (isInit) {
                isInit ? this.contentLoading = true : this.fileLoading = true
                const res = await this.$store.dispatch('defect/lintListCloc', { toolId: 'CLOC', type: 'FILE' })
                if (isInit) {
                    this.contentLoading = false
                    this.isFetched = true
                }
                if (res) {
                    this.fileLoading = false
                    this.clocTree = (res.clocTreeNodeVO && res.clocTreeNodeVO.clocChildren && res.clocTreeNodeVO.clocChildren[0]) || {}
                    const codeRepo = res.codeRepo || []
                    this.codeRepo = codeRepo
                    this.branch = codeRepo[0] && codeRepo[0].branch
                    this.projectName = (codeRepo[0] && codeRepo[0].name) || res.clocTreeNodeVO.name
                    this.handlePath()
                }
            },
            handleTableChange (value) {
                this.$router.push({ name: `defect-cloc-${value}` })
            },
            handleHref (name) {
                this.path = name
            },
            handleRate (num, sum) {
                return (num * 100 / sum).toFixed(2) + '%'
            },
            addTool (query) {
                if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
                    const that = this
                    this.$bkInfo({
                        title: this.$t('配置规则集'),
                        subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
                        }
                    })
                } else {
                    const from = query.from
                    const codeLang = this.taskDetail.codeLang
                    const codeLangKeys = this.toolMeta.LANG.map(lang => {
                        if (lang.key & codeLang) {
                            return lang.key
                        }
                    }).filter(name => name)
                    const installList = codeLangKeys.map(key => {
                        const checkerSetId = this.defaultCheckset[key] && this.defaultCheckset[key][from]
                        if (checkerSetId) {
                            const params = {
                                type: 'TASK',
                                projectId: this.projectId,
                                taskId: this.taskId,
                                checkerSetId
                            }
                            return this.$store.dispatch('checkerset/install', params)
                        }
                    }).filter(item => item)
                    if (installList.length) {
                        Promise.all(installList).then(() => {
                            setTimeout(() => {
                                window.location.reload()
                            }, 500)
                        })
                    } else {
                        this.$bkMessage({ theme: 'warning', message: this.$t('该任务语言暂无合适规则集') })
                    }
                }
            },
            handlePath () {
                let clocData = {}
                if (!this.pathArr.length) {
                    clocData = this.clocTree
                } else {
                    let clocList = this.clocTree.clocChildren.slice()
                    this.pathArr.some(item => {
                        const cur = clocList.find(li => li.name === item)
                        if (!cur || !cur.clocChildren) {
                            clocList = []
                            clocData = cur || {}
                            return true
                        } else {
                            clocData = cur
                            clocList = cur.clocChildren
                        }
                    })
                }
                this.clocData = clocData
            },
            openFolder (name, isFolder) {
                if (name && isFolder) {
                    this.pathArr.push(name)
                    const pathStr = this.pathArr.join('/')
                    this.path = pathStr
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import './defect-list.css';
    .cloc-list {
        padding: 16px 20px 0px 16px;
    }
    .main-container {
        border-top: 1px solid #dcdee5;
        margin: 0px!important;
        background: white;
    }
    .breadcrumb {
        padding: 0px!important;
        .breadcrumb-name {
            background: white;
        }
    }
    .bk-table.cloc-list-table {
        margin: 10px 0;
        >>>.bk-table-body .name {
            /* font-size: 14px!important; */
        }
    }
    .catalog {
        font-size: 14px;
        .project-name {
            font-weight: 600;
        }
    }
    .icon-folders, .icon-file {
        color: #699df4;
    }
    .cloc-link {
        cursor: pointer;
        &:hover {
            color: #3a84ff;
            text-decoration: underline;
        }
    }
    .mr50 {
        padding-right: 50px;
    }
</style>
