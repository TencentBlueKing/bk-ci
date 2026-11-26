<template>
    <div
        v-if="visible"
        class="devops-convention-gate"
        role="dialog"
        aria-modal="true"
        :aria-label="$t('convention.title')"
        @keydown.esc.prevent.stop
    >
        <section
            ref="card"
            class="card"
            :class="{ 'is-failed': failed }"
        >
            <div
                class="scroll"
                :class="{ 'is-failed': failed }"
            >
                <div class="hero">
                    <svg
                        class="wave"
                        viewBox="0 0 800 101"
                        preserveAspectRatio="none"
                        aria-hidden="true"
                    >
                        <path
                            d="M0 0H800V92C668 76 532 76 400 92C268 104 132 104 0 92V0Z"
                            fill="url(#conventionWave)"
                        />
                        <defs>
                            <linearGradient
                                id="conventionWave"
                                x1="-90.6516"
                                y1="24.3201"
                                x2="-27.4588"
                                y2="259.867"
                                gradientUnits="userSpaceOnUse"
                            >
                                <stop stop-color="#F0F5FF" />
                                <stop
                                    offset="1"
                                    stop-color="#F0F5FF"
                                />
                            </linearGradient>
                        </defs>
                    </svg>
                    <h1 class="title">
                        {{ $t('convention.title') }}
                    </h1>
                </div>
                <div
                    ref="bodyHtml"
                    class="convention-html body"
                    v-bk-xss-html="content"
                />
            </div>
            <p
                v-if="failed"
                ref="submitError"
                class="submit-error"
                role="alert"
            >
                <i class="bk-icon icon-exclamation-circle-shape" />
                <span>{{ failMessage }}</span>
            </p>
            <footer
                ref="foot"
                class="foot"
            >
                <bk-checkbox
                    ref="agreeCheckbox"
                    v-model="agreed"
                >
                    {{ $t('convention.agree') }}
                </bk-checkbox>
                <div class="sign-actions">
                    <span
                        class="agree-wrap"
                        v-bk-tooltips="agreed || submitting ? { disabled: true } : { content: $t('convention.tip') }"
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
            </footer>
        </section>
    </div>
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
        failed: boolean = false
        failMessage: string = ''
        private prevBodyOverflow: string = ''

        get hasContent (): boolean {
            return (this.content || '').replace(/<[^>]+>/g, '').trim().length > 0
        }

        get visible (): boolean {
            return this.conventionStatus === 'required' && this.hasContent
        }

        @Watch('visible')
        onVisibleChange (val: boolean) {
            if (val) {
                this.agreed = false
                this.failed = false
                this.failMessage = ''
                this.submitting = false
                this.lockBodyScroll()
                this.$nextTick(() => {
                    this.placeError()
                    this.patchLinks()
                    this.focusGate()
                })
            } else {
                this.unlockBodyScroll()
            }
        }

        @Watch('content')
        onContentChange () {
            if (!this.visible) return
            this.$nextTick(() => this.patchLinks())
        }

        @Watch('failed')
        onFailedChange () {
            this.$nextTick(() => this.placeError())
        }

        mounted () {
            if (this.visible) {
                this.lockBodyScroll()
                this.$nextTick(() => {
                    this.placeError()
                    this.patchLinks()
                    this.focusGate()
                })
            }
        }

        beforeDestroy () {
            this.unlockBodyScroll()
        }

        lockBodyScroll () {
            this.prevBodyOverflow = document.body.style.overflow
            document.body.style.overflow = 'hidden'
        }

        unlockBodyScroll () {
            document.body.style.overflow = this.prevBodyOverflow
            this.prevBodyOverflow = ''
        }

        focusGate () {
            const root = this.$el as HTMLElement | undefined
            if (!root || !root.querySelector) return
            const checkbox = root.querySelector(
                '.bk-form-checkbox input, .bk-checkbox input, input[type="checkbox"]'
            ) as HTMLElement | null
            if (checkbox && typeof checkbox.focus === 'function') {
                checkbox.focus()
                return
            }
            const button = root.querySelector('.agree-wrap button') as HTMLElement | null
            if (button && typeof button.focus === 'function') {
                button.focus()
            }
        }

        patchLinks () {
            const root = this.$refs.bodyHtml as HTMLElement | undefined
            if (!root) return
            root.querySelectorAll('a[href]').forEach((link) => {
                link.setAttribute('target', '_blank')
                link.setAttribute('rel', 'noopener noreferrer')
            })
        }

        placeError () {
            const foot = this.$refs.foot as HTMLElement | undefined
            const card = this.$refs.card as HTMLElement | undefined
            if (!foot || !card) return
            card.style.setProperty('--foot-h', `${foot.offsetHeight}px`)
            const error = this.$refs.submitError as HTMLElement | undefined
            if (!error) return
            const cardRect = card.getBoundingClientRect()
            error.style.width = `${cardRect.width - 40}px`
            const errorRect = error.getBoundingClientRect()
            const delta = cardRect.left + 20 - errorRect.left
            if (Math.abs(delta) < 0.5) return
            const currentLeft = parseFloat(getComputedStyle(error).left) || 0
            error.style.left = `${currentLeft + delta}px`
        }

        async agree () {
            if (!this.agreed || this.submitting || !this.hasContent) return
            this.submitting = true
            this.failed = false
            this.failMessage = ''
            try {
                await this.agreeConvention()
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('convention.done')
                })
            } catch (e) {
                this.failed = true
                this.failMessage = (e && e.message) || String(this.$t('convention.retry'))
            } finally {
                this.submitting = false
            }
        }
    }
</script>

<style lang="scss">
    .devops-convention-gate {
        position: fixed;
        z-index: 3000;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        box-sizing: border-box;
        padding: 16px;
        background: rgba(0, 0, 0, 0.6);
    }
    .devops-convention-gate .card {
        position: relative;
        display: flex;
        flex-direction: column;
        width: min(800px, 100%);
        max-height: min(632px, 100%);
        background: #fff;
        border-radius: 2px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        overflow: hidden;
    }
    .devops-convention-gate .card.is-failed {
        max-height: min(676px, 100%);
    }
    .devops-convention-gate .scroll {
        flex: 1 1 auto;
        min-height: 0;
        overflow: auto;
        padding: 0 32px 24px;
    }
    .devops-convention-gate .scroll.is-failed {
        padding-bottom: 68px;
    }
    .devops-convention-gate .hero {
        position: relative;
        height: 104px;
        margin: 0 -32px 20px;
        padding: 0 32px;
        overflow: visible;
    }
    .devops-convention-gate .title {
        position: relative;
        z-index: 1;
        margin: 0;
        padding: 20px 0;
        color: #313238;
        font-size: 24px;
        font-weight: 700;
        line-height: 32px;
        text-align: center;
    }
    .devops-convention-gate .wave {
        position: absolute;
        top: 0;
        right: 0;
        left: 0;
        display: block;
        width: 100%;
        height: 101px;
    }
    .devops-convention-gate .submit-error {
        position: absolute;
        z-index: 2;
        right: auto;
        bottom: calc(var(--foot-h, 88px) + 16px);
        left: 20px;
        width: calc(100% - 40px);
        display: flex;
        align-items: center;
        gap: 8px;
        height: 42px;
        margin: 0;
        padding: 0 12px;
        box-sizing: border-box;
        border: 1px solid #fdd;
        border-radius: 2px;
        background: #fff0f0;
        box-shadow: 0 2px 2px #f6dada;
        color: #4d4f56;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        pointer-events: none;
        .bk-icon {
            color: #ea3636;
            font-size: 18px;
        }
    }
    .devops-convention-gate .foot {
        display: flex;
        flex: none;
        align-items: center;
        gap: 16px;
        box-sizing: border-box;
        width: 100%;
        padding: 16px 32px;
        border-top: 1px solid #dcdee5;
        background: #fff;
        .bk-form-checkbox {
            flex: 1;
            min-width: 0;
            text-align: left;
            color: #4d4f56;
            font-size: 14px;
            line-height: 22px;
        }
    }
    .devops-convention-gate .sign-actions {
        display: flex;
        flex: none;
        justify-content: flex-end;
    }
    .devops-convention-gate .agree-wrap {
        display: inline-block;
        button[disabled] {
            pointer-events: none;
        }
    }
    .devops-convention-gate .convention-html {
        color: #4d4f56;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        p {
            margin: 0 0 20px;
            &:last-child {
                margin-bottom: 0;
            }
        }
        h3 {
            margin: 0 0 12px;
            color: #313238 !important;
            font-size: 16px !important;
            font-weight: 700 !important;
            line-height: 24px !important;
            background: transparent;
        }
        h3 + p {
            margin-bottom: 24px;
        }
        strong {
            font-weight: 700;
        }
        a {
            color: #1768ef;
            text-decoration: underline;
            text-underline-offset: 2px;
            cursor: pointer;
            white-space: nowrap;
        }
    }
    @media (max-width: 720px) {
        .devops-convention-gate .card,
        .devops-convention-gate .card.is-failed {
            width: min(592px, 100%);
            max-height: min(712px, 100%);
        }
        .devops-convention-gate .scroll {
            padding-right: 20px;
            padding-left: 20px;
        }
        .devops-convention-gate .hero {
            margin-right: -20px;
            margin-left: -20px;
            padding-right: 20px;
            padding-left: 20px;
        }
        .devops-convention-gate .foot {
            flex-direction: column;
            align-items: stretch;
            min-height: 120px;
            padding: 16px 20px 20px;
        }
    }
</style>
