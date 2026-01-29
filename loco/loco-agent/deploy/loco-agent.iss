[Setup]
; App Information
AppName=Loco Agent
AppVersion=1.0
AppPublisher=MyCompany
AppPublisherURL=http://www.example.com/
AppSupportURL=http://www.example.com/
AppUpdatesURL=http://www.example.com/
DefaultDirName={autopf}\Loco Agent
DefaultGroupName=Loco Agent
; Output Information
OutputDir=.
OutputBaseFilename=LocoAgentInstaller
Compression=lzma2/ultra64
SolidCompression=yes
; Admin privileges required for Service installation
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64
DisableDirPage=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; The files to be installed
Source: "loco-agent-1.0-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "loco-agent.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "loco-agent.xml"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Uninstall Loco Agent"; Filename: "{uninstallexe}"

[Run]
; Install the Service
Filename: "{app}\loco-agent.exe"; Parameters: "install"; Flags: runhidden waituntilterminated; StatusMsg: "Installing Loco Agent Service..."
; Start the Service
Filename: "{app}\loco-agent.exe"; Parameters: "start"; Flags: runhidden waituntilterminated; StatusMsg: "Starting Loco Agent Service..."

[UninstallRun]
; Stop the Service
Filename: "{app}\loco-agent.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated; RunOnceId: "StopService"
; Uninstall the Service
Filename: "{app}\loco-agent.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated; RunOnceId: "UninstallService"

[Code]
// verify if Java is installed (simple check)
function InitializeSetup(): Boolean;
var
  ErrorCode: Integer;
begin
  if not Exec('java', '-version', '', SW_HIDE, ewWaitUntilTerminated, ErrorCode) then
  begin
    if MsgBox('Java Runtime Environment does not appear to be installed (or not in PATH).' + #13#10 +
              'This application requires Java to run.' + #13#10 +
              'Do you want to continue anyway?', mbConfirmation, MB_YESNO) = IDYES then
    begin
      Result := True;
    end
    else
    begin
      Result := False;
    end;
  end
  else
  begin
    Result := True;
  end;
end;
