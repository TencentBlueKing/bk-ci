<template>
    <Undeploy
        :service-name="serviceName"
        :service-desc="serviceDesc"
        :guide-url="guideUrl"
    />
</template>

<script lang="ts">
    import Undeploy from '@/components/Undeploy'
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    Component.registerHooks([
        'beforeRouteEnter'
    ])
    
    @Component({
        components: {
            Undeploy
        }
    })
    export default class ExceptionUndeploy extends Vue {
        get service (): Object {
            const currentService = window.serviceObject.serviceMap[this.$route.params.id]
            return currentService || {}
        }

        get serviceName (): string {
            return this.$t('undeployTitle', [this.service.name])
        }
        
        get serviceDesc (): string {
            return this.$t(`${this.$route.params.id}ServiceDesc`)
        }

        get guideUrl (): string {
            return `${DOCS_URL_PREFIX}${this.service.docUrl || ''}`
        }

        beforeRouteEnter (to, from, next) {
            const currentService = window.serviceObject.serviceMap[to.params.id]
            if (currentService.status !== 'developing') {
                location.href = `/console/${to.params.id}`
                return
            }
            next(true)
        }
    }
</script>
