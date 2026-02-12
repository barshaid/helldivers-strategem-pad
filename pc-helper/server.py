import socket
import threading
import json
import ctypes
import time

# Windows API constants and functions for sending keys
VK_CONTROL = 0x11
VK_W = 0x57
VK_A = 0x41
VK_S = 0x53
VK_D = 0x44

KEYEVENTF_KEYUP = 0x0002

user32 = ctypes.windll.user32

# Map key names to virtual key codes
KEY_MAP = {
    "w": VK_W,
    "a": VK_A,
    "s": VK_S,
    "d": VK_D,
    "left ctrl": VK_CONTROL,
}

# Map logical directions to key names
DIRECTION_MAP = {
    "up": "w",
    "down": "s",
    "left": "a",
    "right": "d",
}


HOST = "0.0.0.0"
PORT = 50555  # You can change this if needed

# Track modifier state for Left Ctrl
_ctrl_is_down = False
_ctrl_lock = threading.Lock()


def handle_client(conn: socket.socket, addr):
    print(f"[CLIENT] Connected from {addr}")
    with conn:
        buffer = b""
        while True:
            data = conn.recv(4096)
            if not data:
                break
            buffer += data
            # Expect newline-delimited JSON messages
            while b"\n" in buffer:
                line, buffer = buffer.split(b"\n", 1)
                if not line.strip():
                    continue
                try:
                    msg = json.loads(line.decode("utf-8"))
                except json.JSONDecodeError:
                    print(f"[ERROR] Invalid JSON from {addr}: {line!r}")
                    continue
                process_message(msg)
    print(f"[CLIENT] Disconnected from {addr}")


def process_message(msg: dict):
    """
    Process messages from Android app.
    Supported message types:
    - "toggle_left_ctrl": Toggle Left Ctrl key
    - "direction_down": Hold a direction key (up/down/left/right)
    - "direction_up": Release a direction key
    - "strategem": Send a sequence of keys (used by test_client.py for testing)
    """
    msg_type = msg.get("type")
    if msg_type == "strategem":
        # Legacy/test support - not used by Android app but useful for testing
        name = msg.get("name", "unknown")
        sequence = msg.get("sequence", [])
        print(f"[STRATEGEM] {name}: {sequence}")
        send_sequence(sequence)
    elif msg_type == "toggle_left_ctrl":
        print("[TOGGLE] Left Ctrl requested")
        toggle_left_ctrl()
    elif msg_type in ("direction_down", "direction_up"):
        direction = msg.get("direction")
        key_name = DIRECTION_MAP.get(str(direction).lower())
        if key_name is None:
            print(f"[WARN] Unknown direction: {direction}")
            return
        vk_code = KEY_MAP.get(key_name)
        if vk_code is None:
            print(f"[WARN] No VK mapping for key: {key_name}")
            return
        is_down = msg_type == "direction_down"
        state = "DOWN" if is_down else "UP"
        print(f"[DIR] {direction} {state} (key {key_name})")
        send_key(vk_code, is_down)
    else:
        print(f"[INFO] Unknown message type: {msg_type}, content: {msg}")


def send_key(vk_code, is_press=True):
    """Send a key press or release using Windows API."""
    flags = 0 if is_press else KEYEVENTF_KEYUP
    user32.keybd_event(vk_code, 0, flags, 0)


def send_sequence(keys):
    """Send a sequence of keypresses to the system."""
    for key in keys:
        key_lower = str(key).lower()
        vk_code = KEY_MAP.get(key_lower)
        if vk_code is None:
            print(f"[WARN] Unknown key: {key}")
            continue
        print(f"[KEY] {key}")
        send_key(vk_code, True)
        time.sleep(0.05)  # Slight delay between press and release
        send_key(vk_code, False)


def toggle_left_ctrl():
    """Toggle the Left Ctrl key on/off."""
    # Delegate locking to set_left_ctrl to avoid double-locking.
    set_left_ctrl(not _ctrl_is_down)


def set_left_ctrl(is_down: bool):
    """Force Left Ctrl to be down/up (idempotent)."""
    global _ctrl_is_down
    with _ctrl_lock:
        if is_down and not _ctrl_is_down:
            print("[CTRL] Pressing Left Ctrl (DOWN)")
            send_key(VK_CONTROL, True)
            _ctrl_is_down = True
        elif (not is_down) and _ctrl_is_down:
            print("[CTRL] Releasing Left Ctrl (UP)")
            send_key(VK_CONTROL, False)
            _ctrl_is_down = False


def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"[SERVER] Listening on {HOST}:{PORT}")
        while True:
            conn, addr = s.accept()
            thread = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            thread.start()


if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n[SERVER] Shutting down.")

