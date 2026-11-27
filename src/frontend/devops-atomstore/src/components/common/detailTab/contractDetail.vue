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
                <span class="meta-item">
                    {{ $t('store.覆盖程度') }}：{{ coverageText }}
                </span>
                <bk-tag
                    v-if="desc.staleFlag"
                    theme="warning"
                >
                    {{ $t('store.已过期') }}
                </bk-tag>
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

    const COVERAGE_MAP = {
        VERIFIED: '已人工核验',
        PARTIALLY_VERIFIED: '部分核验',
        AUTO_EXTRACTED: '自动抽取'
    }

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
                return COVERAGE_MAP[coverage] || coverage || '--'
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
            gap: 16px;
            padding: 12px 0 16px;
            color: #63656e;
            font-size: 12px;
        }

        .g-empty {
            padding: 60px 0;
            text-align: center;
            color: #979ba5;
        }
    }
</style>
