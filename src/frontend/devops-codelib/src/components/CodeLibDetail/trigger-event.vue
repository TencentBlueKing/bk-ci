<template>
    <section class="trigger-event">
        <header class="header-warpper">
            <bk-date-picker
                class="date-picker mr15"
                v-model="daterange"
                type="daterange"
                :placeholder="$t('codelib.选择日期范围')"
                :options="{
                    disabledDate: time => time.getTime() > Date.now()
                }"
            >
            </bk-date-picker>
            <bk-search-select
                class="search-select"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="$t('codelib.触发器类型/事件类型/触发人/流水线名称')"
            >
            </bk-search-select>
        </header>
        <section class="timeline-warpper">
            <ul
                class="trigger-timeline"
                @scroll.passive="handleScroll"
            >
                <li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">{{ triggerType }}timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li>
                <li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li><li class="timeline-dot">
                    <div class="timeline-section">
                        <div class="timeline-title">timeline-title</div>
                        <div class="timeline-content"> timeline-content</div>
                    </div>
                </li>
            </ul>
        </section>
    </section>
</template>
<script>
    export default {
        name: 'basicSetting',
        components: {
        },
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                searchValue: [],
                daterange: []
            }
        },
        computed: {
            triggerType () {
                return this.curRepo.type || ''
            },
            triggerTypeList () {
                // const listMap = {
                //     CODE_SVN: [
                //         { name: this.$t('codelib. WEB_HOOK触发'), id: 'CODE_SVN' }
                //     ]
                // }
                return [
                    { name: this.$t('codelib.手动触发'), id: 'MANUAL' },
                    { name: this.$t('codelib.定时触发'), id: 'TIME_TRIGGER' },
                    { name: this.$t('codelib.服务触发'), id: 'SERVICE' },
                    { name: this.$t('codelib.流水线触发'), id: 'PIPELINE' },
                    { name: this.$t('codelib.远程触发'), id: 'REMOTE' }
                ]
            },
            eventTypeList () {
                const listMap = {
                    GITHUB: [
                        { name: this.$t('codelib.创建Branch/Tag事件'), id: 'CREATE' },
                        { name: this.$t('codelib.PR事件'), id: 'PULL_REQUEST' }
                    ],
                    CODE_SVN: [
                        { name: this.$t('codelib.POST_COMMIT事件'), id: 'POST_COMMIT' },
                        { name: this.$t('codelib.LOCK_COMMIT事件'), id: 'LOCK_COMMIT' },
                        { name: this.$t('codelib.PRE_COMMIT事件'), id: 'PRE_COMMIT' }
                    ],
                    CODE_GIT: [
                        { name: this.$t('codelib.推送事件'), id: 'PUSH' },
                        { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
                        { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
                        { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
                        { name: this.$t('codelib.评论事件'), id: 'NOTE' },
                        { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
                    ],
                    CODE_GITLAB: [
                        { name: this.$t('codelib.推送事件'), id: 'PUSH' },
                        { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
                        { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
                        { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
                        { name: this.$t('codelib.评论事件'), id: 'NOTE' },
                        { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
                    ],
                    CODE_TGIT: [
                        { name: this.$t('codelib.推送事件'), id: 'PUSH' },
                        { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
                        { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
                        { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
                        { name: this.$t('codelib.评论事件'), id: 'NOTE' },
                        { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
                    ],
                    CODE_P4: [
                        { name: this.$t('codelib.CHANGE_COMMIT事件'), id: 'CHANGE_COMMIT' },
                        { name: this.$t('codelib.PUSH_SUBMIT事件'), id: '.PUSH_SUBMIT' },
                        { name: this.$t('codelib.CHANGE_CONTENT事件'), id: 'CHANGE_CONTENT' },
                        { name: this.$t('codelib.CHANGE_SUBMIT事件'), id: 'CHANGE_SUBMIT' },
                        { name: this.$t('codelib.PUSH_CONTENT事件'), id: 'PUSH_CONTENT' },
                        { name: this.$t('codelib.PUSH_COMMIT事件'), id: 'PUSH_COMMIT' },
                        { name: this.$t('codelib.FIX_ADD事件'), id: 'FIX_ADD' },
                        { name: this.$t('codelib.FIX_DELETE事件'), id: 'FIX_DELETE' },
                        { name: this.$t('codelib.FORM_COMMIT事件'), id: 'FORM_COMMIT' },
                        { name: this.$t('codelib.SHELVE_COMMIT事件'), id: 'SHELVE_COMMIT' },
                        { name: this.$t('codelib.SHELVE_DELETE事件'), id: 'SHELVE_DELETE' },
                        { name: this.$t('codelib.SHELVE_SUBMIT事件'), id: 'SHELVE_SUBMIT' }
                    ]
                }
                return listMap[this.triggerType] || []
            },
            searchList () {
                const list = [
                    {
                        name: this.$t('codelib.触发器类型'),
                        id: 'triggerType',
                        children: this.triggerTypeList
                    },
                    {
                        name: this.$t('codelib.事件类型'),
                        id: 'eventType',
                        children: this.eventTypeList
                    },
                    {
                        name: this.$t('codelib.触发人'),
                        id: 'triggerUser'
                    },
                    {
                        name: this.$t('codelib.流水线名称'),
                        id: 'pipelineName'
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            }
        },
        created () {
            
        },
        methods: {
            handleScroll (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                // if (bottomDis <= 300 && !this.hasLoadEnd && !this.isLoadingMore) this.getListData()
                if (bottomDis <= 300) {
                    console.log('123 1')
                }
            }
        }
    }
</script>
<style lang='scss' scoped>
    .trigger-event {
        height: 100%;
        .header-warpper {
            display: flex;
        }
        .date-picker {
            max-width: 300px;
            min-width: 200px;
        }
        .search-select {
            width: 80%;
        }
        .timeline-warpper {
            height: calc(100% - 32px);
            padding-top: 20px;
        }
    }
    .trigger-timeline {
        overflow-y: scroll;
        height: 100%;
        padding: 8px;
        &::-webkit-scrollbar {
            background-color: #fff;
            height: 0px !important;
            width: 0px !important;
        }
        .timeline-dot {
            border-left: 1px solid #d8d8d8;
            font-size: 0;
            margin-top: 13px;
            padding-bottom: 24px;
            padding-left: 16px;
            position: relative;
            &::before {
                background: #fff;
                border: 2px solid #d8d8d8;
                border-radius: 50%;
                -webkit-box-sizing: border-box;
                box-sizing: border-box;
                content: "";
                display: inline-block;
                position: absolute;
                height: 15px;
                left: -8px;
                top: -16px;
                width: 15px;
            }
        }
        .timeline-dot:last-child {
            border-left: 1px solid transparent;
            padding-bottom: 0;
        }
        .timeline-section {
            position: relative;
            top: -16px;
        }
        .timeline-title {
            font-size: 14px;
            color: #63656e;
            padding-bottom: 10px;
            display: inline-block;
        }
        .timeline-content {
            word-break: break-all;
            font-size: 14px;
            color: #666;
        }
    }
</style>
