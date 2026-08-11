import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
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
