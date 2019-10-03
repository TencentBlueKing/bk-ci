<template>
    <bk-sideslider class="sodaci-property-panel pipeline-logs" width="820" :is-show.sync="visible" :quick-close="true">
        <div class="options-log" slot="header">
            <p>{{ title }}</p>
            <div>
                <a href="javascript:;" class="log-count-menu dropdown-menu-more" v-if="executeCount > 1">
                    <div class="dropdown-trigger" @click.stop="toggleCountMenu">
                        <span class="log-menu">（{{`${currentCount}/${executeCount}`}}
                            <i :class="['bk-icon icon-angle-down log-menu', { 'icon-flip': isLogMore }]"
                                id="toggleIcon"></i>）
                        </span>
                    </div>
                    <div class="dropdown-list" v-if="showCountMenu">
                        <ul class="list-wrapper">
                            <li :class="['log-item-text log-menu', { 'active': num === currentCount }]" v-for="num in executeCount" :key="num" @click.stop="handleCurrentCount(num)">{{num}}</li>
                        </ul>
                    </div>
                </a>
                <bk-button v-if="buildTag"
                    class="share-log copy-log-link"
                    size="small"
                    theme="default"
                    :data-clipboard-text="copyUrl"
                    @click="copyLink"
                >
                    复制链接
                </bk-button>
                <bk-button class="showtime-log"
                    size="small"
                    :theme="showTime ? 'primary' : 'default'"
                    @click.stop="showTime = !showTime"
                >
                    显示时间
                </bk-button>
                <a class="bk-button bk-button-small export-log" v-if="showExport" download :href="downloadUrl">导出日志</a>
            </div>
        </div>
        <div slot="content" class="slider-log-content" ref="logContainer" :buildNo="buildNo"></div>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import Clipboard from 'clipboard'

    export default {
        name: 'pipeline-log',
        props: {
            buildNo: {
                type: String,
                required: true
            },
            buildNum: {
                type: Number,
                required: true
            },
            buildTag: {
                type: String,
                default: ''
            },
            showExport: {
                type: Boolean,
                default: false
            },
            executeCount: {
                type: Number,
                default: 1
            },
            title: {
                type: String,
                default: '查看日志'
            }
        },
        data () {
            return {
                showTime: false,
                showCountMenu: false,
                isLogMore: false,
                currentCount: 1
            }
        },
        computed: {
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            logUrl () {
                const { $route: { params } } = this
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${params.projectId}/${params.pipelineId}`
            },
            downloadUrl () {
                const { logUrl, buildNo, buildTag } = this
                return `${logUrl}/${buildNo}/download${buildTag ? `?tag=${buildTag}` : ''}`
            },
            copyUrl () {
                const { projectId, pipelineId, buildNo, buildTag } = this
                return `${WEB_URL_PIRFIX}/pipeline/${projectId}/${pipelineId}/detail/${buildNo}#${buildTag}`
            },
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            }
        },
        watch: {
            buildNo (newVal, oldVal) {
                newVal && this.renderLog(newVal)
            },
            buildTag (newVal, oldVal) {
                this.renderLog(this.buildNo)
            },
            showTime (newVal, oldVal) {
                this.renderLog(this.buildNo)
            }
        },
        created () {
            this.addClickListenr()
        },
        beforeDestroy () {
            this._destroyLog()
            this.removeClickListenr()
        },
        mounted () {
            this.currentCount = this.executeCount
            this.buildNo && this.renderLog(this.buildNo)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),
            renderLog (buildNo) {
                window.SodaLog.render(this.$refs.logContainer, this.logUrl, buildNo, this.showTime, this.buildTag, this.currentCount)
            },
            _destroyLog () {
                window.SodaLog.unMount(this.$refs.logContainer)
            },
            addClickListenr () {
                document.addEventListener('mouseup', this.clickMenuHandler)
            },
            removeClickListenr () {
                document.removeEventListener('mouseup', this.clickMenuHandler)
            },
            toggleCountMenu () {
                this.isLogMore = this.showCountMenu = !this.showCountMenu
            },
            clickMenuHandler (event) {
                if (event.target.className.indexOf('log-menu') === -1) {
                    this.showCountMenu = this.isLogMore = false
                }
            },
            copyLink () {
                this.clipboard = new Clipboard('.copy-log-link').on('success', e => {
                    this.$showTips({
                        theme: 'success',
                        message: '复制成功'
                    })
                })
            },
            handleCurrentCount (num) {
                if (num !== this.currentCount) {
                    this.currentCount = num
                    this.renderLog(this.buildNo)
                    this.isLogMore = this.showCountMenu = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './src/scss/conf.scss';

    .pipeline-logs {
        .options-log {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-right: 20px;
        }
        .slider-log-content {
            height: 100%;
        }
        .log-count-menu {
            position: relative;
            display: inline-block;
            margin-right: 5px;
            vertical-align: middle;
            font-size: 14px;
            color: $primaryColor;
            .dropdown-list {
                position: absolute;
                top: 26px;
                right: 0;
                background: #fff;
                min-width: 100%;
                max-height: 250px;
                background: #fff;
                padding: 0;
                margin: 0;
                z-index: 99;
                overflow: auto;
                border-radius: 2px;
                border: 1px solid #c3cdd7;
                transition: all .3s ease;
                box-shadow: 0 2px 6px rgba(51, 60, 72, 0.1);
            }
            .log-item-text {
                display: block;
                line-height: 36px;
                padding: 0 15px;
                text-align: center;
                color: #63656E;
                font-size: 14px;
                cursor: pointer;
                &:hover, &.active {
                    background-color: #ebf4ff;
                    color: #3c96ff;
                }
            }
            .bk-icon {
                display: inline-block;
                margin-left: 5px;
                transition: all ease 0.2s;
                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }
    }
</style>
