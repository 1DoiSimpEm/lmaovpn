#!/bin/bash

# Script to start OpenVPN server
# This requires sudo password

VPN_DIR="$HOME/openvpn-server"
OPENVPN_BIN="/opt/homebrew/opt/openvpn/sbin/openvpn"

if [ ! -f "$VPN_DIR/server.conf" ]; then
    echo "Error: Server configuration not found!"
    echo "Please run ./setup-vpn-server.sh first"
    exit 1
fi

echo "Starting OpenVPN server..."
echo "Server config: $VPN_DIR/server.conf"
echo ""
echo "The server will run in the foreground."
echo "Press Ctrl+C to stop the server."
echo ""

# Check if server is already running
if pgrep -f "openvpn.*server.conf" > /dev/null; then
    echo "OpenVPN server is already running!"
    echo "PID: $(pgrep -f 'openvpn.*server.conf')"
    echo ""
    echo "To stop it, run:"
    echo "  sudo pkill -f 'openvpn.*server.conf'"
    exit 1
fi

# Start server
sudo "$OPENVPN_BIN" --config "$VPN_DIR/server.conf"

