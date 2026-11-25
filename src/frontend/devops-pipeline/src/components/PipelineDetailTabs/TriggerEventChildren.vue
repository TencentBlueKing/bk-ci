<template>
    <ul class="trigger-event-list">
        <li
            v-for="(event, index) in displayEvents"
            :key="index"
            class="trigger-event-item"
        >
            <span
                class="trigger-event-item-indicator"
                :style="`background: ${statusColorMap[event.status]}29`"
            >
                <i :style="`background: ${statusColorMap[event.status]}`"></i>
            </span>
            <p class="trigger-event-desc">
                <EventDesc :event-desc="event.eventDesc" />
                <span class="trigger-event-item-lighter-field">{{ convertTime(event.eventTime) }}</span>
            </p>
            <p class="trigger-event-reason">
                <span>{{ event.reason }}</span>  |
                <em
                    v-bk-overflow-tips
                    v-if="event.reasonSegments.length"
                >
                    <template
                        v-for="(seg, segIndex) in event.reasonSegments"
                    >
                        <a
                            v-if="seg.type === 'link'"
                            :key="`link-${segIndex}`"
                            class="text-link"
                            :href="seg.href"
                            target="_blank"
                            rel="noopener noreferrer"
                        >{{ seg.text }}</a>
                        <span
                            v-else
                            :key="`text-${segIndex}`"
                        >{{ seg.text }}</span>
                    </template>
                </em>
            </p>
            <bk-button
                text
                size="small"
                theme="primary"
                @click="triggerEvent(event)"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: RESOURCE_TYPE.PIPELINE,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
            >
                {{ $t('reTrigger') }}
            </bk-button>
        </li>
    </ul>
</template>
<script>
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import { statusColorMap } from '@/utils/pipelineStatus'
    import { convertTime } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'
    import { parseAnchors } from './eventDescConfig'
    import EventDesc from './EventDesc.vue'

    export default {
        components: {
            EventDesc
        },
        props: {
            events: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            ...mapState('atom', ['pipelineInfo']),
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            RESOURCE_TYPE () {
                return RESOURCE_TYPE
            },
            statusColorMap () {
                return statusColorMap
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            displayEvents () {
                return this.events.map(event => ({
                    ...event,
                    reasonSegments: this.parseEventSegments(event)
                }))
            }
        },
        inject: ['updateList'],
        methods: {
            ...mapActions('pipelines', [
                'reTriggerEvent'
            ]),
            convertTime,
            parseEventSegments (event) {
                if (event.buildNum) {
                    return parseAnchors(event.buildNum)
                }
                if (Array.isArray(event.reasonDetailList)) {
                    return parseAnchors(event.reasonDetailList.join(' | '))
                }
                return []
            },
            async triggerEvent (event) {
                try {
                    const res = await this.reTriggerEvent({
                        projectId: this.$route.params.projectId,
                        detailId: event.detailId
                    })
                    if (res) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('reTriggerSuc'),
                            limit: 1
                        })
                        setTimeout(() => {
                            this.updateList()
                        }, 1000)
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error?.message ?? error
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    .trigger-event-list {
        display: grid;
        grid-gap: 10px;
        grid-auto-rows: 28px;
        width: 100%;
        .trigger-event-item {
            display: grid;
            grid-template-columns: 13px 2fr 1fr max-content;
            grid-gap: 10px;
            align-items: center;
            background: #FAFBFD;
            border-radius: 2px;
            padding: 0 10px;
            font-size: 12px;
            .trigger-event-item-lighter-field {
                color: #979BA5;
            }
            .trigger-event-desc {
                display: flex;
                align-items: center;
                grid-gap: 10px;
                @include ellipsis();
                .text-link,
                > span > a {
                    color: #3A84FF;
                    cursor: pointer;
                    &:hover {
                        color: #699df4;
                    }
                }
            }
            .trigger-event-reason {
                display: flex;
                align-items: center;
                grid-gap: 10px;
                overflow: hidden;
                > em {
                    flex: 1;
                    @include ellipsis();
                    font-style: normal;
                    > a {
                        color: #3A84FF;
                        cursor: pointer;
                        &:hover {
                            color: #699df4;
                        }
                    }
                }
                > span {
                    flex-shrink: 0;
                    @include ellipsis();
                }
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
