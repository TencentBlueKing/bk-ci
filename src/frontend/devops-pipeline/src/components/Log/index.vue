<template>
    <section class="pipeline-logs">
        <div class="options-log">
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
            <bk-button class="showtime-log"
                size="small"
                :theme="showTime ? 'primary' : 'default'"
                @click.stop="showTime = !showTime"
            >
                显示时间
            </bk-button>
            <a class="bk-button bk-button-small export-log" v-if="showExport" download :href="downloadUrl">导出日志</a>
        </div>
        <div ref="logContainer" :buildNo="buildNo"></div>
    </section>
</template>

<script>
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
            logUrl () {
                const { $route: { params } } = this
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${params.projectId}/${params.pipelineId}`
            },
            downloadUrl () {
                const { logUrl, buildNo, buildTag } = this
                return `${logUrl}/${buildNo}/download${buildTag ? `?tag=${buildTag}` : ''}`
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
        height: 100%;
        position: relative;
        .options-log {
            top: -46px;
            position: absolute;
            font-size: 0;
            color: $primaryColor;
            right: 20px;
            height: 32px;
            z-index: 20;
        }
        > div {
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
