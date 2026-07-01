'use strict';

const SVG_NS = 'http://www.w3.org/2000/svg';
const HEALTH_COLOR = { UP: '#34d399', DEGRADED: '#fbbf24', DOWN: '#f87171' };
const NODE_RADIUS = 22;

let requestsChart = null;
let latencyChart = null;
let nodeEls = {};
let timer = null;

function $(id) { return document.getElementById(id); }
function setText(id, value) { $(id).textContent = value; }
function status(msg) { $('status').textContent = msg; }

async function loadTopologies() {
  try {
    const res = await fetch('/api/topologies');
    const names = await res.json();
    const select = $('topology');
    select.innerHTML = '';
    names.forEach(name => {
      const opt = document.createElement('option');
      opt.value = name;
      opt.textContent = name;
      select.appendChild(opt);
    });
  } catch (e) {
    status('No se pudieron cargar las topologías: ' + e.message);
  }
}

function layout(view) {
  const adj = {};
  view.nodes.forEach(n => { adj[n.id] = []; });
  view.edges.forEach(e => { if (adj[e.from]) { adj[e.from].push(e.to); } });

  const depth = {};
  const queue = [view.entryPointId];
  depth[view.entryPointId] = 0;
  while (queue.length) {
    const u = queue.shift();
    (adj[u] || []).forEach(v => {
      if (depth[v] === undefined) { depth[v] = depth[u] + 1; queue.push(v); }
    });
  }
  view.nodes.forEach(n => { if (depth[n.id] === undefined) { depth[n.id] = 0; } });

  const byDepth = {};
  view.nodes.forEach(n => { (byDepth[depth[n.id]] = byDepth[depth[n.id]] || []).push(n.id); });
  const maxDepth = Math.max(0, ...Object.keys(byDepth).map(Number));
  const width = 640;
  const height = 360;
  const pos = {};
  Object.keys(byDepth).map(Number).forEach(d => {
    const ids = byDepth[d];
    const x = maxDepth === 0 ? width / 2 : 70 + d * ((width - 140) / maxDepth);
    ids.forEach((id, i) => { pos[id] = { x, y: (height / (ids.length + 1)) * (i + 1) }; });
  });
  return pos;
}

function renderGraph(view) {
  const svg = $('graph');
  svg.innerHTML = '';
  nodeEls = {};
  const pos = layout(view);

  view.edges.forEach(e => {
    const a = pos[e.from];
    const b = pos[e.to];
    if (!a || !b) { return; }
    const line = document.createElementNS(SVG_NS, 'line');
    line.setAttribute('x1', a.x); line.setAttribute('y1', a.y);
    line.setAttribute('x2', b.x); line.setAttribute('y2', b.y);
    line.setAttribute('class', 'edge');
    svg.appendChild(line);
  });

  view.nodes.forEach(n => {
    const p = pos[n.id];
    const circle = document.createElementNS(SVG_NS, 'circle');
    circle.setAttribute('cx', p.x); circle.setAttribute('cy', p.y);
    circle.setAttribute('r', NODE_RADIUS);
    circle.setAttribute('fill', '#4b5563');
    circle.setAttribute('stroke', '#0d1218');
    circle.setAttribute('stroke-width', '2');
    svg.appendChild(circle);
    nodeEls[n.id] = circle;

    const label = document.createElementNS(SVG_NS, 'text');
    label.setAttribute('x', p.x); label.setAttribute('y', p.y + NODE_RADIUS + 14);
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('class', 'node-label');
    label.textContent = n.id;
    svg.appendChild(label);

    const type = document.createElementNS(SVG_NS, 'text');
    type.setAttribute('x', p.x); type.setAttribute('y', p.y + 3);
    type.setAttribute('text-anchor', 'middle');
    type.setAttribute('class', 'node-type');
    type.textContent = n.type.replace('LOAD_BALANCER', 'LB');
    svg.appendChild(type);
  });
}

function setHealth(id, health) {
  if (nodeEls[id]) { nodeEls[id].setAttribute('fill', HEALTH_COLOR[health] || '#4b5563'); }
}

function makeChart(canvasId, datasets) {
  return new Chart($(canvasId).getContext('2d'), {
    type: 'line',
    data: { labels: [], datasets },
    options: {
      animation: false,
      responsive: true,
      scales: {
        x: { ticks: { color: '#9aa7b4', maxTicksLimit: 8 }, grid: { color: '#2a3543' } },
        y: { beginAtZero: true, ticks: { color: '#9aa7b4' }, grid: { color: '#2a3543' } }
      },
      plugins: { legend: { labels: { color: '#e6edf3' } } }
    }
  });
}

function initCharts() {
  requestsChart = makeChart('chart-requests', [
    { label: 'Completados', data: [], borderColor: '#34d399', tension: 0.2, pointRadius: 0 },
    { label: 'Fallidos', data: [], borderColor: '#f87171', tension: 0.2, pointRadius: 0 }
  ]);
  latencyChart = makeChart('chart-latency', [
    { label: 'Latencia p95 (ms)', data: [], borderColor: '#2dd4bf', tension: 0.2, pointRadius: 0 }
  ]);
}

function resetCharts() {
  [requestsChart, latencyChart].forEach(c => {
    c.data.labels = [];
    c.data.datasets.forEach(d => { d.data = []; });
    c.update('none');
  });
}

function replay(resp) {
  const timeline = resp.report.timeline;
  resetCharts();
  setText('m-generated', resp.report.generatedRequests);
  setText('m-success', '—');
  $('reasons').textContent = '';
  let i = 0;
  const speed = Number($('speed').value);
  const stepMs = Math.max(25, 320 - speed * 15);
  clearInterval(timer);
  timer = setInterval(() => {
    if (i >= timeline.length) { clearInterval(timer); finalize(resp); return; }
    const s = timeline[i];
    const seconds = (s.atMillis / 1000).toFixed(0);
    s.components.forEach(c => setHealth(c.id, c.health));
    $('clock').textContent = seconds + 's';
    requestsChart.data.labels.push(seconds);
    requestsChart.data.datasets[0].data.push(s.completedSoFar);
    requestsChart.data.datasets[1].data.push(s.failedSoFar);
    requestsChart.update('none');
    latencyChart.data.labels.push(seconds);
    latencyChart.data.datasets[0].data.push(s.latencyP95Millis);
    latencyChart.update('none');
    setText('m-completed', s.completedSoFar);
    setText('m-failed', s.failedSoFar);
    i++;
  }, stepMs);
}

function finalize(resp) {
  const r = resp.report;
  setText('m-generated', r.generatedRequests);
  setText('m-completed', r.completedRequests);
  setText('m-failed', r.failedRequests);
  setText('m-success', (r.successRate * 100).toFixed(1) + '%');
  const reasons = r.failuresByReason || {};
  const parts = Object.keys(reasons).map(k => k + '=' + reasons[k]);
  $('reasons').textContent = parts.length ? 'Fallos por causa: ' + parts.join('   ') : 'Sin fallos 🎉';
  status('Listo (' + r.generatedRequests + ' requests en ' + (r.simulatedDurationMillis / 1000).toFixed(0) + 's simulados).');
}

async function run() {
  const faultSpec = $('fault').value.trim();
  const seedValue = $('seed').value.trim();
  const body = {
    topology: $('topology').value,
    seed: seedValue === '' ? null : Number(seedValue),
    faults: faultSpec === '' ? [] : [faultSpec]
  };
  status('Corriendo simulación…');
  $('run').disabled = true;
  try {
    const res = await fetch('/api/run', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) { status('Error: ' + (data.error || res.status)); return; }
    renderGraph(data.topology);
    replay(data);
  } catch (e) {
    status('Error de red: ' + e.message);
  } finally {
    $('run').disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initCharts();
  loadTopologies();
  $('run').addEventListener('click', run);
  document.querySelectorAll('.quick button').forEach(btn => {
    btn.addEventListener('click', () => { $('fault').value = btn.dataset.fault; });
  });
});
