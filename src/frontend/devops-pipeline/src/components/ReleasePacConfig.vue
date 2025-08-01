<template>
    <div
        class="release-pipeline-pac-conf"
    >
        <aside class="release-pipeline-pac-conf-leftside">
            <label for="enablePac">
                {{ $t("pacMode") }}
                <span
                    class="devops-icon icon-info-circle"
                    v-bk-tooltips="pacDesc"
                />
            </label>
            <bk-switcher
                :disabled="pacEnabled"
                theme="primary"
                name="enablePac"
                :title="isTemplatePipeline ? $t('templateYamlNotSupport') : ''"
                v-model="releaseParams.enablePac"
                @change="handlePacEnableChange"
            />
        </aside>
        <aside
            v-if="releaseParams.enablePac && hasPacSupportScmTypeList"
            class="release-pipeline-pac-conf-rightside"
        >
            <label for="enablePac">
                {{ $t("codelibSrc") }}
            </label>
            <bk-radio-group v-model="releaseParams.scmType">
                <bk-radio
                    v-for="item in pacSupportScmTypeList"
                    :key="item.id"
                    :value="item.id"
                >
                    {{ $t(item.value) }}
                </bk-radio>
            </bk-radio-group>
        </aside>
    </div>

</template>

<script>
     export default {
        props: {
            pacEnabled: {
                type: Boolean,
                default: false
            },
            isTemplatePipeline: {
                type: Boolean,
                default: false
            },
            releaseParams: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                
            }
        },
        computed: {
            pacDesc () {
                return {
                    content: this.$t('pacDesc'),
                    maxWidth: 300
                }
            }
        },
        watch: {
           
        },
        created () {
        },
        methods: {
           
        }
    }
</script>