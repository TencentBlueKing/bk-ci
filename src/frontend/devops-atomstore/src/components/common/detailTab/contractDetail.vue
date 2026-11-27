<template>
    <section
        v-bkloading="{ isLoading }"
        class="contract-detail"
    >
        <template v-if="desc">
            <div class="meta-bar">
                <span class="meta-item">
                    {{ $t('store.基于版本') }}：{{ desc.basedOnVersion || '--' }}
                </span>
                <span class="meta-item source-item">
                    <i class="bk-icon icon-magic-box source-icon"></i>
                    {{ coverageText }}
                </span>
                <bk-tag
                    v-if="desc.staleFlag"
                    theme="warning"
                >
                    {{ $t('store.说明可能已过时') }}
                </bk-tag>
                <span
                    v-if="desc.staleFlag"
                    class="meta-hint"
                >
                    {{ $t('store.插件已发布新版本，说明尚未更新') }}
                </span>
            </div>
            <mavon-editor
                :editable="false"
                default-open="preview"
                :subfield="false"
                :toolbars-flag="false"
                :box-shadow="false"
                :external-link="false"
                :language="mavenLang"
                preview-background="#fff"
                v-model="desc.description"
            />
        </template>
        <p
            v-else-if="!isLoading"
            class="g-empty"
        >
            {{ $t('store.暂无使用说明') }}
        </p>
    </section>
</template>

<script>
    import api from '@/api'

    export default {
        props: {
            storeType: {
                type: String,
                default: 'atom'
            },
            code: {
                type: String,
                default: ''
            }
        },

        data () {
            return {
                isLoading: false,
                desc: null,
                mavenLang: 'zh-CN'
            }
        },

        computed: {
            coverageText () {
                const coverage = this.desc && this.desc.coverage
                const map = {
                    VERIFIED: this.$t('store.已人工核验'),
                    PARTIALLY_VERIFIED: this.$t('store.由AI生成部分人工核验'),
                    AUTO_EXTRACTED: this.$t('store.由AI生成')
                }
                return map[coverage] || this.$t('store.由AI生成')
            }
        },

        watch: {
            code: {
                immediate: true,
                handler (val) {
                    if (val) this.fetchDescription()
                }
            }
        },

        methods: {
            async fetchDescription () {
                this.isLoading = true
                try {
                    // $ajax 已解包 data，返回即为 StoreComponentContractDescription|null
                    this.desc = await api.requestStoreComponentDescription(this.storeType, this.code) || null
                } catch (err) {
                    // 无说明或接口不可用时静默降级，避免详情页整体报错
                    this.desc = null
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .contract-detail {
        min-height: 200px;

        .meta-bar {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 16px;
            padding: 12px 0 16px;
            color: #63656e;
            font-size: 12px;
        }

        .source-item {
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        .source-icon {
            font-size: 14px;
            color: #3a84ff;
        }

        .meta-hint {
            color: #979ba5;
        }

        .g-empty {
            padding: 60px 0;
            text-align: center;
            color: #979ba5;
        }
    }
</style>
