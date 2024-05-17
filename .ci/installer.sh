#!/bin/bash -ex

# Make the build dir

mkdir -p $GITHUB_WORKSPACE/build/enhanced-installer

# Go to build dir

cd dist/installer

# Generate Image Data


python imagedata.py

# Check if the file was made

FILE=image_base64.py  
if [ -f $FILE ]; then
   echo "File $FILE exists."
else
   echo "File $FILE does not exist."
fi

# Run PyInstaller to build the applications

pyinstaller --onefile --windowed --icon=Citra-Enhanced.ico --distpath $GITHUB_WORKSPACE/build/enhanced-installer installer.py

echo $GITHUB_WORKSPACE
cd $GITHUB_WORKSPACE
