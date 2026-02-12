import io
import json
import socket
import threading
import tkinter as tk
from tkinter import messagebox

import qrcode
from PIL import Image, ImageTk

from server import HOST, PORT, start_server


def get_lan_ip() -> str:
    """Best-effort detection of the LAN IP used to reach the internet."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            return s.getsockname()[0]
    except OSError:
        # Fallback to configured HOST (may be 0.0.0.0 or localhost)
        return HOST


def generate_qr_image(data: dict) -> Image.Image:
    """Generate a QR code PIL image from a dict."""
    payload = json.dumps(data)
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=6,
        border=2,
    )
    qr.add_data(payload)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    return img.convert("RGB")


def run_server_in_background():
    thread = threading.Thread(target=start_server, daemon=True)
    thread.start()
    return thread


def main():
    root = tk.Tk()
    root.title("Helldivers 2 Strategem Server")
    root.resizable(False, False)

    frame = tk.Frame(root, padx=16, pady=16)
    frame.pack()

    title = tk.Label(
        frame, text="Helldivers 2 Strategem Pad\nPC Helper", font=("Segoe UI", 12, "bold")
    )
    title.pack(pady=(0, 8))

    # Detect LAN IP and build QR payload
    lan_ip = get_lan_ip()
    payload = {"host": lan_ip, "port": PORT}

    info = tk.Label(
        frame,
        text=f"Listening on {lan_ip}:{PORT}\n\n"
        "Scan this QR from the app settings to sync.\n"
        "Phone and PC must be on the same Wi‑Fi.",
        justify="left",
    )
    info.pack(pady=(0, 8))

    # Generate QR image
    qr_img = generate_qr_image(payload)
    # Convert PIL image to Tkinter PhotoImage
    buf = io.BytesIO()
    qr_img.save(buf, format="PNG")
    buf.seek(0)
    tk_img = ImageTk.PhotoImage(Image.open(buf))

    qr_label = tk.Label(frame, image=tk_img)
    qr_label.image = tk_img  # keep reference
    qr_label.pack(pady=(0, 8))

    def on_close():
        if messagebox.askokcancel("Quit", "Stop server and close?"):
            root.destroy()

    root.protocol("WM_DELETE_WINDOW", on_close)

    # Start server thread
    run_server_in_background()

    root.mainloop()


if __name__ == "__main__":
    main()

