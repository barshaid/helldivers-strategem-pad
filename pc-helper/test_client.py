import socket
import json

SERVER_HOST = "127.0.0.1"  # Change to your PC's LAN IP for real phone tests
SERVER_PORT = 50555


def send_message(msg: dict):
    data = (json.dumps(msg) + "\n").encode("utf-8")
    with socket.create_connection((SERVER_HOST, SERVER_PORT)) as s:
        s.sendall(data)


def send_test_strategem():
    print(f"Sending test strategem to {SERVER_HOST}:{SERVER_PORT} ...")
    msg = {
        "type": "strategem",
        "name": "test_orbital",
        "sequence": ["w", "s", "d", "a"],
    }
    send_message(msg)
    print("Done. Check the server window for logs and key presses.")


def toggle_left_ctrl():
    print(f"Toggling Left Ctrl on server {SERVER_HOST}:{SERVER_PORT} ...")
    msg = {
        "type": "toggle_left_ctrl",
    }
    send_message(msg)
    print("Done. Check the server window for [CTRL] logs.")


def main():
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "ctrl":
        toggle_left_ctrl()
    else:
        send_test_strategem()


if __name__ == "__main__":
    main()

