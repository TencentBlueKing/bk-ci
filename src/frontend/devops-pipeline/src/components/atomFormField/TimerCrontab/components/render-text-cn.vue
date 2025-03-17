<template>
    <div class="time-parse">
        <span
            v-if="data[4]"
            class="month"
        >{{ data[4] }}</span>
        <template v-if="data[1]">
            <span class="dayOfMonth">{{ data[2] }}</span>
            <span v-if="data[3]">以及当月</span>
        </template>
        <span
            v-if="data[3]"
            class="dayOfWeek"
        >{{ data[3] }}</span>
        <span
            v-if="data[2]"
            class="hour"
        >{{ data[1] }}</span>
        <span class="minute">{{ data[0] }}</span>
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
