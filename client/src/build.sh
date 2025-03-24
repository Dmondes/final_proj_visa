#!/bin/bash
set -e 

echo "Starting Angular build process..."

ng build --configuration production

echo "Angular build completed. Updating service worker cache manifest..."

#build output directory
BUILD_DIR="dist/client/browser"

# generated files
MAIN_JS=$(find $BUILD_DIR -name "main-*.js" | xargs basename 2>/dev/null || echo "")
POLYFILLS_JS=$(find $BUILD_DIR -name "polyfills-*.js" | xargs basename 2>/dev/null || echo "")
STYLES_CSS=$(find $BUILD_DIR -name "styles-*.css" | xargs basename 2>/dev/null || echo "")
SCRIPTS_JS=$(find $BUILD_DIR -name "scripts-*.js" | xargs basename 2>/dev/null || echo "")

# Check if files were found
if [ -z "$MAIN_JS" ]; then
  echo "Warning: main-*.js not found in $BUILD_DIR"
fi

if [ -z "$POLYFILLS_JS" ]; then
  echo "Warning: polyfills-*.js not found in $BUILD_DIR"
fi

if [ -z "$STYLES_CSS" ]; then
  echo "Warning: styles-*.css not found in $BUILD_DIR"
fi

# Build the new precacheAndRoute array content
NEW_PRECACHE_ARRAY="["
NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY}\n    { url: '/index.html', revision: '1' }"

if [ -n "$MAIN_JS" ]; then
  NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY},\n    { url: '/$MAIN_JS', revision: '1' }"
fi

if [ -n "$POLYFILLS_JS" ]; then
  NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY},\n    { url: '/$POLYFILLS_JS', revision: '1' }"
fi

if [ -n "$STYLES_CSS" ]; then
  NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY},\n    { url: '/$STYLES_CSS', revision: '1' }"
fi

if [ -n "$SCRIPTS_JS" ]; then
  NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY},\n    { url: '/$SCRIPTS_JS', revision: '1' }"
fi

NEW_PRECACHE_ARRAY="${NEW_PRECACHE_ARRAY}\n  ]"

# File to update
SERVICE_WORKER="src/service-worker.js"

# sed replace the precacheAndRoute array
cat "$SERVICE_WORKER" | 
  sed -E "/workbox.precaching.precacheAndRoute\(\[/,/\]\);/ c\\
  workbox.precaching.precacheAndRoute($NEW_PRECACHE_ARRAY);" > "${SERVICE_WORKER}.tmp"

mv "${SERVICE_WORKER}.tmp" "$SERVICE_WORKER"

echo "Service worker updated with the following files:"
if [ -n "$MAIN_JS" ]; then echo "- $MAIN_JS"; fi
if [ -n "$POLYFILLS_JS" ]; then echo "- $POLYFILLS_JS"; fi
if [ -n "$STYLES_CSS" ]; then echo "- $STYLES_CSS"; fi
if [ -n "$SCRIPTS_JS" ]; then echo "- $SCRIPTS_JS"; fi

# Copy service worker to build output
echo "Copying service worker to build output..."
cp $SERVICE_WORKER $BUILD_DIR/

echo "Build process completed successfully!"