'use strict';

const SVG_NS = 'http://www.w3.org/2000/svg';
const HEALTH_COLOR = { UP: '#34d399', DEGRADED: '#fbbf24', DOWN: '#f87171' };
const NODE_RADIUS = 22;

let requestsChart = null;
let latencyChart = null;
let nodeEls = {};
let edgeEls = {};
let timer = null;
let currentTimeline = [];
let currentEdges = [];
let currentResp = null;
let currentIndex = 0;
let playing = false;

function edgeKey(from, to) { return from + '>' + to; }

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
    previewTopology();
  } catch (e) {
    status('No se pudieron cargar las topologías: ' + e.message);
  }
}

/** Dibuja el grafo de la topología elegida (o del YAML propio) SIN correr la simulación. */
async function previewTopology() {
  const ownYaml = $('yaml').value.trim();
  const body = ownYaml !== '' ? { yaml: ownYaml } : { topology: $('topology').value };
  try {
    const res = await fetch('/api/topology', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) { status('No se pudo dibujar la topología: ' + (data.error || res.status)); return; }
    renderGraph(data);
    data.nodes.forEach(n => setHealth(n.id, 'UP'));
    $('clock').textContent = '';
    $('hypothesis').hidden = true; // limpiar el veredicto de una corrida previa
  } catch (e) {
    status('Error al dibujar la topología: ' + e.message);
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
  edgeEls = {};
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
    edgeEls[edgeKey(e.from, e.to)] = line;
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

/** Marca una arista como "circuito abierto" (rojo punteado) o normal.
 *  Usa estilo inline porque una regla CSS de clase le gana a los atributos de presentación SVG. */
function setEdgeOpen(from, to, open) {
  const el = edgeEls[edgeKey(from, to)];
  if (!el) { return; }
  el.style.stroke = open ? '#f87171' : '';
  el.style.strokeWidth = open ? '2.5' : '';
  el.style.strokeDasharray = open ? '6 4' : '';
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
  if (typeof Chart === 'undefined') {
    console.warn('Chart.js no disponible: se omiten las gráficas (el resto del dashboard funciona).');
    return;
  }
  requestsChart = makeChart('chart-requests', [
    { label: 'Completados', data: [], borderColor: '#34d399', tension: 0.2, pointRadius: 0 },
    { label: 'Fallidos', data: [], borderColor: '#f87171', tension: 0.2, pointRadius: 0 }
  ]);
  latencyChart = makeChart('chart-latency', [
    { label: 'Latencia p95 (ms)', data: [], borderColor: '#2dd4bf', tension: 0.2, pointRadius: 0 }
  ]);
}

/** Dibuja el estado de la simulación en el snapshot i (salud de nodos, circuitos, métricas, gráficas). */
function renderFrame(i) {
  const s = currentTimeline[i];
  if (!s) { return; }
  s.components.forEach(c => setHealth(c.id, c.health));
  currentEdges.forEach(e => setEdgeOpen(e.from, e.to, false));
  (s.circuits || []).forEach(c => { if (c.state !== 'CLOSED') { setEdgeOpen(c.fromId, c.toId, true); } });
  $('clock').textContent = (s.atMillis / 1000).toFixed(0) + 's';
  setText('m-completed', s.completedSoFar);
  setText('m-failed', s.failedSoFar);
  const upto = currentTimeline.slice(0, i + 1);
  const labels = upto.map(x => (x.atMillis / 1000).toFixed(0));
  if (requestsChart) {
    requestsChart.data.labels = labels;
    requestsChart.data.datasets[0].data = upto.map(x => x.completedSoFar);
    requestsChart.data.datasets[1].data = upto.map(x => x.failedSoFar);
    requestsChart.update('none');
  }
  if (latencyChart) {
    latencyChart.data.labels = labels;
    latencyChart.data.datasets[0].data = upto.map(x => x.latencyP95Millis);
    latencyChart.update('none');
  }
  $('scrubber').value = String(i);
}

function setPlayLabel() {
  $('playpause').textContent = playing ? '⏸ Pausar' : '▶ Reproducir';
}

function startPlayback(fromIndex) {
  clearInterval(timer);
  currentIndex = fromIndex;
  playing = true;
  setPlayLabel();
  const stepMs = Math.max(25, 320 - Number($('speed').value) * 15);
  timer = setInterval(() => {
    if (currentIndex >= currentTimeline.length) {
      clearInterval(timer);
      playing = false;
      setPlayLabel();
      finalize(currentResp);
      return;
    }
    renderFrame(currentIndex);
    currentIndex++;
  }, stepMs);
}

/** Botón pausar/reproducir. */
function togglePlay() {
  if (!currentTimeline.length) { return; }
  if (playing) {
    clearInterval(timer);
    playing = false;
    setPlayLabel();
  } else {
    startPlayback(currentIndex >= currentTimeline.length ? 0 : currentIndex);
  }
}

// Símbolo legible para cada comparador (el JSON trae el nombre del enum: GTE, LTE, …).
const COMPARISON_SYMBOL = { GTE: '≥', LTE: '≤', GT: '>', LT: '<', EQ: '=' };

/** Muestra enteros sin decimales y fracciones con 3 dígitos (umbrales/tasas). */
function fmtNum(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(3);
}

/** Pinta el veredicto de la hipótesis de estado estable (o lo oculta si no fue declarada). */
function renderHypothesis(hyp) {
  const box = $('hypothesis');
  if (!hyp || !hyp.declared) { box.hidden = true; return; }
  box.hidden = false;
  const badge = $('hyp-badge');
  badge.textContent = hyp.satisfied ? 'PASA' : 'FALLA';
  badge.className = 'badge ' + (hyp.satisfied ? 'pass' : 'fail');
  const list = $('hyp-list');
  list.innerHTML = '';
  (hyp.results || []).forEach(r => {
    const li = document.createElement('li');
    li.className = r.satisfied ? 'ok' : 'no';
    const sym = COMPARISON_SYMBOL[r.comparison] || r.comparison;
    li.innerHTML = '<span class="mark">' + (r.satisfied ? '✓' : '✗') + '</span>'
      + '<span>' + r.metric.toLowerCase() + ' ' + sym + ' ' + fmtNum(r.threshold)
      + ' (obs. ' + fmtNum(r.actual) + ')</span>';
    list.appendChild(li);
  });
}

function replay(resp) {
  currentResp = resp;
  currentTimeline = resp.report.timeline;
  currentEdges = resp.topology.edges;
  setText('m-generated', resp.report.generatedRequests);
  setText('m-success', '—');
  $('reasons').textContent = '';
  renderHypothesis(resp.hypothesis);
  const scrubber = $('scrubber');
  scrubber.max = String(Math.max(0, currentTimeline.length - 1));
  scrubber.value = '0';
  startPlayback(0);
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
  const ownYaml = $('yaml').value.trim();
  const body = {
    seed: seedValue === '' ? null : Number(seedValue),
    faults: faultSpec === '' ? [] : [faultSpec]
  };
  if (ownYaml !== '') {
    body.yaml = ownYaml;
  } else {
    body.topology = $('topology').value;
  }
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
  // Crítico primero (no depende de Chart.js): listeners + carga de topologías (que dibuja el grafo).
  $('run').addEventListener('click', run);
  $('topology').addEventListener('change', previewTopology);
  $('yaml').addEventListener('blur', previewTopology);
  $('playpause').addEventListener('click', togglePlay);
  $('scrubber').addEventListener('input', () => {
    if (!currentTimeline.length) { return; }
    clearInterval(timer);
    playing = false;
    setPlayLabel();
    renderFrame(Number($('scrubber').value));
  });
  document.querySelectorAll('.quick button').forEach(btn => {
    btn.addEventListener('click', () => { $('fault').value = btn.dataset.fault; });
  });
  $('yaml-file').addEventListener('change', (event) => {
    const file = event.target.files[0];
    if (!file) { return; }
    const reader = new FileReader();
    reader.onload = () => {
      $('yaml').value = reader.result;
      document.querySelector('.own-yaml').open = true;
      status('YAML cargado: ' + file.name + ' (se usará al correr).');
      previewTopology();
    };
    reader.readAsText(file);
  });
  loadTopologies();
  // Gráficas: no crítico. Si Chart.js no cargó, se omiten sin romper el resto.
  try {
    initCharts();
  } catch (e) {
    console.warn('initCharts falló; el dashboard sigue funcionando sin gráficas.', e);
  }
});
