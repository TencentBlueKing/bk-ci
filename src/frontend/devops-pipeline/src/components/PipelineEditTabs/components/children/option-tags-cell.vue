<template>
    <div class="option-tags-cell">
        <!-- hidden measurement layer: placed outside the flex container to avoid layout interference.
             IMPORTANT: every attribute/class that affects tag width (closable, etc.) MUST mirror
             the real rendered tag, otherwise measured width will not match real width. -->
        <div
            ref="measureLayerRef"
            class="tags-measure-layer"
            aria-hidden="true"
        >
            <bk-tag
                v-for="(opt, idx) in options"
                :key="`m-${idx}`"
                class="measure-tag"
                :closable="!disabled"
            >
                {{ opt.value || opt.key }}
            </bk-tag>
            <bk-tag class="measure-tag measure-more-tag">
                {{ morePlaceholder }}
            </bk-tag>
        </div>

        <div
            ref="tagsContainerRef"
            :class="['tags-container', { 'is-expanded': expanded }]"
        >
            <template v-if="options && options.length">
                <bk-tag
                    v-for="(opt, idx) in displayedOptions"
                    :key="idx"
                    :closable="!disabled"
                    @close="handleTagClose(idx)"
                >
                    {{ opt.value || opt.key }}
                </bk-tag>
                <bk-tag
                    v-if="hasCollapsed"
                    class="more-toggle-tag"
                    tabindex="0"
                    role="button"
                    :aria-label="expanded ? t('settings.fold') : `+${hiddenCount}`"
                    @click="handleToggleExpand"
                >
                    {{ expanded ? t('settings.fold') : `+${hiddenCount}` }}
                </bk-tag>
            </template>
        </div>

        <a
            class="text-link option-edit-link"
            tabindex="0"
            @click.stop="handleEditClick"
            @keydown.enter.stop.prevent="handleEditClick"
        >
            <i class="bk-icon icon-edit-line" />
            <span v-if="!options || !options.length">{{ t('storeMap.formListFieldOptionAdd') }}</span>
        </a>
    </div>
</template>

<script>
    import UseInstance from '@/hook/useInstance'
    import { computed, defineComponent, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

    // reserved horizontal space budget for the "+N" tag (px). Used as a safety buffer during measurement.
    const MORE_TAG_RESERVE = 48
    // gap between tags, must match css gap below
    const TAG_GAP = 4
    // safety margin (px) to absorb sub-pixel rounding
    const SAFETY_MARGIN = 1

    export default defineComponent({
        name: 'OptionTagsCell',
        props: {
            options: {
                type: Array,
                default: () => ([])
            },
            disabled: {
                type: Boolean,
                default: false
            },
            onDelete: {
                type: Function,
                required: true
            },
            onEdit: {
                type: Function,
                required: true
            }
        },
        setup (props) {
            const { t } = UseInstance()

            const tagsContainerRef = ref(null)
            const measureLayerRef = ref(null)

            const expanded = ref(false)
            const visibleCount = ref(0)

            // placeholder used during measurement to reserve width for "+NN" style label
            const morePlaceholder = computed(() => {
                const len = (props.options || []).length
                return `+${len || 99}`
            })

            const hasCollapsed = computed(() => {
                if (!props.options || !props.options.length) return false
                return visibleCount.value < props.options.length
            })

            const hiddenCount = computed(() => {
                const total = (props.options || []).length
                return Math.max(total - visibleCount.value, 0)
            })

            const displayedOptions = computed(() => {
                if (!props.options) return []
                if (expanded.value) return props.options
                if (visibleCount.value >= props.options.length) return props.options
                return props.options.slice(0, visibleCount.value)
            })

            const calculateVisibleCount = () => {
                // when expanded, skip recalculation so ResizeObserver firing due to layout
                // changes (height growth when wrapped) does not cause state thrashing.
                if (expanded.value) return

                const container = tagsContainerRef.value
                const measureLayer = measureLayerRef.value
                if (!container || !measureLayer) return

                const opts = props.options || []
                if (!opts.length) {
                    visibleCount.value = 0
                    return
                }

                const containerWidth = container.clientWidth
                if (containerWidth <= 0) {
                    // not yet laid out — fallback: show all, will be recalculated later by ResizeObserver
                    visibleCount.value = opts.length
                    return
                }

                const measureNodes = Array.from(
                    measureLayer.querySelectorAll('.measure-tag')
                ).filter(n => !n.classList.contains('measure-more-tag'))
                const moreNode = measureLayer.querySelector('.measure-more-tag')

                if (measureNodes.length !== opts.length) {
                    visibleCount.value = opts.length
                    return
                }

                const widths = measureNodes.map(n => n.getBoundingClientRect().width)
                // if any width is 0, measurement is not ready; fallback and retry later
                if (widths.some(w => w <= 0)) {
                    visibleCount.value = opts.length
                    return
                }

                const moreWidth = moreNode && moreNode.getBoundingClientRect().width > 0
                    ? moreNode.getBoundingClientRect().width
                    : MORE_TAG_RESERVE

                // first try: does everything fit in one row without a "+N" tag?
                let totalWithoutMore = 0
                for (let i = 0; i < widths.length; i++) {
                    totalWithoutMore += widths[i]
                    if (i > 0) totalWithoutMore += TAG_GAP
                }
                if (totalWithoutMore <= containerWidth) {
                    visibleCount.value = opts.length
                    return
                }

                // otherwise, find the largest N such that N tags + gap + "+M" tag fit.
                // Subtract a small safety margin (1px) to absorb sub-pixel rounding from
                // getBoundingClientRect() returning fractional pixels.
                const availableWidth = containerWidth - moreWidth - TAG_GAP - SAFETY_MARGIN
                let used = 0
                let count = 0
                for (let i = 0; i < widths.length; i++) {
                    const nextUsed = count === 0 ? widths[i] : used + TAG_GAP + widths[i]
                    if (nextUsed > availableWidth) break
                    used = nextUsed
                    count += 1
                }
                visibleCount.value = count
            }

            // schedule via nextTick + rAF to ensure bk-tag async rendering has settled
            const scheduleRecalculate = () => {
                nextTick(() => {
                    calculateVisibleCount()
                })
            }

            const handleToggleExpand = () => {
                const next = !expanded.value
                expanded.value = next
                // when collapsing back, re-measure (container width may have changed in the meantime)
                if (!next) {
                    scheduleRecalculate()
                }
            }

            const handleTagClose = (idx) => {
                props.onDelete(idx)
            }

            const handleEditClick = () => {
                props.onEdit()
            }

            // watch options: recalc; reset expanded state so layout re-evaluates cleanly
            watch(
                () => props.options,
                () => {
                    expanded.value = false
                    scheduleRecalculate()
                },
                { deep: true }
            )

            // ResizeObserver for container width
            let resizeObserver = null

            onMounted(() => {
                scheduleRecalculate()
                if (typeof ResizeObserver !== 'undefined' && tagsContainerRef.value) {
                    resizeObserver = new ResizeObserver(() => {
                        calculateVisibleCount()
                    })
                    resizeObserver.observe(tagsContainerRef.value)
                }
            })

            onBeforeUnmount(() => {
                if (resizeObserver) {
                    resizeObserver.disconnect()
                    resizeObserver = null
                }
            })

            return {
                tagsContainerRef,
                measureLayerRef,
                expanded,
                visibleCount,
                hasCollapsed,
                hiddenCount,
                displayedOptions,
                morePlaceholder,
                handleToggleExpand,
                handleTagClose,
                handleEditClick,
                t
            }
        }
    })
</script>

<style lang="scss" scoped>
    .option-tags-cell {
        position: relative;
        display: flex;
        align-items: flex-start;
        gap: 8px;
        width: 100%;
        padding: 6px 0;
        min-height: 0;
    }

    .tags-container {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-wrap: nowrap;
        gap: 4px;
        overflow: hidden;
        

        &.is-expanded {
            flex-wrap: wrap;
            overflow: visible;
            // ensure wrapped content is fully visible and not clipped by max-height from parents
            max-height: none;
            height: auto;
        }

        :deep(.bk-tag) {
            max-width: 120px;
            flex-shrink: 0;
            margin: 0;

            .bk-tag-text {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }

        .more-toggle-tag {
            cursor: pointer;
            user-select: none;

            &:hover {
                :deep(.bk-tag-text) {
                    color: #3A84FF;
                }
            }
        }
    }

    // hidden measurement layer: taken out of flow entirely, placed off-screen so its width is
    // determined only by its own content (no flex constraints from the cell).
    .tags-measure-layer {
        position: fixed;
        top: -9999px;
        left: -9999px;
        visibility: hidden;
        pointer-events: none;
        white-space: nowrap;
        display: inline-block;

        :deep(.bk-tag) {
            max-width: 120px;
            margin: 0 2px 0 0;
            display: inline-flex;
            vertical-align: top;

            .bk-tag-text {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
    }

    .option-edit-link {
        flex-shrink: 0;
        font-size: 12px;
        white-space: nowrap;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: #3A84FF;
        cursor: pointer;
        line-height: 22px;
        align-self: center;

        &:hover {
            color: #699DF4;
        }

        i {
            margin-right: 0;
        }
    }
</style>
