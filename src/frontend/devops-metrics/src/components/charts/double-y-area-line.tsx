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
    titles: Array,
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
      const { data, labels, titles } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'line',
        data: {
          labels,
          datasets: data.map((item, index) => ({
            label: item.label,
            fill: true,
            backgroundColor: item.backgroundColor || 'rgba(43, 124, 255,0.3)',
            borderColor: item.borderColor || 'rgba(43, 124, 255,1)',
            lineTension: 0,
            borderWidth: 2,
            pointRadius: 2,
            pointHitRadius: 3,
            pointHoverRadius: 3,
            data: [...item.list],
            yAxisID: index === 0 ? 'y1' : 'y2',
            datalabels: {
              listeners: {
                leave: function(context) {
                  document.getElementsByClassName('double-line-canvas')[0]['style']['cursor'] = 'auto';
                }
              },
            },
          })),
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          scale: {
            ticks: {
              stepSize: 2
            }
          },
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
            crosshair: {
              enabled: true,
              mode: 'x',
              style: {
                x: {
                  enabled: true,
                  color: '#cde0ff',
                  weight: 1,
                  borderStyle: 'solid',
                },
                y: {
                  enabled: false,
                },
              },
            },
          },
          scales: {
            y1: {
              position: 'left',
              title: {
                display: true,
                text: titles[0],
                align: 'start',
                color: '#979BA5',
              },
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              ticks: {
                color: '#979BA5',
              },
              min: 0,
            },
            y2: {
              position: 'right',
              title: {
                display: true,
                text: titles[1],
                align: 'start',
                color: '#979BA5',
              },
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              ticks: {
                color: '#979BA5',
              },
              min: 0,
            },
            x: {
              scaleLabel: {
                display: true,
                padding: 0,
              },
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
      [
        props.data,
        props.labels,
      ],
      draw,
    );

    onMounted(draw);

    onBeforeUnmount(destoryChart);

    return () => (
        <div class="canvas-wrapper">
          <canvas ref={canvasRef} class="double-line-canvas"></canvas>
        </div>
    );
  },
});

