<template>
    <i18n
        v-if="descKey"
        :path="descKey"
        tag="span"
        class="trigger-event-desc-content"
    >
        <template
            v-for="slotName in slotNames"
            #[slotName]
        >
            <EventDescSlot
                :key="slotName"
                :slot-data="getSlot(slotName)"
            />
        </template>
    </i18n>
    <span v-else>{{ fallbackMessage }}</span>
</template>

<script>
    import {
        EVENT_DESC_PARAM_MAPPERS,
        EVENT_DESC_SLOT_NAMES,
        normalizeEventDesc,
        safeUrl
    } from './eventDescConfig'

    /** codelib 服务的 i18n 命名空间前缀 */
    const I18N_PREFIX = 'codelib.'

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
            eventDesc: {
                type: [Object, String],
                default: () => ({})
            }
        },
        data () {
            return {
                slotNames: EVENT_DESC_SLOT_NAMES
            }
        },
        computed: {
            normalized () {
                return normalizeEventDesc(this.eventDesc)
            },
            descKey () {
                return EVENT_DESC_PARAM_MAPPERS[this.normalized.code] ? `${I18N_PREFIX}${this.normalized.code}` : ''
            },
            fallbackMessage () {
                return this.normalized.defaultMessage
            }
        },
        methods: {
            getSlot (slotName) {
                const mapper = EVENT_DESC_PARAM_MAPPERS[this.normalized.code]
                const param = mapper?.(this.normalized.params || [])[slotName]
                if (!param) {
                    return { type: 'text', text: '' }
                }
                if (param.type !== 'link') {
                    return param
                }
                const href = safeUrl(param.href)
                return href ? { ...param, href } : { type: 'text', text: param.text }
            }
        }
    }
</script>
