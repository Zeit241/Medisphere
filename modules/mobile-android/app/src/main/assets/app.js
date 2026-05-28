/* global maplibregl, turf */

(async function () {
const map = new maplibregl.Map({
container: 'map',
style: 'https://demotiles.maplibre.org/style.json',
center: [42.05962, 55.56294],
zoom: 19.5,
minZoom: 17,
maxZoom: 22,
pitch: 0,
bearing: 0
});

map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right');

const geojsonUrl = 'map_geojson.json';
const data = await fetch(geojsonUrl).then(r => r.json());

function separateFeatures(fc) {
const polygons = [];
const points = [];
for (const f of fc.features) {
if (f.geometry && f.geometry.type === 'Polygon') polygons.push(f);
if (f.geometry && f.geometry.type === 'Point') points.push(f);
}
return { polygons, points };
}

const { polygons, points } = separateFeatures(data);

function makeFeatureCollection(features) {
return { type: 'FeatureCollection', features };
}

function ensureSource(id, source) {
if (map.getSource(id)) return;
map.addSource(id, source);
}

function ensureLayer(layer) {
if (map.getLayer(layer.id)) return;
map.addLayer(layer);
}

function computeBBox(fc) {
try { return turf.bbox(fc); } catch (e) { return null; }
}

function fitToData() {
const bbox = computeBBox(data);
if (bbox) map.fitBounds([[bbox[0], bbox[1]], [bbox[2], bbox[3]]], { padding: 40, maxZoom: 20 });
}

function largestPolygon(polys) {
let best = null;
let bestArea = -Infinity;
for (const f of polys) {
try {
const a = turf.area(f);
if (a > bestArea) { bestArea = a; best = f; }
} catch (_) {}
}
return best;
}

function unionPolygons(features) {
let acc = null;
for (const f of features) {
acc = acc ? turf.union(acc, f) : f;
}
return acc;
}

function computeWalkable(polys) {
const outer = largestPolygon(polys);
if (!outer) return null;
const rooms = polys.filter(f => f !== outer);
if (rooms.length === 0) return outer;
let roomsUnion = null;
try { roomsUnion = unionPolygons(rooms); } catch (_) {}
if (!roomsUnion) return outer;
try {
const diff = turf.difference(outer, roomsUnion);
return diff || outer;
} catch (_) {
return outer;
}
}

function erodeAreaMeters(area, meters) {
if (!area || !meters) return area;
try {
const buf = turf.buffer(area, -Math.abs(meters), { units: 'meters', steps: 8 });
if (!buf) return null;
if (buf.geometry && buf.geometry.type === 'MultiPolygon') {
// pick largest piece to avoid tiny fragments
const parts = buf.geometry.coordinates.map(coords => ({ type: 'Feature', properties: {}, geometry: { type: 'Polygon', coordinates: coords } }));
return largestPolygon(parts) || null;
}
return buf;
} catch (_) {
return null;
}
}

function computeRoutingArea(walkable) {
const candidates = [1.0, 0.8, 0.6, 0.4, 0.3, 0.2, 0.1, 0];
for (const m of candidates) {
const eroded = m === 0 ? walkable : erodeAreaMeters(walkable, m);
if (eroded) return eroded;
}
return walkable;
}

let walkableArea = null;
let routingArea = null;

map.on('load', () => {
ensureSource('building-polygons', { type: 'geojson', data: makeFeatureCollection(polygons) });
ensureSource('poi-points', { type: 'geojson', data: makeFeatureCollection(points) });

ensureLayer({ id: 'polygons-fill', type: 'fill', source: 'building-polygons', paint: { 'fill-color': ['coalesce', ['get', ['get', 'fillcolor'], ['get', 'styledata'], ['get', 'styledetails']], '#627BC1'], 'fill-opacity': 0.35 } });
ensureLayer({ id: 'polygons-outline', type: 'line', source: 'building-polygons', paint: { 'line-color': '#333', 'line-width': 1.5 } });

ensureLayer({ id: 'poi', type: 'circle', source: 'poi-points', paint: { 'circle-radius': 4, 'circle-color': '#B42222' } });

ensureSource('route', { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
ensureLayer({ id: 'route-layer', type: 'line', source: 'route', paint: { 'line-color': '#13b113', 'line-width': 5 } });

walkableArea = computeWalkable(polygons);
routingArea = computeRoutingArea(walkableArea);

initUI(makeFeatureCollection(points));
fitToData();
});

const startSelect = document.getElementById('startSelect');
const endSelect = document.getElementById('endSelect');
const routeBtn = document.getElementById('routeBtn');
const infoEl = document.getElementById('info');

function initUI(pointsFC) {
function nameForPoint(f, idx) {
const p = f.properties || {};
const name = p.name || p.title || p.label || p.room || p.cabinet || p.cab || p.number || p['номер'] || p['кабинет'];
return name ? String(name) : `Точка ${idx+1}`;
}
startSelect.innerHTML = '';
endSelect.innerHTML = '';
pointsFC.features.forEach((f, i) => {
const name = nameForPoint(f, i);
const opt1 = document.createElement('option');
opt1.value = String(i);
opt1.textContent = name;
startSelect.appendChild(opt1);
const opt2 = document.createElement('option');
opt2.value = String(i);
opt2.textContent = name;
endSelect.appendChild(opt2);
});

if (pointsFC.features.length >= 2) {
startSelect.value = '0';
endSelect.value = String(pointsFC.features.length - 1);
}

routeBtn.onclick = () => {
const s = parseInt(startSelect.value, 10);
const t = parseInt(endSelect.value, 10);
if (Number.isNaN(s) || Number.isNaN(t)) return;
computeAndRenderRoute(pointsFC.features[s], pointsFC.features[t]);
};
}

function toXY(lonlat) {
const p = map.project(new maplibregl.LngLat(lonlat[0], lonlat[1]));
return [p.x, p.y];
}

function toLonLat(xy) {
const p = map.unproject({ x: xy[0], y: xy[1] });
return [p.lng, p.lat];
}

function gridFromWalkable(area, desiredCols) {
if (!area) return null;
const bbox = turf.bbox(area);
const minX = bbox[0], minY = bbox[1], maxX = bbox[2], maxY = bbox[3];
const width = maxX - minX;
const height = maxY - minY;
const cols = Math.max(10, desiredCols || 140);
const cellW = width / cols;
const rows = Math.max(10, Math.round(height / cellW));
const cellH = height / rows;
const walkable = new Array(rows);
for (let r = 0; r < rows; r++) {
walkable[r] = new Array(cols).fill(false);
for (let c = 0; c < cols; c++) {
const cx = minX + (c + 0.5) * cellW;
const cy = minY + (r + 0.5) * cellH;
try {
const inside = turf.booleanPointInPolygon([cx, cy], area);
walkable[r][c] = !!inside;
} catch (_) {
walkable[r][c] = false;
}
}
}
return { minX, minY, maxX, maxY, rows, cols, cellW, cellH, walkable };
}

function lonlatToCell(grid, coord) {
const c = Math.floor((coord[0] - grid.minX) / grid.cellW);
const r = Math.floor((coord[1] - grid.minY) / grid.cellH);
return { r, c };
}

function cellToLonlat(grid, r, c) {
const x = grid.minX + (c + 0.5) * grid.cellW;
const y = grid.minY + (r + 0.5) * grid.cellH;
return [x, y];
}

function clampCell(grid, r, c) {
const rr = Math.max(0, Math.min(grid.rows - 1, r));
const cc = Math.max(0, Math.min(grid.cols - 1, c));
return { r: rr, c: cc };
}

function nearestWalkable(grid, r, c) {
const start = clampCell(grid, r, c);
if (grid.walkable[start.r][start.c]) return start;
const deltas = [0,1,2,3,4,5,6,7,8,9,10,12,14,16,18,20];
for (const d of deltas) {
for (let dr = -d; dr <= d; dr++) {
const dc = d - Math.abs(dr);
const candidates = [
{ r: start.r + dr, c: start.c + dc },
{ r: start.r + dr, c: start.c - dc }
];
for (const cand of candidates) {
const cl = clampCell(grid, cand.r, cand.c);
if (grid.walkable[cl.r][cl.c]) return cl;
}
}
}
return null;
}

function isSegmentInsidePolygon(area, a, b) {
const steps = 16;
for (let i = 1; i < steps; i++) { // skip exact endpoints to avoid boundary precision issues
const t = i / steps;
const x = a[0] + (b[0] - a[0]) * t;
const y = a[1] + (b[1] - a[1]) * t;
try {
if (!turf.booleanPointInPolygon([x, y], area)) return false;
} catch (_) {
return false;
}
}
return true;
}

function astar(grid, start, goal, area) {
const key = (r, c) => r + ':' + c;
const open = new Map();
const g = new Map();
const f = new Map();
const came = new Map();
function h(r, c) { const dr = Math.abs(r - goal.r), dc = Math.abs(c - goal.c); return Math.sqrt(dr*dr + dc*dc); }
const startK = key(start.r, start.c);
g.set(startK, 0);
f.set(startK, h(start.r, start.c));
open.set(startK, { r: start.r, c: start.c });
const dirs = [ [1,0,1], [-1,0,1], [0,1,1], [0,-1,1], [1,1,Math.SQRT2], [1,-1,Math.SQRT2], [-1,1,Math.SQRT2], [-1,-1,Math.SQRT2] ];
while (open.size) {
let curK = null; let curF = Infinity; let cur = null;
for (const [k, v] of open.entries()) {
const fv = f.get(k) ?? Infinity;
if (fv < curF) { curF = fv; curK = k; cur = v; }
}
if (!cur) break;
if (cur.r === goal.r && cur.c === goal.c) {
const path = [];
let k = curK;
while (k) {
const [rs, cs] = k.split(':').map(Number);
path.push({ r: rs, c: cs });
k = came.get(k);
}
path.reverse();
return path;
}
open.delete(curK);
for (const d of dirs) {
const nr = cur.r + d[0];
const nc = cur.c + d[1];
if (nr < 0 || nr >= grid.rows || nc < 0 || nc >= grid.cols) continue;
if (!grid.walkable[nr][nc]) continue;
if (d[0] !== 0 && d[1] !== 0) {
const r1 = cur.r + d[0], c1 = cur.c;
const r2 = cur.r, c2 = cur.c + d[1];
if (!grid.walkable[r1][c1] || !grid.walkable[r2][c2]) continue;
}
const a = cellToLonlat(grid, cur.r, cur.c);
const b = cellToLonlat(grid, nr, nc);
if (!isSegmentInsidePolygon(area, a, b)) continue;
const nk = key(nr, nc);
const tentativeG = (g.get(curK) ?? Infinity) + d[2];
if (tentativeG < (g.get(nk) ?? Infinity)) {
came.set(nk, curK);
g.set(nk, tentativeG);
f.set(nk, tentativeG + h(nr, nc));
open.set(nk, { r: nr, c: nc });
}
}
}
return null;
}

function smoothByLineOfSight(coords, area) {
if (coords.length <= 2) return coords;
const out = [coords[0]];
let i = 0;
while (i < coords.length - 1) {
let j = coords.length - 1;
while (j > i + 1 && !isSegmentInsidePolygon(area, coords[i], coords[j])) j--;
out.push(coords[j]);
i = j;
}
return out;
}

function computeAndRenderRoute(startF, endF) {
if (!routingArea) {
infoEl.textContent = 'Нет доступной области для маршрута';
map.getSource('route').setData({ type: 'FeatureCollection', features: [] });
return;
}
const grid = gridFromWalkable(routingArea, 160);
if (!grid) {
infoEl.textContent = 'Не удалось создать сетку';
map.getSource('route').setData({ type: 'FeatureCollection', features: [] });
return;
}
const sCell0 = lonlatToCell(grid, startF.geometry.coordinates);
const tCell0 = lonlatToCell(grid, endF.geometry.coordinates);
const sCell = nearestWalkable(grid, sCell0.r, sCell0.c);
const tCell = nearestWalkable(grid, tCell0.r, tCell0.c);
if (!sCell || !tCell) {
infoEl.textContent = 'Старт/финиш вне проходимой зоны';
map.getSource('route').setData({ type: 'FeatureCollection', features: [] });
return;
}
const cellPath = astar(grid, sCell, tCell, routingArea);
if (!cellPath) {
infoEl.textContent = 'Маршрут не найден';
map.getSource('route').setData({ type: 'FeatureCollection', features: [] });
return;
}
const coords = cellPath.map(p => cellToLonlat(grid, p.r, p.c));
const simplified = smoothByLineOfSight(coords, routingArea);
let finalCoords = simplified.slice();
// prepend start connector if visible inside walkable area or routing area
try {
if ((routingArea && isSegmentInsidePolygon(routingArea, startF.geometry.coordinates, simplified[0])) || isSegmentInsidePolygon(walkableArea, startF.geometry.coordinates, simplified[0])) {
finalCoords.unshift(startF.geometry.coordinates);
}
} catch (_) {}
// append end connector
try {
const last = simplified[simplified.length - 1];
if ((routingArea && isSegmentInsidePolygon(routingArea, last, endF.geometry.coordinates)) || isSegmentInsidePolygon(walkableArea, last, endF.geometry.coordinates)) {
finalCoords.push(endF.geometry.coordinates);
}
} catch (_) {}
const feature = { type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: finalCoords } };
map.getSource('route').setData({ type: 'FeatureCollection', features: [feature] });
infoEl.textContent = 'Готово';
}
})();
