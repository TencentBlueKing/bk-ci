<template>
    <div v-bkloading="{ isLoading }" class="remote-trigger">
        <p>{{ $t('editPage.remoteCurlTips') }}</p>
        <p>{{ $t('editPage.example') }}：</p>
        <p class="curl-url">curl -X POST {{baseUrl}}/external/pipelines/{{value}}/build -H "Content-Type: application/json" -H "X-DEVOPS-UID: " -d "{{stringifyParmas}}" </p>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import { mapActions } from 'vuex'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    export default {
        name: 'remote-curl-url',
        mixins: [atomFieldMixin],
        props: {
            container: Object,
            element: Object
        },
        computed: {
            baseUrl () {
                return `${location.host}${API_URL_PREFIX}/${PROCESS_API_URL_PREFIX}`
            },
            stringifyParmas () {
                const { params } = this.container
                const paramMap = params.filter(param => param.required).reduce((map, param) => {
                    map[param.id] = param.defaultValue
                    return map
                }, {})
                return JSON.stringify(paramMap).replace(/\"/g, '\\"')
            },
            isLoading () {
                return !this.value
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            value (newVal, oldVal) {
                const { params } = this.$route
                if (newVal !== oldVal) {
                    this.getRemoteTriggerToken({
                        ...params,
                        preToken: newVal,
                        element: this.element
                    })
                }
            }
        },
        mounted () {
            const { params } = this.$route
            this.getRemoteTriggerToken({
                ...params,
                preToken: this.value,
                element: this.element
            })
        },
        methods: {
            ...mapActions('atom', [
                'getRemoteTriggerToken'
            ])
        }
    }
</script>

<style lang="scss">
    .remote-trigger {
        p {
            margin: 10px 0;
        }
    }
    .curl-url {
        font-weight: bold;
        color: #c7c7c7;
        background: #373636;
        border-radius: 5px;
        padding: 10px;
        word-break: break-word;
    }
</style>
