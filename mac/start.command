#!/bin/zsh
set -e
cd "$(dirname "$0")"
if ! command -v swiftc >/dev/null 2>&1; then
    echo "Swift compiler is missing. Run: xcode-select --install"
    read -k 1
    exit 1
fi
if [[ ! -s .macswipe_token ]]; then
    /usr/bin/openssl rand -hex 20 > .macswipe_token
    /bin/chmod 600 .macswipe_token
fi
IP="$(ipconfig getifaddr en0 2>/dev/null || true)"
if [[ -z "$IP" ]]; then IP="$(ipconfig getifaddr en1 2>/dev/null || true)"; fi
if [[ -z "$IP" ]]; then
    echo "No Wi-Fi IPv4 address found on en0/en1. Connect the Mac to Wi-Fi."
    read -k 1
    exit 1
fi
if [[ ! -x .MacSwipeReceiver || MacReceiver.swift -nt .MacSwipeReceiver ]]; then
    echo "Building the macOS companion..."
    swiftc -O MacReceiver.swift -o .MacSwipeReceiver
fi
export MACSWIPE_TOKEN="$(cat .macswipe_token)"
export MACSWIPE_BIND_IP="$IP"
echo ""
echo "==========================================="
echo "MacSwipe — connect your Android phone"
echo "IP:       $IP"
echo "Port:     8765"
echo "Token:    $MACSWIPE_TOKEN"
echo "==========================================="
echo "Allow Accessibility permission if prompted, then restart this script."
echo "Both devices must use the same trusted Wi-Fi network."
echo "Do not port-forward 8765 to the internet."
echo ""
./.MacSwipeReceiver
