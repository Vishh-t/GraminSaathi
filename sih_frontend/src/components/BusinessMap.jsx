import { useEffect, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { formatNumber } from '../utils/format';
import { ImageOff } from 'lucide-react';

// Fix for Leaflet default marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// `village` is the real VillageSearchResponse shape (name/district/state/population2011/households2011),
// not the old demo VillageResponse (villageName/population5kmRadius) - see MapPage.jsx for context.
const VillageMarker = ({ position, village }) => (
  <Marker position={position} icon={L.icon({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png', shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png', iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41] })}>
    <Popup>
      <div className="p-1">
        <p className="font-bold">{village.name}</p>
        <p className="text-sm text-gray-600">{village.block ? `${village.block}, ` : ''}{village.district}, {village.state}</p>
        {village.population2011 != null && (
          <p className="text-sm text-gray-600">Population (Census 2011): {formatNumber(village.population2011)}</p>
        )}
      </div>
    </Popup>
  </Marker>
);

const CompetitorMarker = ({ position }) => (
  <Marker position={position} icon={L.icon({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png', shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png', iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41] })}>
    <Popup>
      <div className="p-1 text-center">
        <p className="text-sm font-medium text-red-600">Existing Competitor</p>
      </div>
    </Popup>
  </Marker>
);

// MapContainer's `center`/`zoom` props are only read once, on mount —
// react-leaflet does NOT re-pan the view when they change on a later render
// (that's Leaflet's own design: the map is an imperative, stateful widget
// wrapped in a thin React shell). Without this, switching the village
// dropdown would move the markers but leave the viewport stuck on whichever
// village loaded first. This child renders nothing; it just reaches into the
// live Leaflet map instance via useMap() and flies the camera whenever the
// target coordinates change.
function MapRecenter({ center, zoom }) {
  const map = useMap();

  useEffect(() => {
    map.flyTo(center, zoom, { duration: 1.1 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [center[0], center[1]]);

  return null;
}

function generateCompetitorPositions(center, count, seed) {
  const positions = [];
  // Simple seeded random for consistent positions
  let random = seed;
  const seededRandom = () => {
    random = (random * 9301 + 49297) % 233280;
    return random / 233280;
  };

  for (let i = 0; i < count; i++) {
    const angle = seededRandom() * 2 * Math.PI;
    const radius = 0.01 + seededRandom() * 0.04; // ~1-4km in degrees
    positions.push([
      center[0] + radius * Math.cos(angle),
      center[1] + radius * Math.sin(angle),
    ]);
  }
  return positions;
}

// Headless map surface — intentionally has no card chrome, title, legend, or
// disclaimer of its own. The parent page owns that framing so we don't end up
// with two overlapping legends/disclaimers stacked on top of each other.
export default function BusinessMap({ village, competitorCount, className = '' }) {
  const [tileError, setTileError] = useState(false);
  const mapRef = useRef(null);

  const center = [village.latitude, village.longitude];
  // competitorCount is null for a real village (no per-category competitor-location API yet - see
  // MapPage.jsx) - generateCompetitorPositions(..., null, ...) already yields an empty array (`i < null`
  // compares as `i < 0`), so this stays a no-op scatter rather than fake pins.
  const competitorPositions = generateCompetitorPositions(center, competitorCount ?? 0, village.name.length * 1000);

  if (tileError) {
    return (
      <div className={`h-full min-h-[420px] flex items-center justify-center bg-gray-50 ${className}`}>
        <div className="text-center text-gray-500 p-4">
          <ImageOff className="w-12 h-12 mx-auto mb-2 text-gray-300" />
          <p className="mb-2">Unable to load map tiles</p>
          <p className="text-sm">Map requires internet connection</p>
          <button onClick={() => setTileError(false)} className="mt-2 btn-primary text-sm">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`h-full min-h-[420px] ${className}`}>
      <MapContainer
        ref={mapRef}
        center={center}
        zoom={13}
        scrollWheelZoom={true}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          eventHandlers={{ tileerror: () => setTileError(true) }}
        />
        <MapRecenter center={center} zoom={13} />
        <VillageMarker position={center} village={village} />
        {competitorPositions.map((pos, i) => (
          <CompetitorMarker key={i} position={pos} />
        ))}
        <Circle
          center={center}
          radius={5000}
          color="#16a34a"
          fillColor="#16a34a"
          fillOpacity={0.05}
          weight={1}
          dashArray="5, 5"
        />
      </MapContainer>
    </div>
  );
}
