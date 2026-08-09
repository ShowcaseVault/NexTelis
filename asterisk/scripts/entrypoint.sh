#!/usr/bin/env bash
set -euo pipefail

DYNAMIC_DIR="/etc/asterisk/dynamic"
GENERATED="$DYNAMIC_DIR/dynamic_users.conf"

mkdir -p "$DYNAMIC_DIR"
touch "$GENERATED"

rebuild() {
    echo "; AUTO-GENERATED - do not edit" > "$GENERATED"
    for f in "$DYNAMIC_DIR"/user_*.conf; do
        [ -f "$f" ] || continue
        echo "#include \"$f\"" >> "$GENERATED"
    done
    asterisk -rx "module reload res_pjsip.so" 2>/dev/null || true
    echo "[watcher] Reloaded: $(date)"
}

rebuild

asterisk -f -C /etc/asterisk/asterisk.conf &
AST_PID=$!

sleep 3

# Watch for creates AND deletes
inotifywait -m -e close_write,create,delete,moved_to,moved_from "$DYNAMIC_DIR" \
    --format '%e %f' 2>/dev/null |
while read -r event file; do
    # Only react to user configs, ignore dynamic_users.conf itself
    [[ "$file" == user_*.conf ]] || continue
    # Skip the write event that comes right after create (debounce)
    [[ "$event" == "CLOSE_WRITE,CLOSE" ]] && sleep 0.5
    echo "[watcher] $event → $file"
    rebuild
done &

wait $AST_PID