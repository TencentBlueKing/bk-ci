import {
  defineComponent,
} from 'vue';


export default defineComponent({
  props: {
    title: String,
  },

  setup(props, { slots }) {
    return () => (
      <h3 class="bg-white h-[52px] leading-[52px] pl-[24px] text-[16px] text-[#313238] shadow-3xl">
        {slots.default?.() ?? props.title}
      </h3>
    );
  },
});
