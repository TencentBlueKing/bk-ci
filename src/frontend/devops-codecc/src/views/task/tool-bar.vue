<template>
    <div class="task-list-tool-bar">
        <main class="tool-bar-content">
            <section class="bar-info">
                <bk-button icon="plus" theme="primary" @click="$router.push({ name: 'task-new' })">{{$t('新增任务')}}</bk-button>
                <span class="task-total-count">{{$t('共x个任务', { num: taskCount })}}</span>
            </section>
            <section class="bar-handle">
                <i class="icon codecc-icon icon-filter" @click="isFilter = !isFilter"></i>
                <div v-bk-clickoutside="hideFeedBackMenu" style="display:inline">
                    <i class="icon codecc-icon icon-sort" @click.stop="toggleFeedBackMenu"></i>
                    <ul class="feedback-menu" v-show="showOrderType">
                        <li v-for="(order, index) in orderList" :key="`order${index}`">
                            <a @click.stop="changeOrderType(order.id)">{{ order.name }}</a>
                        </li>
                    </ul>
                </div>
            </section>
        </main>
        <div class="task-filter-bar" v-if="isFilter">
            <div class="task-filter-select">
                <label>{{$t('状态')}}</label>
                <bk-select v-model="searchInfo.taskStatus" style="width: 240px;">
                    <bk-option v-for="option in statusList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </div>
            <!-- <div class="task-filter-select">
                <label>{{$t('代码库')}}</label>
                <bk-select v-model="searchInfo.codelib" style="width: 240px;" searchable>
                    <bk-option v-for="option in codelibList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </div> -->
            <div class="task-filter-select">
                <label>{{$t('任务来源')}}</label>
                <bk-select v-model="searchInfo.taskSource" style="width: 240px;">
                    <bk-option v-for="option in sourceList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            taskCount: {
                type: Number,
                default: 0
            },
            searchInfo: {
                type: Object,
                default: {}
            }
        },
        data () {
            return {
                isFilter: true,
                showOrderType: false,
                statusList: [
                    { id: 'SUCCESS', name: '成功' },
                    { id: 'FAIL', name: '失败' },
                    { id: 'WAITING', name: '待分析' },
                    { id: 'ANALYSING', name: '分析中' },
                    { id: 'DISABLED', name: '已停用' }
                ],
                codelibList: [],
                orderList: [
                    { id: 'SIMPLIFIED_PINYIN', name: `${this.$t('按名称')}A-Z` },
                    { id: 'CREATE_DATE', name: this.$t('按创建时间') },
                    { id: 'LAST_EXECUTE_DATE', name: this.$t('按最近执行时间') }
                ],
                sourceList: [
                    { id: 'bs_pipeline', name: this.$t('流水线') },
                    { id: 'bs_codecc', name: this.$t('自建任务') }
                ]
            }
        },
        methods: {
            hideFeedBackMenu () {
                this.showOrderType = false
            },
            toggleFeedBackMenu () {
                this.showOrderType = !this.showOrderType
            },
            changeOrderType (val) {
                this.showOrderType = false
                this.$emit('changeOrder', val)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .task-list-tool-bar {
        margin-bottom: 16px;
        .tool-bar-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .task-total-count {
            margin-left: 18px;
            font-size: 12px;
            color: $itemBorderColor;
        }
        .bar-handle {
            position: relative;
            .codecc-icon {
                font-size: 22px;
                color: $fontLightColor;
                cursor: pointer;
                &:first-child {
                    margin-right: 6px;
                }
                &:hover {
                    color: $goingColor;
                }
            }
            .icon-sort {
                font-size: 20px;
            }
        }
        .task-filter-bar {
            display: flex;
            margin-top: 16px;
        }
        .task-filter-select {
            display: flex;
            align-items: center;
            margin-right: 40px;
            label {
                margin-right: 16px;
                font-size: 12px;
            }
        }
        .feedback-menu {
            z-index: 3;
            position: absolute;
            background-color: white;
            border: 1px solid $itemBorderColor;
            border-radius: 2px;
            top: 36px;
            right: -8px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            &:before {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $itemBorderColor;
                border-bottom: 0;
                border-right: 0;
                right: 18px;
                top: -5px;
                transform: rotate(45deg);
                background: white;
            }
            li {
                border-bottom: 1px solid $itemBorderColor;
                text-align: center;
                &:last-child {
                    border: 0;
                }
                line-height: 32px;
                a {
                    cursor: pointer;
                    line-height: 30px;
                    white-space: nowrap;
                    padding: 0 14px;
                    font-size: 14px;
                    color: $fontWeightColor;
                    &:hover {
                        color: $primaryColor;
                    }
                }
            }
        }
    }
</style>
