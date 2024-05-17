import base64

with open("Citra-Enhanced.png", "rb") as image_file:
    encoded_string = base64.b64encode(image_file.read()).decode('utf-8')

with open("image_base64.py", "w") as text_file:
    text_file.write(f"image_data = '{encoded_string}'")