<template>
    <bk-timeline :list="timelineList">
        <TriggerEventChildren
            v-for="(item, index) in timelineList"
            :slot="`nodeContent${index}`"
            :key="index"
            :events="item.content"
        />
    </bk-timeline>
</template>
<script>
    import { computed, defineComponent } from 'vue'
    import dayjs from 'dayjs'
    import TriggerEventChildren from './TriggerEventChildren.vue'

    export default defineComponent({
        components: {
            TriggerEventChildren
        },
        props: {
            list: {
                type: Array,
                default: () => []
            }
        },
        setup (props) {
            const timelineList = computed(() => {
                const dateMap = props.list.reduce((acc, item) => {
                    const date = dayjs(item.eventTime).format('YYYY-MM-DD')
                    if (!acc.has(date)) {
                        acc.set(date, [])
                    }
                    acc.get(date).push(item)
                    return acc
                }, new Map())

                return Array.from(dateMap).map(([date, events]) => ({
                    tag: date,
                    content: events
                }))
            })
            return {
                timelineList
            }
        }

    })
</script>
