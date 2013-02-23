; Avis Inno Setup installer script

[Setup]
AppName=Avis
AppVerName=Avis 1.2.2
AppPublisher=Matthew Phillips
AppPublisherURL=http://avis.sourceforge.net
AppVersion=1.2.2
DefaultDirName={pf}\Avis
DefaultGroupName=Avis
SourceDir=..
OutputDir=..\..\build
LicenseFile=license.rtf

[Files]
; Make a backup of wrapper config
Source: "{app}\config\wrapper.conf"; DestDir: "{app}\config\wrapper.conf.orig"; Flags: external skipifsourcedoesntexist

; Router
Source: "..\..\lib\avis-router.jar"; DestDir: "{app}\lib"
Source: "..\..\etc\avisd.config"; DestDir: "{app}\config"; Flags: onlyifdoesntexist
Source: "..\..\doc\LICENSE.txt"; DestDir: "{app}"

; Client utilities
Source: "..\..\..\client\lib\avis-client.jar"; DestDir: "{app}\lib"
Source: "..\..\..\client\lib\avis-tools.jar"; DestDir: "{app}\lib"
Source: "..\..\..\client\bin\ec.cmd"; DestDir: "{app}\bin"
Source: "..\..\..\client\bin\ep.cmd"; DestDir: "{app}\bin"

; Service wrapper
Source: "service\lib\wrapper.jar"; DestDir: "{app}\lib"
Source: "service\lib\wrapper.dll"; DestDir: "{app}\lib"
Source: "service\bin\wrapper.exe"; DestDir: "{app}\bin"
Source: "service\bin\avis.cmd"; DestDir: "{app}\bin"
Source: "service\bin\install_avis_service.cmd"; DestDir: "{app}\bin"
Source: "service\bin\uninstall_avis_service.cmd"; DestDir: "{app}\bin"
Source: "service\bin\start_avis_service.cmd"; DestDir: "{app}\bin"
Source: "service\bin\stop_avis_service.cmd"; DestDir: "{app}\bin"
Source: "service\config\wrapper.conf"; DestDir: "{app}\config"; Flags: onlyifdoesntexist
Source: "service\logs\empty.log"; DestDir: "{app}\logs"; DestName: "avis.log"; Flags: onlyifdoesntexist

; JRE Files
; Source: "@jre_installer@"; DestDir: "{app}"; DestName: "jre.exe"; Flags: deleteafterinstall

[Tasks]
Name: installservice; Description: "Install as a system service"
Name: programicon; Description: "Create icons in the Programs menu"

[Icons]
Name: "{group}\Run Avis In Console"; Filename: "{app}\bin\avis.cmd"; WorkingDir: "{app}\bin"; Tasks: programicon
Name: "{group}\Start Avis Service"; Filename: "{app}\bin\start_avis_service.cmd"; WorkingDir: "{app}\bin"; Tasks: programicon
Name: "{group}\Stop Avis Service"; Filename: "{app}\bin\stop_avis_service.cmd"; WorkingDir: "{app}\bin"; Tasks: programicon
Name: "{group}\Avis Config File"; Filename: "{app}\config\avisd.config"; WorkingDir: "{app}\config"; Tasks: programicon

[Run]
Filename: "{app}\bin\uninstall_avis_service.cmd"; Description: "Install as a system service"; StatusMsg: "Uninstalling existing Avis service..."; Flags: runhidden; Tasks: installservice
Filename: "{app}\bin\install_avis_service.cmd"; Description: "Install as a system service"; StatusMsg: "Installing Avis as a service..."; Flags: runhidden; Tasks: installservice
Filename: "{app}\bin\start_avis_service.cmd"; Description: "Start the Avis service"; StatusMsg: "Starting the Avis service..."; Flags: runhidden; Tasks: installservice
; Filename: "{app}\jre.exe"; Parameters: "/s /v""/qn ADDLOCAL=jrecore IEXPLORER=1 MOZILLA=1"" "; Description: "Install Java runtime"; StatusMsg: "Installing Java..."; Check: JavaCheck

[UninstallRun]
Filename: "{app}\bin\uninstall_avis_service.cmd"; Flags: runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{app}\logs"
