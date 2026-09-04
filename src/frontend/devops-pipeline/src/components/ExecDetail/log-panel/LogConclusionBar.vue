<template>
    <div class="lp-status" :class="`is-${conclusion.tone}`">
        <div class="lp-status-row">
            <div v-if="executeCount > 1" class="lp-exec-wrap">
                <button type="button" class="lp-exec-trigger" @click.stop="execOpen = !execOpen">
                    <span>第 {{ currentExecute }} 次执行</span>
                    <i class="devops-icon icon-angle-down"></i>
                </button>
                <ul v-if="execOpen" class="lp-exec-menu">
                    <li
                        v-for="n in executeCount"
                        :key="n"
                        :class="{ active: n === currentExecute }"
                        @click.stop="pickExecute(n)"
                    >第 {{ n }} 次执行</li>
                </ul>
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
            <span v-if="conclusion.locateLog" class="lp-error-detail">
                <span v-if="conclusion.errorCode" class="lp-code">{{ conclusion.errorCode }}</span>
                <span v-if="conclusion.message" class="lp-msg">{{ conclusion.message }}</span>
                <button type="button" class="lp-link-btn" @click="$emit('locate-log')">定位日志</button>
            </span>
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
        },
        data () {
            return { execOpen: false }
        },
        mounted () {
            document.addEventListener('click', this.onDocClick, true)
        },
        beforeDestroy () {
            document.removeEventListener('click', this.onDocClick, true)
        },
        methods: {
            pickExecute (n) {
                this.execOpen = false
                if (n !== this.currentExecute) this.$emit('change-execute', n)
            },
            onDocClick (e) {
                if (!this.execOpen) return
                if (this.$el && !this.$el.contains(e.target)) this.execOpen = false
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-status {
    flex-shrink: 0;
    background: #242a36;
    border-top: 1px solid #151c28;
    color: #83828c;
    font-size: 12px;
    line-height: 20px;
}
.lp-status.is-success { background: #2b3329; }
.lp-status.is-failed { background: #3d2929; }
.lp-status.is-canceled,
.lp-status.is-pause,
.lp-status.is-waiting { background: #3b342b; }
.lp-status-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    min-height: 35px;
    padding: 7px 24px 7px 16px;
    box-sizing: border-box;
}
.lp-label { font-weight: 700; white-space: nowrap; }
.is-running .lp-label { color: #699df4; }
.is-success .lp-label { color: #45e35f; }
.is-failed .lp-label,
.is-failed .lp-msg { color: #f06e6e; }
.is-canceled .lp-label,
.is-pause .lp-label,
.is-waiting .lp-label { color: #f0aa50; }
.lp-meta { color: #83828c; white-space: nowrap; }
.lp-vdiv { width: 1px; height: 12px; background: #4d4f56; flex-shrink: 0; }
.lp-node-link {
    color: #83828c;
    text-decoration: none;
    border-bottom: 1px dashed #63656e;
    &:hover { color: #699df4; border-bottom-color: #699df4; }
}
.lp-node-ip { color: #63656e; }
.lp-msg { min-width: 0; word-break: break-word; }
.lp-error-detail {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    min-width: min(100%, max-content);
    max-width: 100%;
}
.lp-code {
    height: 16px;
    padding: 0 6px;
    border-radius: 2px;
    background: #503030;
    color: #f06e6e;
    font-size: 10px;
    line-height: 16px;
}
.lp-link-btn, .lp-expand {
    border: 0;
    background: none;
    color: #699df4;
    cursor: pointer;
    padding: 0;
    font-size: 12px;
}
.lp-progress { display: inline-flex; align-items: center; gap: 8px; }
.lp-track {
    width: 120px;
    height: 4px;
    background: #4d4f56;
    border-radius: 2px;
    overflow: hidden;
    i { display: block; height: 100%; background: #3a84ff; }
}
.lp-progress em { font-style: normal; color: #83828c; }
.lp-exec-wrap { position: relative; width: 112px; height: 24px; flex-shrink: 0; }
.lp-exec-trigger {
    display: inline-flex;
    align-items: center;
    width: 112px;
    height: 24px;
    padding: 0 24px 0 8px;
    background: #464953;
    border: none;
    border-radius: 4px;
    color: #c4c6cc;
    font-size: 12px;
    cursor: pointer;
    text-align: left;
    i {
        position: absolute;
        right: 8px;
        color: #979ba5;
        font-size: 12px;
    }
}
.lp-exec-menu {
    position: absolute;
    left: 0;
    top: calc(100% + 4px);
    z-index: 20;
    margin: 0;
    padding: 4px 0;
    min-width: 112px;
    list-style: none;
    background: #2c2d34;
    border: 1px solid #4d4f56;
    border-radius: 2px;
    li {
        padding: 0 12px;
        height: 28px;
        line-height: 28px;
        color: #c4c6cc;
        cursor: pointer;
        &:hover { background: #3a3f4b; color: #fff; }
        &.active { color: #3a84ff; }
    }
}
</style>
