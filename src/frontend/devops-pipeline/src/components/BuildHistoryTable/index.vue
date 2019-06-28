<template>
    <div class="build-history-table-container">
        <bk-table
            class="bkdevops-build-history-table"
            :data="data"
            :row-class-name="handleRowStyle"
            @row-click="handleRowClick"
            empty-text="搜索结果为空"
            size="small">
            <bk-table-column v-for="col in columnList" v-bind="col" :key="col.prop">
                <template v-if="col.prop === 'buildNum'" v-slot="props">
                    <span class="build-num-status">
                        <router-link :class="{ [props.row.status]: true }" style="line-height: 42px;" :to="getArchiveUrl(props.row)">#{{ props.row.buildNum }}</router-link>
                        <i v-if="retryable(props.row)" title="重试" class="bk-icon icon-retry" @click.stop="retry(props.row.id)" />
                        <i v-else-if="props.row.status === 'QUEUE' || props.row.status === 'RUNNING' || !props.row.endTime" title="终止构建" @click.stop="stopExecute(props.row.id)"
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
                        <div v-for="material in props.row.material" :key="material.aliasName" class="material-item">
                            <p :title="generateMaterial(material)" :class="{ 'show-commit-times': material.commitTimes > 1 }">{{ generateMaterial(material) }}</p>
                            <span class="material-commit-id" v-if="material.newCommitId" :title="material.newCommitId" @click.stop="goCodeRecords(props.row, material.aliasName)">
                                <span>{{ material.newCommitId.slice(0, 8) }}</span>
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
                            <p class="artifact-entry history-text-link" @click.stop="e => showArtifactoriesPopup(e, props.row.index)">{{props.row.artifactList.length }}个文件（{{props.row.sumSize}}）</p>
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
                        完整日志
                    </p>
                    <!-- <template v-if="props.row.active">
                        <p class="entry-link" @click="showLog(props.row.id, props.row.buildNum, true)">
                            完整日志
                        </p>
                        <p class="entry-link" v-for="entry in col.entries" :key="entry.type">
                            <router-link :to="getArchiveUrl(props.row, entry.type)">{{ entry.label }}</router-link>
                        </p>
                    </template>
                    <p v-else>
                        <span class='entry-link'><router-link :to="getArchiveUrl(props.row, col.entries[0].type)">{{ col.entries[0].label }}</router-link></span>
                        <bk-popover theme="light" placement="left">
                            <span class="entry-link">更多</span>
                            <div slot="content">
                                <p class="entry-link" @click="showLog(props.row.id, props.row.buildNum, true)">
                                    日志
                                </p>
                                <p class="entry-link" v-for="entry in col.entries" :key="entry.type">
                                    <router-link :to="getArchiveUrl(props.row, entry.type)">{{ entry.label }}</router-link>
                                </p>
                            </div>
                        </bk-popover>
                    </p> -->
                </template>
                <template v-else-if="col.prop === 'remark'" v-slot="props">
                    <p class="remark-cell" :title="props.row.remark">

                        <span v-if="activeRemarkIndex === props.row.index && props.row.active">
                            <i v-if="isChangeRemark" class="bk-icon icon-circle-2-1 spin-icon" />
                            <textarea v-else v-bk-focus="1" rows="5" class="remark-input" v-model.trim="tempRemark" @click.stop @keypress.enter.prevent="triggerRemarkBlur" @blur="handleRemarkChange(props.row)" />
                        </span>
                        <span v-else :class="{ 'remark-span': true, active: props.row.active }" @click.stop="activeRemarkInput(props.row)">
                            {{ props.row.remark || '--' }}
                        </span>
                    </p>
                </template>
                <template v-else v-slot="props">
                    {{ props.row[col.prop] }}
                </template>
            </bk-table-column>
            <empty-tips v-if="emptyTipsConfig" class="build-list-table-empty-tips" slot="empty" v-bind="emptyTipsConfig"></empty-tips>
            <div v-if="loadingMore" class="loading-more" slot="append"><i class="bk-icon icon-circle-2-1 spin-icon"></i><span>数据加载中</span></div>
        </bk-table>
        <portal to="artifactory-popup">
            <div ref="artifactPopup" class="artifact-list-popup" v-show="actifactories.length" v-bk-clickoutside="hideArtifactoriesPopup">
                <div class="artifact-list-header">
                    <h2>构件列表</h2>
                    <span @click.stop="gotoArtifactoryList" class="history-text-link">详情</span>
                </div>
                <span ref="popupTriangle" class="popup-triangle"></span>
                <ul class="artifact-list-ul" v-if="visibleIndex !== -1">
                    <li v-for="artifactory in actifactories" :key="artifactory.name">
                        <p>
                            <span :title="artifactory.name" class="artifact-name">{{ artifactory.name }}</span>
                            <span class="artifact-size">{{ artifactory.size }}</span>
                        </p>
                        <i class="bk-icon icon-download download-link history-text-link" @click.stop="downloadFile(artifactory)" />
                    </li>
                    <footer v-if="needShowAll" @click.stop="showAllArtifactory" class="history-text-link">显示全部</footer>
                </ul>
            </div>
        </portal>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import emptyTips from '@/components/devops/emptyTips'
    import { convertFileSize, convertMStoStringByRule, convertMiniTime } from '@/utils/util'
    import { BUILD_HISTORY_TABLE_DEFAULT_COLUMNS, BUILD_HISTORY_TABLE_COLUMNS_MAP } from '@/utils/pipelineConst'
    import qrcode from '@/components/devops/qrcode'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'

    export default {
        name: 'build-history-table',
        components: {
            Logo,
            qrcode,
            emptyTips
        },
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
            needShowAll () {
                return this.data[this.visibleIndex].needShowAll
            },
            columnList () {
                return this.columns.map(key => BUILD_HISTORY_TABLE_COLUMNS_MAP[key])
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
            },
            retryable (row) {
                return row.pipelineVersion === this.currentPipelineVersion && ['QUEUE', 'SUCCEED', 'RUNNING'].indexOf(row.status) < 0
            },
            triggerRemarkBlur (e) {
                e.target.blur()
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
                            message: '修改备注成功'
                        })
                    } else {
                        this.resetRemark()
                    }
                } catch (e) {
                    this.$showTips({
                        theme: 'error',
                        message: '修改备注失败'
                    })
                }
            },
            resetRemark () {
                this.isChangeRemark = false
                this.tempRemark = ''
                this.activeRemarkIndex = -1
            },
            generateMaterial (material) {
                return material ? `${material.aliasName}${material.branchName ? `@${material.branchName}` : ''}` : '--'
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
                    // if (key === 'url') {
                    //     let res = await this.$store.dispatch('soda/requestExternalUrl', {
                    //         projectId: this.projectId,
                    //         artifactoryType: row.artifactoryType,
                    //         path: row.path
                    //     })

                    //     this.curIndexItemUrl = res.url
                    // } else {
                    const res = await this.$store.dispatch('soda/requestDownloadUrl', {
                        projectId,
                        artifactoryType,
                        path
                    })
                    window.open(res.url, '_self')
                    // }
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
                        message = '重试成功'
                        theme = 'success'

                        this.$emit('update-table')
                    } else {
                        message = '重试失败'
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
                        message = '终止流水线成功'
                        theme = 'success'

                        this.$emit('update-table')
                    } else {
                        message = '终止流水线失败'
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
        color: $fontWeightColor;
        > a {
            color: $fontWeightColor;
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
                .bk-icon.running-icon {
                    cursor: pointer;
                    animation: none;
                    font-size: 8px;
                    &:before {
                        content: "\E953";
                        border: 1px solid $fontWeightColor;
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
                max-width: 160px;
                @include ellipsis();
            }
            .material-commit-id {
                cursor: pointer;
                color: $primaryColor;
                padding: 0 6px;
                display: flex;
                .commit-times {
                    padding: 0 6px;
                    margin-left: 4px;
                    background: $fontWeightColor;
                    color: white;
                    border-radius: 20px;
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
            .remark-span {
                cursor: pointer;
                display: -webkit-box;
                -webkit-box-orient: vertical;
                -webkit-line-clamp: 1;
                overflow: hidden;
                &.active {
                    -webkit-line-clamp: 5;
                }
                &:hover {
                    color: $primaryColor;
                }
            }
            .remark-input {
                background-color: transparent;
                border: 1px solid #e4e4e4;
                border-radius: 2px;
                width: 100%;
                outline: none;
                resize: none;
                &:read-only {
                    border: 0;
                }
            }
            .icon-circle-2-1 {
                display: inline-block;
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
                    padding: 0 18px;
                    font-weight: bold;
                }
            }
            > footer {
                text-align: center;
                line-height: 35px;
            }
        }
    }
</style>
