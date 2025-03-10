<template>
    <div class="bk-form bk-form-vertical">
        <template v-for="(obj, key) in atomPropsModel">
            <form-field
                v-if="!obj.hidden && rely(obj, element)"
                :key="key"
                v-bind="obj"
                :is-error="errors.has(key)"
                :error-msg="errors.first(key)"
            >
                <component
                    :is="obj.component"
                    :name="key"
                    :value="element[key]"
                    :element="element"
                    :disabled="disabled"
                    v-bind="obj"
                />
            </form-field>
        </template>
    </div>
</template>

<script>
    import validMixins from '../../validMixins'
    import atomMixin from '../atomMixin'
    import TimerCronTab from '@/components/atomFormField/TimerCrontab/'
    import BranchParameterArray from '../../AtomFormComponent/BranchParameterArray/index'
    import CodelibSelector from './CodelibSelector'
    export default {
        components: {
            TimerCronTab,
            BranchParameterArray,
            CodelibSelector
        },
        mixins: [atomMixin, validMixins],
        created () {
            console.log(this.atomPropsModel, 'atomPropsModel')
        }
    }
</script>

<style lang="scss">

</style>
