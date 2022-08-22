<template>
    <div class="biz-container artifactory"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <side-bar
            class="artifactory-aside-bar"
            :nav="sideMenuNav"
            :side-menu-list="sideMenuList"
            :sub-system-name="'artifactory'">
        </side-bar>
        <router-view :folder-loading="folderLoading" :permission-config="permissionConfig" @refresh="refresh" @requestList="requestPathInfo" @getItems="getItems"></router-view>
    </div>
</template>

<script>
    import sideBar from '@/components/devops/side-nav'
    import { mapGetters } from 'vuex'
    import { bus } from './../utils/bus'
    import {
        convertStrToNumArr
    } from '@/utils/util'

    export default {
        components: {
            'side-bar': sideBar
        },
        data () {
            return {
                loading: {
                    isLoading: false,
                    title: ''
                },
                sideMenuNav: {
                    icon: 'artifactory',
                    title: VERSION_TYPE === 'tencent' ? '版本仓库' : '制品库'
                },
                baseObj: {
                    icon: 'icon-folder',
                    openIcon: 'icon-folder-open',
                    arrowIcon: 'icon-right-shape',
                    arrowOpenIcon: 'icon-down-shape',
                    isOpen: false,
                    loading: false
                },
                folderLoading: true, // 文件夹数据加载
                firstPipelineLoad: false, // 流水线初始化加载,
                permissionConfig: { // 无权限
                    isShow: false,
                    resource: '',
                    option: '',
                    link: '/perm/apply-perm'
                }
            }
        },
        computed: {
            ...mapGetters({
                curNodeOnTree: 'artifactory/getCurNodeOnTree',
                sideMenuList: 'artifactory/getSideMenuList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            showType () {
                return this.$route.params.type || ''
            }
        },
        watch: {
            'curNodeOnTree.roadMap' (val, oldVal) {
                if (JSON.stringify(val) !== JSON.stringify(oldVal)) {
                    this.branchClickHander(this.curNodeOnTree)
                }
            },
            projectId (val) {
                this.unsetSideMenuParams()
                if (val) {
                    if (this.showType) {
                        this.initTreeData()
                    }
                }
            },
            showType (val) {
                this.selectedType(val)
                if (val) {
                    this.initTreeData()
                }
            },
            isExtendTx () {
                return VERSION_TYPE === 'tencent'
            }
        },
        created () {
            if (VERSION_TYPE === 'tencent') {
                this.$store.dispatch('artifactory/requestProjectList')
            }
            bus.$off('get-item')
            bus.$on('get-item', (data) => {
                this.getItems(data.roadMap, data.list, data.noLoading)
            })
        },
        mounted () {
            if (this.showType) {
                this.selectedType(this.showType)
                this.initTreeData()
            }
        },
        methods: {
            selectedType (val) {
                const {
                    list
                } = this.sideMenuList[0]
                list.forEach(item => {
                    if (val && item.params !== undefined && item.params.type === val) {
                        item.isSelected = true
                        item.isOpen = true
                    } else {
                        item.isOpen = false
                    }
                })
            },
            async getShowType (type) {
                const {
                    sideMenuList
                } = this
                if (type === 'customDir') {
                    const customRes = await this.requestPathInfo({ type: 'customDir' })
                    this.$store.commit('artifactory/updateRootSideMenuList', {
                        index: 1,
                        children: customRes
                    })
                    this.showType === type && this.$store.commit('artifactory/updateCurNodeOnTree', {
                        deepCount: 0,
                        index: 1,
                        roadMap: '1',
                        type: 'customDir',
                        item: { children: sideMenuList[0].list[1].children || [] }
                    })
                } else {
                    const pipelineRes = await this.requestPathInfo({ type: 'pipelines' })
                    this.$store.commit('artifactory/updateRootSideMenuList', {
                        index: 2,
                        children: pipelineRes
                    })
                    this.showType === type && this.$store.commit('artifactory/updateCurNodeOnTree', {
                        deepCount: 0,
                        index: 2,
                        roadMap: '2',
                        type: 'pipelines',
                        item: { children: sideMenuList[0].list[2].children || [] }
                    })
                }
            },
            /**
             * 切换项目后重置菜单参数
             */
            unsetSideMenuParams () {
                this.$store.commit('artifactory/updateRootSideMenuParams', {
                    index: 1,
                    params: {
                        type: 'customDir'
                    }
                })
                this.$store.commit('artifactory/updateRootSideMenuParams', {
                    index: 2,
                    params: {
                        type: 'pipelines'
                    }
                })
            },
            /**
             * 初始化仓库信息
             */
            async initTreeData () {
                const {
                    projectId,
                    showType
                } = this

                if (projectId) {
                    if (showType === 'customDir') {
                        await this.getShowType('customDir')
                    } else {
                        await this.getShowType('pipelines')
                    }
                }
            },
            /**
             * 获取路径信息
             */
            async requestPathInfo ({ type, path = '/', noLoading = false }) {
                const {
                    baseObj
                } = this
                let res = []
                // 排除初始化的流水线加载loading状态
                if (noLoading) {
                    this.firstPipelineLoad = false
                } else {
                    this.firstPipelineLoad && path === '/' ? this.folderLoading = false : this.folderLoading = true
                }

                try {
                    const resPathInfo = await this.$store.dispatch('artifactory/requestPathInfo', {
                        projectCode: this.projectId,
                        type: type === 'pipelines' ? 'PIPELINE' : 'CUSTOM_DIR',
                        path
                    })
                    res = resPathInfo.map(item => {
                        return Object.assign(item, baseObj, {
                            type: type
                        })
                    })
                } catch (err) {
                    if (err.code === 403) { // 没有查看权限
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId: this.$permissionActionMap.view,
                                resourceId: this.$permissionResourceMap.artifactory,
                                instanceId: [],
                                projectId: this.projectId
                            }],
                            applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=artifactory&project_code=${this.projectId}&service_code=artifactory&role_manager=artifactory`
                        })
                    }
                    // this.$bkMessage({
                    //     message: err ? err.message : err,
                    //     theme: 'error'
                    // })
                    console.log(err.message)
                }
                this.folderLoading = false
                return res
            },
            updateSideMenuList ({ newList, path }) {
                const {
                    list
                } = this.sideMenuList[0]

                if (path) {
                    let _list = list

                    while (_list.children && _list.children.length) {
                        if (_list.path !== path) {
                            _list = _list.children
                        } else {
                            this.$set(_list, 'children', newList)
                        }
                    }
                } else {
                    list.children.push(newList)
                }
            },
            /**
             * 树结构中，某个目录被点击
             */
            async branchClickHander (params) {
                const target = await this.getItems(params.roadMap, this.sideMenuList[0].list)
                this.$store.commit('artifactory/updateCurNodeOnTree', {
                    index: this.curNodeOnTree.index,
                    deepCount: this.curNodeOnTree.deepCount,
                    item: { children: target.children },
                    roadMap: this.curNodeOnTree.roadMap
                })
            },
            async getItems (roadMap, list, noLoading = false) {
                let path = ''
                let target = list
                roadMap = convertStrToNumArr(roadMap, ',')
                roadMap.forEach((_pathItem, index) => {
                    const _target = target[_pathItem]

                    path = _target.fullPath || ''

                    if (_target.list) {
                        target = _target.list
                    } else if (index !== roadMap.length - 1 && _target.children) {
                        target = _target.children
                    } else {
                        target = _target
                    }
                })

                target.loading = true
                if (path === '') {
                    path = '/'
                }
                const children = await this.requestPathInfo({
                    type: roadMap[0] > 1 ? 'pipelines' : 'customDir',
                    path,
                    projectCode: this.projectId,
                    noLoading
                })

                this.$set(target, 'children', children)
                this.$nextTick(() => {
                    target.loading = false
                })
                return target
            },
            async refresh () {
                const target = await this.getItems(this.curNodeOnTree.roadMap, this.sideMenuList[0].list)
                this.$store.commit('artifactory/updateCurNodeOnTree', {
                    index: this.curNodeOnTree.index,
                    deepCount: this.curNodeOnTree.deepCount,
                    item: { children: target.children },
                    roadMap: this.curNodeOnTree.roadMap
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    .artifactory {
        width: 100%;
        height: 100%;
        .artifactory-aside-bar {
            max-width: 260px;
            min-width: 260px;
            width: 260px;
            background: #fff;
            border-right: 1px solid #dde4eb;
            .side-menu-list {
                overflow: auto;
                height: calc( 100% - 25px);
                padding-top: 0px;
            }
        }
        .artifactory-main {
            overflow: auto;
            height: 92%;
            .artifactory-absolute {
                position: absolute;
                right: 40px;
            }
        }
        .bkc-menu-title-wrapper {
            &:hover {
                .right-icon.selected {
                    color: $primaryColor;
                }
            }
            .right-icon.selected {
                color: $fontWeightColor;
            }
        }
        .bkc-menu {
            ul {
                width: max-content;
                min-width: 100%;
            }
        }
        .bk-sideslider-wrapper {
            top: 0px;
        }
        .biz-container {
            position: relative;
        }
    }
</style>
