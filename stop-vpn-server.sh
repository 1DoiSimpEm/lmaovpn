#!/bin/bash

# Script to stop OpenVPN server

echo "Stopping OpenVPN server..."

if pgrep -f "openvpn.*server.conf" > /dev/null; then
    PID=$(pgrep -f 'openvpn.*server.conf')
    echo "Found OpenVPN server process: $PID"
    sudo pkill -f 'openvpn.*server.conf'
    sleep 1
    
    if pgrep -f "openvpn.*server.conf" > /dev/null; then
        echo "Server stopped successfully"
    else
        echo "Server may still be running. Try: sudo killall openvpn"
    fi
else
    echo "OpenVPN server is not running"
fi

