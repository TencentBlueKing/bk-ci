<template>
    <details
        class="docker-config"
        open
    >
        <summary>
            Docker 构建
            <span class="summary-value">{{ summary }}</span>
        </summary>
        <div class="scope-tip">
            <p>仅当 Job 启用 Docker 运行构建任务时生效。在构建机上直接启动的任务不受影响。</p>
            <p v-if="!creationNode">
                Job 填写了上限，以 Job 为准；选择构建环境且未填写上限时，以环境为准；
                直接选择节点且未填写上限时，以{{ scope === 'NODE' ? '本节点' : '节点' }}为准。
            </p>
        </div>
        <div
            v-if="scope === 'NODE'"
            class="config-row"
        >
            <span
                class="label"
                v-bk-tooltips="'节点上需要已经安装所选引擎。未安装时，容器任务失败。'"
            >容器引擎</span>
            <bk-radio-group
                v-if="editing"
                v-model="form.containerRuntime"
                :disabled="saving"
            >
                <bk-radio value="docker">Docker</bk-radio>
                <bk-radio value="podman">Podman</bk-radio>
            </bk-radio-group>
            <span v-else>{{ config.containerRuntime === 'podman' ? 'Podman' : 'Docker' }}</span>
        </div>
        <template v-if="!creationNode">
            <div class="config-row">
                <span class="label">CPU {{ scope === 'NODE' ? '默认' : '' }}上限</span>
                <template v-if="editing">
                    <bk-input
                        v-model="form.cpus"
                        placeholder="例如 4"
                        :disabled="saving"
                    /> 核
                    <span>支持小数，例如 0.5、4。</span>
                    <span class="error">{{ errors.cpus }}</span>
                </template>
                <span v-else>{{ config.cpus ? config.cpus + ' 核' : '不限' }}</span>
            </div>
            <div class="config-row">
                <span class="label">内存{{ scope === 'NODE' ? '默认' : '' }}上限</span>
                <template v-if="editing">
                    <bk-input
                        v-model="form.memoryValue"
                        placeholder="例如 4"
                        :disabled="saving"
                    />
                    <bk-select
                        v-model="form.memoryUnit"
                        :clearable="false"
                        :disabled="saving"
                        style="width: 64px"
                    >
                        <bk-option
                            id="m"
                            name="m"
                        /><bk-option
                            id="g"
                            name="g"
                        />
                    </bk-select>
                    <span>支持 m、g，例如 512m、4g，不能低于 6m。</span>
                    <span class="error">{{ errors.memory }}</span>
                </template>
                <span v-else>{{ config.memory ? config.memory.replace(/([mg])$/, ' $1') : '不限' }}</span>
            </div>
        </template>
        <div class="config-row">
            <bk-button
                v-if="!editing"
                text
                :disabled="loading"
                v-perm="permission"
                @click="edit"
            >
                编辑
            </bk-button>
            <template v-else>
                <bk-button
                    theme="primary"
                    :loading="saving"
                    @click="save"
                >
                    确认
                </bk-button>
                <bk-button
                    :disabled="saving"
                    @click="editing = false"
                >
                    取消
                </bk-button>
            </template>
        </div>
        <slot />
    </details>
</template>
<script>
    import { validateDockerLimits } from '@/utils/dockerConfig'
    export default {
        props: {
            scope: { type: String, required: true },
            resourceId: { type: String, required: true },
            projectId: { type: String, required: true },
            creationNode: { type: Boolean, default: false }
        },
        data () {
            return { config: {}, form: {}, editing: false, saving: false, loading: true, errors: {} }
        },
        computed: {
            url () {
                return `environment/api/user/environment/dockerConfig/${this.projectId}/${this.scope}/${this.resourceId}`
            },
            summary () {
                const engine = this.config.containerRuntime === 'podman' ? 'Podman' : 'Docker'
                const limits = `CPU ${this.config.cpus || '不限'} / 内存 ${this.config.memory || '不限'}`
                return this.scope === 'ENV' ? limits : [engine, ...(!this.creationNode ? [limits] : [])].join(' / ')
            },
            permission () {
                const resourceType = this.scope === 'ENV' ? 'environment'
                    : this.creationNode ? 'creative_stream_node' : 'env_node'
                return { permissionData: {
                    projectId: this.projectId, resourceType, resourceCode: this.resourceId, action: `${resourceType}_edit`
                } }
            }
        },
        watch: { url: { immediate: true, handler: 'load' } },
        methods: {
            async load () {
                const url = this.url
                this.loading = true
                this.editing = false
                try {
                    const value = await this.$ajax.get(url)
                    if (url === this.url) this.config = value || {}
                } catch (e) {
                    this.$bkMessage({ theme: 'error', message: e.message || String(e) })
                } finally {
                    if (url === this.url) this.loading = false
                }
            },
            edit () {
                const memory = this.config.memory || ''
                this.form = {
                    cpus: this.config.cpus || '', containerRuntime: this.config.containerRuntime || 'docker',
                    memoryValue: memory.slice(0, -1), memoryUnit: memory.slice(-1) || 'g'
                }
                this.errors = {}
                this.editing = true
            },
            async save () {
                const memory = this.form.memoryValue ? this.form.memoryValue + this.form.memoryUnit : ''
                this.errors = validateDockerLimits(this.form.cpus, memory)
                if (Object.keys(this.errors).length) return
                const payload = this.creationNode ? {} : { cpus: this.form.cpus, memory }
                if (this.scope === 'NODE') payload.containerRuntime = this.form.containerRuntime
                this.saving = true
                try {
                    await this.$ajax.post(this.url, payload)
                    this.$bkMessage({ theme: 'success', message: '保存成功' })
                    await this.load()
                } catch (e) {
                    this.$bkMessage({ theme: 'error', message: e.message || String(e) })
                } finally {
                    this.saving = false
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
.docker-config {
    grid-column: 1 / -1;
    margin: 16px 0;
    padding: 16px;
    background: #fafbfd;
    summary { cursor: pointer; font-weight: 600; }
    .summary-value { margin-left: 24px; font-weight: normal; color: #63656e; }
    .scope-tip { color: #979ba5; line-height: 24px; margin: 12px 0; }
    .config-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin: 12px 0; }
    .label { width: 120px; text-decoration: underline dotted; }
    .bk-form-control { width: 160px; }
    .error { color: #ea3636; }
}
</style>
