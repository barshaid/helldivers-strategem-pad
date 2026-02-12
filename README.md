# Helldivers 2 Strategem Pad

A wireless input pad for Helldivers 2 that lets you control strategems from your Android phone.

This project is a two-part system:

- **Android app**: A full-screen D-pad with arrow buttons (WASD) and a Left Ctrl toggle button. Features sound effects, haptic feedback, and QR code sync.
- **PC helper (Windows)**: A GUI application that listens on your local network, displays a QR code for easy setup, and sends keyboard input to your PC when you tap buttons on the phone.

## Structure

- `android-app/` – Android Studio project for the Android strategem pad
- `pc-helper/` – Windows helper application (GUI and console versions)
- `final/` – Release folder containing ready-to-use APK and EXE files

## Quick Start (Using Pre-built Files)

The easiest way to get started is using the files in the `final/` folder:

1. **Run the PC helper**: Double-click `helldivers_pad_gui.exe` (recommended) or `helldivers_pad_server.exe`
   - The GUI version shows a window with a QR code for easy setup
   - You can minimize it to the system tray (only one instance can run at a time)
   - X button closes with confirmation, minimize button (_) minimizes to tray

2. **Install the Android app**: Copy `HelldiversStrategemPad.apk` to your phone and install it
   - Enable "Install from unknown sources" if needed

3. **Sync via QR code** (easiest method):
   - Open the app, tap the settings gear icon (top-left)
   - Tap "Scan QR to Sync"
   - Scan the QR code displayed in the PC helper window
   - Your phone will automatically connect to your PC

4. **Manual setup** (alternative):
   - In app settings, enter your PC's LAN IP address manually
   - Default port is `50555` (should match automatically)

5. **Make sure both devices are on the same Wi-Fi network**

## Features

### Android App
- **D-pad layout**: Arrow buttons arranged like keyboard arrows (inverted T-shape)
- **Arrow buttons**: Turn yellow when held, send WASD keys to PC
- **Left Ctrl toggle**: Yellow skull button toggles Left Ctrl on/off
- **Sound effects**: 10 different button sounds, selectable in settings with preview
- **Haptic feedback**: Configurable strength (0-3) with on/off toggle
- **QR code sync**: Scan QR code from PC helper to automatically configure connection
- **Strategems overlay**: Shows "STRATAGEMS OFF" when Ctrl is not held
- **Fullscreen mode**: Immersive fullscreen experience

### PC Helper
- **GUI version** (`helldivers_pad_gui.exe`):
  - Visual window with server status
  - QR code display for easy phone setup
  - System tray support (minimize to tray)
  - Single instance check (prevents multiple instances)
  - Skull icon
  
- **Console version** (`helldivers_pad_server.exe`):
  - Lightweight console application
  - Shows connection logs
  - Useful for debugging

## Development Setup

### PC Helper (Python)

1. Make sure you have **Python 3.9+** installed.
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
   python gui_server.py    # GUI version
   python server.py         # Console version
   ```

### Building Executables

To build the executables yourself:

1. Install PyInstaller:
   ```bash
   pip install pyinstaller
   ```
2. Build the executables:
   ```bash
   cd pc-helper
   pyinstaller --clean helldivers_pad_gui.spec      # GUI version
   pyinstaller --clean helldivers_pad_server.spec   # Console version
   ```
3. Find the executables in `pc-helper/dist/`

### Android App (Android Studio)

1. Open Android Studio and choose **Open an Existing Project**.
2. Select the `android-app` folder in this repository.
3. Let Gradle sync and finish indexing.
4. Build an APK:
   - From the menu, choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - After it completes, click the notification to find the generated APK file.
   - The APK will be in `android-app/app/build/outputs/apk/debug/app-debug.apk`
5. Copy the APK to your Android device and install it (you may need to allow installs from unknown sources).

**Note**: The app now uses a settings screen for configuration. You can either:
- Use QR code sync (recommended): Scan the QR code from the PC helper GUI
- Manual setup: Enter your PC's LAN IP address in the app settings

## Usage

1. **Start the PC helper** (`helldivers_pad_gui.exe` recommended)
2. **Open the Android app** on your phone
3. **Sync connection** via QR code (settings → Scan QR) or enter IP manually
4. **In Helldivers 2**:
   - Tap the **yellow skull button** to enable strategems (toggles Left Ctrl)
   - **Hold arrow buttons** to send WASD keys (arrows turn yellow when held)
   - Release arrows to stop sending keys
   - Tap skull again to disable strategems

## Settings (Android App)

Access settings via the gear icon (top-left):

- **PC Host**: Your PC's LAN IP address
- **Port**: Server port (default: 50555)
- **Haptic strength**: 0-3 (with on/off toggle)
- **Arrow button sound**: Select from 10 sounds with preview
- **Scan QR to Sync**: Automatically configure from PC helper QR code

## Requirements

- **PC**: Windows 10/11
- **Android**: Android 6.0+ (API level 23+)
- **Network**: Both devices must be on the same Wi-Fi network
- **Permissions**: 
  - Android: Internet, Camera (for QR scanning), Vibration
  - PC: May need administrator privileges for key simulation

## Troubleshooting

- **Keys not working**: Try running the EXE as administrator (right-click → Run as administrator)
- **Connection timeout**: Check that both devices are on the same Wi-Fi network
- **QR code not scanning**: Ensure camera permission is granted
- **Multiple instances**: Only one instance of the PC helper can run at a time
