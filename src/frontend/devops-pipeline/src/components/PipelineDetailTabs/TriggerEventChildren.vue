<template>
    <ul class="trigger-event-list">
        <li v-for="(event, index) in events" :key="index" class="trigger-event-item">
            <span
                class="trigger-event-item-indicator"
                :style="`background: ${statusColorMap[event.status]}29`"
            >
                <i :style="`background: ${statusColorMap[event.status]}`"></i>
            </span>
            <p>
                [{{event.branch}}] commit
                [<span class="text-link">{{event.commitId}}</span>]
                pushed
                <span class="trigger-event-item-lighter-field">by {{ event.triggerUser }} 202201010 20:12:12</span>
            </p>
            <span>
                {{ event.trigger }}  |
                <e>#{{event.buildNum}}</e>
            </span>
            <span class="text-link">{{$t('重新触发')}}</span>
        </li>
    </ul>
</template>
<script>
    import { statusColorMap } from '@/utils/pipelineStatus'
    export default {
        props: {
            events: {
                type: Array,
                default: () => [{
                    branch: 'master',
                    commitId: 'ca0c98a1',
                    trigger: 'TRIGGER_SUCCESS',
                    triggerUser: 'fayewang',
                    status: 'CANCELED',
                    buildNum: 1
                }, {
                    branch: 'master',
                    commitId: 'ca0c98a1',
                    trigger: 'TRIGGER_SUCCESS',
                    triggerUser: 'fayewang',
                    status: 'SUCCEED',
                    buildNum: 1
                }, {
                    branch: 'master',
                    commitId: 'ca0c98a1',
                    trigger: 'TRIGGER_SUCCESS',
                    triggerUser: 'fayewang',
                    status: 'FAILED',
                    buildNum: 1
                }, {
                    branch: 'master',
                    commitId: 'ca0c98a1',
                    trigger: 'TRIGGER_SUCCESS',
                    triggerUser: 'fayewang',
                    status: 'RUNNING',
                    buildNum: 1
                }]
            }
        },
        computed: {
            statusColorMap () {
                return statusColorMap
            }
        }
    }
</script>

<style lang="scss">

    .trigger-event-list {
        display: grid;
        grid-gap: 10px;
        grid-auto-rows: 28px;
        .trigger-event-item {
            display: grid;
            grid-template-columns: 13px 1fr 1fr max-content;
            grid-gap: 10px;
            align-items: center;
            background: #FAFBFD;
            border-radius: 2px;
            padding: 0 10px;
            font-size: 12px;
            .trigger-event-item-lighter-field {
                color: #979BA5;
            }
            &-indicator {
                width: 13px;
                height: 13px;
                border-radius: 50%;
                background: #979BA529;
                position: relative;
                > i {
                    position: absolute;
                    top: 3px;
                    left: 3px;
                    width: 7px;
                    height: 7px;
                    border-radius: 50%;
                    background: #979BA5
                }
            }
        }
    }
</style>
