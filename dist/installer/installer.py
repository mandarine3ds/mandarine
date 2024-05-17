import os
import sys
import requests
from PyQt6.QtWidgets import QMainWindow, QApplication, QLabel, QPushButton, QVBoxLayout, QWidget, QCheckBox, QStackedLayout, QHBoxLayout, QGroupBox, QComboBox, QProgressBar, QLineEdit, QMessageBox, QFileDialog, QVBoxLayout
from PyQt6.QtGui import QPixmap, QIcon, QImage
from PyQt6.QtCore import Qt, QThread, pyqtSignal, QByteArray, QFile, QTextStream , QObject, pyqtSignal, pyqtSlot
import zipfile
import shutil
import tempfile
import win32com.client
import logging
import re
import winreg
import base64
from image_base64 import image_data
from icons import styledark_rc
from stylesheet import Style
from pathlib import Path

class QtUi(QMainWindow, Style):
    def __init__(self):
        super().__init__()
        self.logic = Logic()
        self.ui()  # Init UI
        self.load_stylesheet()
        
    # Icon Header
    def Header(self):
        # Header Layout
        self.headerLayout = QVBoxLayout()
        self.headerLayout.setContentsMargins(0, 20, 0, 0)
        # Icon Widget
        CEicon = QLabel()
        image_bytes = base64.b64decode(image_data)
        image = QImage.fromData(QByteArray(image_bytes))
        pixmap = QPixmap.fromImage(image)
        ## Scale the pixmap
        scaledPixmap = pixmap.scaled(180, 180,)
        CEicon.setPixmap(scaledPixmap)
        # Text Widget
        CElabel = QLabel("<b>Citra-Enhanced Installer")
        # Set Widgets
        self.headerLayout.addWidget(CEicon, alignment=Qt.AlignmentFlag.AlignCenter)
        self.headerLayout.addWidget(CElabel, alignment=Qt.AlignmentFlag.AlignCenter)
        return self.headerLayout
    
    # Widgets
    def ui(self):
        # Init Window
        self.setWindowTitle('Installer') # Window name
        self.setCentralWidget(QWidget(self))  # Set a central widget
        self.layout = QStackedLayout(self.centralWidget())  # Set the layout on the central widget
        self.setFixedSize(700, 550) # Fixed window size
        
        # Welcome page
        self.welcomePage = QWidget()
        ## Layout and gorup
        welcomerLayout = QVBoxLayout()
        welcomerGroup = QGroupBox("")
        welcomerGroupLayout = QVBoxLayout()
        buttonsLayout = QHBoxLayout()
        welcomerGroup.setLayout(welcomerGroupLayout)
        self.welcomePage.setLayout(welcomerLayout) # Set the welcomepage layout
        ## Widgets
        welcomerLabel = QLabel('<b>Citra Enhanced is a fork of Citra which aims to incrase')
        welcomerLabel.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.installButton = QPushButton('Install')
        self.installButton.clicked.connect(self.logic.clickfunction)
        self.updateButton = QPushButton('Update')
        self.updateButton.clicked.connect(self.logic.clickfunction)
        self.uninstallButton = QPushButton('Uninstall')
        self.uninstallButton.clicked.connect(self.logic.clickfunction)
        ## Add widgets / layouts
        welcomerLayout.addLayout(self.Header()) 
        welcomerLayout.addWidget(welcomerGroup)
        welcomerGroupLayout.addWidget(welcomerLabel)
        buttonsLayout.addWidget(self.installButton)
        buttonsLayout.addWidget(self.updateButton)
        buttonsLayout.addWidget(self.uninstallButton)
        welcomerLayout.addLayout(buttonsLayout)
        ## Add the welcome page to the layout
        self.layout.addWidget(self.welcomePage)


        # Install page
        self.installPage = QWidget()
        ## Layout and gorup
        installLayout = QVBoxLayout()
        checkboxLayout = QVBoxLayout()
        checkboxGroup = QGroupBox("Do you want to create shortcuts?") # Checkboxes
        checkboxGroup.setLayout(checkboxLayout) # Set the layout of Checkboxes
        pathSelectorLayout = QHBoxLayout() # Browse widget layout
        self.installPage.setLayout(installLayout) # Set the installpage layout
        ## Widgets
        InstalOpt = QLabel('<b>Installation Options')
        InstalOpt.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.installationSourceComboBox = QComboBox() # Dropdown menu
        self.installationSourceComboBox.addItem("Latest Release") 
        self.installationSourceComboBox.addItem("Latest Nightly") 
        self.desktopShortcutCheckbox = QCheckBox("Create a desktop shortcut") # Checkboxes
        self.startMenuShortcutCheckbox = QCheckBox("Create a start menu shortcut")
        self.installationPathLineEdit = QLineEdit(os.path.join(os.environ['LOCALAPPDATA'], 'Citra Enhanced')) # Browse for installation path widget
        self.browseButton = QPushButton("Browse")
        self.browseButton.clicked.connect(self.logic.InstallPath)
        self.installCitraButton = QPushButton('Install') # Install button
        self.installCitraButton.clicked.connect(self.logic.clickfunction)
        ## Add widgets / layouts
        installLayout.addLayout(self.Header()) ### Icon self.header
        installLayout.addWidget(InstalOpt) ### Instalation Option Label
        installLayout.addWidget(self.installationSourceComboBox) ## Install Sorce Widget
        pathSelectorLayout.addWidget(self.installationPathLineEdit) ## Browse Widget
        pathSelectorLayout.addWidget(self.browseButton)
        installLayout.addLayout(pathSelectorLayout)
        checkboxLayout.addWidget(self.desktopShortcutCheckbox) # Checkboxes
        checkboxLayout.addWidget(self.startMenuShortcutCheckbox)
        installLayout.addWidget(checkboxGroup)
        installLayout.addWidget(self.installCitraButton)
        ## Add the install page to the layout
        self.layout.addWidget(self.installPage)


        # Progress bar page
        self.progressBarPage = QWidget()
        ## Layout and groups
        progressBarLayout = QVBoxLayout()
        self.progressBarPage.setLayout(progressBarLayout)
        ## Widgets
        self.downloadProgressBar = QProgressBar()
        self.downloadProgressBar.setRange(0, 100)  # Progress bar Widgets
        self.extractionProgressBar = QProgressBar()
        self.extractionProgressBar.setRange(0, 100)    
        labeldown = QLabel("Downloading Citra Enhanced:")
        labelext = QLabel("Extracting Citra-Enhanced:")
        ## Add widgets / layouts
        progressBarLayout.addLayout(self.Header())  # Add the icon self.header
        progressBarLayout.addWidget(labeldown)
        progressBarLayout.addWidget(self.downloadProgressBar)
        progressBarLayout.addWidget(labelext)
        progressBarLayout.addWidget(self.extractionProgressBar)
        # Add the progress bar page to the layout
        self.layout.addWidget(self.progressBarPage)


        # Finish page
        self.finishPage = QWidget()
        ## Layout and groups
        finishLayout = QVBoxLayout()
        self.finishPage.setLayout(finishLayout)
        ## Widgets
        finishLabel = QLabel("<b>Installation Complete!")
        finishLabel.setAlignment(Qt.AlignmentFlag.AlignCenter)  # Center the text horizontally
        finishButton = QPushButton("Finish")
        finishButton.clicked.connect(self.close)
        ## Add widgets / layouts
        finishLayout.addLayout(self.Header())  # Add the icon self.header
        finishLayout.addWidget(finishLabel)
        finishLayout.addWidget(finishButton)
        # Add the progress bar page to the layout
        self.layout.addWidget(self.finishPage)  
        
    def load_stylesheet(app):
            app.setStyleSheet(Style.dark_stylesheet)
            

class Logic:
    def __init__(self):
        self.value = None  # Declare the reg value as none
        self.mode = None # Declare the instalation mode as none

    # Disable buttons depanding on if it the program is already installed or not
    def disableButtons(self):
        regvalue = self.checkreg() 
        if regvalue is None:
            qtui.installButton.setEnabled(True) 
            qtui.updateButton.setEnabled(False)
            qtui.uninstallButton.setEnabled(False)                    
        else:
            qtui.installButton.setEnabled(False) 
            qtui.updateButton.setEnabled(True)
            qtui.uninstallButton.setEnabled(True)               

    #Welcome page buttons clicking linking and declaring the installation mode
    def clickfunction(self):
        button = qtui.sender()
        if button is qtui.installButton:
            self.mode = "Install"
            qtui.layout.setCurrentIndex(1)
        elif button is qtui.updateButton:
            self.mode = "Update"
            qtui.layout.setCurrentIndex(2)
            self.PrepareDownload()
        elif button is qtui.uninstallButton:
            self.mode = "Uninstall" # Unused for now
            self.uninstall()
        if button is qtui.installCitraButton:
            qtui.layout.setCurrentIndex(2)
            self.PrepareDownload()
        return self.mode
    
    # Select installation path function (Needs cleaning up)
    def InstallPath(self):
        self.CitraEnhancedDirectory = qtui.installationPathLineEdit.text()
        if qtui.browseButton.clicked:
            selectedDirectory = QFileDialog.getExistingDirectory(qtui, "Select Installation Directory", qtui.installationPathLineEdit.text())
            if selectedDirectory:  # Check if a directory was selected
                # Append "Citra-Enhanced" to the selected directory path
                CitraEnhancedDirectory = os.path.join(selectedDirectory, 'Citra-Enhanced')
                qtui.installationPathLineEdit.setText(CitraEnhancedDirectory)
        else:
            pass        
        return self.CitraEnhancedDirectory     

    # Fetching the update channel and setting the download URL
    def PrepareDownload(self):
        test = self.checkreg()        
        if test is not None: # Checks the update channel
            selection = self.updatevalue
        else:
            selection = qtui.installationSourceComboBox.currentText()
        if selection == "Latest Nightly":
            self.url = "https://nightly.link/Gamer64ytb/Citra-Enhanced/workflows/build/master/windows-msvc.zip"
            response = requests.get(self.url, stream=True)
            self.DownloadCitraEnhanced()
            return self.url
        elif selection == "Latest Release":
            api_url = "https://api.github.com/repos/Gamer64ytb/Citra-Enhanced/releases/latest"
            response = requests.get(api_url)
            release = response.json()
            assets = release.get('assets', [])
            for asset in assets:
                if "windows-msvc.zip" in asset['name']:
                    self.url = asset['browser_download_url']
                    print("Download URL: ", self.url)
                    self.DownloadCitraEnhanced()
                    return self.url
            QMessageBox.critical(qtui, "Error", "No suitable release found.")
            return None
    # Download function    
    def DownloadCitraEnhanced(self):
        temp_file = tempfile.NamedTemporaryFile(delete=False).name
        self.installationPath = self.value or qtui.installationPathLineEdit.text()
        os.makedirs(self.installationPath, exist_ok=True)
        # Threads
        self.download_thread = QThread()
        self.download_worker = DownloadWorker(self.url, temp_file)
        self.download_worker.moveToThread(self.download_thread)
        self.download_thread.started.connect(self.download_worker.do_download)
        self.download_worker.progress.connect(qtui.downloadProgressBar.setValue)
        self.download_thread.start()
        self.download_worker.finished.connect(lambda: self.extract_and_install(temp_file, self.installationPath))

    # Clear directory MEANT TO BE USED WITH THE ZIP FUNCTION (Needs major cleanup along with the ZIP function)
    def clear_directory(self, directory):
        for item in os.listdir(directory):
            item_path = os.path.join(directory, item)
            try:
                if os.path.isfile(item_path) or os.path.islink(item_path):
                    os.unlink(item_path)
                elif os.path.isdir(item_path):
                    shutil.rmtree(item_path)
            except Exception as e:
                logging.error(f'Failed to delete {item_path}. Reason: {e}')

# This can be cleaned up alot but i was running into issues with the nested folder citra-msvc folder (Needs major cleanup) 
    def extract_and_install(self, temp_file, extract_to):
        print(f"Extracting {temp_file} to {extract_to}.")
        try:
            # Clear the target directory before extracting new files
            if os.path.exists(extract_to):
                self.clear_directory(extract_to)

            # Rename the temporary file to have a .zip extension and create a temporary extraction folder
            zip_file_path = f"{temp_file}.zip"
            os.rename(temp_file, zip_file_path)
            temp_extract_folder = tempfile.mkdtemp()

            # Extract the main zip file to a temporary folder
            with zipfile.ZipFile(zip_file_path, 'r') as zip_ref:
                zip_ref.extractall(temp_extract_folder)
                logging.info("Main zip extraction completed to temporary folder.")

            # Handle nested zip files or direct extraction
            nested_zip_path = self.find_nested_zip(temp_extract_folder)
            if nested_zip_path:
                self.extract_nested_zip(nested_zip_path, extract_to)
            else:
                self.move_files(temp_extract_folder, extract_to)

            # Clean up nested directories if present
            self.handle_nested_directories(extract_to)

            # Finalize installation
            qtui.extractionProgressBar.setValue(100)
            self.installation_complete()
        except Exception as e:
            logging.error(f"Failed to extract zip file: {e}. File: {temp_file}")
    def find_nested_zip(self, temp_extract_folder):
        for root, dirs, files in os.walk(temp_extract_folder):
            for file in files:
                if file.endswith('.zip'):
                    return os.path.join(root, file)
        return None
    def extract_nested_zip(self, nested_zip_path, extract_to):
        with zipfile.ZipFile(nested_zip_path, 'r') as nested_zip_ref:
            nested_zip_ref.extractall(extract_to)
            logging.info(f"Nested zip extraction completed to {extract_to}.")
    def move_files(self, source_folder, destination_folder):
        for item in os.listdir(source_folder):
            src_path = os.path.join(source_folder, item)
            dest_path = os.path.join(destination_folder, item)
            if os.path.isdir(src_path):
                shutil.move(src_path, dest_path)
            else:
                shutil.copy2(src_path, dest_path)
        logging.info("Moved extracted files to the desired location.")
    def handle_nested_directories(self, extract_to):
        for item in os.listdir(extract_to):
            if os.path.isdir(os.path.join(extract_to, item)) and 'windows-msvc' in item:
                nested_dir_path = os.path.join(extract_to, item)
                self.move_files(nested_dir_path, extract_to)
                shutil.rmtree(nested_dir_path)
       
    # After the extracting and moving of the files is done   
    def installation_complete(self):
        executable_path = os.path.normpath(os.path.join(qtui.installationPathLineEdit.text(), 'citra-qt.exe')) # Declare exe path for the shortcuts
        if qtui.desktopShortcutCheckbox.isChecked():
            self.define_desktop_shortcut(executable_path)
        if qtui.startMenuShortcutCheckbox.isChecked():
            self.define_start_menu_shortcut(executable_path)
        self.checkreg()
        qtui.layout.setCurrentIndex(qtui.layout.indexOf(qtui.finishPage))  # Switch to finish page
 
    # Function to check the reg values
    def checkreg(self):
        try:
            self.registry_key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Software\\Citra-Enhanced", 0, winreg.KEY_READ)
            self.value, regtype = winreg.QueryValueEx(self.registry_key, 'CitraEnhancedDirectory')
            self.updatevalue, regtype = winreg.QueryValueEx(self.registry_key, 'CitraEnhancedUpdateChannel')
            winreg.CloseKey(self.registry_key)
            return self.value, self.updatevalue  
        except FileNotFoundError:
            if self.mode == 'Install':
                self.createreg()
            else:            
                print("Found the key, skipping creation")
    # Function to create the reg values            
    def createreg (self):   
        winreg.CreateKey(winreg.HKEY_CURRENT_USER, "Software\\Citra-Enhanced")
        self.registry_key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, "Software\\Citra-Enhanced", 0, 
                                        winreg.KEY_WRITE)
        
        winreg.SetValueEx(self.registry_key, 'CitraEnhancedDirectory', 0, winreg.REG_SZ, qtui.installationPathLineEdit.text())
        winreg.SetValueEx(self.registry_key, 'CitraEnhancedUpdateChannel', 0, winreg.REG_SZ, qtui.installationSourceComboBox.currentText())
        winreg.CloseKey(self.registry_key)
        print ("Key Created/ Updated")

    # Function to create the shortcuts (Thsi works but can be cleaned up a bit)
    def define_desktop_shortcut(self, target):
        desktop_path = os.path.join(os.environ['USERPROFILE'], 'Desktop')
        shortcut_path = os.path.join(desktop_path, 'Citra-Enhanced.lnk')
        self.create_shortcut(target, shortcut_path)
    def define_start_menu_shortcut(self, target):
        start_menu_path = os.path.join(os.environ['APPDATA'], 'Microsoft', 'Windows', 'Start Menu', 'Programs')
        shortcut_path = os.path.join(start_menu_path, 'Citra-Enhanced.lnk')
        self.create_shortcut(target, shortcut_path)
    def create_shortcut(self, target, shortcut_path, description="", arguments="", hotkey=""):
        # Verify the target exists
        if not os.path.exists(target):
            logging.error(f"Shortcut target does not exist: {target}")
            return

        try:
            shell = win32com.client.Dispatch("WScript.Shell")
            shortcut = shell.CreateShortCut(shortcut_path)
            shortcut.TargetPath = target
            shortcut.WorkingDirectory = os.path.dirname(target)
            shortcut.Description = description
            shortcut.Arguments = arguments
            if hotkey:
                shortcut.Hotkey = hotkey
            shortcut.IconLocation = target  # You can customize this if needed
            shortcut.save()
            logging.info(f"Shortcut created successfully at {shortcut_path}")
        except Exception as e:
            logging.error(f"Failed to create shortcut: {e}")

    # Uninstall function that doesnt delete the working direcotry (YAY)
    def uninstall(self):
        self.checkreg()
        if self.value is not None:
            # Paths to the shortcuts
            desktopshortcut = os.path.join(os.environ['USERPROFILE'], 'Desktop', 'Citra-Enhanced.lnk')
            startmenushortcut = os.path.join(os.environ['APPDATA'], 'Microsoft', 'Windows', 'Start Menu', 'Programs', 'Citra-Enhanced.lnk')

            reply = QMessageBox.question(qtui, "Uninstall", "Are you sure you want to uninstall Citra-Enhanced?", QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
            if reply == QMessageBox.StandardButton.No:
                return
            else:
                
                # Remove the shortcut and the, DIR and REG keys
                if os.path.exists(desktopshortcut):
                    os.remove(desktopshortcut)
                if os.path.exists(startmenushortcut):
                    os.remove(startmenushortcut)
                dirpath = Path(self.value)
                if dirpath.exists() and dirpath.is_dir():
                    shutil.rmtree(dirpath)
                    print (dirpath)
                    winreg.DeleteKey(winreg.HKEY_CURRENT_USER, "Software\\Citra-Enhanced")
                    QMessageBox.information(qtui, "Uninstall", "Citra-Enhanced has been successfully uninstalled.")
                    exit()
                else:
                    QMessageBox.critical(qtui, "Error", "The direcotry might have been moved or deleted. Please reinstall the program.")
                    winreg.DeleteKey(winreg.HKEY_CURRENT_USER, "Software\\Citra-Enhanced")
                    qtui.updateButton.setEnabled(False)   
                    qtui.uninstallButton.setEnabled(False)    
                    qtui.installButton.setEnabled(True) 
        else:
            QMessageBox.critical(qtui, "Error",("Failed to read the registry key. Try and reinstall again!"))
            qtui.layout.setCurrentIndex(1)        

# Download Worker class (Help used)
class DownloadWorker(QThread):
    progress = pyqtSignal(int)

    def __init__(self, url, dest):
        super().__init__()
        self.url = url
        self.dest = dest

    @pyqtSlot()
    def do_download(self):
        try:
            response = requests.get(self.url, stream=True)
            total_size = int(response.headers.get('content-length', 0))
            if total_size == 0:
                print("The content-length of the response is zero.")
                return

            downloaded_size = 0
            with open(self.dest, 'wb') as file:
                for data in response.iter_content(1024):
                    downloaded_size += len(data)
                    file.write(data)
                    progress_percentage = (downloaded_size / total_size) * 100
                    self.progress.emit(int(progress_percentage))
            print(f"Download completed. File saved to {self.dest}")
            self.finished.emit()
        except Exception as e:
            print("Error doing download.")
            self.finished.emit()

if __name__ == "__main__":
    app = QApplication(sys.argv)
    qtui = QtUi()
    qtui.show()
    self = Logic()
    exec, Logic.disableButtons(self)
    sys.exit(app.exec())



