#define AppVer GetEnv("V")
[Setup]
AppId={{6B1F0E52-3C0D-4C77-9E0A-EC40A15D0A01}
AppName=Echo Music
AppVersion={#AppVer}
AppPublisher=Echo Music
DefaultDirName={autopf}\Echo Music
DefaultGroupName=Echo Music
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=out
OutputBaseFilename=EchoMusic-Setup-v{#AppVer}
SetupIconFile=orig-core\icons\app.ico
UninstallDisplayIcon={app}\EchoMusic.exe
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; Flags: unchecked

[Files]
Source: "orig-core\build\compose\binaries\main\app\EchoMusic\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion

[Icons]
Name: "{group}\Echo Music"; Filename: "{app}\EchoMusic.exe"
Name: "{autodesktop}\Echo Music"; Filename: "{app}\EchoMusic.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\EchoMusic.exe"; Description: "Start Echo Music"; Flags: nowait postinstall skipifsilent
