<template>
    <svg 
        xmlns='http://www.w3.org/2000/svg'
        xmlns:xlink='http://www.w3.org/1999/xlink'
        width="79px" :height='`${height + 100}px`'>
        <path 
            fill-rule="evenodd"
            :stroke='strokeColor'
            :stroke-width='strokeWidth'
            stroke-dasharray='4, 4'
            stroke-linecap='butt'
            stroke-linejoin='miter'
            fill='none'
            :d='d'/>
        </svg>
</template>


<script>
    export default {
        props: {
            strokeColor: {
                type: String,
                default: 'black'
            },
            strokeWidth: {
                type: String,
                default: '2px'
            },
            height: {
                type: Number,
                default: 100
            }
        },
        computed: {
            d () {
                const distance = this.height + 8
                const cruveEnd = `C7.000,4.686 4.314,2.000 1.000,2.000`
                return `M39.000,${distance}.000 L13.000,${distance}.000 C9.686,${distance} 7.000,${distance - 3}.314 7.000,${distance - 6} L7.000,8.000 C7.000,4.686 4.314,2.000 1.000,2.000`
            }
        }
    }
</script>

