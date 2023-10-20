<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field
                :key="key"
                :desc="obj.desc"
                :desc-link="obj.descLink"
                :desc-link-text="obj.descLinkText"
                :required="obj.required"
                :label="obj.label">
                <component
                    :disabled="true"
                    :is="obj.component"
                    :name="key"
                    :value="element[key]"
                    v-bind="obj"
                ></component>
            </form-field>
        </template>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    export default {
        name: 'normal-atom',
        mixins: [atomMixin],
        props: {
            element: Object,
            atomPropsModel: Object
        }
    }
</script>

<style lang="scss">
    .atom-form-label {
        font-size: 12px !important;
    }
</style>
