<template>
    <div class="view-part-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <bk-table class="part-table" v-if="showContent && partList.length" :data="partList">
            <bk-table-column :label="$t('details.artifactName')">
                <div slot-scope="{ row }" @click.stop="showDetail(row)">
                    <Logo v-if="row.artifactoryType === 'IMAGE'" class="image-icon" name="docker-svgrepo-com" size="30" />
                    <i v-else :class="['devops-icon', `icon-${extForFile(row.name)}`]"></i>
                    <span :title="row.name">{{ row.name }}</span>
                </div>
            </bk-table-column>
            <bk-table-column :label="$t('details.path')" prop="fullName"></bk-table-column>
            <bk-table-column :label="$t('details.filesize')" prop="size">
                <span slot-scope="{ row }">
                    {{convertInfoItem('size', row.size)}}
                </span>
            </bk-table-column>
            <bk-table-column :label="$t('details.repoType')">
                <span slot-scope="{ row }">{{repoTypeNameMap[row.artifactoryType]}}</span>
            </bk-table-column>
            <bk-table-column class="part-item-handler" :label="$t('operate')">
                <div class="part-item-handler" slot-scope="{ row }">
                    <!-- <i @click.stop="gotoArtifactory" class="devops-icon icon-position-shape handler-btn" :title="$t('editPage.atomForm.toArtifactory')"></i> -->
                    <i class="devops-icon icon-new-download handler-btn" v-if="hasPermission && row.artifactoryType !== 'IMAGE'" :title="$t('download')"
                        @click="requestUrl(row, 'download')"></i>
                    <!-- <i class="devops-icon icon-tree-module-shape handler-btn" v-if="hasPermission && isMof && isWindows && isApkOrIpa(row)" :title="$t('details.mofDownload')"
                            @click="requestUrl(row, 'download', null, 'MoF')"></i> -->
                    <!-- <span class="handler-btn-tool copy" v-if="row.artifactoryType === 'PIPELINE'" :title="$t('details.saveToCustom')" @click="copyToCustom(row)">
                            <Logo class="icon-copy" name="copy" size="15"></Logo>
                        </span> -->
                    <span class="handler-btn-tool qrcode"
                        v-if="(extForFile(row.name) === 'ipafile' || extForFile(row.name) === 'apkfile') && hasPermission">
                        <i class="devops-icon icon-qrcode handler-btn"
                            id="partviewqrcode"
                            :title="$t('details.qrcode')"
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
                        <i @click="requestDownloadPermission" class="devops-icon icon-new-download disabled-btn"></i>
                        <template slot="content">
                            <p>{{ $t('details.noDownloadPermTips') }}</p>
                        </template>
                    </bk-popover>
                </div>
            </bk-table-column>
        </bk-table>
        <div class="artifactory-empty" v-if="showContent && !partList.length">
            <div class="no-data-right">
                <img src="../../images/box.png">
                <p>{{ $t('details.noArtifact') }}</p>
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
                    <tab :active-name="'detailInfo'">
                        <tab-panel name="detailInfo" :title="$t('settings.baseInfo')">
                            <div class="detail-info">
                                <div class="detail-info-label"><span>Info</span></div>
                                <ul>
                                    <li v-for="(item, key) of sideSliderConfig.detailData.info"
                                        :key="`detail${key}`">
                                        <template v-if="!(lastClickItem.folder && item.key === 'size')">
                                            <span class="bk-lable">{{ item.name }}：</span>
                                            <span>{{ item.key === 'name' ? (sideSliderConfig.data[item.key] || lastClickItem.name) : convertInfoItem(item.key, sideSliderConfig.data[item.key]) }}</span>
                                        </template>
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
                        </tab-panel>
                        <tab-panel name="metaDate" :title="$t('metaData')" v-if="!lastClickItem.folder">
                            <table class="bk-table has-thead-bordered has-table-striped" v-if="Object.keys(sideSliderConfig.data.meta).length">
                                <thead>
                                    <tr>
                                        <th>{{ $t('view.key') }}</th>
                                        <th>{{ $t('view.value') }}</th>
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
                                <div style="text-align:center;padding: 30px 0;">{{ $t('details.noArtifact') }}</div>
                            </div>
                        </tab-panel>
                    </tab>
                </div>
            </template>
        </bk-sideslider>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import qrcode from '@/components/devops/qrcode'
    import { convertFileSize, convertTime } from '@/utils/util'

    export default {
        components: {
            qrcode,
            Logo
        },
        data () {
            return {
                showContent: false,
                hasPermission: true,
                curIndexItemUrl: '',
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
                    title: this.$t('details.viewDetail'),
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
                repoTypeNameMap: {
                    CUSTOM_DIR: this.$t('details.customRepo'),
                    PIPELINE: this.$t('details.pipelineRepo'),
                    IMAGE: this.$t('details.imageRepo')
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
            isWindows () {
                return /WINDOWS/.test(window.navigator.userAgent.toUpperCase())
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
                loading.title = this.$t('loadingTips')

                const params = {
                    props: {
                        pipelineId: this.pipelineId,
                        buildId: this.buildNo
                    }
                }

                try {
                    const res = await this.$store.dispatch('common/requestPartFile', {
                        projectId: this.projectId,
                        params
                    })

                    this.partList.splice(0, this.partList.length)
                    res.records.forEach(item => {
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
                        const res = await this.$store.dispatch('common/requestExternalUrl', {
                            projectId: this.projectId,
                            type: row.artifactoryType,
                            path: row.path
                        })

                        this.curIndexItemUrl = res.url
                    } else {
                        const res = await this.$store.dispatch('common/requestDownloadUrl', {
                            projectId: this.projectId,
                            artifactoryType: row.artifactoryType,
                            path: row.path
                        })
                        const url = res.url2
                        window.location.href = type ? `${API_URL_PREFIX}/pc/download/devops_pc_forward.html?downloadUrl=${url}` : url
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.download,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineId
                        }],
                        projectId: this.projectId
                    }])
                }
            },

            requestDownloadPermission () {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        actionId: this.$permissionActionMap.download,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineId
                        }],
                        projectId: this.projectId
                    }]
                })
            },
            clickHandler (event) {
                if (event.target.id !== 'partviewqrcode') {
                    this.partList.forEach(item => {
                        item.display = false
                    })
                }
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
                    const res = await this.$store.dispatch('common/requestExecPipPermission', {
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
                    const type = row.artifactoryType
                    const res = await this.$store.dispatch('common/requestFileInfo', {
                        projectId: projectId,
                        type: type,
                        path: `${row.fullPath}`
                    })
                    sideSliderConfig.show = true
                    sideSliderConfig.title = res.name || row.name
                    sideSliderConfig.data = res
                    sideSliderConfig.isLoading = false
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.view,
                        resourceId: this.$permissionResourceMap.artifactory,
                        instanceId: [],
                        projectId: this.projectId
                    }])
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
            },
            async copyToCustom (artifactory) {
                let message, theme
                try {
                    const params = {
                        files: [artifactory.name],
                        copyAll: false
                    }
                    const res = await this.$store.dispatch('common/requestCopyArtifactory', {
                        ...this.$route.params,
                        params
                    })
                    if (res) {
                        message = this.$t('saveSuc')
                        theme = 'success'
                    }
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$showTips({
                        message,
                        theme
                    })
                }
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
            color: $fontLighterColor;
            cursor: url(../../images/cursor-lock.png),auto;
            &:hover {
                color: $fontLighterColor;
            }
        }
        .handler-btn-tool {
            position: relative;
            display: inline-block;
        }
        .icon-copy {
            fill: $fontWeightColor;
            cursor: pointer;
            &:hover {
                fill: $primaryColor;
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
