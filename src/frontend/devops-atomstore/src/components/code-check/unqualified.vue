<template>
    <section>
        <p class="code-check-tip" v-if="!isInDetailPage"><i class="bk-icon icon-info-circle"></i>{{ $t('store.修复代码检查发现的问题后，请重新构建并测试插件') }}</p>
        <section class="code-ckeck-status">
            <i class="bk-icon icon-exclamation-circle status-icon"></i>
            <section class="code-check-summary">
                <h3 class="summary-head">{{ $t('store.代码质量不合格') }}</h3>
                <h5 class="summary-desc">{{ $t('store.codeMeasurement', [codeSecurityQualifiedScore, codeStyleQualifiedScore, codeMeasureQualifiedScore]) }}</h5>
                <h5 class="summary-link" v-if="isInDetailPage">{{ $t('store.最近检查') }}:<span class="link-txt" @click="goToLink(repoUrl)">{{ commitId | commitFilter }}</span>{{ lastAnalysisTime | timeFilter }} <span class="link-txt" @click="goToLink(codeccUrl)">{{ $t('store.查看详情') }}</span></h5>
            </section>
            <bk-button theme="primary" class="code-check-button" v-if="isInDetailPage" @click="startCodeCC" :loading="startChecking">{{ $t('store.重新检查') }}</bk-button>
            <bk-button theme="primary" class="code-check-button" v-else-if="codeccUrl" @click="goToLink(codeccUrl)">{{ $t('store.查看详情') }}</bk-button>
        </section>
    </section>
</template>

<script>
    import { convertTime } from '@/utils'

    export default {
        filters: {
            commitFilter (val) {
                return (val || '').slice(0, 7)
            },
            timeFilter (val) {
                return convertTime(val)
            }
        },

        props: {
            codeccUrl: String,
            commitId: String,
            repoUrl: String,
            startChecking: Boolean,
            codeStyleQualifiedScore: String,
            codeSecurityQualifiedScore: String,
            codeMeasureQualifiedScore: String,
            lastAnalysisTime: String
        },

        computed: {
            isInDetailPage () {
                return this.$route.name === 'check'
            }
        },

        methods: {
            startCodeCC () {
                this.$emit('startCodeCC')
            },

            goToLink (url) {
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .icon-exclamation-circle {
        color: #ea3636;
    }
</style>
