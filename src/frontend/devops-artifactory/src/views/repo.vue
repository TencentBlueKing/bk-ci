<template>
    <div class="artifactory-wrapper bix-content">
        <inner-header>
            <div slot="left"
                @click.stop="resetSelectedItem">
                <bk-breadcrumbs
                    :list="breadcrumbs">
                </bk-breadcrumbs>
            </div>
        </inner-header>
        <div v-if="folderLoading" class="folder-loading">
            <i class="devops-icon title-icon icon-circle-2-1 spin-icon" />
        </div>
        <template v-else>
            <section class="artifactory-main sub-view-port"
                @click.stop="resetSelectedItem">
                <div
                    class="artifactory-empty"
                    v-if="rowList.length === 0">
                    <div class="no-data-right">
                        <img src="./../images/box.png">
                        <p>{{ emptyTipsConfig.title }}</p>
                    </div>
                </div>
                <div class="artifactory-table"
                    v-else>
                    <div class="repo-table-simulator">
                        <div class="table-sim-head">
                            <div class="table-sim-item sim-item-name">名称</div>
                            <div class="table-sim-item sim-item-update">上次修改时间</div>
                            <div class="table-sim-item sim-item-size">文件大小</div>
                        </div>
                        <div class="table-sim-body">
                            <div class="table-sim-row"
                                v-for="(col, index) of rowList"
                                :class="{
                                    selected: lastClickIndex === index,
                                    'no-con-select': !textSelected
                                }"
                                :key="col.id"
                                @dblclick.stop="dbClick($event, index, col)"
                                @click.stop="selectRowHandler($event, index, col)">
                                <div class="table-sim-item sim-item-name" :title="`${col.name}`">
                                    <img :src="folder" alt="folder" v-if="col.folder">
                                    <i :class="['devops-icon', `icon-${extForFile(col.name)}`]"
                                        v-else></i>
                                    <span class="repo-name">{{ col.name }}</span>
                                </div>
                                <div class="table-sim-item sim-item-update">{{ convertInfoItem('modifiedTime', col.modifiedTime) }}</div>
                                <div class="table-sim-item sim-item-size" v-if="col.size !== -1">{{ convertInfoItem('size', col.size) }}</div>
                                <div class="table-sim-item sim-item-size" style="color:#3c96ff;cursor:pointer" @click.stop="getFolderSize(index, col)" v-if="col.folder && col.size === -1">计算</div>
                            </div>
                        </div>
                    </div>
                </div>
                <aside class="artifactory-ops">
                    <div class="artifactory-absolute">
                        <button type="button" class="bk-button bk-primary ops-button" @click.stop="showDetail()"
                            v-if="lastClickItem.name">查看详情</button>
                        <ul class="artifactory-ops-group" v-if="lastClickItem.name">
                            <template
                                v-if="lastClickIndex > -1">
                                <template
                                    v-if="!pipelineMap && lastClickItem.fullPath">
                                    <li @click.stop="renameRes()">
                                        <i class="devops-icon icon-edit"></i>重命名
                                    </li>
                                    <li @click.stop="moveRes()">
                                        <i class="devops-icon icon-move"></i>移动
                                    </li>
                                    <li @click.stop="copyRes()">
                                        <i class="devops-icon icon-save"></i>复制
                                    </li>
                                    <li @click.stop="deleteRes()">
                                        <i class="devops-icon icon-delete"></i>删除
                                    </li>
                                </template>
                                <li v-if="lastClickItem.folder === false" @click.stop="handlerShare()">
                                    <i class="devops-icon icon-none"></i>共享
                                </li>
                                <li
                                    @click.stop="handlerDownload()"
                                    v-if="lastClickItem.fullPath && lastClickItem.folder === false">
                                    <i class="devops-icon icon-download"></i>下载
                                </li>
                                <li
                                    v-if="isExtendTx && lastClickItem.fullPath && lastClickItem.folder === false && isWindows && isApkOrIpa() && isMof"
                                    @click.stop="handlerDownload($event, 'MoF')">
                                    <i class="devops-icon icon-download"></i>魔方有线安装
                                </li>
                            </template>
                            <template
                                v-else>
                                <li v-if="!pipelineMap"
                                    @click.stop="addFolder()">
                                    <i class="devops-icon icon-folder-plus"></i>新建文件夹
                                </li>
                                <li @click.stop="refreshFolder()">
                                    <i class="devops-icon icon-refresh"></i>刷新
                                </li>
                            </template>
                        </ul>
                    </div>
                </aside>
            </section>
        </template>

        <bk-sideslider
            class="artifactory-side-slider"
            :is-show.sync="sideSliderConfig.show"
            :title="sideSliderConfig.title"
            :quick-close="sideSliderConfig.quickClose"
            :width="sideSliderConfig.width">
            <template slot="content">
                <div class="artifactory-slider-info"
                    v-if="sideSliderConfig.show && sideSliderConfig.data"
                    v-bkloading="{
                        isLoading: sideSliderConfig.isLoading
                    }">
                    <bk-tab :active-name="'detailInfo'" type="unborder-card">
                        <bk-tab-panel name="detailInfo" label="基础信息">
                            <div class="detail-info">
                                <div class="detail-info-label"><span>Info</span></div>
                                <ul>
                                    <template v-for="(item, key) of sideSliderConfig.detailData.info">
                                        <li
                                            v-if="!(lastClickItem.folder && item.key === 'size')"
                                            :key="`detail${key}`">
                                            <span class="bk-lable">{{ item.name }}：</span>
                                            <span>{{ item.key === 'name' ? (sideSliderConfig.data[item.key] || lastClickItem.name) : convertInfoItem(item.key, sideSliderConfig.data[item.key]) }}</span>
                                        </li>
                                    </template>
                                </ul>
                            </div>
                            <div class="detail-info" v-if="!lastClickItem.folder">
                                <div class="detail-info-label"><span>Checksums</span></div>
                                <ul>
                                    <li v-for="(value, key) of sideSliderConfig.data.checksums"
                                        :key="`checksums${key}`">
                                        <span class="bk-lable">{{ key.toUpperCase() }}：</span>
                                        <span>{{ value }}</span>
                                    </li>
                                </ul>
                            </div>
                        </bk-tab-panel>
                        <bk-tab-panel name="metaDate" label="元数据" v-if="!lastClickItem.folder">
                            <table class="bk-table has-thead-bordered has-table-striped" v-if="Object.keys(sideSliderConfig.data.meta).length">
                                <thead>
                                    <tr>
                                        <th>属性键</th>
                                        <th>属性值</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="(value, key) of sideSliderConfig.data.meta"
                                        :key="`meta${key}`">
                                        <td>{{ key }}</td>
                                        <td>{{ convertInfoItem(key, value) }}</td>
                                    </tr>
                                </tbody>
                            </table>
                            <div v-else>
                                <div style="text-align:center;padding: 30px 0;">暂无元数据</div>
                            </div>
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            </template>
        </bk-sideslider>

        <share-dialog
            :share-config="shareConfig"
            @shared="sharedConfirm"
            @cancelShared="shareCancel">
        </share-dialog>

        <bk-dialog
            ext-cls="preview-container"
            :value="previewConfig.isShow"
            :width="1200"
            :close-icon="false"
            :show-footer="false"
            :position="{ top: '100' }">
            <section class="preview-dialog">
                <div class="preview-header">
                    <div class="preview-header-left">
                        <i @click.stop="previewCancel()" class="devops-icon icon-arrows-left"></i>
                        {{previewConfig.title}}
                    </div>
                </div>
                <div v-if="previewConfig.canShowExt"><pre class="preview-body">{{ previewConfig.context }}</pre></div>
                <div class="preview-body-empty" v-else>
                    不支持该类文件的预览
                </div>
            </section>
        </bk-dialog>
        <no-permission :permission-config="permissionConfig" @cancel="cancelHandler"></no-permission>

        <bk-dialog
            class="simple-dialog"
            v-model="simpleConfig.isShow"
            :title="simpleConfig.title"
            :close-icon="false"
            :quick-close="false"
            :loading="simpleConfig.loading"
            width="600"
            padding="'0 30px 30px 30px'"
            header-position="left"
            @confirm="simpleConfirmHandler"
            @cancel="simpleCancelHandler">
            <section class="bk-form bk-form-vertical">
                <div class="bk-form-item is-required" v-if="simpleConfig.title">
                    <label class="bk-label">名称</label>
                    <div class="bk-form-content">
                        <input
                            v-if="simpleConfig.isShow"
                            id="nameInput"
                            class="bk-form-input"
                            placeholder="请输入名称"
                            v-model="simpleConfig.name"
                            :name="'name'"
                            v-validate="{ required: true, regex: /^[a-zA-Z0-9\.\-\_]{1,200}$/ }"
                            :class="{ 'is-danger': errors.has('name') }"
                            v-bk-focus
                        />
                        <div v-if="errors.has('name')" class="error-tips">名称只能包含字母数字、英文句号、下划线、中划线</div>
                    </div>
                </div>
            </section>
        </bk-dialog>

        <bk-dialog
            class="simple-dialog"
            v-model="treeDialogConfig.isShow"
            :title="treeDialogConfig.title"
            :close-icon="false"
            :quick-close="false"
            width="600"
            padding="'0 30px 30px 30px'"
            header-position="left"
            @confirm="treeConfirmHandler"
            @cancel="treeCancelHandler">
            <section class="tree-view">
                <div v-if="treeDialogConfig.isLoading" class="folder-loading" style="padding-top:20px">
                    <i class="devops-icon title-icon icon-circle-2-1 spin-icon" />
                </div>
                <div v-if="treeDialogConfig.title">
                    <bk-trees
                        v-if="!treeDialogConfig.isLoading"
                        :key="'dia-tree'"
                        :list="treeDialogConfig.list"
                        :deep-count="1"
                        :road-map="0">
                    </bk-trees>
                </div>
            </section>
        </bk-dialog>
    </div>
</template>

<script>
    import { bus } from './../utils/bus'
    import innerHeader from '@/components/devops/inner_header'
    import bkBreadcrumbs from '@/components/common/bk-breadcrumbs'
    import folder from '@/images/folder.png'
    import shareDialog from '@/components/devops/shares'
    import bkTrees from '@/components/common/bk-trees/dialogTree.vue'
    import { mapGetters, mapState } from 'vuex'
    import {
        convertTime,
        convertFileSize
    } from '@/utils/util'

    export default {
        components: {
            'inner-header': innerHeader,
            'bk-breadcrumbs': bkBreadcrumbs,
            'share-dialog': shareDialog,
            bkTrees
        },
        props: {
            folderLoading: {
                type: Boolean,
                default: false
            },
            permissionConfig: {
                type: Object
            }
        },
        data () {
            return {
                searchConfig: {
                    value: ''
                },
                rowList: [],
                textSelected: true, // 文字是否可选中
                rowSelectedList: [false], // 保存每一行是否选中的状态
                lastClickIndex: -1, // 最后一个点击的item的index
                lastClickItem: {}, // 最后一个点击的item
                sideSliderConfig: {
                    show: false,
                    title: '查看详情',
                    quickClose: true,
                    width: 820,
                    data: {},
                    detailData: {
                        info: [
                            { key: 'name', name: 'Name' },
                            { key: 'fullName', name: 'Path' },
                            { key: 'size', name: 'Size' },
                            { key: 'createdTime', name: 'Created' },
                            { key: 'modifiedTime', name: 'Last Modified' }
                        ]
                    },
                    isLoading: false
                },
                shareConfig: {
                    isShow: false,
                    title: '',
                    fileUri: '',
                    loading: false
                },
                timer: null,
                pipelineMap: 0,
                emptyTipsConfig: {
                    title: ''
                },
                previewConfig: {
                    isShow: false,
                    title: '文件预览',
                    context: '',
                    canShowExt: true,
                    allowExts: ['.txt', '.json', '.md'],
                    maxSize: 1024 * 1024
                },
                folder,
                iconExts: {
                    txt: ['.json', '.txt', '.md'],
                    zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar'],
                    apkfile: ['.apk'],
                    ipafile: ['.ipa']
                },
                simpleConfig: { // 新建文件夹 || 重命名
                    isShow: false,
                    title: '',
                    type: '',
                    name: '',
                    loading: false
                },
                treeDialogConfig: { // 复制 || 移动
                    isShow: false,
                    isLoading: false,
                    title: '',
                    // handler: this.changeDialogTree,
                    list: [{
                        arrowIcon: 'icon-right-shape',
                        arrowOpenIcon: 'icon-down-shape',
                        icon: 'icon-folder',
                        openIcon: 'icon-folder-open',
                        name: '自定义仓库',
                        isOpen: true,
                        isSelected: false,
                        fullPath: '/',
                        folder: true,
                        type: 'customDir',
                        children: []
                    }]
                }
            }
        },
        computed: {
            ...mapState('artifactory', [
                'projectList'
            ]),
            ...mapGetters({
                curNodeOnTree: 'artifactory/getCurNodeOnTree',
                curDialogTree: 'artifactory/getCurDialogTree',
                sideMenuList: 'artifactory/getSideMenuList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            isWindows () {
                return /WINDOWS/.test(window.navigator.userAgent.toUpperCase())
            },
            isMof () {
                const projectId = this.$route.params.projectId
                return this.projectList.find(item => {
                    return (item.deptName === '魔方工作室群' && item.projectCode === projectId)
                })
            },
            isExtendTx () {
                return VERSION_TYPE === 'tencent'
            },
            breadcrumbs () {
                const breadcrumbs = []
                const mapArr = this.curNodeOnTree.roadMap.toString().split(',').map(item => parseInt(item))
                let item = this.sideMenuList[0].list
                for (let i = 0; i < mapArr.length; i++) {
                    const index = mapArr[i]
                    let disabled = false
                    if (i === mapArr.length - 1) {
                        disabled = true
                    }

                    const breadItem = {
                        text: item[index].name,
                        disabled,
                        handler: this.handleClickBread,
                        count: i
                    }
                    breadcrumbs.push(breadItem)
                    item = item[index].children
                }
                return breadcrumbs
            }
        },
        watch: {
            'curNodeOnTree.item.children' (val) {
                if (val) {
                    this.rowList.splice(0, this.rowList.length, ...val)
                    this.lastClickItem = {}
                    this.lastClickIndex = -1
                }
                const roadMap = this.curNodeOnTree.roadMap.toString()
                if (roadMap === '1') {
                    this.emptyTipsConfig.title = '当前自定义仓库为空'
                } else if (roadMap === '2') {
                    this.emptyTipsConfig.title = '当前流水线仓库为空'
                } else {
                    this.emptyTipsConfig.title = '当前文件夹为空'
                }

                this.resetSelectedItem()
            },
            'curNodeOnTree.roadMap' (val) {
                if (val) {
                    const roadMap = val.toString().match(/^2(,|$)/)
                    this.pipelineMap = roadMap ? 1 : 0
                }
            }
        },
        created () {
            // 点击左侧菜单空白区域取消选中
            window.addEventListener('click', (e) => {
                if (!e.target.className.includes('side-menu-list')) {
                    e.stopPropagation()
                } else {
                    this.resetSelectedItem()
                }
            }, false)
            bus.$off('dialog-tree-click')
            bus.$on('dialog-tree-click', (data) => {
                this.changeDialogTree(data)
            })
        },
        methods: {
            async changeDialogTree (data) {
                this.$emit('getItems', data, this.treeDialogConfig.list, true)
                // await this.getItems(data, this.treeDialogConfig.list)
            },
            /**
             *  双击选中
             */
            async dbClick ($event, index, col) {
                this.timer && clearTimeout(this.timer)
                if (!col.folder) {
                    const {
                        previewConfig
                    } = this
                    this.lastClickItem = col
                    this.lastClickIndex = index
                    const fileExt = col.name.slice(col.name.lastIndexOf('.'))
                    if (fileExt && previewConfig.allowExts.filter(ext => ext === fileExt).length === 1 && col.size <= previewConfig.maxSize) {
                        try {
                            const url = await this.getDownloadUrl(col)
                            const context2 = await this.$ajax.get(url, {
                                transformResponse: [(response) => {
                                    return {
                                        result: true,
                                        data: response
                                    }
                                }]
                            })
                            previewConfig.isShow = true
                            previewConfig.canShowExt = true
                            previewConfig.title = `文件预览（${col.name}）`
                            previewConfig.context = context2
                        } catch (err) {
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message || err
                            })
                        }
                    } else {
                        previewConfig.canShowExt = false
                        previewConfig.isShow = true
                        previewConfig.title = `文件预览（${col.name}）`
                    }
                    return
                }
                this.lastClickItem = {}
                this.textSelected = false
                const data = Object.assign({}, {
                    index: index,
                    deepCount: this.curNodeOnTree.deepCount,
                    item: this.curNodeOnTree.item.children[index],
                    src: 'right'
                })
                bus.$emit('tree-item-click', data)
            },
            // 关闭预览窗口
            previewCancel () {
                this.previewConfig.isShow = false
            },
            selectRowHandler ($event, index, row) {
                const _this = this
                this.lastClickIndex = index
                this.textSelected = true
                this.timer && clearTimeout(this.timer)
                this.timer = setTimeout(function () {
                    _this.lastClickItem = row
                    // _this.rowSelectedList[index] = !_this.rowSelectedList[index] // 多选
                }, 300)
            },
            /**
             *  查看详情
             */
            async showDetail () {
                const { sideSliderConfig } = this
                const {
                    lastClickItem,
                    projectId
                } = this
                try {
                    sideSliderConfig.isLoading = true
                    const type = this.pipelineMap ? 'PIPELINE' : 'CUSTOM_DIR'
                    const res = await this.$store.dispatch('artifactory/requestFileInfo', {
                        projectCode: projectId,
                        type: type,
                        path: `${lastClickItem.fullPath}`
                    })
                    sideSliderConfig.show = true
                    sideSliderConfig.title = res.name || lastClickItem.name
                    sideSliderConfig.data = res
                    sideSliderConfig.isLoading = false
                } catch (err) {
                    if (err.code === 403) {
                        // this.setPermissionConfig('查看')
                    } else {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
                }
            },
            /**
             * 转换参数
             */
            convertInfoItem (key, value) {
                if (key.includes('Time')) {
                    return convertTime(value * 1000)
                } else if (key.includes('size')) {
                    return (value >= 0 && convertFileSize(value, 'B')) || ''
                } else {
                    return value
                }
            },
            /**
             * 检测devnet网关的连通性
             */
            async getDevnetGateway () {
                try {
                    const res = await this.$ajax.get('/artifactory/api/user/artifactories/checkDevnetGateway')
                    return res
                } catch (err) {
                    return false
                }
            },
            /**
             * 文件下载地址
             */
            async getDownloadUrl (item) {
                const {
                    projectId
                } = this
                const type = this.pipelineMap ? 'PIPELINE' : 'CUSTOM_DIR'
                try {
                    const isDevnet = await this.getDevnetGateway()
                    const res = await this.$store.dispatch('artifactory/requestDownloadUrl', {
                        projectId,
                        type: type,
                        path: `${item.fullPath}`
                    })
                    const url = isDevnet ? res.url : res.url2
                    return url
                } catch (err) {
                    if (err.code === 403) { // 没有权限下载
                        const res = await this.$store.dispatch('artifactory/requestFilePipelineInfo', {
                            projectCode: projectId,
                            type: type,
                            path: item.fullPath
                        })
                        const pipelineId = res.pipelineId
                        const instanceId = [{
                            id: pipelineId,
                            name: res.pipelineName || pipelineId
                        }]
                        // 下载构件
                        this.setPermissionConfig(instanceId, pipelineId)
                    } else {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
                }
            },
            /**
             * 下载
             */
            async handlerDownload (event, type) {
                const url = await this.getDownloadUrl(this.lastClickItem)
                url && window.open(type ? `${API_URL_PREFIX}/pc/download/devops_pc_forward.html?downloadUrl=${url}` : url, '_self')
            },
            /**
             * 共享
             */
            handlerShare () {
                const { shareConfig } = this
                const { lastClickItem } = this
                shareConfig.title = `与他人共享（${lastClickItem.name}）`
                shareConfig.isShow = true
                shareConfig.fileUri = lastClickItem.fullPath
            },
            async sharedConfirm (params) {
                const {
                    lastClickItem,
                    projectId
                } = this
                let theme, message
                const type = this.pipelineMap ? 'PIPELINE' : 'CUSTOM_DIR'
                try {
                    const res = await this.$store.dispatch('artifactory/requestShareUrl', {
                        projectId,
                        type: type,
                        path: `${lastClickItem.fullPath}`,
                        ttl: Math.round(params.expire * 86400),
                        downloadUsers: params.members.join(',')
                    })
                    message = res ? '共享成功，请注意查收邮件' : '共享失败'
                    theme = res ? 'success' : 'error'
                } catch (err) {
                    if (err.code === 403) { // 没有权限共享
                        const res = await this.$store.dispatch('artifactory/requestFilePipelineInfo', {
                            projectCode: projectId,
                            type: type,
                            path: lastClickItem.fullPath
                        })
                        const pipelineId = res.pipelineId
                        const instanceId = [{
                            id: pipelineId,
                            name: res.pipelineName || pipelineId
                        }]
                        // 分享构件
                        this.setPermissionConfig(instanceId, pipelineId)
                    } else {
                        theme = 'error'
                        message = err.message || err
                    }
                } finally {
                    message && this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            shareCancel () {
                this.shareConfig.isShow = false
            },
            handleClickBread ({ count }) {
                const maps = this.curNodeOnTree.roadMap.toString().split(',').filter((item, index) => index <= count).map(item => parseInt(item))
                const item = this.getClickItem(maps)

                const data = Object.assign({}, {
                    index: maps[count],
                    deepCount: count + 1,
                    item: item,
                    src: 'right',
                    roadMap: maps.join(',')
                })
                bus.$emit('tree-item-click', data)
            },
            getClickItem (maps = []) {
                const list = this.sideMenuList[0].list
                let item = null
                if (maps.length) {
                    item = list
                    for (let i = 0; i < (maps.length - 1); i++) {
                        const index = maps[i]
                        item = item[index].children
                    }
                    const lastIndex = maps.length - 1
                    const lastItem = maps[lastIndex]
                    item = item[lastItem]
                }
                return item
            },
            /**
             * 取消选中
             */
            resetSelectedItem () {
                this.lastClickIndex = -1
                const maps = this.curNodeOnTree.roadMap.toString().split(',').map(item => parseInt(item))
                const item = this.getClickItem(maps)
                this.lastClickItem = item
            },
            /**
             * 判断文件类型
             */
            extForFile (name) {
                const { iconExts } = this
                let icon
                const names = name.split('.')
                if (names.length > 1) {
                    const ext = `.${names[names.length - 1]}`
                    const ext2 = names.length > 2 ? `.${names[names.length - 2]}.${names[names.length - 1]}` : ''
                    Object.keys(iconExts).forEach(key => {
                        if (!icon) {
                            iconExts[key].forEach(item => {
                                if (ext === item || ext2 === item) {
                                    icon = key
                                }
                            })
                        }
                    })
                }
                return icon || 'file'
            },
            setPermissionConfig (instanceId, pipelineId) {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        actionId: this.$permissionActionMap.view,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId,
                        projectId: this.projectId
                    }],
                    applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_viewer=pipeline:${pipelineId}`
                })
            },
            cancelHandler () {
                this.permissionConfig.isShow = false
            },
            /**
             * 刷新文件夹
             */
            refreshFolder () {
                this.$emit('refresh')
            },
            /**
             * 新建文件夹
             */
            addFolder () {
                const {
                    simpleConfig
                } = this
                simpleConfig.isShow = true
                simpleConfig.title = '新建文件夹'
                simpleConfig.type = 'folder'
                simpleConfig.name = ''
                simpleConfig.loading = false
                this.$validator.reset()
            },
            /**
             * 重命名文件
             */
            renameRes () {
                const {
                    simpleConfig,
                    lastClickItem
                } = this
                simpleConfig.isShow = true
                simpleConfig.title = '重命名'
                simpleConfig.type = 'rename'
                simpleConfig.name = lastClickItem.name
                this.$validator.reset()
            },
            /**
             * 新建文件夹or重命名的确认回调
             */
            simpleConfirmHandler () {
                // this.simpleConfig.loading = true
                this.$validator.validateAll().then(async result => {
                    if (result) {
                        const {
                            projectId,
                            simpleConfig,
                            lastClickItem
                        } = this
                        let theme, message
                        try {
                            let res
                            const fullPath = lastClickItem.fullPath
                            if (simpleConfig.type === 'folder') {
                                res = await this.$store.dispatch('artifactory/requestMakeDir', {
                                    projectId,
                                    path: `${fullPath}${simpleConfig.name}/`
                                })
                            } else {
                                const srcPath = fullPath
                                let destPath = `${fullPath.slice(0, fullPath.lastIndexOf('/'))}/${simpleConfig.name}`
                                if (this.lastClickItem.folder === true) {
                                    const tmpPath = fullPath.slice(0, fullPath.length - 1)
                                    destPath = `${tmpPath.slice(0, tmpPath.lastIndexOf('/'))}/${simpleConfig.name}/`
                                }
                                res = await this.$store.dispatch('artifactory/requestRename', {
                                    projectId,
                                    srcPath,
                                    destPath
                                })
                            }
                            const mes = simpleConfig.type === 'folder' ? '新建文件夹' : '重命名'

                            theme = res ? 'success' : 'error'
                            if (res) {
                                this.refreshFolder()
                                message = res ? `${mes}成功` : `${mes}失败`
                            }
                        } catch (err) {
                            if (err.code === 403) { // 没有权限新建or编辑
                                simpleConfig.isShow = false
                                // this.setPermissionConfig(simpleConfig.type === 'folder' ? '新建' : '编辑')
                            } else {
                                theme = 'error'
                                message = err.message || err
                            }
                        } finally {
                            message && this.$bkMessage({
                                message,
                                theme
                            })
                        }
                    }
                })
            },
            simpleCancelHandler () {
                this.simpleConfig.isShow = false
            },
            async requestPathInfo (path = '/') {
                const baseObj = {
                    icon: 'icon-folder',
                    openIcon: 'icon-folder-open',
                    arrowIcon: 'icon-right-shape',
                    arrowOpenIcon: 'icon-down-shape',
                    isOpen: false,
                    loading: false
                }
                const type = 'CUSTOM_DIR'
                let res = []
                try {
                    const resPathInfo = await this.$store.dispatch('artifactory/requestPathInfo', {
                        projectCode: this.projectId,
                        type,
                        path
                    })
                    res = resPathInfo.map(item => {
                        return Object.assign(item, baseObj)
                    })
                } catch (err) {
                    // this.$bkMessage({
                    //     message: err ? err.message : err,
                    //     theme: 'error'
                    // })
                    console.log(err.message)
                }
                return res
            },
            /**
             * 移动
             */
            async moveRes () {
                const {
                    treeDialogConfig
                } = this
                treeDialogConfig.isShow = true
                treeDialogConfig.title = `将(${this.lastClickItem.name})移动到...`
                treeDialogConfig.action = 'move'
                this.resetDialogTree()
            },
            /**
             * 复制
             */
            async copyRes () {
                const {
                    treeDialogConfig
                } = this
                treeDialogConfig.isShow = true
                treeDialogConfig.title = `将(${this.lastClickItem.name})复制到...`
                treeDialogConfig.action = 'copy'
                this.resetDialogTree()
            },
            /**
             * 复制or移动的确认回调
             */
            async treeConfirmHandler () {
                const {
                    projectId,
                    treeDialogConfig,
                    lastClickItem,
                    curDialogTree
                } = this
                let theme, message
                const srcPaths = []
                const destPath = curDialogTree.fullPath
                try {
                    let res
                    srcPaths.push(lastClickItem.fullPath)
                    if (treeDialogConfig.action === 'move') {
                        res = await this.$store.dispatch('artifactory/requestMove', {
                            projectId,
                            srcPaths,
                            destPath
                        })
                    } else {
                        res = await this.$store.dispatch('artifactory/requestCopy', {
                            projectId,
                            srcPaths,
                            destPath
                        })
                    }
                    const mes = treeDialogConfig.action === 'move' ? '移动' : '复制'

                    theme = res ? 'success' : 'error'
                    if (res) {
                        this.refreshFolder()
                        message = res ? `${mes}成功` : `${mes}失败`
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限新建or编辑
                        // this.setPermissionConfig(treeDialogConfig.action === 'move' ? '移动' : '复制')
                    } else {
                        theme = 'error'
                        message = err.message || err
                    }
                } finally {
                    treeDialogConfig.isShow = false
                    message && this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            treeCancelHandler () {
                this.treeDialogConfig.isShow = false
            },
            async resetDialogTree () {
                this.treeDialogConfig.isLoading = true
                const res = await this.requestPathInfo('/')
                this.treeDialogConfig.list[0].children = res
                this.treeDialogConfig.isLoading = false
                this.$store.commit('artifactory/updateDialogTree', {
                    roadMap: '0',
                    fullPath: '/'
                })
            },
            /**
             * 删除
             */
            async deleteRes () {
                const {
                    projectId,
                    lastClickItem
                } = this
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `删除${lastClickItem.folder ? '文件夹' : '文件'} ${lastClickItem.name}`)

                this.$bkInfo({
                    title: '确认',
                    content,
                    confirmFn: async () => {
                        const {
                            simpleConfig
                        } = this
                        let theme, message
                        try {
                            const res = await this.$store.dispatch('artifactory/requestDelete', {
                                projectId,
                                paths: [`${lastClickItem.fullPath}`]
                            })
                            theme = res ? 'success' : 'error'
                            if (res) {
                                this.refreshFolder()
                                message = res ? '删除成功' : '删除失败'
                            }
                        } catch (err) {
                            if (err.code === 403) { // 没有权限新建or编辑
                                simpleConfig.isShow = false
                            } else {
                                theme = 'error'
                                message = err.message || err
                            }
                        } finally {
                            message && this.$bkMessage({
                                message,
                                theme
                            })
                        }
                    }
                })
            },
            async getFolderSize (index, item) {
                if (item.folder) {
                    try {
                        const type = this.pipelineMap ? 'PIPELINE' : 'CUSTOM_DIR'
                        const res = await this.$store.dispatch('artifactory/requestFolderSize', {
                            projectId: this.projectId,
                            type,
                            path: item.fullPath
                        })
                        item.size = res.size
                    } catch (err) {
                        if (err.code === 403) {
                            // this.setPermissionConfig('查看')
                        } else {
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message || err
                            })
                        }
                    }
                }
            },
            isApkOrIpa () {
                const type = this.lastClickItem.name.toUpperCase().substring(this.lastClickItem.name.lastIndexOf('.') + 1)
                return type === 'APK' || type === 'IPA'
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';

    $selectedBorderColor: #c1ddfe;

    %flex {
        display: flex;
        align-items: center;
    }
    $asideWidth: 304px;

    .artifactory-wrapper {
        width: 100%;
        .inner-header-left {
            // width: calc(100% - 268px);
            width: 100%;
            overflow: hidden;
            .bk-breadcrumbs-item {
                overflow: hidden;
                span {
                    max-width: 175px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    float: left;
                }
                @media screen and (min-width:1920px) {
                    span {
                        max-width: 260px;
                    }
                }
            }
        }
        .inner-header-right {
            // width: 268px;
            width: 0;
        }
        .artifactory-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                margin-top: 200px;
                p {
                    line-height: 60px;
                }
            }
        }
        .artifactory-search {
            float: right;
            margin-top: 12px;
            margin-right: 20px;
            right: 0;
        }
        .folder-loading {
            text-align: center;
            padding-top: 60px;
            font-size: 24px;
            i {
                display: inline-block;
            }
        }
        .artifactory-main {
            display: flex;
        }
        .artifactory-table {
            flex: 1;
            width: 700px;
            tbody tr:hover {
                background-color: #fff;
            }
        }
        .artifactory-ops {
            width: $asideWidth;
            text-align: center;
            .ops-button {
                margin-top: 51px;
                width: 224px;
            }
            &-group {
                font-size: 14px;
                color: $primaryColor;
                line-height: 32px;
                text-align: left;
                width: 200px;
                margin: 22px auto;
                li {
                    cursor: pointer;
                }
                i {
                    font-size: 16px;
                    padding-right: 9px;
                }
            }
        }
        .sim-item-name {
            padding-left: 20px;
            i {
                font-size: 30px;
                width: 38px;
                color: #C3CDD7;
            }
        }
        .table-sim-head {
            @extend %flex;
            height: 52px;
            color: $fontColor;
            font: {
                weight: bold;
                size: 14px;
            }
        }
        .table-sim-body {
            .table-sim-row {
                @extend %flex;
                height: 60px;
                border-top: 1px solid $borderColor;
                cursor: default;
                font-size: 14px;
                &:hover {
                    background-color: #fff;
                }
                &:first-child {
                    border-top-color: $borderWeightColor;
                }
                &:last-child {
                    border-bottom: 1px solid $borderColor;
                }
                &.selected {
                    border-top-color: $selectedBorderColor;
                    background-color: $primaryLightColor;
                    & + .table-sim-row {
                        border-top-color: $selectedBorderColor;
                    }
                    &:last-child {
                        border-bottom-color: $selectedBorderColor;
                    }
                }
                &.no-con-select {
                    -webkit-user-select: none;
                    -moz-user-select: none;
                    -ms-user-select: none;
                    user-select: none;
                }
            }
            .sim-item-name {
                @extend %flex;
                .repo-name {
                    position: relative;
                    top: 1px;
                    margin-left: 10px;
                    padding-right: 8px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    width: 100%;
                }
            }
        }
        .table-sim-item {
            overflow: hidden;
        }
        .sim-item-name {
            flex: 5;
        }
        .sim-item-source,
        .sim-item-update {
            flex: 2;
        }
        .sim-item-size {
            flex: 1;
        }
        .artifactory-slider-info {
            padding: 5px 50px;
            .bk-tab2 {
                border: none;
                .bk-table {
                    margin-top: 18px;
                    th, td {
                        height: 42px;
                    }
                }
                .empty-tips {
                    text-align: center;
                    padding: 50px;
                }
            }
            .detail-info {
                border: 1px solid $borderWeightColor;
                padding: 15px;
                margin: 40px 0;
                font-size: 14px;
                &-label {
                    font-weight: 700;
                    margin-top: -25px;
                    span {
                        padding: 0 10px;
                        background: #fff;
                    }
                }
                ul {
                    padding-top: 5px;
                    span {
                        padding: 10px;
                        width: calc(100% - 165px);
                        display: inline-block;
                        word-break: break-all;
                        vertical-align: top;
                    }
                    .bk-lable {
                        width: 160px;
                        text-align: right;
                    }
                }
            }
        }
    }
    .preview-container {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0px;
        }
        .preview-dialog {
            .preview-header {
                height: 56px;
                color: $fontBoldColor;
                padding: 0 50px 0 15px;
                border-bottom: 1px solid $borderWeightColor;
                background: $bgHoverColor;
                overflow: hidden;
                .preview-header-left {
                    line-height: 56px;
                    text-align: left;
                    font-size: 18px;
                    i {
                        width: 16px;
                        color: $iconPrimaryColor;
                        padding-right: 7px;
                        cursor: pointer;
                    }
                }
                .preview-header-right {
                    position: absolute;
                    right: 50px;
                    top: 12px;
                    float: right;
                }
            }
            .preview-body {
                padding: 30px 50px;
                overflow: auto;
                word-wrap: break-word;
                height: 660px;
                line-height: 1.3;
            }
            .preview-body-empty {
                padding: 100px 50px;
                text-align: center;
                font-size: 18px;
                margin-top: 50px;
                height: 660px;
            }
        }
    }
    .simple-dialog {
        .bk-dialog-title {
            text-align: left;
        }
        .tree-view {
            border: 1px solid $fontLighterColor;
            padding: 20px 0;
            margin: 20px 0 0;
            max-height: 500px;
            overflow: auto;
        }
    }
</style>
