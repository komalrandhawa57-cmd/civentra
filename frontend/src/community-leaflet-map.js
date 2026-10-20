import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const DEFAULT_TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const DEFAULT_CENTER = [0, 0];
const DEFAULT_ZOOM = 2;

class CivCommunityMap extends HTMLElement {
  constructor() {
    super();
    this._issuesJson = '[]';
    this._tileUrl = DEFAULT_TILE_URL;
    this._attribution = '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap contributors</a>';
    this._contrastView = false;
    this._defaultCenter = [...DEFAULT_CENTER];
    this.markers = [];
  }

  connectedCallback() {
    if (this.map) return;
    this.innerHTML = '<div class="civ-leaflet-map" role="region" aria-label="Community issue map"></div><div class="civ-leaflet-empty" hidden></div><div class="civ-leaflet-error" hidden></div>';
    this.mapElement = this.querySelector('.civ-leaflet-map');
    this.emptyElement = this.querySelector('.civ-leaflet-empty');
    this.errorElement = this.querySelector('.civ-leaflet-error');
    this.map = L.map(this.mapElement, { zoomControl: false, scrollWheelZoom: true }).setView(this._defaultCenter, DEFAULT_ZOOM);
    this.tileLayer = L.tileLayer(this._tileUrl, {
      maxZoom: 19,
      attribution: this._attribution
    }).addTo(this.map);
    this.tileLayer.on('tileerror', () => {
      if (this.errorElement.hidden) {
        this.errorElement.textContent = 'Map tiles are temporarily unavailable. Your issue list is still available.';
        this.errorElement.hidden = false;
      }
    });
    this.resizeObserver = new ResizeObserver(() => this.map.invalidateSize({ pan: false }));
    this.resizeObserver.observe(this);
    this.renderMarkers(true);
  }

  disconnectedCallback() {
    if (this.resizeObserver) this.resizeObserver.disconnect();
    if (this.map) this.map.remove();
    this.map = null;
  }

  set issuesJson(value) {
    this._issuesJson = value || '[]';
    this.renderMarkers(true);
  }

  get issuesJson() { return this._issuesJson; }

  set tileUrl(value) {
    this._tileUrl = value || DEFAULT_TILE_URL;
    if (this.tileLayer) this.tileLayer.setUrl(this._tileUrl);
  }

  get tileUrl() { return this._tileUrl; }

  set attribution(value) {
    this._attribution = value || '';
    if (this.tileLayer) this.tileLayer.setAttribution(this._attribution);
  }

  get attribution() { return this._attribution; }

  set defaultLatitude(value) {
    const latitude = Number(value);
    if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90 || (latitude === 0 && this._defaultCenter[1] === 0)) return;
    this._defaultCenter[0] = latitude;
    if (this.map && this.getIssues().length === 0) this.map.setView(this._defaultCenter, 12);
  }

  get defaultLatitude() { return this._defaultCenter[0]; }

  set defaultLongitude(value) {
    const longitude = Number(value);
    if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180 || (longitude === 0 && this._defaultCenter[0] === 0)) return;
    this._defaultCenter[1] = longitude;
    if (this.map && this.getIssues().length === 0) this.map.setView(this._defaultCenter, 12);
  }

  get defaultLongitude() { return this._defaultCenter[1]; }

  set contrastView(value) {
    this._contrastView = Boolean(value);
    this.classList.toggle('contrast-view', this._contrastView);
  }

  get contrastView() { return this._contrastView; }

  zoomIn() { if (this.map) this.map.zoomIn(); }
  zoomOut() { if (this.map) this.map.zoomOut(); }

  recenter() {
    if (!this.map) return;
    const points = this.getIssues();
    if (points.length === 1) this.map.setView([points[0].latitude, points[0].longitude], 15);
    else if (points.length > 1) this.map.fitBounds(L.latLngBounds(points.map(point => [point.latitude, point.longitude])), { padding: [36, 36], maxZoom: 15 });
    else this.map.setView(this._defaultCenter, this._defaultCenter[0] === 0 && this._defaultCenter[1] === 0 ? DEFAULT_ZOOM : 12);
  }

  locateUser() {
    if (!this.map || !navigator.geolocation) {
      if (this.errorElement) {
        this.errorElement.textContent = 'Location is unavailable in this browser. You can still browse the community map.';
        this.errorElement.hidden = false;
      }
      return;
    }
    navigator.geolocation.getCurrentPosition(position => {
      const point = [position.coords.latitude, position.coords.longitude];
      this.map.setView(point, 15);
      if (this.userMarker) this.userMarker.remove();
      this.userMarker = L.circleMarker(point, {
        radius: 8, color: '#FFFFFF', weight: 3, fillColor: '#2563EB', fillOpacity: 1
      }).addTo(this.map).bindTooltip('Your location');
      if (this.errorElement) this.errorElement.hidden = true;
    }, error => {
      if (!this.errorElement) return;
      this.errorElement.textContent = error.code === error.PERMISSION_DENIED
        ? 'Location permission was denied. Allow location access in your browser and try again.'
        : 'Your location could not be determined. Check device location and try again.';
      this.errorElement.hidden = false;
    }, { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
  }

  getIssues() {
    try {
      const value = JSON.parse(this._issuesJson);
      return Array.isArray(value) ? value.filter(item => Number.isFinite(item.latitude) && Number.isFinite(item.longitude)) : [];
    } catch (_) {
      return [];
    }
  }

  renderMarkers(fit) {
    if (!this.map) return;
    const issues = this.getIssues();
    this.markers.forEach(marker => marker.remove());
    this.markers = [];
    this.emptyElement.hidden = issues.length > 0;
    this.emptyElement.textContent = issues.length ? '' : 'No reports have saved coordinates yet. See the nearby issue list.';

    issues.forEach(issue => {
      const marker = L.circleMarker([issue.latitude, issue.longitude], {
        radius: 9,
        color: this.statusColor(issue.status),
        weight: 3,
        fillColor: this.categoryColor(issue.category),
        fillOpacity: .92
      }).addTo(this.map);
      const tooltip = document.createElement('span');
      tooltip.textContent = `${issue.title} · ${issue.status.replaceAll('_', ' ')}`;
      marker.bindTooltip(tooltip, { direction: 'top' });
      marker.on('click', () => marker.bindPopup(this.createPopup(issue), { maxWidth: 300 }).openPopup());
      this.markers.push(marker);
    });

    if (fit && issues.length === 1) this.map.setView([issues[0].latitude, issues[0].longitude], 15);
    else if (fit && issues.length > 1) this.map.fitBounds(L.latLngBounds(issues.map(issue => [issue.latitude, issue.longitude])), { padding: [36, 36], maxZoom: 15 });
  }

  createPopup(issue) {
    const content = document.createElement('article');
    content.className = 'civ-map-popup';
    const title = document.createElement('h3');
    title.textContent = issue.title;
    const meta = document.createElement('p');
    meta.textContent = `${issue.category} · ${issue.status.replaceAll('_', ' ')} · ${issue.priority}`;
    const location = document.createElement('p');
    location.textContent = issue.ward;
    const description = document.createElement('p');
    description.textContent = issue.description;
    content.append(title, meta, location, description);
    return content;
  }

  categoryColor(value) {
    const category = (value || '').toLowerCase();
    if (category.includes('light') || category.includes('electric')) return '#FBBF24';
    if (category.includes('water') || category.includes('leak')) return '#38BDF8';
    if (category.includes('garbage') || category.includes('sanitation')) return '#10B981';
    if (category.includes('road') || category.includes('pothole') || category.includes('footpath')) return '#F43F5E';
    return '#9333EA';
  }

  statusColor(value) {
    const status = (value || '').toUpperCase();
    if (['RESOLVED', 'CLOSED', 'CITIZEN_CONFIRMED'].includes(status)) return '#10B981';
    if (['IN_PROGRESS', 'ASSIGNED', 'UNDER_REVIEW', 'VERIFIED'].includes(status)) return '#FBBF24';
    if (['REJECTED', 'CANCELLED'].includes(status)) return '#9CA3AF';
    return '#FFF7ED';
  }
}

customElements.define('civ-community-map', CivCommunityMap);
