// Copyright 2017 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include <QIcon>
#include "aboutdialog.h"
#include "common/scm_rev.h"
#include "ui_aboutdialog.h"
#include "util/mica.h"

AboutDialog::AboutDialog(QWidget* parent)
    : QDialog(parent, Qt::WindowTitleHint | Qt::WindowCloseButtonHint | Qt::WindowSystemMenuHint),
      ui(std::make_unique<Ui::AboutDialog>()) {
    ui->setupUi(this);
    ui->labelLogo->setPixmap(QIcon::fromTheme(QStringLiteral("citra")).pixmap(200));
    ui->labelBuildInfo->setText(ui->labelBuildInfo->text().arg(
        QString::fromUtf8(Common::g_build_fullname), QString::fromUtf8(Common::g_scm_branch),
        QString::fromUtf8(Common::g_scm_desc), QString::fromUtf8(Common::g_build_date).left(10)));
}

AboutDialog::~AboutDialog() = default;

void AboutDialog::showEvent(QShowEvent* event) {
    QDialog::showEvent(event); // Call the base class method first

#ifdef _WIN32
    HWND hwnd = reinterpret_cast<HWND>(this->winId());
    Utils::EnableDarkMicaForWindow(hwnd);
#endif
}
