import threading
import tkinter as tk
from tkinter import messagebox

from server import HOST, PORT, start_server


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
    title.pack(pady=(0, 12))

    info = tk.Label(
        frame,
        text=f"Listening on {HOST}:{PORT}\n\n"
        "Make sure this window stays open.\n"
        "Phone and PC must be on the same Wi‑Fi.",
        justify="left",
    )
    info.pack()

    def on_close():
        if messagebox.askokcancel("Quit", "Stop server and close?"):
            root.destroy()

    root.protocol("WM_DELETE_WINDOW", on_close)

    # Start server thread
    run_server_in_background()

    root.mainloop()


if __name__ == "__main__":
    main()

