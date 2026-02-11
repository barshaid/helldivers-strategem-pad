 # Helldivers 2 Strategem Pad

This project is a two-part system:

- **Android app**: A simple on-screen pad with a few big buttons, each mapped to a Helldivers 2 strategem.
- **PC helper (Windows)**: A small program that listens on your local network and sends keyboard input to your PC when you tap buttons on the phone.

## Structure

- `android-app/` – Android Studio project for the Android strategem pad
- `pc-helper/` – Windows helper script that receives commands and presses keys

## Getting started (PC helper)

1. Make sure you have **Python 3.9+** installed and on your PATH.
2. Open a terminal in the `pc-helper` directory.
3. Create a virtual environment (optional but recommended):
   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   ```
4. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
5. Run the server:
   ```bash
   python server.py
   ```
6. (Optional) From the same PC, you can send a test strategem or toggle Left Ctrl:
   ```bash
   python test_client.py          # test strategem
   python test_client.py ctrl     # toggle Left Ctrl
   ```

The helper will listen on a TCP port (configurable in `server.py`) for simple commands from the Android app (or the `test_client.py` script) and simulate key presses on your PC.

### Packaging the PC helper as an .exe

If you prefer a standalone `.exe`:

1. Install PyInstaller (in your venv or globally):
   ```bash
   pip install pyinstaller
   ```
2. From the `pc-helper` directory, build the executable:
   ```bash
   pyinstaller --onefile --name helldivers_pad_server server.py
   ```
3. After it finishes, you’ll get `dist/helldivers_pad_server.exe`. Run it to start the server without needing `python server.py`.

## Getting started (Android app)

1. Open Android Studio and choose **Open an Existing Project**.
2. Select the `android-app` folder in this repository.
3. Let Gradle sync and finish indexing.
4. In `app/src/main/java/com/theone/helldiverspad/MainActivity.kt`, set `PC_HOST` to your PC’s LAN IP (for example `192.168.0.42`). Make sure the port matches `PORT` in `pc-helper/server.py` (default `50555`).
5. Build an APK:
   - From the menu, choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - After it completes, click the notification to find the generated APK file.
6. Copy the APK to your Android device and install it (you may need to allow installs from unknown sources).
7. Make sure your phone and PC are on the **same Wi‑Fi / LAN**, run the PC helper, then use the app’s buttons to trigger strategems or toggle Left Ctrl.

## Next steps

- Adjust the strategem sequences in `MainActivity.kt` to match your exact Helldivers 2 key inputs.
- Polish the Android UI (add more buttons, different layouts, etc.).

