<template>
    <bk-dialog
        v-model="createVisible"
        :theme="'primary'"
        :width="579"
        @confirm="save"
        @cancel="cancel"
        :position="{ top: 100, left: 5 }">
        <bk-tab type="unborder-card" class="create-tab" @tab-change="changeTab">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
            </bk-tab-panel>
            <!-- 添加路径 -->
            <div v-show="tabSelect === 'choose'" class="create-tab-1">
                <bk-form :label-width="0" :model="formData">
                    <bk-form-item property="searchData" :rules="rules.searchData">
                        <bk-input
                            style="margin:15px 0;"
                            :placeholder="$t('filter.搜索文件夹告警路径名称')"
                            :right-icon="'bk-icon icon-search'"
                            @change="search"
                            :clearable="true"
                            v-model="formData.searchData">
                        </bk-input>
                    </bk-form-item>
                    <div class="data-tree" v-if="treeList.length">
                        <bk-tree
                            ref="tree"
                            :multiple="true"
                            :data="treeList"
                            :has-border="true"
                            :node-key="'name'"
                        >
                        </bk-tree>
                    </div>
                    <div v-if="!treeList.length">
                        <empty size="small" :title="$t('filter.无告警文件')" />
                    </div>
                </bk-form>
            </div>
            <!-- /添加路径 -->
            <!-- 系统默认 -->
            <div v-show="tabSelect === 'input'" class="create-tab-2">
                <div class="input-info">
                    <div class="input-info-left"><i class="bk-icon icon-info-circle-shape"></i></div>
                    <div class="input-info-right"></div>
                    {{$t('filter.屏蔽某类文件如protobuffer生成的')}}<br />
                    {{$t('filter.屏蔽所有分支中某个文件夹如P2PLive')}}<br />
                    {{$t('filter.屏蔽某个文件夹下某类文件如P2PLive')}}
                </div>
                <div class="input-paths">
                    <bk-form :label-width="0" :model="formData">
                        <bk-form-item
                            class="input-paths-position"
                            v-for="(path, index) in formData.paths"
                            :rules="rules.customPath"
                            :property="`paths.${index}.customPath`"
                            :key="index">
                            <bk-input :placeholder="$t('filter.请输入屏蔽路径')" class="input-stlye" v-model="path.customPath"></bk-input>
                            <div class="input-paths-icon">
                                <i class="bk-icon icon-plus-circle-shape" @click="addPath"></i>
                                <i class="bk-icon icon-minus-circle-shape" v-if="formData.paths.length > 1" @click="cutPath(index)"></i>
                            </div>
                        </bk-form-item>
                    </bk-form>
                </div>
            </div>
            <!-- /系统默认 -->
        </bk-tab>
    </bk-dialog>
</template>

<script>
    import { mapState } from 'vuex'
    import Empty from '@/components/empty'

    export default {
        components: {
            Empty
        },
        props: {
            visible: Boolean
        },
        data () {
            return {
                panels: [
                    { name: 'choose', label: this.$t('filter.选择路径'), count: 10 },
                    { name: 'input', label: this.$t('filter.手动输入'), count: 20 }
                ],
                active: 'choose',
                formData: {
                    searchData: '',
                    name: '',
                    paths: [{}]
                },
                paths: [
                    {}
                ],
                tabSelect: '',
                postData: {
                    taskId: '',
                    pathType: 'CUSTOM',
                    filterFile: [],
                    filterDir: [],
                    customPath: []
                },
                Dir: [],
                childrenSelected: true,
                allPathNode: [],
                repeat: 0,
                allNode: [],
                rules: {
                    customPath: [
                        {
                            max: 50,
                            message: '不能多于50个字符',
                            trigger: 'blur'
                        }
                    ],
                    searchData: [
                        {
                            max: 50,
                            message: '不能多于50个字符',
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState('task', {
                taskIgnoreTree: 'ignoreTree'
            }),
            ...mapState(['taskId']),
            treeList () {
                const treeList = this.taskIgnoreTree.name ? [this.taskIgnoreTree] : []
                return treeList
            },
            createVisible: {
                get () {
                    return this.visible
                },
                set (value) {
                    this.$emit('visibleChange', value)
                }
            }
        },
        watch: {
            formData: {
                handler () {
                    if (this.treeList[0].name) {
                        if (this.formData.searchData) {
                            // this.treeList[0].expanded = true
                            this.openTree(this.treeList[0])
                        } else {
                            this.treeList[0].expanded = false
                        }
                    }
                },
                deep: true
            }
        },
        created () {
            this.$store.dispatch('task/ignoreTree')
        },
        methods: {
            openTree (arr) {
                if (arr.children) {
                    arr.expanded = true
                    arr.children.forEach(item => {
                        this.openTree(item)
                    })
                }
            },
            getTree () {
                let files = []
                let filestrs = []
                let dirs = []
                let dirstrs = []
                const checkedList = this.$refs.tree.getNode(['name', 'parent'])
                const getPath = function (node, path) {
                    if (node.parent.halfcheck || node.parent.checked) {
                        path.unshift(node.name)
                        if (node.parent.parent) {
                            getPath(node.parent, path)
                        } else {
                            filestrs.push(path.join('/'))
                            files = []
                        }
                    }
                }
                const getDirPath = function (node, path) {
                    if (node.parent.checked && !node.parent.halfcheck) {
                        // 上级全选
                        if (node.parent.parent && !node.parent.parent.halfcheck) {
                            // 上上级全选，跳过当前文件夹
                            if (!node.parent.parent.parent) {
                                // 结果为第二级文件夹时直接保存路径
                                dirstrs.unshift(node.parent.name)
                            }
                            getDirPath(node.parent, path)
                        } else if (node.parent.parent && node.parent.parent.halfcheck && node.parent.parent.parent) {
                            // 解决组件反选后上上级为空的情景
                            dirs = []
                            path = []
                            path.unshift(node.parent.name)
                            getDirPath(node.parent, path)
                        } else if (node.parent.parent && node.parent.parent.halfcheck && !node.parent.parent.parent) {
                            // 全选到头，保存当前上级文件夹名
                            path.unshift(node.parent.name)
                            getDirPath(node.parent, path)
                        } else {
                            path.unshift(node.parent.name)
                        }
                    } else {
                        path.unshift(node.parent.name)
                        if (node.parent.parent) {
                            // 非全选，有上级，继续获取路径
                            getDirPath(node.parent, path)
                        } else {
                            // 结束，合并生成路径
                            dirstrs.push(path.join('/'))
                            dirs = []
                        }
                    }
                }
                checkedList.map(i => {
                    if (i.parent) {
                        if (i.parent.checked && !i.parent.halfcheck) {
                            // 最终为文件夹
                            getDirPath(i, dirs)
                        } else {
                            // 最终为文件
                            getPath(i, files)
                        }
                    }
                })
                if (dirstrs) {
                    // 存相对路径
                    for (const i in dirstrs) {
                        if (dirstrs[i].indexOf('/') !== -1) {
                            dirstrs[i] = dirstrs[i].slice(dirstrs[i].indexOf('/') + 1)
                        }
                    }
                }
                // 去重
                dirstrs = [...new Set(dirstrs)]
                filestrs = [...new Set(filestrs)]
                this.postData.filterFile = filestrs
                this.postData.filterDir = dirstrs
            },
            save () {
                if (this.tabSelect === 'input') {
                    this.postData.filterFile = []
                    this.postData.filterDir = []
                    for (let i = 0; i < this.formData.paths.length; i++) {
                        for (let y = i + 1; y < this.formData.paths.length; y++) {
                            if (this.formData.paths[i].customPath === this.formData.paths[y].customPath) {
                                this.$bkMessage({ theme: 'error', message: this.$t('filter.路径重复') })
                                this.formData.paths = [{}]
                                return
                            }
                        }
                        if (this.formData.paths[i].customPath) {
                            this.postData.customPath.push(this.formData.paths[i].customPath)
                        } else if (this.formData.paths.length === 1 && this.formData.paths[0] === {}) {
                            return
                        }
                    }
                } else if (this.tabSelect === 'choose') {
                    this.getTree()
                    this.postData.customPath = []
                }
                this.postData.taskId = this.taskId
                this.$store.dispatch('task/createIgnore', this.postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('filter.路径添加成功') })
                        this.$store.dispatch('task/ignore', this.taskId)
                        this.$store.dispatch('task/ignoreTree')
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('filter.路径添加失败') })
                }).finally(() => {
                    this.postData.customPath = []
                    this.postData.filterFile = []
                    this.postData.filterDir = []
                    this.formData.paths = [{}]
                })
                this.createVisible = false
            },
            cancel () {
                this.formData.paths = [{}]
                this.createVisible = false
            },
            changeTab (name) {
                this.tabSelect = name
            },
            // 添加input框
            addPath () {
                if (this.formData.paths) {
                    this.formData.paths.push({})
                }
            },
            cutPath (index) {
                if (this.formData.paths.length > 1) {
                    this.formData.paths.splice(index, 1)
                }
            },
            // 筛选路径方法
            search (value, event) {
                this.$refs.tree.searchNode(this.formData.searchData)
            }
        }
    }
</script>
<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .create-tab {
        >>>.bk-tab-section {
            padding: 15px 0;
            display: inline;
        }
        /* 选择路径页 start */
        .create-tab-1 {
           .data-tree {
                overflow: auto;
                height: 249px;
                border-radius: 2px;
                border: 1px solid $borderColor;
                padding: 10px;
                >>>.tree-node {
                    margin-left: 5px;
                },
                >>>.node-icon {
                    margin-right: 5px;
                }
            }
        }
        /* 选择路径页 end */
        /* 手动输入页 start */
        .create-tab-2 {
            .input-info {
                margin-top: 15px;
                background-color: $bgColor;
                width: 531px;
                height: 90px;
                border-radius: 2px;
                border: 1px solid #a3c5fd;
                font-size: 12px;
                text-align: left;
                padding-top: 8px;
                .input-info-left {
                    float: left;
                    line-height: 89px;
                    width: 36px;
                    height: 89px;
                    margin-top: -8px;
                    background-color: #a3c5fd;
                    margin-right: 17px;
                    .bk-icon {
                        font-size: 18px;
                        color: #ffffff;
                        padding-left: 8px;
                    }
                }
                .input-info-right {
                    float: left;
                    line-height: 57px;
                    width: 47px;
                    padding-left: 20px;
                }
            }
            .input-paths {
                margin-top: 12px;
                overflow: auto;
                height: 214px;
                >>>.bk-form-item+.bk-form-item {
                    margin-top: 48px;
                }
                >>>.bk-form-item.is-error {
                    .bk-icon.icon-exclamation-circle-shape.tooltips-icon {
                        right: 78px!important;
                        top: 16px;
                    }
                }
                .input-paths-position {
                    .input-stlye {
                        width: 462px;
                        float: left;
                        margin: 8px 0;
                    }
                    .input-paths-icon {
                        line-height: 48px;
                        float: left;
                        padding-left: 20px;
                        .bk-icon {
                            cursor: pointer;
                            font-size: 16px;
                        }
                        .icon-plus-circle-shape, .icon-minus-circle-shape {
                            color: $itemBorderColor;
                        }
                        .icon-minus-circle-shape:hover, .icon-plus-circle-shape:hover {
                            color: $fontLightColor;
                        }
                    }
                }
            }
        }
        /* 手动输入页 end */
    }

</style>
