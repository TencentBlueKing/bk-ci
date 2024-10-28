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

        get languageCode (): string {
            const languageCodeMatch = this.$i18n.locale.match(/^[A-Za-z]{2}/)
            if (languageCodeMatch) {
                return languageCodeMatch[0].toUpperCase();
            }
            return 'ZH'
        }

        get bkCiVersion (): string {
            const versionMatch = BK_CI_VERSION.match(/^(\d+)\.(\d+)/);
            if (versionMatch) {
                return `${versionMatch[1]}.${versionMatch[2]}`;
            }
            return ''
        }

        get guideUrl (): string {
            return `${DOCS_URL_PREFIX}/markdown/${this.languageCode}/Devops/${this.bkCiVersion}${this.service.docUrl || ''}`
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
