<template>
    <div class="view-part-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <div class="part-table" v-if="showContent && partList.length">
            <div class="table-head">
                <div class="table-part-item part-item-name">构件名称</div>
                <div class="table-part-item part-item-path">路径</div>
                <div class="table-part-item part-item-size">文件大小</div>
                <div class="table-part-item part-item-type">仓库类型</div>
                <div class="table-part-item part-item-handler">操作</div>
            </div>
            <div class="table-body">
                <div class="table-row" v-for="(row, index) of partList" :key="index">
                    <div class="table-part-item part-item-name" @click.stop="showDetail(row)">
                        <i :class="['bk-icon', `icon-${extForFile(row.name)}`]"></i>
                        <span :title="row.name">{{ row.name }}</span>
                    </div>
                    <div class="table-part-item part-item-path">
                        <span :title="row.fullName">{{ row.fullName }}</span>
                    </div>
                    <div class="table-part-item part-item-size">
                        <span>{{ convertInfoItem('size', row.size) }}</span>
                    </div>
                    <div class="table-part-item part-item-type">
                        <span v-if="row.artifactoryType === 'CUSTOM_DIR'">自定义仓库</span>
                        <span v-if="row.artifactoryType === 'PIPELINE'">流水线仓库</span>
                    </div>
                    <div class="table-part-item part-item-handler">
                        <i class="bk-icon icon-new-download handler-btn" v-if="hasPermission" title="下载"
                            @click="requestUrl(row, 'download')"></i>
                        <span class="handler-btn-tool qrcode"
                            v-if="(extForFile(row.name) === 'ipafile' || extForFile(row.name) === 'apkfile') && hasPermission">
                            <i class="bk-icon icon-qrcode handler-btn"
                                id="partviewqrcode"
                                title="二维码"
                                @click="requestUrl(row, 'url', index)"></i>
                            <p class="qrcode-box" v-if="row.display"
                                v-bkloading="{
                                    isLoading: !curIndexItemUrl,
                                    title: ''
                                }">
                                <qrcode class="qrcode-view" :text="curIndexItemUrl" :size="100"></qrcode>
                            </p>
                        </span>
                        <bk-popover placement="left" v-if="!hasPermission">
                            <i class="bk-icon icon-new-download disabled-btn"></i>
                            <template slot="content">
                                <p> 你没有该流水线的下载构件权限，无法下载</p>
                            </template>
                        </bk-popover>
                        <!--<bk-popover placement="left" v-if="!hasPermission">
                            <i class="bk-icon icon-qrcode disabled-btn"></i>
                            <template slot="content">
                                <p> 你没有该流水线的下载构件权限，无法下载</p>
                            </template>
                        </bk-popover>-->
                    </div>
                </div>
            </div>
        </div>
        <div class="artifactory-empty" v-if="showContent && !partList.length">
            <div class="no-data-right">
                <img src="../../images/box.png">
                <p>暂时没有任何构件</p>
            </div>
        </div>
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
                    <bk-tab :active="'detailInfo'" type="unborder-card">
                        <bk-tab-panel name="detailInfo" label="基础信息">
                            <div class="detail-info">
                                <div class="detail-info-label"><span>Info</span></div>
                                <ul>
                                    <li v-for="(item, key) of sideSliderConfig.detailData.info"
                                        v-if="!(lastClickItem.folder && item.key === 'size')"
                                        :key="`detail${key}`">
                                        <span class="bk-lable">{{ item.name }}：</span>
                                        <span>{{ item.key === 'name' ? (sideSliderConfig.data[item.key] || lastClickItem.name) : convertInfoItem(item.key, sideSliderConfig.data[item.key]) }}</span>
                                    </li>
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
    </div>
</template>

<script>
    import qrcode from '@/components/devops/qrcode'
    import { convertFileSize, convertTime } from '@/utils/util'

    export default {
        components: {
            qrcode
        },
        data () {
            return {
                showContent: false,
                hasPermission: true,
                curIndexItemUrl: '',
                emptyTitle: 'wushuhsdjkghafjko',
                iconExts: {
                    txt: ['.json', '.txt', '.md'],
                    zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar'],
                    apkfile: ['.apk'],
                    ipafile: ['.ipa']
                },
                partList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                lastClickItem: {},
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
                permissionConfig: { // 无权限
                    isShow: false,
                    resource: '',
                    option: '',
                    link: '/perm/apply-perm'
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
            buildNo () {
                return this.$route.params.buildNo
            },
            artifactoryUrl () {
                return `${WEB_URL_PIRFIX}/artifactory/${this.projectId}/?pipelineId=${this.pipelineId}&buildId=${this.buildNo}`
            }
        },
        watch: {
            buildNo () {
                this.init()
            }
        },
        async created () {
            this.requestHasPermission()
            await this.init()
            this.addClickListenr()
        },
        beforeDestroy () {
            this.removeClickListenr()
        },
        methods: {
            async init () {
                const { loading } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                const params = {
                    props: {
                        pipelineId: this.pipelineId,
                        buildId: this.buildNo
                    }
                }

                try {
                    const res = await this.$store.dispatch('soda/requestPartFile', {
                        projectId: this.projectId,
                        params
                    })

                    this.partList.splice(0, this.partList.length)
                    res.records.map(item => {
                        item.display = false
                        this.partList.push(item)
                    })
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.showContent = true
                        this.loading.isLoading = false
                    }, 500)
                }
            },
            async requestUrl (row, key, index, type) {
                this.curIndexItemUrl = ''

                this.partList.forEach((vv, kk) => {
                    if (kk === index) {
                        vv.display = !vv.display
                    } else {
                        vv.display = false
                    }
                })

                try {
                    if (key === 'url') {
                        const res = await this.$store.dispatch('soda/requestExternalUrl', {
                            projectId: this.projectId,
                            artifactoryType: row.artifactoryType,
                            path: row.path
                        })

                        this.curIndexItemUrl = res.url
                    } else {
                        const res = await this.$store.dispatch('soda/requestDownloadUrl', {
                            projectId: this.projectId,
                            artifactoryType: row.artifactoryType,
                            path: row.path
                        })
                        window.location.href = res.url
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$showTips({
                        message,
                        theme
                    })
                }
            },
            clickHandler (event) {
                if (event.target.id !== 'partviewqrcode') {
                    this.partList.forEach(item => {
                        item.display = false
                    })
                }
            },
            gotoArtifactory () {
                window.open(this.artifactoryUrl, '_blank')
            },
            addClickListenr () {
                document.addEventListener('mouseup', this.clickHandler)
            },
            removeClickListenr () {
                document.removeEventListener('mouseup', this.clickHandler)
            },
            async requestHasPermission () {
                const {
                    pipelineId
                } = this
                const permission = 'DOWNLOAD'

                try {
                    const res = await this.$store.dispatch('soda/requestExecPipPermission', {
                        projectId: this.projectId,
                        pipelineId,
                        permission
                    })

                    this.hasPermission = res
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$showTips({
                        message,
                        theme
                    })
                }
            },
            /**
             * 查看详情
             */
            async showDetail (row) {
                this.lastClickItem = row

                const { sideSliderConfig } = this
                const {
                    projectId
                } = this
                try {
                    sideSliderConfig.isLoading = true
                    const type = row.artifactoryType === 'PIPELINE' ? 'PIPELINE' : 'CUSTOM_DIR'
                    const res = await this.$store.dispatch('soda/requestFileInfo', {
                        projectId: projectId,
                        type: type,
                        path: `${row.fullPath}`
                    })
                    sideSliderConfig.show = true
                    sideSliderConfig.title = res.name || row.name
                    sideSliderConfig.data = res
                    sideSliderConfig.isLoading = false
                } catch (err) {
                    if (err.code === 403) {
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: '版本仓库',
                                option: '查看'
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=artifactory&project_code=${this.projectId}&service_code=artifactory&role_manager=artifactory`
                        })
                    } else {
                        this.$showTips({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
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
            convertInfoItem (key, value) {
                if (key.includes('Time')) {
                    return convertTime(value * 1000)
                } else if (key.includes('size')) {
                    return (value >= 0 && convertFileSize(value, 'B')) || ''
                } else {
                    return value
                }
            },
            isApkOrIpa (row) {
                const type = row.name.toUpperCase().substring(row.name.lastIndexOf('.') + 1)
                return type === 'APK' || type === 'IPA'
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    %flex {
        display: flex;
        align-items: center;
    }
    .view-part-wrapper {
        height: 100%;
        overflow: auto;
        .part-table {
            border: 1px solid $borderWeightColor;
        }
        .table-head,
        .table-row {
            padding: 0 20px;
            @extend %flex;
            height: 43px;
            font-size: 14px;
            color: #333C48;
            cursor: default;
        }
        .table-body {
            background-color: #fff;
        }
        .table-row {
            height: 60px;
            border-top: 1px solid $borderWeightColor;
            color: $fontWeightColor;
            .part-item-name, .part-item-type {
                // color: $primaryColor;
                // cursor: pointer;
                // a {
                //     color: $primaryColor;
                // }
            }
        }
        .table-part-item {
            flex: 1;
            padding-right: 20px;
        }
        .part-item-name {
            flex: 2;
            position: relative;
            padding-left: 38px;
            line-height: 60px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            i {
                position: absolute;
                top: 15px;
                left: 0;
                font-size: 30px;
                width: 38px;
                color: $fontLigtherColor;
            }
        }
        .part-item-path {
            flex: 3;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .part-item-handler {
            flex: 2;
            max-width: 180px;
            font-size: 16px;
            cursor: pointer;
            i {
                margin-right: 16px;
            }
            i:last-child {
                margin-right: 0px;
            }
            .handler-btn:hover {
                color: $primaryColor;
            }
        }
        .qrcode {
            position: relative;
        }
        .qrcode-box {
            position: absolute !important;
            top: 32px;
            right: 6px;
            width: 120px;
            height: 125px;
            text-align: center;
            border: 1px solid $borderWeightColor;
            background-color: #fff;
            z-index: 999;

            &:before {
                content: '';
                padding-top: 4px;
                position: absolute;
                top: -6px;
                left: 92px;
                width: 10px;
                height: 6px;
                border: 1px solid $borderWeightColor;
                border-right-color: transparent;
                border-bottom-color: transparent;
                background-color: #fff;
                transform: rotate(45deg);
            }

            img {
                width: 100%;
                height: 100%;
            }

            .qrcode-view {
                display: inline-block;
                vertical-align: middle;
                height: 100%;
                margin-top: 12px;
            }
        }
        .artifactory-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                padding-top: 200px;
                p {
                    line-height: 60px;
                }
            }
        }
        .disabled-btn {
            color: $fontLigtherColor;
            &:hover {
                color: $fontLigtherColor;
            }
        }
        .handler-btn-tool {
            position: relative;
            display: inline-block;
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
