<template>
    <section
        v-bkloading="{ isLoading }"
        class="contract-detail"
    >
        <template v-if="desc">
            <!-- 说明来源提示：统一一句，不区分状态 -->
            <div class="source-banner">
                <i class="bk-icon icon-magic-box banner-icon"></i>
                <div class="banner-content">
                    <p class="banner-main">{{ bannerMain }}</p>
                    <p class="banner-sub">
                        {{ $t('store.可能与实际行为存在差异，请以插件配置页为准。') }}
                    </p>
                </div>
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
                v-model="renderedDescription"
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
            // 统一提示条：不区分 coverage / stale，只说清来源和免责
            bannerMain () {
                const version = (this.desc && this.desc.basedOnVersion) || '--'
                return this.$t('store.本说明由AI分析自动生成', [version])
            },

            // mavon-editor 不渲染原始 HTML，把 <details><summary> 折叠块降级成普通小标题，
            // 否则页面上会原样显示 "<summary>附录：其他配置</summary>"
            renderedDescription () {
                const raw = (this.desc && this.desc.description) || ''
                return raw
                    // <details><summary>标题</summary> → #### 标题（前后补空行，避免和正文粘连）
                    .replace(
                        /<details[^>]*>\s*<summary>([\s\S]*?)<\/summary>/gi,
                        (match, title) => `\n\n#### ${String(title).trim()}\n\n`
                    )
                    // 其余残留的 details/summary 标签直接去掉
                    .replace(/<\/?details[^>]*>/gi, '')
                    .replace(/<\/?summary[^>]*>/gi, '')
                    // 清掉降级后可能出现的连续空行
                    .replace(/\n{3,}/g, '\n\n')
                    .trim()
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

        .source-banner {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            margin: 8px 0 20px;
            padding: 12px 16px;
            border-radius: 2px;
            border-left: 3px solid #3a84ff;
            background: #f0f5ff;
            font-size: 13px;
            line-height: 20px;
        }

        .banner-icon {
            margin-top: 3px;
            font-size: 16px;
            flex-shrink: 0;
            color: #3a84ff;
        }

        .banner-content {
            flex: 1;
            min-width: 0;
        }

        .banner-main {
            color: #313238;
            margin: 0;
        }

        .banner-sub {
            margin: 4px 0 0;
            color: #63656e;
        }

        .g-empty {
            padding: 60px 0;
            text-align: center;
            color: #979ba5;
        }
    }
</style>
