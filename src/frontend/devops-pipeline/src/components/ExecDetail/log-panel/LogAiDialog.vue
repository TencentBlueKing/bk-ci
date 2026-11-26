<template>
    <bk-dialog
        :value="true"
        :title="$t('logPanel.ai.title')"
        :width="760"
        :show-footer="false"
        @value-change="onValueChange"
    >
        <p v-if="!enabled">
            {{ $t('logPanel.ai.unavailable') }}
            <a
                v-if="docsUrl"
                :href="docsUrl"
                target="_blank"
                rel="noopener noreferrer"
            >{{ $t('logPanel.ai.docs') }}</a>
        </p>
        <template v-else>
            <div
                ref="content"
                class="ai-content"
            >
                <mavon-editor
                    v-if="message"
                    :value="message"
                    :editable="false"
                    :subfield="false"
                    :toolbars-flag="false"
                    :box-shadow="false"
                    :html="false"
                    :external-link="false"
                    default-open="preview"
                    preview-background="#fff"
                />
                <p
                    v-if="loading"
                    role="status"
                >
                    {{ $t('loading') }}
                </p>
                <p
                    v-if="loadFailed"
                    role="alert"
                >
                    {{ $t('logPanel.ai.loadFailed') }}
                </p>
            </div>
            <div class="ai-actions">
                <bk-button
                    v-if="loadFailed"
                    theme="primary"
                    text
                    @click="loadAnalysis(lastRefresh)"
                >
                    {{ $t('retry') }}
                </bk-button>
                <bk-button
                    v-else
                    theme="primary"
                    text
                    :disabled="loading || feedbackBusy"
                    @click="loadAnalysis(true)"
                >
                    {{ $t('logPanel.ai.regenerate') }}
                </bk-button>
                <bk-button
                    v-for="option in feedbackOptions"
                    :key="String(option.score)"
                    text
                    :theme="option.active ? 'primary' : 'default'"
                    :aria-pressed="String(option.active)"
                    :title="option.users.join('、')"
                    :disabled="loading || loadFailed || feedbackBusy || feedbackFailed"
                    @click="submitFeedback(option)"
                >
                    {{ $t(option.label) }} ({{ option.users.length }})
                </bk-button>
            </div>
            <p
                v-if="feedbackFailed"
                role="alert"
            >
                {{ $t('logPanel.ai.feedbackFailed') }}
                <bk-button
                    text
                    theme="primary"
                    :disabled="feedbackBusy"
                    @click="loadFeedback"
                >
                    {{ $t('retry') }}
                </bk-button>
            </p>
        </template>
    </bk-dialog>
</template>

<script>
    import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

    export default {
        name: 'LogAiDialog',
        props: {
            target: { type: Object, required: true },
            enabled: { type: Boolean, required: true }
        },
        emits: ['close'],
        setup (props, { emit }) {
            const vm = getCurrentInstance().proxy
            const content = ref(null)
            const message = ref('')
            const loading = ref(false)
            const lastRefresh = ref(false)
            const loadFailed = ref(false)
            const feedbackBusy = ref(false)
            const feedbackFailed = ref(false)
            const goodUsers = ref([])
            const badUsers = ref([])
            let active = true
            let stream

            const docsUrl = vm.$pipelineDocs?.AIAnalysis
            const user = vm.$userInfo?.username
            const feedbackOptions = computed(() => [
                { score: true, label: 'logPanel.ai.like', users: goodUsers.value, active: goodUsers.value.includes(user) },
                { score: false, label: 'logPanel.ai.dislike', users: badUsers.value, active: badUsers.value.includes(user) }
            ])
            const dispatch = (action, params = {}) => vm.$store.dispatch(`atom/${action}`, { ...props.target, ...params })

            async function loadAnalysis (refresh) {
                if (!props.enabled || loading.value || feedbackBusy.value) return
                stream = new AbortController()
                loading.value = true
                lastRefresh.value = refresh
                message.value = ''
                loadFailed.value = false
                try {
                    await dispatch('getLogAIMessage', {
                        refresh,
                        signal: stream.signal,
                        callBack (chunk) {
                            if (!active) return
                            message.value += chunk
                            nextTick(() => {
                                if (content.value) content.value.scrollTop = content.value.scrollHeight
                            })
                        }
                    })
                    if (active && !message.value.trim()) loadFailed.value = true
                } catch {
                    if (active) loadFailed.value = true
                } finally {
                    if (active) {
                        loading.value = false
                        if (refresh && !loadFailed.value) loadFeedback()
                    }
                }
            }

            async function readFeedback () {
                const { data } = await dispatch('getPraiseAiInfo')
                if (!active) return
                goodUsers.value = Array.isArray(data?.goodUsers) ? data.goodUsers : []
                badUsers.value = Array.isArray(data?.badUsers) ? data.badUsers : []
            }

            async function loadFeedback () {
                if (!props.enabled || feedbackBusy.value) return
                feedbackBusy.value = true
                feedbackFailed.value = false
                try {
                    await readFeedback()
                } catch {
                    if (active) feedbackFailed.value = true
                } finally {
                    if (active) feedbackBusy.value = false
                }
            }

            async function submitFeedback ({ score, active: remove }) {
                if (!props.enabled || loading.value || loadFailed.value || feedbackBusy.value || feedbackFailed.value) return
                feedbackBusy.value = true
                try {
                    await dispatch(remove ? 'cancelPraiseAi' : 'praiseAi', { score })
                    if (!active) return
                    await readFeedback()
                    const key = score
                        ? (remove ? 'successCancelPraise' : 'successPraise')
                        : (remove ? 'successCancelDownPraise' : 'successDownPraise')
                    if (active) vm.$bkMessage({ theme: 'success', message: vm.$t(key) })
                } catch {
                    if (active) feedbackFailed.value = true
                } finally {
                    if (active) feedbackBusy.value = false
                }
            }

            onMounted(() => {
                if (!props.enabled) return
                loadAnalysis(false)
                loadFeedback()
            })
            onBeforeUnmount(() => {
                active = false
                stream?.abort()
            })

            return {
                content, docsUrl, message, loading, lastRefresh, loadFailed, feedbackBusy, feedbackFailed, feedbackOptions,
                loadFeedback, loadAnalysis, submitFeedback, onValueChange: visible => {
                    if (!visible) emit('close')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.ai-content { max-height: 55vh; overflow: auto; }
.ai-actions { display: flex; gap: 16px; margin-top: 12px; }
</style>
