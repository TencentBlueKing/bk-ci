<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field v-if="!obj.hidden && rely(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component
                    :is="obj.component"
                    :name="key"
                    :value="element[key]"
                    :disabled="true"
                    v-bind="obj">
                </component>
            </form-field>
        </template>
        <!-- <form-field v-if="Object.keys(customTriggerControlModel).length">
            <accordion show-checkbox :show-content="enableThirdFilter" key="customTriggerControl" :is-version="true">
                <header class="var-header" style="height: 16px;" slot="header">
                    <span>
                        {{ $t('editPage.customTriggerControl') }}
                        <i class="bk-icon icon-info-circle ml5" v-bk-tooltips="$t('editPage.customTriggerControlTips')"></i>
                        <a class="title-link" target="blink" :href="customTriggerDocsLink">{{ $t('editPage.customTriggerLinkDesc') }}</a>
                    </span>
                    <input class="accordion-checkbox" :disabled="disabled" :checked="enableThirdFilter" type="checkbox" @click.stop />
                </header>
                <div slot="content" class="bk-form bk-form-vertical" v-if="enableThirdFilter">
                    <template v-for="(obj, key) in customTriggerControlModel">
                        <form-field :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                            <component
                                :is="obj.component"
                                :name="key"
                                :value="element[key]"
                                v-bind="obj">
                            </component>
                        </form-field>
                    </template>
                </div>
            </accordion>
        </form-field> -->
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    export default {
        name: 'code-git-web-hook-trigger',
        mixins: [atomMixin],
        props: {
            element: Object,
            atomPropsModel: Object
        },
        data () {
            return {
                customTriggerControlModel: {},
                enableThirdFilter: false,
                customTriggerDocsLink: 'https://github.com/Tencent/bk-ci/issues/7743#issue-1391717634'
            }
        }
    }
</script>

<style lang="scss">
    .title-link {
        cursor: pointer;
        margin-left: 10px;
        color: #3c96ff;
    }
</style>
