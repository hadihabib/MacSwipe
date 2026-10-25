import Foundation
import AppKit
import ApplicationServices
import CoreGraphics
import Darwin

// MacSwipe: LAN-only, token-authenticated input receiver for macOS.
// Protocol: newline-delimited UTF-8 JSON over TCP; first message is {"type":"hello","token":"..."}.
// Only predefined input commands are accepted; no shell commands are executed.

let port: UInt16 = 8765
let token = ProcessInfo.processInfo.environment["MACSWIPE_TOKEN"] ?? ""
let bindIP = ProcessInfo.processInfo.environment["MACSWIPE_BIND_IP"] ?? "127.0.0.1"
if token.count < 24 {
    fputs("Missing or too-short MACSWIPE_TOKEN. Start with ./start.command\n", stderr)
    exit(1)
}

let accessOptions: [String: Any] = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true]
if !AXIsProcessTrustedWithOptions(accessOptions as CFDictionary) {
    print("Accessibility access is required: System Settings -> Privacy & Security -> Accessibility.")
    print("After granting access to Terminal or MacSwipe, restart ./start.command.")
}

func sendLine(_ fd: Int32, _ text: String) {
    let bytes = Array((text + "\n").utf8)
    bytes.withUnsafeBytes { raw in
        if let ptr = raw.baseAddress { _ = Darwin.send(fd, ptr, bytes.count, 0) }
    }
}

func mousePosition() -> CGPoint { CGEvent(source: nil)?.location ?? CGPoint(x: 400, y: 300) }

final class InputController {
    private var dragging = false
    private let lock = NSLock()

    private func mouse(_ type: CGEventType, _ button: CGMouseButton = .left, at point: CGPoint? = nil) {
        let location = point ?? mousePosition()
        CGEvent(mouseEventSource: nil, mouseType: type,
                mouseCursorPosition: location, mouseButton: button)?.post(tap: .cghidEventTap)
    }

    private func keyboard(_ code: CGKeyCode, flags: CGEventFlags = []) {
        guard let down = CGEvent(keyboardEventSource: nil, virtualKey: code, keyDown: true),
              let up = CGEvent(keyboardEventSource: nil, virtualKey: code, keyDown: false) else { return }
        down.flags = flags
        up.flags = flags
        down.post(tap: .cghidEventTap)
        usleep(9000)
        up.post(tap: .cghidEventTap)
    }

    func releaseDrag() {
        lock.lock(); defer { lock.unlock() }
        if dragging { mouse(.leftMouseUp); dragging = false }
    }

    func handle(_ message: [String: Any]) {
        lock.lock(); defer { lock.unlock() }
        guard let type = message["type"] as? String else { return }
        let dx = max(-100.0, min(100.0, (message["dx"] as? NSNumber)?.doubleValue ?? 0))
        let dy = max(-100.0, min(100.0, (message["dy"] as? NSNumber)?.doubleValue ?? 0))
        switch type {
        case "move":
            let pos = mousePosition()
            mouse(dragging ? .leftMouseDragged : .mouseMoved,
                  at: CGPoint(x: pos.x + dx, y: pos.y + dy))
        case "click":
            mouse(.leftMouseDown)
            mouse(.leftMouseUp)
        case "right_click":
            mouse(.rightMouseDown, .right)
            mouse(.rightMouseUp, .right)
        case "scroll":
            let vertical = Int32(max(-120, min(120, (-dy * 2).rounded())))
            let horizontal = Int32(max(-120, min(120, (-dx * 2).rounded())))
            if vertical != 0 || horizontal != 0 {
                CGEvent(scrollWheelEvent2Source: nil, units: .pixel, wheelCount: 2,
                        wheel1: vertical, wheel2: horizontal, wheel3: 0)?.post(tap: .cghidEventTap)
            }
        case "drag_start":
            if !dragging { mouse(.leftMouseDown); dragging = true }
        case "drag_end":
            if dragging { mouse(.leftMouseUp); dragging = false }
        case "space_left": keyboard(123, flags: .maskControl) // Ctrl + Left
        case "space_right": keyboard(124, flags: .maskControl) // Ctrl + Right
        case "mission_control": keyboard(126, flags: .maskControl) // Ctrl + Up
        case "app_expose": keyboard(125, flags: .maskControl) // Ctrl + Down
        case "show_desktop": keyboard(99, flags: .maskCommand) // Cmd + F3 (Mission Control key)
        case "escape": keyboard(53)
        case "enter": keyboard(36)
        case "copy": keyboard(8, flags: .maskCommand)
        case "paste": keyboard(9, flags: .maskCommand)
        default: break
        }
    }
}

let input = InputController()

func processClient(_ fd: Int32) {
    defer { input.releaseDrag(); Darwin.close(fd) }
    var pending = Data()
    var authenticated = false
    var buffer = [UInt8](repeating: 0, count: 4096)
    while true {
        let count = recv(fd, &buffer, buffer.count, 0)
        if count <= 0 { break }
        pending.append(contentsOf: buffer[..<count])
        if pending.count > 16384 { break }
        while let index = pending.firstIndex(of: 10) { // newline
            let line = Data(pending[..<index])
            pending.removeSubrange(...index)
            guard let obj = try? JSONSerialization.jsonObject(with: line) as? [String: Any],
                  let type = obj["type"] as? String else { continue }
            if !authenticated {
                if type == "hello", let candidate = obj["token"] as? String, candidate == token {
                    authenticated = true
                    sendLine(fd, "{\"type\":\"ready\"}")
                } else {
                    sendLine(fd, "{\"type\":\"error\",\"message\":\"Invalid token\"}")
                    return
                }
            } else if type == "ping" {
                sendLine(fd, "{\"type\":\"pong\"}")
            } else {
                input.handle(obj)
            }
        }
    }
}

let server = Darwin.socket(AF_INET, SOCK_STREAM, 0)
if server < 0 { perror("socket"); exit(1) }
var reuse: Int32 = 1
_ = withUnsafePointer(to: &reuse) {
    setsockopt(server, SOL_SOCKET, SO_REUSEADDR, $0, socklen_t(MemoryLayout<Int32>.size))
}
var address = sockaddr_in()
address.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
address.sin_family = sa_family_t(AF_INET)
address.sin_port = port.bigEndian
let pton = bindIP.withCString { inet_pton(AF_INET, $0, &address.sin_addr) }
if pton != 1 { fputs("Invalid Wi-Fi IP: \(bindIP)\n", stderr); exit(1) }
let bindCode = withUnsafePointer(to: &address) { ptr in
    ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { socketAddr in
        Darwin.bind(server, socketAddr, socklen_t(MemoryLayout<sockaddr_in>.size))
    }
}
if bindCode != 0 { perror("bind"); exit(1) }
if Darwin.listen(server, 4) != 0 { perror("listen"); exit(1) }
print("MacSwipe is running at \(bindIP):\(port) | Ctrl+C to stop")
fflush(stdout)
while true {
    var address = sockaddr()
    var length = socklen_t(MemoryLayout<sockaddr>.size)
    let client = Darwin.accept(server, &address, &length)
    if client >= 0 {
        DispatchQueue.global(qos: .userInteractive).async { processClient(client) }
    }
}
