<template>
    <div class="experience-input">
        <vuex-input v-bind="$props" />
        <span class="experience-input-link" @click="goToArtifactory">查看仓库</span>
    </div>
</template>

<script>
    import VuexInput from '../VuexInput'
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'experience-input',
        components: {
            VuexInput
        },
        mixins: [atomFieldMixin],
        props: {
            placeholder: String
        },
        computed: {
            artifactoryUrl () {
                return `${WEB_URL_PIRFIX}/artifactory/`
            }
        },
        methods: {
            goToArtifactory () {
                window.open(this.artifactoryUrl, '_blank')
            }
        }

    }
</script>

<style lang="scss">
    @import '../../../scss/conf';
    .experience-input {
        display: flex;
        align-items: center;
        > input {
            flex: 1;
        }
        &-link {
            padding-left: 10px;
            cursor: pointer;
            color: $primaryColor;
        }
    }
</style>
