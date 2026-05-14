<template>
    <ul class="trigger-event-list">
        <li
            v-for="(event, index) in events"
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
                <i18n-t
                    v-if="getEventDescKey(event.eventDesc)"
                    :keypath="getEventDescKey(event.eventDesc)"
                    tag="span"
                    class="trigger-event-desc-content"
                >
                    <template #branch>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'branch')" />
                    </template>
                    <template #commit>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'commit')" />
                    </template>
                    <template #user>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'user')" />
                    </template>
                    <template #issue>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'issue')" />
                    </template>
                    <template #mr>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'mr')" />
                    </template>
                    <template #source>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'source')" />
                    </template>
                    <template #tag>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'tag')" />
                    </template>
                    <template #note>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'note')" />
                    </template>
                    <template #review>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'review')" />
                    </template>
                    <template #pr>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'pr')" />
                    </template>
                    <template #change>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'change')" />
                    </template>
                    <template #action>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'action')" />
                    </template>
                    <template #revision>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'revision')" />
                    </template>
                    <template #remoteUser>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'remoteUser')" />
                    </template>
                    <template #pipeline>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'pipeline')" />
                    </template>
                    <template #event>
                        <EventDescSlot :slot-data="getEventDescSlot(event.eventDesc, 'event')" />
                    </template>
                </i18n-t>
                <span v-else>{{ getEventDescFallback(event.eventDesc) }}</span>
                <span class="trigger-event-item-lighter-field">{{ convertTime(event.eventTime) }}</span>
            </p>
            <p class="trigger-event-reason">
                <span>{{ event.reason }}</span>  |
                <em
                    v-if="event.buildNum"
                >
                    <a
                        v-if="getBuildNumLink(event.buildNum)"
                        class="text-link"
                        :href="getBuildNumLink(event.buildNum).href"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        {{ getBuildNumLink(event.buildNum).text }}
                    </a>
                    <span v-else>{{ event.buildNum }}</span>
                </em>
                <em
                    v-bk-overflow-tips
                    v-else-if="Array.isArray(event.reasonDetailList)"
                >
                    {{ event.reasonDetailList.join(' | ') }}
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

    const BUILD_NUM_LINK_REG = /^<a href="([^"]+)" target="_blank">([^<]+)<\/a>$/
    const toText = value => (value === undefined || value === null ? '' : String(value))
    const textParam = index => params => ({ type: 'text', text: toText(params[index]) })
    const userParam = index => params => ({ type: 'user', text: toText(params[index]) })
    const linkParam = (hrefIndex, textIndex, prefix = '', hrefFormatter) => params => ({
        type: 'link',
        href: hrefFormatter ? hrefFormatter(params[hrefIndex]) : params[hrefIndex],
        text: `${prefix}${toText(params[textIndex])}`
    })
    const mapParams = mapping => params => Object.entries(mapping).reduce((acc, [name, mapper]) => {
        acc[name] = mapper(params)
        return acc
    }, {})
    const tgitIssueParams = mapParams({
        issue: linkParam(0, 1, '!'),
        user: userParam(2)
    })
    const tgitMrParams = mapParams({
        mr: linkParam(0, 1, '!'),
        user: userParam(2)
    })
    const tgitReviewParams = mapParams({
        review: linkParam(0, 1),
        user: userParam(2)
    })
    const EVENT_DESC_PARAM_MAPPERS = {
        bkTgitPushEventDesc: mapParams({
            branch: textParam(0),
            commit: linkParam(1, 2),
            user: userParam(3)
        }),
        bkTgitIssueCreatedEventDesc: tgitIssueParams,
        bkTgitIssueUpdatedEventDesc: tgitIssueParams,
        bkTgitIssueClosedEventDesc: tgitIssueParams,
        bkTgitIssueReopenedEventDesc: tgitIssueParams,
        bkTgitMrCreatedEventDesc: tgitMrParams,
        bkTgitMrUpdatedEventDesc: tgitMrParams,
        bkTgitMrClosedEventDesc: tgitMrParams,
        bkTgitMrReopenedEventDesc: tgitMrParams,
        bkTgitMrPushUpdatedEventDesc: tgitMrParams,
        bkTgitMrMergedEventDesc: tgitMrParams,
        bkTgitTagPushEventDesc: mapParams({
            source: textParam(0),
            tag: linkParam(1, 2),
            user: userParam(3)
        }),
        bkTgitTagDeleteEventDesc: mapParams({
            source: textParam(0),
            tag: linkParam(1, 2),
            user: userParam(3)
        }),
        bkTgitNoteEventDesc: mapParams({
            note: linkParam(0, 1),
            user: userParam(2)
        }),
        bkTgitReviewCreatedEventDesc: tgitReviewParams,
        bkTgitReviewApprovedEventDesc: tgitReviewParams,
        bkTgitReviewApprovingEventDesc: mapParams({
            review: linkParam(0, 1)
        }),
        bkTgitReviewClosedEventDesc: mapParams({
            review: linkParam(0, 1)
        }),
        bkTgitReviewChangeDeniedEventDesc: tgitReviewParams,
        bkTgitReviewChangeRequiredEventDesc: tgitReviewParams,
        bkGithubPushEventDesc: mapParams({
            branch: textParam(0),
            commit: linkParam(1, 2),
            user: userParam(3)
        }),
        bkGithubCreateTagEventDesc: mapParams({
            tag: linkParam(0, 1),
            user: userParam(2)
        }),
        bkGithubCreateBranchEventDesc: mapParams({
            branch: linkParam(0, 1),
            user: userParam(2)
        }),
        bkGithubPrEventDesc: mapParams({
            pr: linkParam(0, 1, '!'),
            user: userParam(2),
            action: textParam(3)
        }),
        bkP4EventDesc: mapParams({
            change: textParam(0),
            user: userParam(1),
            action: textParam(2)
        }),
        bkSvnCommitEventDesc: mapParams({
            revision: textParam(0),
            user: userParam(1)
        }),
        bkManualStartEventDesc: mapParams({
            user: userParam(0)
        }),
        bkRemoteStartEventDesc: params => ({
            remoteUser: {
                type: 'user',
                text: `${toText(params[0])} [${toText(params[1])}]`
            }
        }),
        bkServiceStartEventDesc: mapParams({
            user: userParam(0)
        }),
        bkPipelineStartEventDesc: mapParams({
            pipeline: linkParam(1, 2),
            user: userParam(0)
        }),
        bkTimingStartEventDesc: mapParams({
            user: userParam(0)
        }),
        bkEventReplayDesc: mapParams({
            event: linkParam(0, 0, '', value => `?eventId=${value}`),
            user: userParam(1)
        }),
        bkRepoEnablePacEventDesc: mapParams({
            user: userParam(0)
        })
    }
    const EventDescSlot = {
        functional: true,
        props: {
            slotData: {
                type: Object,
                default: () => ({})
            }
        },
        render (h, { props }) {
            const slotData = props.slotData || {}
            if (slotData.type === 'link' && slotData.href) {
                return h('a', {
                    class: 'text-link',
                    attrs: {
                        href: slotData.href,
                        target: '_blank',
                        rel: 'noopener noreferrer'
                    }
                }, slotData.text)
            }
            return h('span', {
                class: {
                    'trigger-user': slotData.type === 'user'
                }
            }, slotData.text || '')
        }
    }

    export default {
        components: {
            EventDescSlot
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
            }
        },
        inject: ['updateList'],
        methods: {
            ...mapActions('pipelines', [
                'reTriggerEvent'
            ]),
            convertTime,
            getEventDescKey (eventDesc) {
                const desc = this.normalizeEventDesc(eventDesc)
                const localeMessage = this.$i18n.getLocaleMessage(this.$i18n.locale) || {}
                return localeMessage[desc.code] ? desc.code : ''
            },
            getEventDescFallback (eventDesc) {
                return this.normalizeEventDesc(eventDesc).defaultMessage
            },
            normalizeEventDesc (eventDesc) {
                if (eventDesc && typeof eventDesc === 'object') {
                    return {
                        code: eventDesc.code || '',
                        params: eventDesc.params || [],
                        defaultMessage: eventDesc.defaultMessage || ''
                    }
                }
                if (typeof eventDesc === 'string') {
                    try {
                        const desc = JSON.parse(eventDesc)
                        return {
                            code: desc.code || '',
                            params: desc.params || [],
                            defaultMessage: desc.defaultMessage || ''
                        }
                    } catch (e) {
                        return {
                            code: '',
                            params: [],
                            defaultMessage: eventDesc
                        }
                    }
                }
                return {
                    code: '',
                    params: [],
                    defaultMessage: ''
                }
            },
            getEventDescSlot (eventDesc, slotName) {
                const desc = this.normalizeEventDesc(eventDesc)
                const mapper = EVENT_DESC_PARAM_MAPPERS[desc.code]
                const param = mapper?.(desc.params || [])[slotName]
                if (!param) {
                    return { type: 'text', text: '' }
                }

                if (param.type !== 'link') {
                    return param
                }

                const href = this.safeUrl(param.href)
                return href ? { ...param, href } : { type: 'text', text: param.text }
            },
            toText (value) {
                return toText(value)
            },
            safeUrl (url) {
                const value = this.toText(url).trim()
                if (!value) {
                    return ''
                }

                try {
                    const parsed = new URL(value, window.location.origin)
                    if (!['http:', 'https:'].includes(parsed.protocol)) {
                        return ''
                    }
                    return /^(\/(?!\/)|[?#])/.test(value) ? value : parsed.href
                } catch (e) {
                    return ''
                }
            },
            getBuildNumLink (buildNum) {
                const match = this.toText(buildNum).match(BUILD_NUM_LINK_REG)
                if (!match) {
                    return null
                }
                const href = this.safeUrl(match[1])
                return href ? { href, text: match[2] } : null
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
