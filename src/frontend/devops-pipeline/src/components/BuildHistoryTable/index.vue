<template>
    <div class="build-history-table-container">
        <bk-table
            class="bkdevops-build-history-table"
            :data="data"
            :row-class-name="handleRowStyle"
            :empty-text="$t('history.filterNullTips')"
            @row-click="handleRowClick"
            @header-dragend="handleDragend"
            size="small">
            <bk-table-column v-for="col in columnList" v-bind="col" :key="col.prop">
                <template v-if="col.prop === 'buildNum'" v-slot="props">
                    <span class="build-num-status">
                        <router-link :class="{ [props.row.status]: true }" style="line-height: 42px;" :to="getArchiveUrl(props.row)">#{{ props.row.buildNum }}</router-link>
                        <i v-if="retryable(props.row)" :title="$t('retry')" class="bk-icon icon-retry" @click.stop="retry(props.row.id)" />
                        <i v-else-if="props.row.status === 'QUEUE' || props.row.status === 'RUNNING' || !props.row.endTime" :title="$t('history.stopBuild')" @click.stop="stopExecute(props.row.id)"
                            :class="{
                                'bk-icon': true,
                                'spin-icon': true,
                                'running-icon': true,
                                'icon-hourglass': props.row.status === 'QUEUE',
                                'icon-circle-2-1': props.row.status === 'RUNNING' || !props.row.endTime
                            }"
                        >
                        </i>
                    </span>
                </template>
                <template v-else-if="col.prop === 'material'" v-slot="props">
                    <template v-if="Array.isArray(props.row.material) && props.row.material.length > 0">
                        <div @click.stop="" v-for="material in props.row.material" :key="material.aliasName" class="material-item">
                            <p :title="generateMaterial(material)" :class="{ 'show-commit-times': material.commitTimes > 1 }">{{ generateMaterial(material) }}</p>
                            <span class="material-commit-id" v-if="material.newCommitId" :title="material.newCommitId" @click.stop="goCodeRecords(props.row, material.aliasName)">
                                <span class="commit-nums">{{ material.newCommitId.slice(0, 8) }}</span>
                                <span class="commit-times" v-if="material.commitTimes > 1">{{ material.commitTimes }} commit</span>
                            </span>
                        </div>
                    </template>
                    <span v-else>--</span>
                </template>
                <template v-else-if="col.prop === 'artifactList'" v-slot="props">
                    <template v-if="props.row.hasArtifactories">
                        <div class="artifact-list-cell">
                            <qrcode v-if="props.row.active && props.row.shortUrl" :text="props.row.shortUrl" :size="76">{{props.row.shortUrl}}</qrcode>
                            <p class="artifact-entry history-text-link" @click.stop="e => showArtifactoriesPopup(e, props.row.index)">{{ $t('history.fileUnit', [props.row.artifactList.length]) }}（{{props.row.sumSize}}）</p>
                        </div>
                    </template>
                    <span v-else>--</span>
                </template>
                <template v-else-if="col.prop === 'appVersions'" v-slot="props">
                    <template v-if="props.row.appVersions.length">
                        <div class="app-version-list-cell">
                            <p v-for="(appVersion, index) in props.row.appVersions" :key="index">{{ appVersion }}</p>
                        </div>
                    </template>
                    <span v-else>--</span>
                </template>
                <template v-else-if="col.prop === 'startType'" v-slot="props">
                    <p class="trigger-cell">
                        <logo size="14" :name="props.row.startType" />
                        <span>{{ props.row.userId }}</span>
                    </p>
                </template>
                <template v-else-if="col.prop === 'entry'" v-slot="props">
                    <p class="entry-link" @click.stop="showLog(props.row.id, props.row.buildNum, true)">
                        {{ $t('history.completedLog') }}
                    </p>
                </template>
                <template v-else-if="col.prop === 'remark'" v-slot="props">
                    <div class="remark-cell">
                        <span :class="{ 'remark-span': true, active: props.row.active }" :title="props.row.remark">
                            {{ props.row.remark || '--' }}
                        </span>
                        <bk-popover ref="remarkPopup" trigger="click" theme="light" placement="left">
                            <i class="bk-icon icon-edit remark-entry" @click.stop="activeRemarkInput(props.row)" />
                            <div slot="content">
                                <bk-input type="textarea" ref="remarkInput" rows="3" class="remark-input" v-model.trim="tempRemark" />
                                <div class="remark-edit-footer">
                                    <bk-button size="small" theme="primary" @click="handleRemarkChange(props.row)">{{ $t('confirm') }}</bk-button>
                                    <bk-button size="small" @click="resetRemark">{{ $t('cancel') }}</bk-button>
                                </div>
                            </div>
                        </bk-popover>
                    </div>
                </template>
                <template v-else v-slot="props">
                    {{ props.row[col.prop] }}
                </template>
            </bk-table-column>
            <empty-tips v-if="emptyTipsConfig" class="build-list-table-empty-tips" slot="empty" v-bind="emptyTipsConfig"></empty-tips>
            <div v-if="loadingMore" class="loading-more" slot="append"><i class="bk-icon icon-circle-2-1 spin-icon"></i><span>{{ $t('loadingTips') }}</span></div>
        </bk-table>
        <portal to="artifactory-popup">
            <div ref="artifactPopup" class="artifact-list-popup" v-show="actifactories.length" v-bk-clickoutside="hideArtifactoriesPopup">
                <div class="artifact-list-header">
                    <h2>{{ $t('history.artifactList') }}</h2>
                    <span @click.stop="gotoArtifactoryList" class="history-text-link">{{ $t('detail') }}</span>
                </div>
                <span ref="popupTriangle" class="popup-triangle"></span>
                <ul class="artifact-list-ul" v-if="visibleIndex !== -1">
                    <li v-for="artifactory in actifactories" :key="artifactory.name">
                        <p>
                            <span :title="artifactory.name" class="artifact-name">{{ artifactory.name }}</span>
                            <span class="artifact-size">{{ artifactory.size }}</span>
                        </p>
                        <i class="bk-icon icon-download download-link history-text-link" @click.stop="downloadFile(artifactory)" />
                        <Logo class="icon-copy" name="copy" size="12" v-if="artifactory.artifactoryType === 'PIPELINE'" @click.stop.native="copyToCustom(artifactory)"></Logo>
                    </li>
                    <footer v-if="needShowAll" @click.stop="showAllArtifactory" class="history-text-link">{{ $t('history.showAll') }}</footer>
                </ul>
            </div>
        </portal>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import emptyTips from '@/components/devops/emptyTips'
    import { convertFileSize, convertMStoStringByRule, convertMiniTime } from '@/utils/util'
    import { BUILD_HISTORY_TABLE_DEFAULT_COLUMNS } from '@/utils/pipelineConst'
    import qrcode from '@/components/devops/qrcode'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'

    export default {
        name: 'build-history-table',
        components: {
            Logo,
            qrcode,
            emptyTips
        },
        mixins: [pipelineConstMixin],
        props: {
            buildList: {
                type: Array,
                default: []
            },
            columns: {
                type: Array,
                default: BUILD_HISTORY_TABLE_DEFAULT_COLUMNS
            },
            emptyTipsConfig: {
                type: Object
            },
            currentPipelineVersion: {
                type: [String, Number],
                default: 1
            },
            showLog: {
                type: Function
            },
            loadingMore: {
                type: Boolean
            }
        },
        data () {
            return {
                activeIndex: 0,
                tempRemark: '',
                isChangeRemark: false,
                activeRemarkIndex: -1,
                visibleIndex: -1,
                isShowAll: false,
                retryingMap: {},
                stoping: {}
            }
        },
        computed: {
            data () {
                return this.buildList.map((item, index) => {
                    const active = index === this.activeIndex
                    const hasArtifactories = Array.isArray(item.artifactList) && item.artifactList.length > 0
                    let shortUrl = ''
                    const appVersions = []
                    let sumSize = 0
                    const artifactories = hasArtifactories ? item.artifactList.map(artifactory => {
                        if (artifactory.shortUrl) {
                            shortUrl = artifactory.shortUrl
                        }
                        if (artifactory.appVersion) {
                            appVersions.push(artifactory.appVersion)
                        }
                        sumSize += artifactory.size
                        return {
                            ...artifactory,
                            name: artifactory.name,
                            size: convertFileSize(artifactory.size, 'B')
                        }
                    }) : []
                    const needShowAll = hasArtifactories && item.artifactList.length > 11 && !this.isShowAll
                    return {
                        ...item,
                        index,
                        active,
                        hasArtifactories,
                        needShowAll,
                        shortUrl,
                        appVersions,
                        startTime: item.startTime ? convertMiniTime(item.startTime) : '--',
                        endTime: item.endTime ? convertMiniTime(item.endTime) : '--',
                        queueTime: item.queueTime ? convertMiniTime(item.queueTime) : '--',
                        totalTime: item.totalTime ? convertMStoStringByRule(item.totalTime) : '--',
                        material: !active && Array.isArray(item.material) && item.material.length > 1 ? item.material.slice(0, 1) : item.material,
                        sumSize: convertFileSize(sumSize, 'B'),
                        artifactories: needShowAll ? artifactories.slice(0, 11) : artifactories,
                        visible: this.visibleIndex === index
                    }
                })
            },
            actifactories () {
                const { data, visibleIndex } = this
                return data[visibleIndex] && data[visibleIndex].artifactories ? data[visibleIndex].artifactories : []
            },
            currentBuildId () {
                const { data, visibleIndex } = this
                return data[visibleIndex] && data[visibleIndex].id
            },
            needShowAll () {
                return this.data[this.visibleIndex].needShowAll
            },
            columnList () {
                return this.columns.map(key => this.column[key])
            },
            column () {
                Object.keys(this.BUILD_HISTORY_TABLE_COLUMNS_MAP).map(item => {
                    if (item === 'material') {
                        const localStorageVal = localStorage.getItem('materialWidth')
                        this.BUILD_HISTORY_TABLE_COLUMNS_MAP[item].width = localStorageVal || 500
                    }
                })
                return this.BUILD_HISTORY_TABLE_COLUMNS_MAP
            }
        },
        watch: {
            buildList () {
                this.resetRemark()
            }
        },
        methods: {
            activeRemarkInput (row) {
                this.activeRemarkIndex = row.index
                this.tempRemark = row.remark
                const instance = this.getRemarkPopupInstance(row.index)
                if (instance) {
                    instance.show()
                    this.$nextTick(() => {
                        const el = this.$refs.remarkInput && this.$refs.remarkInput[row.index]
                        el && el.focus()
                    })
                }
            },
            getRemarkPopupInstance (activeRemarkIndex) {
                return this.$refs.remarkPopup && this.$refs.remarkPopup[activeRemarkIndex] && this.$refs.remarkPopup[activeRemarkIndex].instance
            },
            retryable (row) {
                return row.pipelineVersion === this.currentPipelineVersion && ['QUEUE', 'SUCCEED', 'RUNNING'].indexOf(row.status) < 0
            },
            async handleRemarkChange (row) {
                try {
                    const { $route: { params }, tempRemark } = this
                    if (tempRemark !== row.remark) {
                        this.isChangeRemark = true

                        await this.$ajax.post(`${PROCESS_API_URL_PREFIX}/user/builds/${params.projectId}/${params.pipelineId}/${row.id}/updateRemark`, {
                            remark: tempRemark
                        })
                        this.$emit('update-table')
                        this.$showTips({
                            theme: 'success',
                            message: this.$t('updateSuc')
                        })
                        this.resetRemark()
                    } else {
                        this.resetRemark()
                    }
                } catch (e) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('updateFail')
                    })
                }
            },
            resetRemark () {
                const remarkPopupInstance = this.getRemarkPopupInstance(this.activeRemarkIndex)
                remarkPopupInstance && remarkPopupInstance.hide()

                this.$nextTick(() => {
                    this.isChangeRemark = false
                    this.tempRemark = ''
                    this.activeRemarkIndex = -1
                })
            },
            generateMaterial (material) {
                return material ? `${material.aliasName || '--'}${material.branchName ? `@${material.branchName}` : ''}` : '--'
            },
            handleRowStyle ({ row, rowIndex }) {
                return rowIndex === this.activeIndex ? 'expand-row is-row-hover' : 'is-row-hover'
            },
            handleRowClick (row, e) {
                this.hideArtifactoriesPopup()
                if (this.activeIndex === row.index) {
                    const url = this.getArchiveUrl(row)
                    this.$router.push(url)
                } else {
                    this.activeIndex = row.index
                }
            },
            handleDragend (newWidth, oldWidth, column) {
                if (column.property === 'material') localStorage.setItem('materialWidth', newWidth)
            },
            getArchiveUrl ({ id: buildNo }, type = '', codelib = '') {
                const { projectId, pipelineId } = this.$route.params
                return {
                    name: 'pipelinesDetail',
                    params: {
                        projectId,
                        pipelineId,
                        buildNo,
                        ...(type ? { type } : {})
                    },
                    hash: codelib ? `#${codelib}` : ''
                }
            },

            gotoArtifactoryList () {
                const { data, visibleIndex } = this
                const row = data[visibleIndex]
                if (row) {
                    const url = this.getArchiveUrl(data[visibleIndex], 'partView')
                    this.$router.push(url)
                }
            },

            goCodeRecords (row, aliasName) {
                if (row) {
                    const url = this.getArchiveUrl(row, 'codeRecords', aliasName)
                    url && this.$router.push(url)
                }
            },

            hideArtifactoriesPopup () {
                this.isShowAll = false
                this.visibleIndex = -1
            },
            showArtifactoriesPopup (e, index = -1) {
                this.visibleIndex = index
                this.$nextTick(() => {
                    const ele = this.$refs.artifactPopup
                    const popupTriangle = this.$refs.popupTriangle
                    if (ele && popupTriangle) {
                        const triangleStyle = getComputedStyle(popupTriangle)
                        const eleStyle = getComputedStyle(ele)
                        const targetRect = e.target.getBoundingClientRect()

                        ele.style.top = `${targetRect.y - parseInt(triangleStyle.top)}px`
                        ele.style.left = `${targetRect.x - parseInt(eleStyle.width) - 16}px`
                    }
                })
            },
            showAllArtifactory () {
                this.isShowAll = true
            },
            async downloadFile ({ artifactoryType, path }, key = 'download') {
                try {
                    const { projectId } = this.$route.params
                    const isDevnet = await this.$store.dispatch('soda/requestDevnetGateway')
                    const res = await this.$store.dispatch('soda/requestDownloadUrl', {
                        projectId,
                        artifactoryType,
                        path
                    })
                    const url = isDevnet ? res.url : res.url2
                    window.open(url, '_self')
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$showTips({
                        message,
                        theme
                    })
                }
            },
            async copyToCustom (artifactory) {
                let message, theme
                try {
                    const { projectId, pipelineId } = this.$route.params
                    const params = {
                        files: [artifactory.name],
                        copyAll: false
                    }
                    const res = await this.$store.dispatch('soda/requestCopyArtifactory', {
                        projectId,
                        pipelineId,
                        buildId: this.currentBuildId,
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
            },
            /**
             * 重试流水线
             */
            async retry (buildId) {
                if (this.retryingMap[buildId]) return
                let message, theme
                const { $store } = this
                try {
                    this.retryingMap[buildId] = true
                    // 请求执行构建
                    const res = await $store.dispatch('pipelines/requestRetryPipeline', {
                        ...this.$route.params,
                        buildId
                    })

                    if (res.id) {
                        message = this.$t('subpage.retrSuc')
                        theme = 'success'

                        this.$emit('update-table')
                    } else {
                        message = this.$t('subpage.retryFail')
                        theme = 'error'
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        // this.setPermissionConfig(`流水线：${this.curPipeline.pipelineName}`, '执行')
                        return
                    } else {
                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    delete this.retryingMap[buildId]
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            /**
             *  终止流水线
             */
            async stopExecute (buildId) {
                if (this.stoping[buildId]) return

                let message, theme

                try {
                    const { $store } = this
                    this.stoping[buildId] = true
                    const res = await $store.dispatch('pipelines/requestTerminatePipeline', {
                        ...this.$route.params,
                        buildId
                    })

                    this.status = 'ready'
                    if (res) {
                        message = this.$t('subpage.stopSuc')
                        theme = 'success'

                        this.$emit('update-table')
                    } else {
                        message = this.$t('subpage.stopFail')
                        theme = 'error'
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        // this.setPermissionConfig(`流水线：${this.curPipeline.pipelineName}`, '执行')
                    } else {
                        message = err.message || err
                        theme = 'error'
                    }
                } finally {
                    // delete this.stoping[buildId]
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss" >
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';
    @import '../../scss/pipelineStatus';

    .entry-link {
        padding: 2px 0;
        font-size: 12px;
        cursor: pointer;
        color: #333333;
        > a {
            color: #333333;
        }
        &:hover {
            color: $primaryColor;
            > a {
                color: $primaryColor;
            }
        }
    }
    .build-history-table-container {

        margin-top: 10px;
        display: flex;
        flex-direction: column;
    }

    .build-list-table-empty-tips {
        width: auto;
        margin: 0;
    }

    .history-text-link {
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
    }

    .expand-row {
        height: 148px;
        cursor: pointer;
    }
    .bkdevops-build-history-table {
        border-top: 1px solid #e6e6e6;
        border-left: 1px solid #e6e6e6;
        color: #333333;
        .bk-table-header-wrapper {
            height: 43px;
        }
        .bk-table-body-wrapper {
            tr:hover {
                .remark-entry {
                    display: inline-block;
                }
                .bk-icon.running-icon {
                    cursor: pointer;
                    animation: none;
                    font-size: 8px;
                    &:before {
                        content: "\E953";
                        border: 1px solid #333333;
                        padding: 2px;
                        border-radius: 50%;
                    }
                    &:hover {
                        color: $primaryColor;
                        &:before {
                            border: 1px solid $primaryColor;
                        }
                    }
                }
            }
        }
        tr:hover {
            background-color: transparent;
            > td {
                background-color: transparent !important;
            }
        }
        .build-num-status {
            display: flex;
            align-items: center;
            .bk-icon {
                margin-left: 6px;
                display: inline-block;
            }
            .icon-retry {
                font-size: 14px;
                cursor: pointer;
                &:hover {
                    color: $primaryColor;
                }
            }

        }
        .material-item {
            display: flex;
            p.show-commit-times  {
                @include ellipsis();
            }
            .material-commit-id {
                cursor: pointer;
                color: $primaryColor;
                padding: 0 6px;
                display: flex;
                .commit-nums {
                    min-width: 64px;
                }
                .commit-times {
                    padding: 0 6px;
                    margin-left: 4px;
                    min-width: 82px;
                    background: #333333;
                    color: white;
                    border-radius: 20px;
                    text-align: center;
                    @include ellipsis();
                }
            }

        }
        .trigger-cell {
            display: flex;
            align-items: center;
            > svg {
                fill: currentColor;
            }
            > span {
                padding-left: 4px;
            }
        }
        .artifact-list-cell {
            height: 100%;
            canvas {
                padding: 2px;
                border: 1px solid #DDE4EB;
            }
        }
        .remark-cell {
            position: relative;
            display: flex;
            .remark-span {
                cursor: pointer;
                display: -webkit-box;
                -webkit-box-orient: vertical;
                -webkit-line-clamp: 1;
                overflow: hidden;
                &.active {
                    -webkit-line-clamp: 5;
                }

            }
            .remark-entry {
                display: none;
                cursor: pointer;
                vertical-align: middle;
                margin-left: 10px;
                &:hover {
                    color: $primaryColor;
                }
            }
        }
    }
    .loading-more {
        display: flex;
        height: 36px;
        justify-content: center;
        align-items: center;
        .bk-icon {
            margin-right: 8px;
        }
    }
    .artifact-list-popup {
        position: absolute;
        width: 408px;
        background: white;
        right: 150px;
        top: 0;
        box-shadow: 0px 2px 5px 0px rgba(0,0,0,0.35);
        z-index: 1;
        font-size: 12px;
        .popup-triangle {
            position: absolute;
            width: 10px;
            height: 10px;
            right: -5px;
            top: 50px;
            background: white;
            transform: rotate(-45deg);
            box-shadow: 0px 2px 5px 0px rgba(0,0,0,0.35);
        }
        .artifact-list-header {
            display: flex;
            justify-content: space-around;
            align-items: center;
            padding: 0 20px 0 24px;
            height: 40px;
            background: $bgHoverColor;
            border-bottom: 2px solid #E6E7EA;
            > h2 {
                position: relative;
                margin: 0;
                flex: 1;
                color: #4A4A4A;
                font-size: 14px;
                cursor: default;
                &:before {
                    content: '';
                    position: absolute;
                    height: 14px;
                    width: 4px;
                    left: -6px;
                    top: 3.5px;
                    background: $fontWeightColor;
                }
            }
        }
        .artifact-list-ul {
            overflow: auto;
            max-height: 430px;
            position: relative;
            background-color: white;
            z-index: 2;
            > li {
                height: 32px;
                border-bottom: 1px solid #E6E7EA;
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin: 0 20px;
                > p {
                    flex: 1;
                    display: flex;
                    align-items: center;
                }
                .artifact-name {
                    max-width: 222px;
                    @include ellipsis();
                }
                .artifact-size {
                    color: $fontLigtherColor;
                    margin-left: 30px;
                }
                .download-link {
                    margin-right: 18px;
                    font-weight: bold;
                }
                .icon-copy {
                    fill: $fontWeightColor;
                    cursor: pointer;
                    &:hover {
                        fill: $primaryColor;
                    }
                }
            }
            > footer {
                text-align: center;
                line-height: 35px;
            }
        }
    }

    .remark-edit-footer {
        margin: 10px 0;
        text-align: right;
    }
</style>
