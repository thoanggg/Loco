
import http.server
import socketserver
import json
import socket

PORT = 9876

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/ping':
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"pong|Administrator|MOCK-WIN-SERVER")
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == '/get-logs':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            
            print(f"Received request: {post_data.decode('utf-8')}")
            
            # Return Mock XML
            xml_response = """
<Events>
    <Event xmlns='http://schemas.microsoft.com/win/2004/08/events/event'>
        <System>
            <Provider Name='Microsoft-Windows-Security-Auditing' Guid='{54849625-5478-4994-A5BA-3E3B0328C30D}'/>
            <EventID>4624</EventID>
            <Version>2</Version>
            <Level>0</Level>
            <Task>12544</Task>
            <Opcode>0</Opcode>
            <Keywords>0x8020000000000000</Keywords>
            <TimeCreated SystemTime='2023-10-27T10:00:00.000000000Z'/>
            <EventRecordID>1</EventRecordID>
            <Correlation/>
            <Execution ProcessID='4' ThreadID='123'/>
            <Channel>Security</Channel>
            <Computer>MOCK-WIN-SERVER</Computer>
            <Security/>
        </System>
        <EventData>
            <Data Name='SubjectUserSid'>S-1-5-18</Data>
            <Data Name='SubjectUserName'>MOCK-WIN-SERVER$</Data>
            <Data Name='TargetUserSid'>S-1-5-21-1234567890-1234567890-1234567890-500</Data>
            <Data Name='TargetUserName'>Administrator</Data>
            <Data Name='LogonType'>2</Data>
        </EventData>
    </Event>
    <Event xmlns='http://schemas.microsoft.com/win/2004/08/events/event'>
        <System>
            <Provider Name='Microsoft-Windows-Sysmon' Guid='{5770385F-C22A-43E0-BF4C-06F5698FFBD9}'/>
            <EventID>1</EventID>
            <Version>5</Version>
            <Level>4</Level>
            <Task>1</Task>
            <Opcode>0</Opcode>
            <Keywords>0x8000000000000000</Keywords>
            <TimeCreated SystemTime='2023-10-27T10:05:00.000000000Z'/>
            <EventRecordID>2</EventRecordID>
            <Correlation/>
            <Execution ProcessID='1000' ThreadID='1000'/>
            <Channel>Microsoft-Windows-Sysmon/Operational</Channel>
            <Computer>MOCK-WIN-SERVER</Computer>
            <Security UserID='S-1-5-18'/>
        </System>
        <EventData>
            <Data Name='RuleName'>-</Data>
            <Data Name='UtcTime'>2023-10-27 10:05:00.000</Data>
            <Data Name='ProcessGuid'>{A0B1C2D3-E4F5-6789-0100-000000000000}</Data>
            <Data Name='ProcessId'>1234</Data>
            <Data Name='Image'>C:\Windows\System32\cmd.exe</Data>
            <Data Name='CommandLine'>cmd.exe /c whoami</Data>
            <Data Name='User'>MOCK-WIN-SERVER\Administrator</Data>
        </EventData>
    </Event>
</Events>
            """
            self.send_response(200)
            self.send_header('Content-type', 'application/xml')
            self.end_headers()
            self.wfile.write(xml_response.encode('utf-8'))

with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Mock Agent serving at port {PORT}")
    httpd.serve_forever()
