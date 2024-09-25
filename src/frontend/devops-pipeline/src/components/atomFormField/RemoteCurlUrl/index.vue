<template>
    <div
        v-bkloading="{ isLoading }"
        class="remote-trigger"
    >
        <route-tips
            :visible="true"
            :tips="$t('editPage.remoteCurlTips')"
        ></route-tips>
        <p>{{ $t('editPage.example') }}：</p>
        <p class="curl-url">curl -X POST {{ baseUrl }}/external/pipelines/{{ value }}/build -H "Content-Type: application/json" -H "X-DEVOPS-UID: " -d "{{ stringifyParmas }}" </p>
    </div>
</template>

<script>
    import RouteTips from '@/components/atomFormField/RouteTips'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { mapActions } from 'vuex'
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'remote-curl-url',
        components: {
            RouteTips
        },
        mixins: [atomFieldMixin],
        props: {
            container: Object,
            element: Object
        },
        computed: {
            baseUrl () {
                return `${location.origin}${API_URL_PREFIX}/${PROCESS_API_URL_PREFIX}`
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
        color: #63656E;
        background: #F5F7FA;
        padding: 10px;
        word-break: break-word;
    }
</style>
