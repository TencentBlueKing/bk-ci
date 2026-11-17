<template>
    <div class="job-queue-detail">
        <p v-if="queue.group">{{ $t('logPanel.queue.mutexGroup') }}：{{ queue.group }}</p>
        <div
            class="queue-lanes"
            :class="{ 'has-running': queue.running.length }"
        >
            <section
                v-for="section in queueSections"
                :key="section.kind"
                :class="['queue-section', `${section.kind}-section`]"
            >
                <div class="queue-section-title">
                    <i :class="`is-${section.kind}`"></i>{{ $t(section.titleKey, [section.count]) }}
                </div>
                <div class="queue-items">
                    <div
                        v-for="(item, index) in section.visible"
                        :key="`${section.kind}-${item.buildNum}-${index}`"
                        class="queue-item"
                    >
                        <span class="queue-build-tag">#{{ item.buildNum }}</span>
                        <span class="queue-item-info">
                            <strong v-if="item.detailVisible">{{ item.title }}</strong>
                            <small>{{ [item.detailVisible && item.owner, item.elapsedMinutes && $t(section.durationKey, [item.elapsedMinutes])].filter(Boolean).join(' · ') }}</small>
                        </span>
                        <span
                            v-if="item.showStatus"
                            class="queue-item-status"
                        >{{ statusLabel(item.status) }}</span>
                        <a
                            v-if="item.linkUrl"
                            class="queue-view"
                            :href="item.linkUrl"
                            target="_blank"
                            rel="noopener"
                        >{{ $t('logPanel.queue.view') }}</a>
                    </div>
                    <bk-popover
                        v-if="section.hasOverflow"
                        class="queue-more-popover"
                        trigger="click"
                        theme="log-panel-queue-menu"
                        placement="bottom-end"
                        :arrow="false"
                        :tippy-options="popoverOptions"
                    >
                        <button
                            type="button"
                            class="queue-more"
                            :aria-label="$t('logPanel.queue.more')"
                        >
                            …
                        </button>
                        <div
                            slot="content"
                            class="queue-overflow"
                        >
                            <strong class="queue-overflow-title">{{ $t(section.titleKey, [section.count]) }}</strong>
                            <div
                                v-for="(item, index) in section.all"
                                :key="`overflow-${section.kind}-${item.buildNum}-${index}`"
                                class="queue-overflow-item"
                            >
                                <span class="queue-build-tag">#{{ item.buildNum }}</span>
                                <span class="queue-item-info">
                                    <strong v-if="item.detailVisible">{{ item.title }}</strong>
                                    <small>{{ [item.detailVisible && item.owner, item.elapsedMinutes && $t(section.durationKey, [item.elapsedMinutes])].filter(Boolean).join(' · ') }}</small>
                                </span>
                                <span
                                    v-if="item.showStatus"
                                    class="queue-item-status"
                                >{{ statusLabel(item.status) }}</span>
                                <a
                                    v-if="item.linkUrl"
                                    class="queue-view"
                                    :href="item.linkUrl"
                                    target="_blank"
                                    rel="noopener"
                                >{{ $t('logPanel.queue.view') }}</a>
                            </div>
                            <small
                                v-if="section.truncated"
                                class="queue-truncated"
                            >{{ $t('logPanel.queue.aheadTruncated', [section.all.length]) }}</small>
                        </div>
                    </bk-popover>
                </div>
            </section>
        </div>
    </div>
</template>

<script>
    import { resolveQueueCardStatusLabel, splitQueueItems } from './logPanelAdapter'

    export default {
        name: 'JobQueueDetail',
        props: { queue: { type: Object, required: true } },
        computed: {
            queueSections () {
                const running = Array.isArray(this.queue.running) ? this.queue.running : []
                const ahead = Array.isArray(this.queue.ahead) ? this.queue.ahead : []
                const aheadCount = Number(this.queue.aheadCount) || 0
                const runningItems = splitQueueItems(running, 1)
                const aheadItems = splitQueueItems(ahead, 3)
                return [
                    {
                        kind: 'running',
                        titleKey: 'logPanel.queue.occupied',
                        durationKey: 'logPanel.queue.elapsedMinutes',
                        count: running.length,
                        all: running,
                        ...runningItems,
                        hasOverflow: runningItems.overflow.length > 0,
                        truncated: false
                    },
                    {
                        kind: 'queued',
                        titleKey: 'logPanel.queue.ahead',
                        durationKey: 'logPanel.queue.waitedMinutes',
                        count: Math.max(aheadCount, ahead.length),
                        all: ahead,
                        ...aheadItems,
                        hasOverflow: aheadItems.overflow.length > 0 || aheadCount > ahead.length,
                        truncated: aheadCount > ahead.length
                    }
                ].filter(section => section.count > 0 || section.all.length > 0)
            },
            popoverOptions () {
                return { interactive: true, offset: '0, 4', distance: 0 }
            }
        },
        methods: {
            statusLabel (status) {
                return resolveQueueCardStatusLabel(status, key => this.$t(key))
            }
        }
    }
</script>

<style lang="scss" scoped>
    .job-queue-detail { display: flex; flex-direction: column; gap: 16px; padding: 12px 16px; color: #83828c; background: #2b2722; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .job-queue-detail p { margin: 0; }
    .queue-lanes { display: grid; grid-template-columns: minmax(0, 1fr); align-items: start; gap: 16px; min-width: 0; }
    .queue-lanes.has-running { grid-template-columns: minmax(0, 1fr) minmax(0, 3fr); }
    .queue-section { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
    .queue-section-title { display: inline-flex; align-items: center; gap: 6px; color: #c4c6cc; font: 12px/normal "Microsoft YaHei", sans-serif; }
    .queue-section-title i { flex: 0 0 6px; width: 6px; height: 6px; border-radius: 50%; background: #f0aa50; }
    .queue-section-title i.is-running { background: #5ac882; }
    .queue-items { display: flex; gap: 8px; min-width: 0; }
    .queue-item { display: flex; flex: 1 1 0; align-items: center; gap: 12px; min-width: 0; height: 58px; box-sizing: border-box; padding: 10px 12px; border-radius: 4px; }
    .running-section .queue-item { background: #2f3a2f; }
    .queued-section .queue-item { background: #36343a; }
    .running-section .queue-items { max-width: 280px; }
    .queue-build-tag { display: inline-flex; flex: 0 0 auto; align-items: center; height: 18px; padding: 0; color: #83828c; font: 12px/18px "Microsoft YaHei", sans-serif; white-space: nowrap; }
    .queue-item-info { display: flex; flex: 1; flex-direction: column; min-width: 0; gap: 0; font: 12px/18px "Microsoft YaHei", sans-serif; }
    .queue-item-info strong { overflow: hidden; color: #f0f1f5; font-weight: normal; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }
    .queue-item-info small { color: #979ba5; font: 12px/18px "Microsoft YaHei", sans-serif; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .queue-item-status { flex: 0 0 auto; color: #c4c6cc; white-space: nowrap; }
    .queue-view { flex: 0 0 auto; padding: 0; color: #63656e; font: inherit; text-decoration: none; }
    a.queue-view { color: #699df4; cursor: pointer; }
    .queue-more-popover { display: inline-flex; flex: 0 0 auto; align-items: center; }
    .queue-more { width: 36px; height: 58px; padding: 0; color: #c4c6cc; background: #36343a; border: 0; border-radius: 4px; font: 16px/58px "Microsoft YaHei", sans-serif; cursor: pointer; }
    .queue-more:hover { color: #fff; background: #414049; }
</style>

<style lang="scss">
    .tippy-tooltip.log-panel-queue-menu-theme {
        padding: 0;
        color: #63656e;
        background: #fff;
        border: 1px solid #dcdee5;
        box-shadow: 0 2px 8px rgba(0, 0, 0, .16);
    }
    .queue-overflow {
        box-sizing: border-box;
        width: 360px;
        max-height: 320px;
        overflow-y: auto;
        padding: 12px;
        background: #fff;
        font: 12px/20px "Microsoft YaHei", sans-serif;
    }
    .queue-overflow-title { display: block; margin-bottom: 8px; color: #313238; font-weight: normal; }
    .queue-overflow-item { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 6px 0; color: #63656e; }
    .queue-overflow-item .queue-item-info { color: #63656e; }
    .queue-overflow-item .queue-item-info strong { color: #313238; }
    .queue-overflow-item .queue-item-info small { color: #979ba5; }
    .queue-truncated { display: block; padding-top: 8px; color: #979ba5; border-top: 1px solid #eaebf0; }
</style>
