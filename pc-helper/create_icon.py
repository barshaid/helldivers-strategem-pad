from PIL import Image
import os

# Open the skull PNG
img = Image.open(os.path.join(os.path.dirname(__file__), '..', 'android-app', 'app', 'src', 'main', 'res', 'drawable', 'ic_skull.png'))

# Create ICO with multiple sizes
img.save(os.path.join(os.path.dirname(__file__), 'icon.ico'), format='ICO', sizes=[(256,256), (128,128), (64,64), (48,48), (32,32), (16,16)])
print("Icon created successfully!")
