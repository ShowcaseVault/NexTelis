import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  // GitHub Pages serves this from /NexTelis/, not the domain root. The nginx
  // build serves it from / instead, so the workflow sets BASE_PATH and
  // everything else (dev server, Docker) keeps the default.
  base: process.env.BASE_PATH || "/",
  plugins: [react()],
  server: {
    // Mirrors what nginx does in production (frontend/nginx.conf), so the
    // dev server and the built site behave the same. Targets the host-run
    // backend from `make dev`; if you're running the containerized one
    // instead, it publishes the same port, so this works either way.
    proxy: {
      "/api": {
        target: "http://localhost:8000",
        changeOrigin: true,
      },
    },
  },
});
