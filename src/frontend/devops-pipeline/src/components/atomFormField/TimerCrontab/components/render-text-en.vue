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
        <template v-if="data[2]">
            <span class="dayOfMonth"> {{ data[2] }}</span>
        </template>
        <template v-if="data[3]">
            <span class="dayOfWeek"> {{ data[3] }}</span>
        </template>
        <template v-if="data[4]">
            <span class="month"> {{ data[4] }}</span>
        </template>
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
