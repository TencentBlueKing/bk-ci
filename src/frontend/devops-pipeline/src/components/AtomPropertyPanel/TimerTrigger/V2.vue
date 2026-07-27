<template>
    <div class="bk-form bk-form-vertical">
        <form-field
            :required="true"
            :label="$t('editPage.timerTimeZone')"
            :desc="$t('editPage.timerTimeZoneDesc')"
        >
            <time-zone-select
                name="timeZone"
                :value="element.timeZone"
                :disabled="disabled"
                :handle-change="handleChange"
                :placeholder="$t('editPage.timerTimeZonePlaceholder')"
            />
        </form-field>
        <template v-for="(obj, key) in atomPropsModel">
            <form-field
                v-if="!obj.hidden && rely(obj, element)"
                :class="{ 'changed-prop': atomVersionChangedKeys.includes(key) }"
                :key="key"
                v-bind="obj"
                :is-error="errors.has(key)"
                :error-msg="errors.first(key)"
            >
                <component
                    :is="obj.component"
                    v-bind="obj"
                    :name="key"
                    :value="element[key]"
                    :element="element"
                    :disabled="disabled"
                    :handle-change="handleChange"
                    :time-zone="element.timeZone"
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
    import TimeZoneSelect from './TimeZoneSelect'
    export default {
        components: {
            TimerCronTab,
            BranchParameterArray,
            CodelibSelector,
            TimeZoneSelect
        },
        mixins: [atomMixin, validMixins],
        methods: {
            updateProps (newParam) {
                this.updateAtom({
                    element: this.element,
                    newParam
                })
            },
            handleChange (name, value) {
                this.updateProps({
                    [name]: value
                })
            }
        }
    }
</script>

<style lang="scss">

</style>
