import {
  defineComponent,
  PropType,
  watch,
  onMounted,
  onBeforeUnmount,
  ref,
} from 'vue';
import BKChart from '@blueking/bkcharts';

export interface IData {
  label: string,
  list: Array<string | number>,
  backgroundColor?: string,
  borderColor?: string
}

export default defineComponent({
  props: {
    data: Array as PropType<Array<IData>>,
    labels: Array,
    title: String,
  },

  emits: ['point-click', 'point-hover'],

  setup(props, { emit }) {
    const canvasRef = ref(null);
    let chart;

    const destoryChart = () => {
      chart?.destroy();
    };
    const draw = () => {
      destoryChart();
      const { data, labels, title } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'bar',
        data: {
          labels,
          datasets: data.map(item => ({
            label: item.label,
            backgroundColor: item.backgroundColor || 'rgba(43, 124, 255,0.3)',
            borderColor: item.borderColor || 'rgba(43, 124, 255,0.3)',
            borderSkipped: 'bottom',
            borderWidth: 1,
            data: [...item.list],
            datalabels: {
              listeners: {
                leave: function(context) {
                  document.getElementsByClassName('bar-canvas')[0]['style']['cursor'] = 'auto';
                }
              },
            },
          })),
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          plugins: {
            tooltip: {
              bodySpacing: 10,
              mode: 'x',
              intersect: false,
              enableItemActive: true,
              singleInRange: true,
            },
            legend: {
              position: 'bottom',
              legendIcon: 'arc',
              align: 'center',
              labels: {
                padding: 10,
                usePointStyle: true,
                pointStyle: 'dash',
              },
            },
          },
          scale: {
            ticks: {
              stepSize: 2
            }
          },
          scales: {
            y: {
              stacked: true,
              title: {
                display: true,
                text: title,
                align: 'start',
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              min: 0,
            },
            x: {
              stacked: true,
              grid: {
                drawTicks: false,
                display: false,
              },
            },
          },
          onClick (_, datasets) {
            if (datasets.length > 0) {
              emit('point-click', datasets)
            }
          },
          onHover (_, datasets) {
            if (datasets.length > 0) {
              emit('point-hover', datasets)
            }
          },
        },
      });
    };

    watch(
      props,
      draw,
    );

    onMounted(draw);

    onBeforeUnmount(destoryChart);

    return () => (
      <div class="canvas-wrapper">
        <canvas class="bar bar-canvas" ref={canvasRef}></canvas>
      </div>
    );
  },
});
