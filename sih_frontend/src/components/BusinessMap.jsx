import { useEffect, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { formatNumber } from '../utils/format';

// Fix for Leaflet default marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const VillageMarker = ({ position, village }) => (
  <Marker position={position} icon={L.icon({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png', shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png', iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41] })}>
    <Popup>
      <div className="p-1">
        <p className="font-bold">{village.villageName}</p>
        <p className="text-sm text-gray-600">{village.block}, {village.district}, {village.state}</p>
        <p className="text-sm text-gray-600">Population: {formatNumber(village.population5kmRadius)}</p>
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

export default function BusinessMap({ village, competitorCount, className = '' }) {
  const [mapLoaded, setMapLoaded] = useState(false);
  const [tileError, setTileError] = useState(false);
  const mapRef = useRef(null);

  const center = [village.latitude, village.longitude];
  const competitorPositions = generateCompetitorPositions(center, competitorCount, village.villageName.length * 1000);

  useEffect(() => {
    if (mapRef.current) {
      setTimeout(() => mapRef.current.invalidateSize(), 100);
    }
  }, [mapLoaded]);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
        Local Business Map
      </h3>

      {!mapLoaded && !tileError ? (
        <div className="h-96 flex items-center justify-center bg-gray-50 rounded-lg">
          <div className="text-center text-gray-500">
            <svg className="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <p>Loading map...</p>
          </div>
        </div>
      ) : tileError ? (
        <div className="h-96 flex items-center justify-center bg-gray-50 rounded-lg">
          <div className="text-center text-gray-500 p-4">
            <svg className="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <p className="mb-2">Unable to load map tiles</p>
            <p className="text-sm">Map requires internet connection</p>
            <button onClick={() => { setTileError(false); setMapLoaded(false); }} className="mt-2 btn-primary text-sm">
              Retry
            </button>
          </div>
        </div>
      ) : (
        <div className="relative h-96 rounded-lg overflow-hidden">
          <MapContainer
            ref={mapRef}
            center={center}
            zoom={13}
            scrollWheelZoom={true}
            whenCreated={() => setMapLoaded(true)}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              onLoad={() => {}}
              onError={() => setTileError(true)}
            />
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
          
          {/* Legend */}
          <div className="absolute bottom-4 left-4 bg-white rounded-lg shadow-lg p-3 z-10">
            <div className="flex items-center gap-2 mb-1">
              <div className="w-4 h-4 rounded-full bg-blue-500 border-2 border-white shadow-sm" />
              <span className="text-sm font-medium">Village Center</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-red-500 border-2 border-white shadow-sm" />
              <span className="text-sm font-medium">{competitorCount} Competitors</span>
            </div>
          </div>

          {/* Disclaimer */}
          <div className="absolute bottom-4 right-4 bg-white/90 backdrop-blur-sm rounded-lg shadow-lg p-2 z-10 max-w-xs">
            <p className="text-xs text-gray-600 text-center">
              ⚠️ Competitor positions are illustrative — not real GPS locations
            </p>
          </div>
        </div>
      )}
    </div>
  );
}