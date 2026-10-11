#!/usr/bin/env node
'use strict';

/**
 * Assert that cartoj.props/props->js knows about every prop in the wrapped
 * react-map-gl (MapLibre) components.
 *
 * React-map-gl v8 publishes its component prop types in
 * `@vis.gl/react-maplibre/dist/components/*.d.ts`. This script reads those
 * declaration files with the TypeScript compiler API, extracts every prop
 * name from the components cartoj wraps, and checks each name against the
 * conversion keys declared in `src/cartoj/props.cljs` plus an explicit
 * pass-through allowlist below.
 *
 * Why this matters: props->js converts top-level kebab-case keyword keys to
 * camelCase, but nested map/vector values need a conversion category
 * (camelCase vs kebab-case). If a react-map-gl upgrade adds or renames a
 * prop, this script fails loudly instead of letting the new prop silently
 * pass through with a CLJS map/vector still attached.
 */

const fs = require('fs');
const os = require('os');
const path = require('path');
const ts = require('typescript');

const PROJECT_ROOT = path.join(__dirname, '..');
const COMPONENTS_DIR = path.join(
  PROJECT_ROOT,
  'node_modules',
  '@vis.gl',
  'react-maplibre',
  'dist',
  'components'
);
const PROPS_CLJS = path.join(PROJECT_ROOT, 'src', 'cartoj', 'props.cljs');

// The react-map-gl components that cartoj passes through cartoj.props/props->js.
// `type` names an exported `XxxProps` type alias; `component` names an
// exported component variable (used for MapProvider, which has no Props alias).
//
// Layer is intentionally absent: cartoj.core/layer uses clj->js directly
// because MapLibre layer props are style-spec kebab-case keys, not the
// camelCase component props handled by props->js.
const COMPONENTS = [
  { file: 'map', type: 'MapProps' },
  { file: 'marker', type: 'MarkerProps' },
  { file: 'popup', type: 'PopupProps' },
  { file: 'source', type: 'SourceProps' },
  { file: 'navigation-control', type: 'NavigationControlProps' },
  { file: 'geolocate-control', type: 'GeolocateControlProps' },
  { file: 'fullscreen-control', type: 'FullscreenControlProps' },
  { file: 'scale-control', type: 'ScaleControlProps' },
  { file: 'attribution-control', type: 'AttributionControlProps' },
  { file: 'terrain-control', type: 'TerrainControlProps' },
  { file: 'globe-control', type: 'GlobeControlProps' },
  { file: 'logo-control', type: 'LogoControlProps' },
  { file: 'use-map', component: 'MapProvider' },
];

// Props that props->js intentionally passes through unchanged. These are the
// scalar, callback, DOM-element, instance, and React-node props in the wrapped
// components. Object/array-valued props must instead be in
// camel-convert-keys or maplibre-spec-keys in props.cljs.
const PASSTHROUGH = [
  'anchor',
  'anisotropicFilterPitch',
  'aroundCenter',
  'attribution',
  'baseShift',
  'bearing',
  'bearingSnap',
  'blueFactor',
  'buffer',
  'cancelPendingTileRequestsWhileZooming',
  'centerClampedToGround',
  'children',
  'className',
  'clickTolerance',
  'closeButton',
  'closeOnClick',
  'closeOnMove',
  'cluster',
  'clusterMaxZoom',
  'clusterMinPoints',
  'clusterRadius',
  'collectResourceTiming',
  'color',
  'compact',
  'containerId',
  'cooperativeGestures',
  'crossSourceCollisions',
  'cursor',
  'doubleClickZoom',
  'draggable',
  'dragRotate',
  'element',
  'elevation',
  'encoding',
  'exaggeration',
  'experimentalZoomLevelsToOverscale',
  'fadeDuration',
  'focusAfterOpen',
  'generateId',
  'gl',
  'greenFactor',
  'hash',
  'id',
  'interactive',
  'keyboard',
  'latitude',
  'lineMetrics',
  'localIdeographFontFamily',
  'locationOccludedOpacity',
  'logoPosition',
  'longitude',
  'mapLib',
  'maplibreLogo',
  'maxParallelImageRequests',
  'maxPitch',
  'maxTileCacheSize',
  'maxTileCacheZoomLevels',
  'maxWidth',
  'maxzoom',
  'maxZoom',
  'minPitch',
  'minzoom',
  'minZoom',
  'onBoxZoomCancel',
  'onBoxZoomEnd',
  'onBoxZoomStart',
  'onClick',
  'onClose',
  'onContextMenu',
  'onData',
  'onDblClick',
  'onDrag',
  'onDragEnd',
  'onDragStart',
  'onError',
  'onGeolocate',
  'onIdle',
  'onLoad',
  'onMouseDown',
  'onMouseEnter',
  'onMouseLeave',
  'onMouseMove',
  'onMouseOut',
  'onMouseOver',
  'onMouseUp',
  'onMove',
  'onMoveEnd',
  'onMoveStart',
  'onOpen',
  'onOutOfMaxBounds',
  'onPitch',
  'onPitchEnd',
  'onPitchStart',
  'onRemove',
  'onRender',
  'onResize',
  'onRotate',
  'onRotateEnd',
  'onRotateStart',
  'onSourceData',
  'onStyleData',
  'onTouchCancel',
  'onTouchEnd',
  'onTouchMove',
  'onTouchStart',
  'onTrackUserLocationEnd',
  'onTrackUserLocationStart',
  'onWheel',
  'onZoom',
  'onZoomEnd',
  'onZoomStart',
  'opacity',
  'opacityWhenCovered',
  'pitch',
  'pitchAlignment',
  'pitchWithRotate',
  'pixelRatio',
  'popup',
  'position',
  'pseudo',
  'redFactor',
  'reduceMotion',
  'refreshExpiredTiles',
  'renderWorldCopies',
  'reuseMaps',
  'roll',
  'rollEnabled',
  'rotation',
  'rotationAlignment',
  'scale',
  'scheme',
  'showAccuracyCircle',
  'showCompass',
  'showUserLocation',
  'showZoom',
  'source',
  'styleDiffing',
  'subpixelPositioning',
  'tileSize',
  'tolerance',
  'trackResize',
  'trackUserLocation',
  'transformCameraUpdate',
  'transformConstrain',
  'transformRequest',
  'type',
  'unit',
  'url',
  'validateStyle',
  'visualizePitch',
  'visualizeRoll',
  'volatile',
  'workerCount',
  'workerUrl',
  'zoom',
  'zoomSnap',
];

// ---------------------------------------------------------------------------
// Parse the conversion keys from props.cljs so the coverage check can never
// drift from the actual implementation.
// ---------------------------------------------------------------------------

function kebabToCamel(name) {
  const parts = name.split('-');
  return (
    parts[0] +
    parts
      .slice(1)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join('')
  );
}

function readKeywordSet(source, setName) {
  const re = new RegExp('\\(def \\^:private ' + setName + '\\s+#\\{([\\s\\S]*?)\\}\\)');
  const match = source.match(re);
  if (!match) {
    throw new Error('Could not find ' + setName + ' in ' + PROPS_CLJS);
  }
  return new Set(
    (match[1].match(/:[a-zA-Z][\w-]*/g) || []).map((kw) => kw.slice(1))
  );
}

// ---------------------------------------------------------------------------
// Extract prop names from the react-map-gl .d.ts files with TypeScript.
// ---------------------------------------------------------------------------

function reactStubSource() {
  return `
declare module 'react' {
  export type CSSProperties = any;
  export type ReactNode = any;
  export type RefAttributes<T> = { ref?: any };
  export type ForwardRefExoticComponent<P> = (props: P) => any;
  export type MemoExoticComponent<T> = T;
  export type FC<P> = (props: P) => any;
  export const createElement: any;
  export default createElement;
}
`;
}

function createProgram() {
  const stubPath = path.join(os.tmpdir(), 'cartoj-react-stub.d.ts');
  fs.writeFileSync(stubPath, reactStubSource());

  const rootNames = fs
    .readdirSync(COMPONENTS_DIR)
    .filter((f) => f.endsWith('.d.ts'))
    .map((f) => path.join(COMPONENTS_DIR, f));
  rootNames.push(stubPath);

  const options = {
    target: ts.ScriptTarget.ES2020,
    module: ts.ModuleKind.NodeNext,
    moduleResolution: ts.ModuleResolutionKind.NodeNext,
    allowJs: false,
    skipLibCheck: true,
    noEmit: true,
    baseUrl: os.tmpdir(),
    paths: { react: [path.basename(stubPath)] },
    lib: ['lib.es2020.d.ts', 'lib.dom.d.ts'],
    types: [],
  };

  const program = ts.createProgram(rootNames, options);
  const diags = ts
    .getPreEmitDiagnostics(program)
    .filter((d) => d.category === ts.DiagnosticCategory.Error);

  if (diags.length > 0) {
    for (const d of diags.slice(0, 20)) {
      const file = d.file ? d.file.fileName : '<unknown>';
      console.error(
        'typescript diagnostic: ' +
          file +
          ': ' +
          ts.flattenDiagnosticMessageText(d.messageText, '\n')
      );
    }
    throw new Error('TypeScript diagnostics while reading react-map-gl .d.ts files');
  }

  return program;
}

function findTypeAlias(sourceFile, name) {
  let found;
  function visit(node) {
    if (ts.isTypeAliasDeclaration(node) && node.name.text === name) {
      found = node;
      return;
    }
    ts.forEachChild(node, visit);
  }
  visit(sourceFile);
  return found;
}

function findVariable(sourceFile, name) {
  let found;
  function visit(node) {
    if (
      ts.isVariableDeclaration(node) &&
      ts.isIdentifier(node.name) &&
      node.name.text === name
    ) {
      found = node;
      return;
    }
    ts.forEachChild(node, visit);
  }
  visit(sourceFile);
  return found;
}

function collectPropNames(checker, type, seen = new Set()) {
  if (seen.has(type)) {
    return [];
  }
  seen.add(type);

  // react-map-gl's SourceProps/LayerProps intersect a union of style-spec
  // types. getPropertiesOfType on the intersection returns only the common
  // properties (id/type), which would hide most props. Walk union and
  // intersection constituents explicitly so every possible prop is checked.
  if (type.flags & (ts.TypeFlags.Union | ts.TypeFlags.Intersection)) {
    const out = new Set();
    for (const member of type.types) {
      for (const name of collectPropNames(checker, member, seen)) {
        out.add(name);
      }
    }
    return [...out];
  }

  return checker.getPropertiesOfType(type).map((p) => p.name);
}

function propNamesFromType(checker, type) {
  return collectPropNames(checker, type).sort();
}

function extractProps(program, entry) {
  const checker = program.getTypeChecker();
  const fileName = path.join(COMPONENTS_DIR, entry.file + '.d.ts');
  const sourceFile = program.getSourceFile(fileName);
  if (!sourceFile) {
    throw new Error('Missing declaration file: ' + fileName);
  }

  if (entry.type) {
    const node = findTypeAlias(sourceFile, entry.type);
    if (!node) {
      throw new Error('Missing type ' + entry.type + ' in ' + fileName);
    }
    return propNamesFromType(checker, checker.getTypeAtLocation(node.name));
  }

  const node = findVariable(sourceFile, entry.component);
  if (!node) {
    throw new Error('Missing component ' + entry.component + ' in ' + fileName);
  }
  const type = checker.getTypeOfSymbolAtLocation(
    checker.getSymbolAtLocation(node.name),
    node.name
  );
  const signatures = checker.getSignaturesOfType(type, ts.SignatureKind.Call);
  if (signatures.length === 0) {
    throw new Error('No call signature for ' + entry.component);
  }
  const propsParam = signatures[0].getParameters()[0];
  if (!propsParam) {
    throw new Error('No props parameter for ' + entry.component);
  }
  const propsType = checker.getTypeOfSymbolAtLocation(
    propsParam,
    propsParam.valueDeclaration || node
  );
  return propNamesFromType(checker, propsType);
}

// ---------------------------------------------------------------------------
// Coverage assertion
// ---------------------------------------------------------------------------

function main() {
  if (!fs.existsSync(COMPONENTS_DIR)) {
    throw new Error(
      'react-map-gl type definitions not found at ' + COMPONENTS_DIR
    );
  }

  const propsSource = fs.readFileSync(PROPS_CLJS, 'utf8');
  const camelKeys = readKeywordSet(propsSource, 'camel-convert-keys');
  const maplibreKeys = readKeywordSet(propsSource, 'maplibre-spec-keys');
  const deepJs = new Set(
    [...camelKeys, ...maplibreKeys].map((name) => kebabToCamel(name))
  );

  const passthrough = new Set(PASSTHROUGH);
  if (passthrough.size !== PASSTHROUGH.length) {
    throw new Error('PASSTHROUGH contains duplicate prop names');
  }
  const overlap = [...passthrough].filter((name) => deepJs.has(name));
  if (overlap.length > 0) {
    throw new Error(
      'Props listed in both props.cljs conversion keys and PASSTHROUGH: ' +
        overlap.join(', ')
    );
  }

  const program = createProgram();
  const perComponent = {};
  const actual = new Set();

  for (const entry of COMPONENTS) {
    const props = extractProps(program, entry);
    const label = entry.component || entry.type;
    if (props.length === 0) {
      throw new Error(
        'No props extracted for ' + label + '; .d.ts type resolution may have failed'
      );
    }
    perComponent[label] = props;
    props.forEach((name) => actual.add(name));
  }

  const known = new Set([...deepJs, ...passthrough]);
  const missing = [...actual].filter((name) => !known.has(name));

  console.log(
    'prop coverage: ' +
      actual.size +
      ' react-map-gl props, ' +
      deepJs.size +
      ' deep-convert keys, ' +
      passthrough.size +
      ' pass-through props'
  );

  if (missing.length > 0) {
    console.error('\nprops->js coverage failure.');
    console.error(
      'The following react-map-gl props are not covered by camel-convert-keys,'
    );
    console.error('maplibre-spec-keys, or the PASSTHROUGH allowlist:\n');
    for (const name of missing) {
      const owners = COMPONENTS.filter((entry) => {
        const label = entry.component || entry.type;
        return (perComponent[label] || []).includes(name);
      }).map((entry) => entry.component || entry.type);
      console.error('  - ' + name + '  (' + owners.join(', ') + ')');
    }
    console.error(
      '\nAdd object/array-valued props to camel-convert-keys or maplibre-spec-keys'
    );
    console.error(
      'in src/cartoj/props.cljs; add scalar/callback/instance props to PASSTHROUGH'
    );
    console.error('in scripts/check-prop-coverage.js.\n');
    process.exit(1);
  }

  console.log('prop coverage OK');
}

main();
