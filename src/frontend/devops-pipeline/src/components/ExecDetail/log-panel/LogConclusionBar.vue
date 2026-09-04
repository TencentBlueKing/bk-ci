<template>
    <div class="lp-status" :class="`is-${conclusion.tone}`">
        <div class="lp-status-row">
            <div v-if="executeCount > 1" class="lp-exec">
                <select :value="currentExecute" @change="$emit('change-execute', +$event.target.value)">
                    <option v-for="n in executeCount" :key="n" :value="n">第 {{ n }} 次执行</option>
                </select>
            </div>
            <strong class="lp-label">{{ conclusion.label }}</strong>
            <span v-if="conclusion.elapsed" class="lp-meta">耗时 {{ conclusion.elapsed }}</span>
            <template v-if="conclusion.node">
                <span class="lp-vdiv"></span>
                <span class="lp-meta">
                    运行节点
                    <a
                        v-if="conclusion.nodeLink"
                        class="lp-node-link"
                        :href="conclusion.nodeLink"
                        target="_blank"
                        rel="noopener"
                        @click.stop
                    >{{ conclusion.node }}</a>
                    <template v-else>{{ conclusion.node }}</template>
                    <span v-if="conclusion.nodeIp" class="lp-node-ip">（{{ conclusion.nodeIp }}）</span>
                </span>
            </template>
            <template v-if="progress != null">
                <span class="lp-progress">
                    <span class="lp-track"><i :style="{ width: progress + '%' }"></i></span>
                    <em>{{ progress }}%</em>
                    <button
                        v-if="hasSubtasks"
                        type="button"
                        class="lp-expand"
                        @click.stop="$emit('toggle-progress')"
                    >{{ progressExpanded ? '收起' : '展开' }}</button>
                </span>
            </template>
            <span v-if="conclusion.message && !conclusion.locateLog" class="lp-msg">{{ conclusion.message }}</span>
            <template v-if="conclusion.locateLog">
                <span v-if="conclusion.errorCode" class="lp-code">{{ conclusion.errorCode }}</span>
                <span v-if="conclusion.message" class="lp-msg">{{ conclusion.message }}</span>
                <button type="button" class="lp-link-btn" @click="$emit('locate-log')">定位日志</button>
            </template>
            <button
                v-if="conclusion.askAssistant"
                type="button"
                class="lp-link-btn"
                @click="$emit('ask-assistant')"
            >问助手</button>
            <button
                v-if="conclusion.handleAction"
                type="button"
                class="lp-link-btn"
                @click="$emit('handle')"
            >{{ conclusion.handleAction }}</button>
        </div>
        <slot></slot>
    </div>
</template>

<script>
    export default {
        props: {
            conclusion: { type: Object, required: true },
            executeCount: { type: Number, default: 1 },
            currentExecute: { type: Number, default: 1 },
            progress: { type: Number, default: null },
            hasSubtasks: { type: Boolean, default: false },
            progressExpanded: { type: Boolean, default: false }
        }
    }
</script>

<style lang="scss" scoped>
.lp-status { padding: 8px 16px; font-size: 12px; color: #c4c6cc; border-bottom: 1px solid #3a3c45; }
.lp-status-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.lp-label { font-weight: 600; }
.is-success .lp-label { color: #2dcb56; }
.is-failed .lp-label { color: #ea3636; }
.is-running .lp-label { color: #3a84ff; }
.is-canceled .lp-label { color: #979ba5; }
.is-pause .lp-label, .is-waiting .lp-label { color: #ff9c01; }
.lp-meta { color: #979ba5; }
.lp-vdiv { width: 1px; height: 12px; background: #4b4d55; }
.lp-node-link { color: #3a84ff; }
.lp-node-ip { color: #63656e; }
.lp-msg { color: #e6e7ea; }
.lp-code { color: #ea3636; }
.lp-link-btn, .lp-expand {
    border: 0;
    background: none;
    color: #3a84ff;
    cursor: pointer;
}
.lp-progress { display: inline-flex; align-items: center; gap: 6px; }
.lp-track {
    width: 72px;
    height: 4px;
    background: #3a3c45;
    border-radius: 2px;
    overflow: hidden;
    i { display: block; height: 100%; background: #3a84ff; }
}
.lp-exec select {
    background: #2e3342;
    color: #fff;
    border: 1px solid #4b4d55;
}
</style>
