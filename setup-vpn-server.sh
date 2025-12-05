#!/bin/bash

# OpenVPN Server Setup Script for macOS
# This script sets up an OpenVPN server on your Mac that your Android phone can connect to

set -e

VPN_DIR="$HOME/openvpn-server"
EASYRSA_DIR="$VPN_DIR/easy-rsa"
SERVER_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "127.0.0.1")
SERVER_PORT=1194

echo "=========================================="
echo "OpenVPN Server Setup for macOS"
echo "=========================================="
echo ""

# Check if running as root (needed for some operations)
if [ "$EUID" -eq 0 ]; then 
   echo "Please don't run this script as root"
   exit 1
fi

# Install OpenVPN if not installed
if ! command -v openvpn &> /dev/null && [ ! -f /opt/homebrew/opt/openvpn/sbin/openvpn ]; then
    echo "Installing OpenVPN via Homebrew..."
    brew install openvpn
fi

# Set OpenVPN path
OPENVPN_BIN="/opt/homebrew/opt/openvpn/sbin/openvpn"
if [ ! -f "$OPENVPN_BIN" ]; then
    OPENVPN_BIN=$(which openvpn)
fi

# Install easy-rsa for certificate generation
if ! command -v easyrsa &> /dev/null && [ ! -d /opt/homebrew/etc/easy-rsa ]; then
    echo "Installing easy-rsa via Homebrew..."
    brew install easy-rsa
fi

# Create VPN directory
echo "Creating VPN directory: $VPN_DIR"
mkdir -p "$VPN_DIR"
cd "$VPN_DIR"

# Initialize PKI
EASYRSA_PKI="$EASYRSA_DIR/pki"
if [ ! -d "$EASYRSA_PKI" ]; then
    echo "Initializing Easy-RSA..."
    mkdir -p "$EASYRSA_DIR"
    cd "$EASYRSA_DIR"
    
    # Set EASYRSA_PKI environment variable
    export EASYRSA_PKI="$EASYRSA_PKI"
    
    # Initialize PKI
    easyrsa init-pki
    
    # Build CA
    echo "Building Certificate Authority..."
    easyrsa --batch build-ca nopass
    
    # Build server certificate
    echo "Building server certificate..."
    easyrsa --batch build-server-full server nopass
    
    # Generate Diffie-Hellman parameters
    echo "Generating DH parameters (this may take a while)..."
    easyrsa gen-dh
    
    # Generate TLS auth key
    echo "Generating TLS auth key..."
    mkdir -p "$EASYRSA_PKI"
    $OPENVPN_BIN --genkey secret "$EASYRSA_PKI/ta.key"
fi

cd "$VPN_DIR"

# Create server configuration
echo "Creating server configuration..."
cat > server.conf <<EOF
# OpenVPN Server Configuration
port $SERVER_PORT
proto udp
dev tun

# Certificate files
ca $EASYRSA_DIR/pki/ca.crt
cert $EASYRSA_DIR/pki/issued/server.crt
key $EASYRSA_DIR/pki/private/server.key
dh $EASYRSA_DIR/pki/dh.pem
tls-auth $EASYRSA_DIR/pki/ta.key 0
# crl-verify $EASYRSA_DIR/pki/crl.pem  # Commented out - will be created when needed

# Network settings
server 10.8.0.0 255.255.255.0
push "redirect-gateway def1 bypass-dhcp"
push "dhcp-option DNS 8.8.8.8"
push "dhcp-option DNS 8.8.4.4"

# Security
cipher AES-256-CBC
auth SHA256
tls-version-min 1.2

# Connection settings
keepalive 10 120
persist-key
persist-tun
# comp-lzo deprecated, use allow-compression instead if needed

# Logging
verb 3
mute 20

# User authentication (optional - for username/password)
# auth-user-pass-verify /path/to/script via-env
EOF

# Create client configuration template
echo "Creating client configuration template..."
cat > client-template.ovpn <<EOF
# OpenVPN Client Configuration
# Generated for Android app

client
dev tun
proto udp
remote $SERVER_IP $SERVER_PORT
resolv-retry infinite
nobind
persist-key
persist-tun
comp-lzo
verb 3
mute 20

# Certificate files (will be embedded)
<ca>
# CA certificate will be inserted here
</ca>

<cert>
# Client certificate will be inserted here
</cert>

<key>
# Client key will be inserted here
</key>

<tls-auth>
# TLS auth key will be inserted here
</tls-auth>
key-direction 1

# Cipher settings
cipher AES-256-CBC
auth SHA256
EOF

# Generate client certificate
echo "Generating client certificate..."
cd "$EASYRSA_DIR"
export EASYRSA_PKI="$EASYRSA_PKI"
CLIENT_NAME="android-client"
easyrsa --batch build-client-full "$CLIENT_NAME" nopass

# Create client config with embedded certificates
echo "Creating client configuration file..."
cd "$VPN_DIR"
cat > android-client.ovpn <<EOF
# OpenVPN Client Configuration for Android
# Server: $SERVER_IP:$SERVER_PORT

client
dev tun
proto udp
remote $SERVER_IP $SERVER_PORT
resolv-retry infinite
nobind
persist-key
persist-tun
comp-lzo
verb 3

<ca>
$(cat $EASYRSA_DIR/pki/ca.crt)
</ca>

<cert>
$(cat $EASYRSA_DIR/pki/issued/$CLIENT_NAME.crt)
</cert>

<key>
$(cat $EASYRSA_DIR/pki/private/$CLIENT_NAME.key)
</key>

<tls-auth>
$(cat $EASYRSA_DIR/pki/ta.key)
</tls-auth>
key-direction 1

cipher AES-256-CBC
auth SHA256
EOF

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Server IP: $SERVER_IP"
echo "Server Port: $SERVER_PORT"
echo ""
echo "To start the VPN server, run:"
echo "  sudo openvpn --config $VPN_DIR/server.conf"
echo ""
echo "Client configuration saved to:"
echo "  $VPN_DIR/android-client.ovpn"
echo ""
echo "To add this server to your Android app:"
echo "  1. Copy the server IP and port: $SERVER_IP:$SERVER_PORT"
echo "  2. Add it as a server in your app"
echo ""
echo "Note: Make sure your Mac's firewall allows UDP port $SERVER_PORT"
echo "      System Preferences > Security & Privacy > Firewall > Firewall Options"
echo ""

