<template>
    <bk-dialog
        :value="visible"
        ext-cls="devops-convention-dialog"
        :width="800"
        :title="$t('convention.title')"
        :close-icon="false"
        :esc-close="false"
        :quick-close="false"
        :mask-close="false"
        :show-footer="true"
        header-position="left"
        @cancel="keepOpen"
        @value-change="onValueChange"
    >
        <div class="sign-body">
            <div
                class="convention-html"
                v-bk-xss-html="content"
            />
        </div>
        <div
            slot="footer"
            class="sign-foot"
        >
            <bk-checkbox v-model="agreed">
                {{ $t('convention.agree') }}
            </bk-checkbox>
            <div class="sign-actions">
                <span
                    class="agree-wrap"
                    v-bk-tooltips="agreed ? { disabled: true } : { content: $t('convention.tip') }"
                >
                    <bk-button
                        theme="primary"
                        :disabled="!agreed || submitting || !hasContent"
                        :loading="submitting"
                        @click="agree"
                    >
                        {{ $t('convention.button') }}
                    </bk-button>
                </span>
            </div>
        </div>
    </bk-dialog>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { Action, State } from 'vuex-class'

    @Component
    export default class ConventionDialog extends Vue {
        @State('conventionContent') content!: string
        @State('conventionStatus') conventionStatus!: string
        @Action agreeConvention

        agreed: boolean = false
        submitting: boolean = false
        /** 强制保持打开时用于触发视图刷新 */
        forceTick: number = 0

        get hasContent (): boolean {
            return (this.content || '').replace(/<[^>]+>/g, '').trim().length > 0
        }

        get visible (): boolean {
            // forceTick 参与计算，cancel 后可强制重新为 true
            void this.forceTick
            return this.conventionStatus === 'required' && this.hasContent
        }

        @Watch('visible')
        onVisibleChange (val: boolean) {
            if (val) {
                this.agreed = false
            }
        }

        keepOpen () {
            this.forceTick += 1
        }

        onValueChange (val: boolean) {
            if (!val && this.conventionStatus === 'required') {
                this.forceTick += 1
            }
        }

        async agree () {
            if (!this.agreed || this.submitting || !this.hasContent) return
            this.submitting = true
            try {
                await this.agreeConvention()
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('convention.done')
                })
            } catch (e) {
                this.$bkMessage({
                    theme: 'error',
                    message: (e && e.message) || this.$t('convention.retry')
                })
            } finally {
                this.submitting = false
            }
        }
    }
</script>

<style lang="scss">
    .devops-convention-dialog {
        .bk-dialog-wrapper .bk-dialog-body {
            padding: 8px 24px 16px;
            max-height: 60vh;
            overflow-y: auto;
        }
        .bk-dialog-footer {
            padding: 16px 24px;
        }
    }
    .sign-body {
        .convention-html {
            color: #313238;
            font-size: 14px;
            line-height: 24px;
            p {
                margin: 0 0 16px;
                &:last-child {
                    margin-bottom: 0;
                }
            }
            strong {
                font-weight: 600;
            }
            a {
                color: #3a84ff;
                white-space: nowrap;
                text-decoration: none;
                cursor: pointer;
                &:hover {
                    color: #699df4;
                }
            }
        }
    }
    .sign-foot {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        width: 100%;
        .bk-form-checkbox {
            flex: 1;
            min-width: 0;
            text-align: left;
        }
    }
    .sign-actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
    }
    .agree-wrap {
        display: inline-block;
        button[disabled] {
            pointer-events: none;
        }
    }
</style>
