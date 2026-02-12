import io
import json
import os
import socket
import sys
import threading
import tkinter as tk
from tkinter import messagebox

import qrcode
import pystray
from PIL import Image, ImageDraw, ImageTk

from server import HOST, PORT, start_server

# Single instance check using Windows mutex
_mutex = None  # Keep mutex alive for the lifetime of the application

def check_single_instance():
    """Check if another instance is already running."""
    global _mutex
    try:
        import win32event
        import win32api
        import winerror
        
        mutex_name = "Global\\HelldiversStrategemPadMutex"
        _mutex = win32event.CreateMutex(None, False, mutex_name)
        last_error = win32api.GetLastError()
        
        if last_error == winerror.ERROR_ALREADY_EXISTS:
            return False
        return True
    except ImportError:
        # Fallback: use a lock file approach
        import os
        import tempfile
        lock_file = os.path.join(tempfile.gettempdir(), "helldivers_pad_gui.lock")
        
        try:
            if os.path.exists(lock_file):
                # Check if process is still running
                with open(lock_file, 'r') as f:
                    pid = int(f.read().strip())
                try:
                    os.kill(pid, 0)  # Check if process exists
                    return False  # Process is still running
                except (OSError, ProcessLookupError):
                    # Process doesn't exist, remove stale lock file
                    os.remove(lock_file)
            
            # Create lock file with current PID
            with open(lock_file, 'w') as f:
                f.write(str(os.getpid()))
            return True
        except Exception:
            return True  # If we can't check, allow it to run


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


def get_icon_path():
    """Get the path to icon.ico, handling both script and PyInstaller executable."""
    if getattr(sys, 'frozen', False):
        # Running as compiled executable
        return os.path.join(sys._MEIPASS, "icon.ico")
    else:
        # Running as script
        return os.path.join(os.path.dirname(__file__), "icon.ico")


def create_tray_icon(root):
    """Create system tray icon with menu."""
    icon_path = get_icon_path()
    
    try:
        if os.path.exists(icon_path):
            icon_image = Image.open(icon_path)
            # Convert to RGB if needed and resize
            if icon_image.mode != 'RGB':
                icon_image = icon_image.convert('RGB')
            icon_image = icon_image.resize((16, 16), Image.Resampling.LANCZOS)
        else:
            raise FileNotFoundError
    except:
        # Create a simple yellow circle icon
        icon_image = Image.new('RGB', (16, 16), color='#D6FF3A')
        # Draw a simple circle
        draw = ImageDraw.Draw(icon_image)
        draw.ellipse([2, 2, 14, 14], fill='#D6FF3A', outline='#000000', width=1)
    
    def show_window(icon, item):
        """Show and restore the window."""
        root.after(0, lambda: (
            root.deiconify(),
            root.lift(),
            root.focus_force()
        ))
    
    def quit_app(icon, item):
        """Quit the application."""
        root.after(0, root.quit)
    
    menu = pystray.Menu(
        pystray.MenuItem("Show", show_window, default=True),
        pystray.MenuItem("Exit", quit_app)
    )
    
    icon = pystray.Icon("HelldiversPad", icon_image, "Helldivers Strategem Pad", menu)
    return icon


def main():
    # Check for single instance
    if not check_single_instance():
        messagebox.showwarning(
            "Already Running",
            "Helldivers Strategem Pad is already running.\n"
            "Check the system tray for the application icon."
        )
        sys.exit(1)
    
    root = tk.Tk()
    root.title("Helldivers 2 Strategem Server")
    root.resizable(False, False)
    
    # Set window icon
    try:
        icon_path = get_icon_path()
        if os.path.exists(icon_path):
            root.iconbitmap(icon_path)
    except Exception:
        # Icon setting failed, continue anyway
        pass

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

    # System tray icon
    tray_icon = None
    is_closing = False
    
    def show_window():
        """Show and restore the window."""
        root.deiconify()
        root.lift()
        root.focus_force()
    
    def hide_window():
        """Hide window to system tray."""
        root.withdraw()
    
    def on_close():
        """Handle window close (X button) - prompt and quit."""
        nonlocal is_closing
        is_closing = True
        # Unbind minimize handler to prevent it from firing
        root.unbind("<Unmap>")
        if messagebox.askokcancel("Quit", "Stop server and close?"):
            if tray_icon:
                tray_icon.stop()
            root.quit()
            root.destroy()
        else:
            # User cancelled - rebind minimize handler
            is_closing = False
            root.bind("<Unmap>", on_unmap)
    
    def on_unmap(event):
        """Handle window unmap - only minimize if not closing."""
        nonlocal is_closing
        if is_closing:
            return  # Don't minimize if we're closing
        
        # Check after a short delay if window is still iconic (minimized)
        def check_state():
            try:
                if root.winfo_exists() and root.state() == 'iconic' and not is_closing:
                    hide_window()
            except:
                pass
        
        root.after(10, check_state)
    
    root.protocol("WM_DELETE_WINDOW", on_close)
    
    # Bind to unmap event - but check state to distinguish minimize from close
    root.bind("<Unmap>", on_unmap)
    
    # Create and start tray icon in a separate thread
    def setup_tray():
        nonlocal tray_icon
        tray_icon = create_tray_icon(root)
        tray_icon.run()
    
    tray_thread = threading.Thread(target=setup_tray, daemon=True)
    tray_thread.start()

    # Start server thread
    run_server_in_background()

    root.mainloop()
    
    # Stop tray icon when closing
    if tray_icon:
        tray_icon.stop()


if __name__ == "__main__":
    main()

