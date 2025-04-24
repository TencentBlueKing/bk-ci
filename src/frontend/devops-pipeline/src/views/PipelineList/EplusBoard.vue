<template>
    <div
        class="eplus-iframe-wrapper"
        v-bkloading="{ isLoading }"
    >
        <iframe
            v-if="eplusUrl"
            :src="eplusUrl"
            ref="eplusIframe"
            class="eplus-iframe"
        ></iframe>
    </div>
</template>
<script>
    import request from '@/utils/request'
    import { computed, defineComponent, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
    export default defineComponent({
        setup () {
            const isLoading = ref(false)
            const eplusIframe = ref(null)
            const vm = getCurrentInstance()
            const projectId = computed(() => vm.proxy?.$route?.params?.projectId)
            const eplusUrl = ref()
            const abort = new AbortController()
            let eventAdded = false

            watch(projectId, () => {
                nextTick(fetchEplusUrl)
            })

            watch(eplusUrl, () => {
                nextTick(() => {
                    if (eplusIframe.value && !eventAdded) {
                        eplusIframe.value.addEventListener('load', () => {
                            isLoading.value = false
                        }, {
                            signal: abort.signal
                        })
                        eventAdded = true
                    }
                })
            })

            onMounted(() => {
                fetchEplusUrl()
            })

            onBeforeUnmount(() => {
                abort.abort()
            })

            async function fetchEplusUrl () {
                try {
                    if (isLoading.value || !vm.proxy.$route?.params?.projectId) return
                    isLoading.value = true
                    const res = await request.get('/project/api/user/services/61/url/get')
                    eplusUrl.value = res.data
                } catch (error) {
                    console.log(error)
                }
            }
            return {
                isLoading,
                eplusIframe,
                eplusUrl
            }
        }
    })
</script>

<style lang="scss">
    .eplus-iframe-wrapper {
        width: 100%;
        height: 100%;

        .eplus-iframe {
            width: 100%;
            height: 100%;
            border: none;
        }
    }
</style>
