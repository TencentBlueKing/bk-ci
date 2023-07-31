<template>
    <div class="trigger-event-wrapper" v-bk-loading="{ isLoading }">
        <header class="trigger-event-filter-bar">
            <time-picker
                style="width: 320px"
                name="dateTimeRange"
                id="dateTimeRange"
                :value="dateTimeRange"
                :date-picker-conf="datePickerConf"
                :handle-change="handleDateRangeChange"
            />
            <search-select
                :placeholder="$t('triggerEventfilterTips')"
                :data="filterData"
                :values="searchKey"
                @change="updateSearchKey"
            >
            </search-select>
        </header>
        <bk-timeline
            ext-cls="trigger-event-timeline"
            :list="timelineList"
        >
        </bk-timeline>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import TriggerEventChildren from './TriggerEventChildren.vue'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import SearchSelect from '@blueking/search-select'

    import '@blueking/search-select/dist/styles/index.css'
    export default {
        components: {
            // eslint-disable-next-line vue/no-unused-components
            TriggerEventChildren,
            TimePicker,
            SearchSelect
        },
        data () {
            return {
                dateTimeRange: [],
                searchKey: [],
                triggerTypeList: [],
                eventTypeList: []
            }
        },
        computed: {
            datePickerConf () {
                return {
                    format: 'yyyy-MM-dd HH:mm:ss',
                    type: 'datetimerange'
                }
            },
            filterData () {
                return [
                    {
                        name: this.$t('triggerType'),
                        id: 'triggerType',
                        multiable: true,
                        children: this.triggerTypeList
                    },
                    {
                        name: this.$t('eventType'),
                        id: 'eventType',
                        multiable: true,
                        children: this.eventTypeList
                    },
                    {
                        name: this.$t('details.trigger'),
                        id: 'triggerUser'
                    }
                ]
            },
            timelineList () {
                return [{
                    tag: '2022-10-10',
                    content: this.$createElement(TriggerEventChildren)
                }, {
                    tag: '2022-10-10',
                    content: this.$createElement(TriggerEventChildren)
                }]
            }
        },
        methods: {
            ...mapActions('pipelines', [
                'getTriggerEventList',
                'getTriggerTypeList',
                'getEventTypeList'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const [
                        triggerEventList,
                        triggerTypeList,
                        eventTypeList
                    ] = await Promise.all([
                        this.getTriggerEventList(),
                        this.getTriggerTypeList(),
                        this.getEventTypeList()
                    ])
                    console.log(triggerEventList, triggerTypeList, eventTypeList)
                    this.triggerEventList = triggerEventList
                    this.triggerTypeList = triggerTypeList
                    this.eventTypeList = eventTypeList
                } catch (error) {
                    console.error(error)
                } finally {
                    this.isLoading = false
                }
            },
            handleDateRangeChange (name, value) {
                this.dateTimeRange = value
            },
            updateSearchKey (searchKey) {
                this.searchKey = searchKey
            }

        }
    }
</script>

<style lang="scss">
    .trigger-event-wrapper {
        display: flex;
        flex-direction: column;
        padding: 24px;
        height: 100%;
        overflow: auto;
        background: white;
        .trigger-event-filter-bar {
            display: grid;
            grid-gap: 10px;
            grid-template-columns: 320px 1fr;
            margin-bottom: 16px;
            flex-shrink: 0;
        }
        .trigger-event-timeline {
            flex: 1;
            overflow: visible;
            .bk-timeline-dot .bk-timeline-content {
                max-width: none; // TODO: hack
            }
        }
    }
</style>
