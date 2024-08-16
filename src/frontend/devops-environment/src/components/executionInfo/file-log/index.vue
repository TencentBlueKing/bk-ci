<template>
    <div
        ref="contentBox"
        class="file-download-log"
        @scroll="handleScroll"
        v-bkloading="{ isLoading, opacity: .1 }">
        <div>
            <file-item
                v-for="(item, index) in renderList"
                :key="index"
                :index="index"
                :open-memo="openMemo"
                :data="item"
                :render-content-map="renderContentMap"
                @on-toggle="handleToggle" />
        </div>
        <!-- <div ref="load" class="load-more" v-if="fileTaskLogs.length && !renderList.length">
            <div class="loading-flag">
                <icon name="loading" size="16" />
            </div>
            <div>{{ $t('environment.加载中') }}</div>
        </div> -->
    </div>
</template>
<script>
    import { mapActions } from 'vuex'
    import { throttle } from '@/utils/util'
    import FileItem from './log-item'

    export default {
        name: '',
        components: {
            FileItem
        },
        // mixins: [
        //     mixins
        // ],
        props: {
            name: String,
            hostId: Number,
            finished: Boolean,
            ip: {
                type: String
            },
            mode: {
                type: String
            }
        },
        data () {
            return {
                // 基础信息loading
                isLoading: false,
                // 异步获取日志内容loading
                // 文件基础信息列表
                fileTaskLogs: [],
                // 前端分页页码
                page: 0,
                pageSize: 10,
                // 被展开的日志记录
                openMemo: {},
                // 被展开的文件具体日志内容
                renderContentMap: {},
                isMemoChange: false
            }
        },
        computed: {
            renderList () {
                let uploadLogs = []
                // let downloadLogs = []
                this.fileTaskLogs.forEach(log => {
                    uploadLogs = log.fileLogList
                    // const mode = log.fileLogList[0].mode
                    // if (mode === 0) {
                    //     uploadLogs = log.fileLogList
                    // } else if (mode === 1) {
                    //     downloadLogs = log.fileLogList
                    // }
                })
                // return this.mode === 'upload' ? uploadLogs : downloadLogs
                return uploadLogs
            },
            jobInstanceId () {
                return this.$route.query.jobInstanceId
            },
            stepInstanceId () {
                return this.$route.query.stepInstanceId
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            /**
             * @desc 查看的日志目标改变，重新获取日志
             * 展开第一个文件的日志,其他收起
             */
            hostId: {
                handler (val) {
                    if (!val) {
                        this.fileTaskLogs = []
                        return
                    }
                    this.isLoading = true
                    this.page = 0
                    this.openMemo = {
                        0: true
                    }

                    this.fetchData()
                    if (this.$refs.contentBox) {
                        this.$refs.contentBox.scrollTop = 0
                    }
                },
                immediate: true
            }
        },
        created () {
            // 前端分页加载器，控制触发频率
            this.timer = null
        },
        mounted () {
            window.addEventListener('resize', this.handleScroll)
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.handleScroll)
            })
        },
        methods: {
            ...mapActions('environment', [
                'getJobInstanceLogs'
            ]),
            /**
             * @desc 获取文件日志
             *
             * 默认展开第一个文件的日志
             * 如果文件信息里面不包含日志内容，需要异步获取文件内容
             */
            fetchData () {
                if (!this.hostId) {
                    this.isLoading = false
                    this.fileTaskLogs = []
                }
                this.getJobInstanceLogs({
                    projectId: this.projectId,
                    jobInstanceId: this.jobInstanceId,
                    stepInstanceId: this.stepInstanceId,
                    hostIdList: [this.hostId]
                })
                    .then(res => {
                        this.fileTaskLogs = res.fileTaskLogs || []
                        
                        if (this.fileTaskLogs.length > 0 && !this.isMemoChange) {
                            this.openMemo = {
                                0: true
                            }
                            this.isMemoChange = true
                        }

                        if (!this.finished) {
                            setTimeout(() => {
                                this.fetchData()
                            }, 5000)
                        }
                    })
                    .finally(() => {
                        this.isLoading = false
                    })
            },
            /**
             * @desc 外部调用
             */
            resize () {
                this.handleScroll()
            },
            /**
             * @desc 滚动分页加载
             */
            handleScroll: throttle(function () {
                if (!this.$refs.load) {
                    return
                }
                const windowHeight = window.innerHeight
                const { top } = this.$refs.load.getBoundingClientRect()
                if (windowHeight + 50 > top && !this.timer) {
                    // 本地日志加载器
                    this.timer = setTimeout(() => {
                        this.page += 1
                        this.timer = 0
                    }, 350)
                }
            }, 80),
            /**
             * @desc 文件日志展开收起
             */
            handleToggle (index, toggle) {
                const openMemo = { ...this.openMemo }
                openMemo[index] = toggle
                this.openMemo = Object.freeze(openMemo)
            }
        }
    }
</script>
<style lang='scss'>
    @keyframes file-log-loading-ani {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .file-download-log {
        height: 100%;
        padding-top: 10px;
        overflow-y: scroll;

        &::-webkit-scrollbar {
            background-color: #1d1d1d !important;
            width: 14px;
            height: 14px;
        }

        &::-webkit-scrollbar-thumb {
            background-color: #3b3c42 !important;
            border-radius: 0 !important;
            border: 1px solid #63656e !important;
        }

        &::-webkit-scrollbar-corner {
            background-color: transparent !important;;
        }

        .load-more {
            display: flex;
            align-items: center;
            height: 20px;
            padding-top: 20px;
            padding-bottom: 24px;
            padding-left: 44px;
            color: #fff;

            .loading-flag {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 20px;
                height: 20px;
                transform-origin: center center;
                animation: file-log-loading-ani 1s linear infinite;
            }
        }
    }
</style>
