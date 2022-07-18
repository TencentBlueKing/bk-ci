<template>
    <div class="artifactory-home-wrapper bix-content">
        <inner-header>
            <div slot="left">
                所有构件
            </div>
        </inner-header>
        <section class="artifactory-main sub-view-port" ref="scrollBox">
            <div class="artifactory-table">
                <div class="artifactory-table-pipeline">
                    <div v-if="pipelineLoading" class="folder-loading">
                        <i class="devops-icon title-icon icon-circle-2-1 spin-icon" />
                    </div>
                    <div v-else class="repo-table-simulator">
                        <div
                            class="artifactory-empty"
                            v-if="rowList.length === 0">
                            <div class="no-data-right">
                                <img src="./../images/box.png">
                                <p>暂时没有任何构件</p>
                            </div>
                        </div>
                        <section v-else>
                            <div class="table-sim-head">
                                <div class="table-sim-item sim-item-name">名称</div>
                                <div class="table-sim-item sim-item-source">路径</div>
                                <div class="table-sim-item sim-item-update">上次修改时间</div>
                                <div class="table-sim-item sim-item-size">文件大小</div>
                                <div class="table-sim-item sim-item-type">仓库类型</div>
                            </div>
                            <div class="table-sim-body">
                                <div class="table-sim-row"
                                    v-for="(col, index) of rowList"
                                    :class="{
                                        selected: lastClickIndex === index
                                    }"
                                    :key="col.id"
                                    @click.stop="selectRowHandler($event, index, col)">
                                    <div class="table-sim-item sim-item-name" :title="`${col.name}`">
                                        <i :class="['devops-icon', `icon-${extForFile(col.name)}`]"></i>
                                        <span class="repo-name">{{ col.name }}</span>
                                    </div>
                                    <div class="table-sim-item sim-item-source" :title="`${col.fullName}`">{{ col.fullName }}</div>
                                    <div class="table-sim-item sim-item-update">{{ calcLatestTime(col.modifiedTime) }}</div>
                                    <div class="table-sim-item sim-item-size">{{ convertInfoItem('size', col.size) }}</div>
                                    <div class="table-sim-item sim-item-type">{{ col.artifactoryType === 'PIPELINE' ? '流水线仓库' : '自定义仓库' }}</div>
                                </div>
                                <div v-if="moreLoading" class="folder-loading">
                                    <i class="devops-icon title-icon icon-circle-2-1 spin-icon" />
                                </div>
                            </div>
                        </section>
                    </div>
                </div>
            </div>
            <aside class="artifactory-ops">
                <div class="artifactory-absolute" v-if="lastClickItem.name">
                    <button type="button" class="bk-button bk-primary ops-button"
                        @click.stop="showDetail()">查看详情</button>
                    <ul class="artifactory-ops-group">
                        <li @click.stop="handlerShare()">
                            <i class="devops-icon icon-none"></i>共享
                        </li>
                        <li @click.stop="handlerDownload()">
                            <i class="devops-icon icon-download"></i>下载
                        </li>
                        <li @click.stop="handlerDownload($event, 'MoF')" v-if="isExtendTx && isWindows && isApkOrIpa() && isMof">
                            <i class="devops-icon icon-download"></i>魔方有线安装
                        </li>
                        <li @click.stop="refresh()">
                            <i class="devops-icon icon-refresh"></i>刷新
                        </li>
                    </ul>
                </div>
            </aside>
        </section>

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

        <no-permission :permission-config="permissionConfig" @cancel="cancelHandler"></no-permission>
    </div>
</template>

<script>
    import innerHeader from '@/components/devops/inner_header'
    import folder from '@/images/folder.png'
    import shareDialog from '@/components/devops/shares'
    import { mapState } from 'vuex'

    import {
        convertTime,
        convertFileSize,
        convertMStoString
    } from '@/utils/util'

    export default {
        components: {
            innerHeader,
            shareDialog
        },
        props: {
            permissionConfig: {
                type: Object
            }
        },
        data () {
            return {
                pipelineLoading: false,
                moreLoading: false,
                rowList: [],
                currentTimestamp: 0,
                lastClickIndex: -1,
                lastClickItem: {},
                folder,
                config: {
                    page: 1,
                    pageSize: 30
                },
                iconExts: {
                    txt: ['.json', '.txt', '.md'],
                    zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar'],
                    apkfile: ['.apk'],
                    ipafile: ['.ipa']
                },
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
                    fileUri: ''
                },
                scrollDisable: false,
                searchKeys: {} // 搜索条件
            }
        },
        computed: {
            ...mapState('artifactory', [
                'projectList'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            searchKeysLen () {
                return Object.keys(this.searchKeys).length || 0
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
            }
        },
        watch: {
            projectId (val) {
                this.searchKeys = {}
                this.refresh()
                this.scrollDisable = false
            },
            '$route.query' (val) {
                this.searchKeys = val
                this.refresh()
                this.scrollDisable = false
            }
        },
        async created () {
            if (Object.keys(this.$route.query).length) {
                this.searchKeys = this.$route.query
            }
            await this.init()

            this.$refs.scrollBox.addEventListener('scroll', this.handleScroll)
        },
        beforeDestroy () {
            this.$refs.scrollBox.removeEventListener('scroll', this.handleScroll)
        },
        methods: {
            /**
             * 跳转
             */
            goToList (type) {
                this.$router.push({
                    name: 'artifactoryList',
                    params: {
                        type
                    }
                })
            },
            selectRowHandler ($event, index, row) {
                this.lastClickIndex = index
                this.lastClickItem = row
            },
            async init () {
                this.config.page = 1
                this.pipelineLoading = true
                let res = []
                try {
                    if (this.searchKeysLen) {
                        res = await this.requestSearchList(this.config.page)
                    } else {
                        res = await this.requestFileList(this.config.page)
                    }
                    this.rowList = res.records || []
                    this.currentTimestamp = res.timestamp
                    this.config.page = this.config.page + 1
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
                this.pipelineLoading = false
            },
            async refresh () {
                await this.init()
                this.lastClickIndex = -1
                this.lastClickItem = {}
            },
            async requestFileList (page) {
                const {
                    $store,
                    config
                } = this
                let response = []
                try {
                    response = await $store.dispatch('artifactory/requestOwnFileList', {
                        projectId: this.projectId,
                        page,
                        pageSize: config.pageSize
                    })
                } catch (err) {
                    if (err.code === 403) { // 没有权限查看
                        // this.setPermissionConfig('查看')
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }
                return response
            },
            /**
             * 搜索请求
             */
            async requestSearchList (page) {
                const {
                    $store,
                    config
                } = this
                let response = []
                try {
                    response = await $store.dispatch('artifactory/requestSearchList', {
                        projectId: this.projectId,
                        page,
                        pageSize: config.pageSize,
                        params: {
                            props: {
                                ...this.searchKeys
                            }
                        }
                    })
                } catch (err) {
                    if (err.code === 403) { // 没有权限查看
                        // this.setPermissionConfig('查看')
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }
                return response
            },
            calcLatestTime (time) {
                if (this.currentTimestamp && time) {
                    let result = convertMStoString((this.currentTimestamp - time) * 1000).match(/^[0-9]{1,}([\u4e00-\u9fa5]){1,}/)[0]
                    if (result.indexOf('分') > 0) {
                        result += '钟'
                    }
                    return `${result}前`
                } else {
                    return '--'
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
                    const res = await this.$store.dispatch('artifactory/requestFileInfo', {
                        projectCode: projectId,
                        type: `${lastClickItem.artifactoryType}`,
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
             * 共享
             */
            handlerShare () {
                const { shareConfig } = this
                const { lastClickItem } = this
                if (lastClickItem.name.length > 28) {
                    lastClickItem.name = lastClickItem.name.substring(0, 28) + '...'
                }
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
                try {
                    const res = await this.$store.dispatch('artifactory/requestShareUrl', {
                        projectId,
                        type: `${lastClickItem.artifactoryType}`,
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
                            type: lastClickItem.artifactoryType,
                            path: lastClickItem.fullPath
                        })
                        const pipelineId = res.pipelineId
                        const instance = [{
                            id: pipelineId,
                            name: res.pipelineName || pipelineId
                        }]
                        // 分享构件
                        this.setPermissionConfig(instance, pipelineId)
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
            async handleScroll () {
                const node = this.$refs.scrollBox
                const scrollTop = node.scrollTop
                if (scrollTop + window.innerHeight >= node.scrollHeight) {
                    // 触发加载数据
                    if (!this.scrollDisable) {
                        this.scrollDisable = true
                        this.moreLoading = true
                        let res = []
                        if (this.searchKeysLen) {
                            res = await this.requestSearchList(this.config.page)
                        } else {
                            res = await this.requestFileList(this.config.page)
                        }
                        if (res) {
                            if (res.records !== undefined && res.records.length) {
                                this.config.page = this.config.page + 1
                                this.rowList = this.rowList.concat(res.records)
                                this.scrollDisable = false
                            } else {
                                this.scrollDisable = true
                            }
                        }
                        this.moreLoading = false
                    }
                }
            },
            /**
             * 文件下载地址
             */
            async getDownloadUrl (item) {
                const {
                    projectId
                } = this
                const type = item.artifactoryType
                try {
                    const res = await this.$store.dispatch('artifactory/requestDownloadUrl', {
                        projectId,
                        type: type,
                        path: `${item.fullPath}`
                    })
                    const url = res.url
                    return url
                } catch (err) {
                    if (err.code === 403) { // 没有权限下载
                        const res = await this.$store.dispatch('artifactory/requestFilePipelineInfo', {
                            projectCode: projectId,
                            type: type,
                            path: item.fullPath
                        })
                        const pipelineId = res.pipelineId
                        const instance = [{
                            id: pipelineId,
                            name: res.pipelineName || pipelineId
                        }]
                        // 下载构件
                        this.setPermissionConfig(instance, pipelineId)
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
    %ellipsis {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .artifactory-home-wrapper {
        width: 100%;
        .inner-header-left {
            width: 100%;
            overflow: hidden;
        }
        .inner-header-right {
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

            .artifactory-table-label {
                height: 50px;
                font-size: 14px;
                color: $fontLighterColor;
                line-height: 50px;
                padding-left: 20px;
            }
            .artifactory-table {
                flex: 1;
                width: 700px;
                tbody tr:hover {
                    background-color: #fff;
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
            .table-sim-item {
                padding-right: 8px;
                @extend %ellipsis;
            }
            .sim-item-name {
                @extend %flex;
                width: 30%;
                padding-left: 20px;
                .repo-name {
                    position: relative;
                    top: 1px;
                    margin-left: 10px;
                    @extend %ellipsis;
                    width: 100%;
                }
                i {
                    font-size: 30px;
                    width: 38px;
                    color: #C3CDD7;
                }
            }
            .sim-item-source {
                width: 34%;
            }
            .sim-item-update,
            .sim-item-size,
            .sim-item-type {
                width: 12%;
            }
            .artifactory-ops {
                width: 304px;
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
</style>
