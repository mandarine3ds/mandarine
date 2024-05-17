    # Define the dark stylesheet
class Style:
    dark_stylesheet = """
        QWidget {
            background-color: rgb(32, 32, 32);
            color: rgb(255, 255, 255);
            font-size: 17px;
            font-family: "Segoe UI Variable Small", serif;
            font-weight: 400;
        }
        /*PUSHBUTTON*/
        QPushButton {
            background-color: #323234;
            border: 1px solid rgb(68, 68, 68);
            border-radius: 7px;
            min-height: 38px;
            max-height: 38px;
        }

        QPushButton:hover {
            background-color: rgb(100, 100, 100);
            border: 1px solid rgb(255, 255, 255, 10);
        }

        QPushButton::pressed {
            background-color: rgb(255, 255, 255, 7);
            border: 1px solid rgb(255, 255, 255, 13);
            color: rgb(255, 255, 255, 200);
        }

        QPushButton::disabled {
            color: rgb(150, 150, 150);
            background-color: rgb(255, 255, 255, 13);
        }
        QLabel, QCheckBox {
            color: #ffffff;
        }
        /*GROUPBOX*/
        QGroupBox {
            border-radius: 5px;
            border: 0px solid rgb(255, 255, 255, 13);
            margin-top: 36px;
        }

        QGroupBox::title {
            subcontrol-origin: margin;
            subcontrol-position: top left;
            background-color: rgb(255, 255, 255, 16);
            padding: 7px 15px;
            margin-left: 5px;
            border-top-left-radius: 5px;
            border-top-right-radius: 5px;
        }

        QGroupBox::title::disabled {
            color: rgb(150, 150, 150)
        }
        /*COMBOBOX*/
        QComboBox {
            background-color: rgb(66, 66, 66);
            border: 1px solid rgb(68, 68, 68);
            border-radius: 5px;
            padding-left: 10px;
            min-height: 38px;
            max-height: 38px;
        }

        QComboBox:hover {
            background-color: rgb(120, 120, 120);
            border: 1px solid rgb(255, 255, 255, 10);
        }

        QComboBox::pressed {
            background-color: rgb(66, 66, 66);
            border: 1px solid #0078D4;
            color: rgb(255, 255, 255, 200);
        }

        QComboBox::down-arrow {
            image: url(:/ComboBox/img dark/ComboBox.png);
        }

        QComboBox::drop-down {
            background-color: transparent;
            min-width: 50px;
        }

        QComboBox:disabled {
            color: rgb(150, 150, 150);
            background-color: rgb(255, 255, 255, 13);
            border: 1px solid rgb(255, 255, 255, 5);
        }

        QComboBox::down-arrow:disabled {
            image: url(:/ComboBox/img dark/ComboBoxDisabled.png);
        }
        /*CHECKBOX*/
        QCheckBox {
            min-height: 30px;
            max-height: 30px;
        }

        QCheckBox::indicator {
            width: 22px;
            height: 22px;
            border-radius: 5px;
            border: 2px solid #848484;
            background-color: rgb(255, 255, 255, 0);
            margin-right: 5px;
        }

        QCheckBox::indicator:hover {
            background-color: rgb(255, 255, 255, 16);
        }

        QCheckBox::indicator:pressed {
            background-color: rgb(255, 255, 255, 20);
            border: 2px solid #434343;
        }

        QCheckBox::indicator:checked {
            background-color: #0078D4;
            border: 2px solid #0078D4;
            image: url(:/CheckBox/img dark/CheckBox.png);
        }

        QCheckBox::indicator:checked:pressed {
            image: url(:/CheckBox/img dark/CheckBoxPressed.png);
        }

        QCheckBox:disabled {
            color: rgb(150, 150, 150);
        }

        QCheckBox::indicator:disabled {
            border: 2px solid #646464;
            background-color: rgb(255, 255, 255, 0);
        }
        /*PROGRESSBAR*/
        QProgressBar {
            background-color: qlineargradient(spread:reflect, x1:0.5, y1:0.5, x2:0.5, y2:1, stop:0.119403 rgba(255, 255, 255, 250), stop:0.273632 rgba(0, 0, 0, 0));
            border-radius: 2px;
            min-height: 4px;
            max-height: 4px;
        }

        QProgressBar::chunk {
            background-color: #0078D4;;
            border-radius: 2px;
        }
        /*MENU*/
        QMenu {
            background-color: transparent;
            padding-left: 1px;
            padding-top: 1px;
            border-radius: 5px;
            border: 1px solid rgb(255, 255, 255, 13);
        }

        QMenu::item {
            background-color: transparent;
            padding: 5px 15px;
            border-radius: 5px;
            min-width: 60px;
            margin: 3px;
        }

        QMenu::item:selected {
            background-color: rgb(255, 255, 255, 16);
        }

        QMenu::item:pressed {
            background-color: rgb(255, 255, 255, 10);
        }

        QMenu::right-arrow {
            image: url(:/TreeView/img dark/TreeViewClose.png);
            min-width: 40px;
            min-height: 18px;
        }

        QMenuBar:disabled {
            color: rgb(150, 150, 150);
        }

        QMenu::item:disabled {
            color: rgb(150, 150, 150);
            background-color: transparent;
        }
        /*LINEEDIT*/
        QLineEdit {
            background-color: #323234;
            border: 1px solid rgb(68, 68, 68);
            font-size: 16px;
            font-family: "Segoe UI", serif;
            font-weight: 500;
            border-radius: 7px;
            border-bottom: 1px solid rgb(255, 255, 255, 150);
            padding-top: 0px;
            padding-left: 5px;
        }

        QLineEdit:hover {
            background-color: rgb(120, 120, 120);
            border: 1px solid rgb(255, 255, 255, 10);
            border-bottom: 1px solid rgb(255, 255, 255, 150);
        }

        QLineEdit:focus {
            border-bottom: 2px solid #0078D4;
            background-color: rgb(100, 100, 100);
            border-top: 1px solid rgb(255, 255, 255, 13);
            border-left: 1px solid rgb(255, 255, 255, 13);
            border-right: 1px solid rgb(255, 255, 255, 13);
        }

        QLineEdit:disabled {
            color: rgb(150, 150, 150);
            background-color: rgb(255, 255, 255, 13);
            border: 1px solid rgb(255, 255, 255, 5);
        }
        """