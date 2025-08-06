<template>
    <div class="time-parse">
        <template v-if="data[1] && /^At/.test(data[1])">
            <span class="hour">{{ data[1] }}</span>
            <span class="minute">{{ data[0] }}</span>
        </template>
        <template v-else>
            <span class="minute">{{ data[0] }}</span>
            <template v-if="data[1]">
                <span class="hour"> {{ data[1] }}</span>
            </template>
        </template>
        <span
            v-if="data[2]"
            class="dayOfMonth"
        > {{ data[2] }}</span>
        <span
            v-if="data[3]"
            class="dayOfWeek"
        > {{ data[3] }}</span>
        <span
            v-if="data[4]"
            class="month"
        > {{ data[4] }}</span>
    </div>
</template>
<script set>
    export default {
        props: {
            data: {
                type: Array,
                require: true
            }
        }
    }
</script>
