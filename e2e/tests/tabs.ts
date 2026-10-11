/**
 * The demo tabs under test, mirroring the `tabs` sorted-map in
 * dev/cartoj/demo.cljs. Keep this list in sync when tabs are added/removed.
 *
 *   id    – URL hash fragment that selects the tab (#<id>)
 *   maps  – number of MapLibre maps the tab mounts (side-by-side mounts two)
 */
export interface TabSpec {
  id: string;
  maps: number;
}

export const TABS: TabSpec[] = [
  { id: 'barebones', maps: 1 },
  { id: 'basemaps', maps: 1 },
  { id: 'cluster', maps: 1 },
  { id: 'controls', maps: 1 },
  { id: 'drawing', maps: 1 },
  { id: 'dynamic-style', maps: 1 },
  { id: 'flyto', maps: 1 },
  { id: 'geocoder', maps: 1 },
  { id: 'geojson-http', maps: 1 },
  { id: 'geojson-manual', maps: 1 },
  { id: 'globe', maps: 1 },
  { id: 'heatmap', maps: 1 },
  { id: 'labels', maps: 1 },
  { id: 'layer-interact', maps: 1 },
  { id: 'layer-switcher', maps: 1 },
  { id: 'limit-interact', maps: 1 },
  { id: 'on-click-event', maps: 1 },
  { id: 'on-move-event', maps: 1 },
  { id: 'on-mouse-move-event', maps: 1 },
  { id: 'overlays', maps: 1 },
  { id: 'reframe', maps: 1 },
  { id: 'render-counter', maps: 1 },
  { id: 'side-by-side', maps: 2 },
  { id: 'sources', maps: 1 },
  { id: 'style-by-numeric', maps: 1 },
  { id: 'terrain', maps: 1 },
];
