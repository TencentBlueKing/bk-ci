<template>
    <bk-form
        class="import-form"
        :class="{ 'is-locked': !!denyReason }"
        label-width="180"
        :model="form"
    >
        <!-- 后端判定不允许重装：展示原因并锁定表单（生成按钮同步禁用，见父组件） -->
        <p
            v-if="denyReason"
            class="deny-alert"
        >
            <i class="devops-icon icon-exclamation-circle-shape"></i>
            <span>{{ $t('environment.installSession.denyReinstallAlert', { reason: denyReason }) }}</span>
        </p>

        <p class="form-block-title">{{ $t('environment.installSession.basicConfig') }}</p>

        <bk-form-item
            :label="$t('environment.installSession.osLabel')"
            required
            class="form-item-tight"
        >
            <bk-radio-group v-model="form.os">
                <bk-radio
                    value="LINUX"
                    :disabled="isReinstall"
                >
                    Linux
                </bk-radio>
                <bk-radio
                    value="MACOS"
                    :disabled="isReinstall"
                >
                    macOS
                </bk-radio>
                <bk-radio
                    value="WINDOWS"
                    :disabled="isReinstall"
                >
                    Windows
                </bk-radio>
            </bk-radio-group>
        </bk-form-item>

        <bk-form-item
            :label="$t('environment.installSession.zoneLabel')"
            required
            :desc="$t('environment.installSession.zoneDesc')"
        >
            <bk-radio-group v-model="form.zone">
                <bk-radio
                    v-for="g in gatewayList"
                    :key="g.zoneName"
                    :value="g.zoneName"
                    :disabled="isReinstall"
                >
                    {{ g.showName }}
                    <span
                        v-if="g.coverage"
                        class="zone-cover"
                    >（{{ g.coverage }}）</span>
                </bk-radio>
            </bk-radio-group>
        </bk-form-item>

        <bk-form-item
            v-if="isWindows"
            :label="$t('environment.installSession.agentInstallMode')"
            required
        >
            <bk-radio-group v-model="form.installType">
                <bk-radio
                    v-for="item in installTypeList"
                    :key="item.id"
                    :value="item.id"
                >
                    <bk-popover
                        placement="top"
                        :max-width="360"
                        ext-cls="import-form-tip"
                    >
                        <span class="install-type-label">{{ $t(item.label) }}</span>
                        <template #content>{{ $t(item.tips) }}</template>
                    </bk-popover>
                </bk-radio>
            </bk-radio-group>
        </bk-form-item>

        <p class="form-block-title">
            {{ $t('environment.installSession.nodeConfig') }}
            <bk-popover
                placement="top"
                :max-width="320"
                ext-cls="import-form-tip"
            >
                <i class="devops-icon icon-info-circle hint-icon"></i>
                <template #content>
                    {{ isReinstall ? $t('environment.installSession.nodeConfigTipReinstall') : $t('environment.installSession.nodeConfigTipImport') }}
                </template>
            </bk-popover>
        </p>

        <bk-form-item :label="$t('environment.installSession.maxParallel')">
            <bk-input
                v-model="form.parallelTaskCount"
                class="num-input"
                type="number"
                :min="0"
                :max="100"
                :precision="0"
                :placeholder="$t('environment.installSession.parallelPlaceholder', { n: defaults.parallelTaskCount })"
            />
        </bk-form-item>
        <bk-form-item
            v-if="dockerSupported"
            :label="$t('environment.installSession.dockerMaxParallel')"
            :desc="$t('environment.installSession.dockerMaxParallelDesc')"
        >
            <bk-input
                v-model="form.dockerParallelTaskCount"
                class="num-input"
                type="number"
                :min="0"
                :max="100"
                :precision="0"
                :placeholder="$t('environment.installSession.parallelPlaceholder', { n: defaults.dockerParallelTaskCount })"
            />
        </bk-form-item>
    
        <bk-form-item
            :label="$t('environment.installSession.tagsLabel')"
            :desc="$t('environment.installSession.tagsDesc')"
        >
            <div
                ref="tagRows"
                class="tag-rows"
            >
                <div
                    v-for="(row, idx) in form.tags"
                    :key="idx"
                    class="tag-row"
                >
                    <bk-select
                        v-model="row.tagKeyId"
                        class="tag-select"
                        :placeholder="$t('environment.installSession.tagKeyPlaceholder')"
                        searchable
                        :clearable="false"
                        @change="onTagKeyChange(row)"
                    >
                        <bk-option
                            v-for="k in selectableTags"
                            :key="k.tagKeyId"
                            :id="k.tagKeyId"
                            :name="k.tagKeyName"
                            :disabled="usedKeys(idx).includes(k.tagKeyId)"
                        />
                    </bk-select>
                    <bk-select
                        v-model="row.tagValueId"
                        class="tag-select"
                        :placeholder="$t('environment.installSession.tagValuePlaceholder')"
                        searchable
                        :clearable="false"
                    >
                        <bk-option
                            v-for="v in valuesOf(row.tagKeyId)"
                            :key="v.tagValueId"
                            :id="v.tagValueId"
                            :name="v.tagValueName"
                        />
                    </bk-select>
                    <!-- 行尾加减号：加 = 末尾追加空行；减 = 删本行（仅一行时禁用） -->
                    <bk-button
                        text
                        class="tag-action"
                        :title="$t('environment.installSession.addTagRow')"
                        @click="addTag"
                    >
                        <i class="devops-icon icon-plus-circle tag-action-icon"></i>
                    </bk-button>
                    <bk-button
                        text
                        class="tag-action is-remove"
                        :class="{ 'is-disabled': form.tags.length <= 1 }"
                        :disabled="form.tags.length <= 1"
                        :title="$t('environment.installSession.removeTagRow')"
                        @click="removeTag(idx)"
                    >
                        <i class="devops-icon icon-minus-circle tag-action-icon"></i>
                    </bk-button>
                </div>
            </div>

            <EnvPreview
                v-if="showEnvPreview"
                :preview="envPreview"
                :loading="envPreviewLoading"
            />
        </bk-form-item>
    </bk-form>
</template>

<script>
    import EnvPreview from './EnvPreview.vue'
    export default {
        name: 'ImportConfigForm',
        components: {
            EnvPreview
        },
        props: {
            /** 表单状态（reactive 对象，父组件持有） */
            form: {
                type: Object,
                required: true
            },
            /** 重装 Agent 模式：OS / 地区锁定 */
            isReinstall: {
                type: Boolean,
                default: false
            },
            isWindows: {
                type: Boolean,
                default: false
            },
            /** 当前 OS 是否支持 Docker 并发配置 */
            dockerSupported: {
                type: Boolean,
                default: false
            },
            /** 默认并发数（占位提示用） */
            defaults: {
                type: Object,
                default: () => ({})
            },
            /** 网关（接入点）列表 */
            gatewayList: {
                type: Array,
                default: () => []
            },
            /** Windows 安装模式选项 */
            installTypeList: {
                type: Array,
                default: () => []
            },
            /** 项目自定义标签（canUpdate === 'TRUE'） */
            customTags: {
                type: Array,
                default: () => []
            },
            /** 标签 → 动态环境预览数据 */
            envPreview: {
                type: Object,
                default: null
            },
            /** 是否展示环境预览 */
            showEnvPreview: {
                type: Boolean,
                default: false
            },
            /** 环境预览加载中（防抖等待 + 请求返回前） */
            envPreviewLoading: {
                type: Boolean,
                default: false
            },
            /** 重装被后端拒绝的原因（reinstallContext.canReinstall=false）；非空时表单锁定 */
            denyReason: {
                type: String,
                default: ''
            }
        },
        computed: {
            /** 可选项：剔除系统内置标签（os/arch，负 tagKeyId 或 canUpdate=INTERNAL），用户不可选、不展示 */
            selectableTags () {
                return this.customTags.filter((t) => {
                    const id = Number(t.tagKeyId)
                    return !Number.isNaN(id) && id > 0 && t.canUpdate !== 'INTERNAL'
                })
            }
        },
        methods: {
            valuesOf (keyId) {
                return this.customTags.find((t) => t.tagKeyId === keyId)?.tagValues || []
            },
            /**
             * 切换标签键：仅当原值不属于新键时才清空。
             * 不能无条件清空——重装回显时键由空变为有效值也会触发 change，会把已回填的值抹掉。
             */
            onTagKeyChange (row) {
                if (!this.valuesOf(row.tagKeyId).some((v) => v.tagValueId === row.tagValueId)) {
                    row.tagValueId = ''
                }
            },
            /** 其余行已占用的标签键，用于禁用重复选择 */
            usedKeys (idx) {
                return this.form.tags.map((r, i) => (i === idx ? '' : r.tagKeyId)).filter(Boolean)
            },
            addTag () {
                this.form.tags.push({ tagKeyId: '', tagValueId: '' })
                this.$nextTick(() => {
                    const el = this.$refs.tagRows
                    if (el) el.scrollTop = el.scrollHeight
                })
            },
            removeTag (idx) {
                if (this.form.tags.length <= 1) return
                this.form.tags.splice(idx, 1)
            },
        }
    }
</script>

<style lang="scss" scoped>
    /* 重装被拒警示条：浅黄底与「另有构建机尚未接入」的次级提示同风格 */
    .deny-alert {
        display: flex;
        align-items: center;
        gap: 6px;
        margin: 0 0 16px;
        padding: 6px 12px;
        background: #fff8e6;
        border-radius: 2px;
        font-size: 12px;
        line-height: 20px;
        color: #63656e;

        .devops-icon {
            flex: none;
            font-size: 14px;
            color: #ff9c01;
        }
    }
    /* 锁定态：整体不可交互、降透明度（生成按钮在父组件页脚同步禁用） */
    .import-form.is-locked {
        pointer-events: none;
        opacity: 0.6;
    }
    /* 分组标题栏：对齐 Figma Collapse（#f0f1f5 通栏，高 32，字 14/22 Regular） */
    .form-block-title {
        display: flex;
        align-items: center;
        gap: 8px;
        box-sizing: border-box;
        height: 32px;
        margin: 16px 0;
        padding: 0 16px;
        border-radius: 2px;
        background: #f0f1f5;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        color: #313238;
    }
    .import-form > .form-block-title:first-child {
        margin-top: 0;
    }
    /* 仅操作系统 → 地区：两行 radio 间距 12px；其余表单行仍用组件库默认 24px */
    .import-form ::v-deep .form-item-tight {
        margin-bottom: 12px;
    }
    /* 表单字段：label 与值统一 12 号字（含只读文本、radio、选择器；input 默认已是 12） */
    .import-form ::v-deep .bk-form-label,
    .import-form ::v-deep .bk-form-content {
        font-size: 12px;
    }
    .import-form ::v-deep .bk-radio-label,
    .import-form ::v-deep .bk-select,
    .import-form ::v-deep .bk-select-trigger,
    .import-form ::v-deep .bk-select-tag,
    .import-form ::v-deep .bk-select-name,
    .import-form ::v-deep .bk-select-placeholder {
        font-size: 12px;
    }
    .import-form ::v-deep .bk-form-radio {
        margin-right: 16px;
    }
    /* radio 上挂 popover 的说明文字：对齐 form-item desc 的虚线下划线观感 */
    .install-type-label {
        border-bottom: 1px dashed #979ba5;
        cursor: default;
    }
    /* 区块标题旁的 ⓘ（非 form-item，desc 用不了） */
    .hint-icon {
        flex: none;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;
    }
    .num-input {
        width: 240px;
    }
    .tag-rows {
        display: flex;
        flex-direction: column;
        gap: 8px;
        align-items: flex-start;
        /* 标签行（单行 32 + 间距 8 = 40）超过 4 行时内部滚动，避免弹窗被无限撑高 */
        max-height: 152px;
        padding-right: 4px;
        overflow-y: auto;
    }
    /* 窗口高度大于 1100px 时，标签区固定为 230px，避免大屏下标签区过高 */
    @media (min-height: 1101px) {
        .tag-rows {
            max-height: 280px;
        }
    }
    .tag-row {
        display: flex;
        align-items: center;
        gap: 8px;
    }
    .tag-select {
        width: 240px;
    }
    .tag-action {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex: none;
        min-width: auto;
        padding: 0;
        height: auto;
        color: #979ba5;

        &:hover:not(.is-disabled):not(:disabled) {
            color: #3a84ff;
        }
        &.is-remove:hover:not(.is-disabled):not(:disabled) {
            color: #ea3636;
        }
        &.is-disabled,
        &:disabled {
            color: #c4c6cc;
            cursor: not-allowed;
        }
    }
    .tag-action-icon {
        font-size: 16px;
    }
    .zone-cover {
        margin-left: 4px;
        font-size: 12px;
        color: #979ba5;
    }
    .empty-hint {
        margin: 0;
        font-size: 12px;
        color: #63656e;
        line-height: 20px;
    }
</style>
