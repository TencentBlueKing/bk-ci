<template>
    <bk-dialog
        :value="value"
        title="批量修改 CPU、内存上限"
        :width="720"
        :mask-close="false"
        @cancel="$emit('close')"
    >
        <p>将修改 {{ eligible.length }} 个</p>
        <details :open="eligible.length <= 8">
            <summary v-if="eligible.length > 8">查看节点</summary>
            <p
                v-for="node in eligible"
                :key="node.nodeHashId"
            >
                {{ label(node) }}
            </p>
        </details>
        <template v-if="skipped.length">
            <details>
                <summary>以下 {{ skipped.length }} 个不修改</summary>
                <p
                    v-for="node in skipped"
                    :key="node.nodeHashId"
                >
                    {{ label(node) }}
                </p>
            </details>
            <p>容器 CPU、内存上限只能在 Linux 构建节点上设置。</p>
        </template>
        <section
            v-for="field in fields"
            :key="field.key"
            style="margin-top: 20px"
        >
            <p>{{ field.name }}上限</p>
            <bk-radio-group
                v-model="modes[field.key]"
                :disabled="saving"
            >
                <bk-radio value="keep">保持不变</bk-radio>
                <bk-radio value="unlimited">不限</bk-radio>
                <bk-radio value="value">指定数值</bk-radio>
            </bk-radio-group>
            <div
                v-if="modes[field.key] === 'value'"
                style="margin-top: 8px; display: flex; gap: 8px"
            >
                <bk-input
                    v-model="values[field.key]"
                    placeholder="例如 4"
                    :disabled="saving"
                />
                <span v-if="field.key === 'cpus'">核，支持小数。</span>
                <template v-else>
                    <bk-select
                        v-model="unit"
                        :clearable="false"
                        :disabled="saving"
                        style="min-width: 64px"
                    >
                        <bk-option
                            id="m"
                            name="m"
                        /><bk-option
                            id="g"
                            name="g"
                        />
                    </bk-select>
                    <span>不能低于 6m。</span>
                </template>
            </div>
            <p
                v-if="errors[field.key]"
                style="color: #ea3636"
            >
                {{ errors[field.key] }}
            </p>
        </section>
        <p
            v-if="errors.empty"
            style="color: #ea3636"
        >
            请至少修改一项
        </p>
        <template slot="footer">
            <bk-button
                theme="primary"
                :loading="saving"
                :disabled="modes.cpus === 'keep' && modes.memory === 'keep'"
                @click="save"
            >
                确认
            </bk-button>
            <bk-button
                :disabled="saving"
                @click="$emit('close')"
            >
                取消
            </bk-button>
        </template>
    </bk-dialog>
</template>
<script>
    import { validateDockerLimits } from '@/utils/dockerConfig'
    import { osOf } from '@/components/devops/environment/import-third-party/constants'
    export default {
        props: {
            value: Boolean,
            nodes: { type: Array, required: true },
            projectId: { type: String, required: true }
        },
        data () {
            return {
                modes: { cpus: 'keep', memory: 'keep' }, values: { cpus: '', memory: '' }, unit: 'g',
                errors: {}, saving: false, fields: [{ key: 'cpus', name: 'CPU' }, { key: 'memory', name: '内存' }]
            }
        },
        computed: {
            eligible () {
                return this.nodes.filter(node => node.nodeType === 'THIRDPARTY' && osOf(node) === 'LINUX')
            },
            skipped () {
                return this.nodes.filter(node => !this.eligible.includes(node))
            }
        },
        watch: {
            value (open) {
                if (open) {
                    this.modes = { cpus: 'keep', memory: 'keep' }
                    this.values = { cpus: '', memory: '' }
                    this.errors = {}
                }
            }
        },
        methods: {
            label (node) {
                const ip = node.ip || node.nodeIp
                return node.displayName && ip ? `${node.displayName}（${ip}）` : node.displayName || ip
            },
            async save () {
                const config = {}
                this.errors = {}
                this.fields.forEach(({ key, name }) => {
                    if (this.modes[key] === 'keep') return
                    config[key] = this.modes[key] === 'unlimited' ? '' : this.values[key]
                    if (this.modes[key] === 'value' && !config[key]) this.errors[key] = `请填写${name}上限`
                    if (key === 'memory' && config[key]) config[key] += this.unit
                })
                this.errors = { ...this.errors, ...validateDockerLimits(config.cpus, config.memory) }
                if (!Object.keys(config).length) this.errors.empty = true
                if (Object.keys(this.errors).length) return
                this.saving = true
                try {
                    await this.$ajax.post(`environment/api/user/environment/dockerConfig/${this.projectId}/nodes/batch`, {
                        nodeIds: this.nodes.map(node => node.nodeHashId), config
                    })
                    this.$bkMessage({ theme: 'success', message: '保存成功' })
                    this.$emit('close')
                } catch (e) {
                    this.$bkMessage({ theme: 'error', message: e.message || String(e) })
                } finally {
                    this.saving = false
                }
            }
        }
    }
</script>
