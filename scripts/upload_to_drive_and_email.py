#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AUTOMAÇÃO PÓS-COMPILAÇÃO: Upload para Google Drive e Notificação por E-mail
Projeto: Digital_OBDII
Destinatário: gregoruti@gmail.com
"""

import os
import sys
import json
import time
import smtplib
from datetime import datetime
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

# Google API Imports
try:
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload
    from google.oauth2 import service_account
    from google.oauth2.credentials import Credentials
    from google_auth_oauthlib.flow import InstalledAppFlow
    from google.auth.transport.requests import Request
except ImportError:
    print("[ERRO] Bibliotecas do Google não encontradas.")
    print("Execute: pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib")
    sys.exit(1)

# Configurações de Escopo da API
SCOPES = [
    'https://www.googleapis.com/auth/drive.file',
    'https://www.googleapis.com/auth/drive'
]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(BASE_DIR, 'config.json')
CREDENTIALS_FILE = os.path.join(BASE_DIR, 'credentials.json')
TOKEN_FILE = os.path.join(BASE_DIR, 'token.json')

DEFAULT_CONFIG = {
    "sender_email": "gregoruti@gmail.com",
    "gmail_app_password": "As3n4@1981",
    "recipient_email": "gregoruti@gmail.com",
    "drive_folder_name": "Digital_OBDII_APKs",
    "apk_relative_path": "../app/build/outputs/apk/debug/app-debug.apk"
}

def load_or_create_config():
    if not os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
            json.dump(DEFAULT_CONFIG, f, indent=4, ensure_ascii=False)
        print(f"[INFO] Arquivo de configuracao criado em: {CONFIG_FILE}")
        return DEFAULT_CONFIG
    try:
        with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"[AVISO] Erro ao ler config.json, usando padrao: {e}")
        return DEFAULT_CONFIG

def get_drive_service():
    """Autentica na API do Google Drive via OAuth2 ou Conta de Servico."""
    creds = None

    # 1. Verifica se ha token OAuth salvo anteriormente
    if os.path.exists(TOKEN_FILE):
        try:
            creds = Credentials.from_authorized_user_file(TOKEN_FILE, SCOPES)
        except Exception as e:
            print(f"[AVISO] Token expirado ou invalido: {e}")
            creds = None

    # Se credenciais validas existem, verifica se precisa de refresh
    if creds and creds.expired and creds.refresh_token:
        try:
            creds.refresh(Request())
            with open(TOKEN_FILE, 'w', encoding='utf-8') as token:
                token.write(creds.to_json())
        except Exception as e:
            print(f"[AVISO] Falha ao renovar token: {e}")
            creds = None

    # 2. Se nao ha credenciais validas, tenta credentials.json
    if not creds:
        if not os.path.exists(CREDENTIALS_FILE):
            print("\n" + "="*60)
            print("[ATENCAO] Arquivo 'credentials.json' nao encontrado!")
            print("Para conectar ao Google Drive:")
            print("1. Crie credenciais OAuth Desktop ou Conta de Servico no Google Cloud Console")
            print(f"2. Baixe o JSON e salve exatamente como:\n-> {CREDENTIALS_FILE}")
            print("="*60 + "\n")
            return None

        try:
            with open(CREDENTIALS_FILE, 'r', encoding='utf-8') as f:
                cred_data = json.load(f)

            # Verifica se eh uma Conta de Servico ou OAuth Desktop
            if cred_data.get("type") == "service_account":
                print("[INFO] Autenticando via Conta de Servico...")
                creds = service_account.Credentials.from_service_account_file(
                    CREDENTIALS_FILE, scopes=SCOPES
                )
            else:
                print("[INFO] Autenticando via OAuth Desktop Flow...")
                flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_FILE, SCOPES)
                creds = flow.run_local_server(port=0)
                # Salva o token para nao precisar autenticar novamente
                with open(TOKEN_FILE, 'w', encoding='utf-8') as token:
                    token.write(creds.to_json())
                print("[SUCESSO] Token OAuth salvo em token.json.")

        except Exception as e:
            print(f"[ERRO] Falha ao carregar credentials.json: {e}")
            return None

    return build('drive', 'v3', credentials=creds)

def find_or_create_folder(service, folder_name):
    """Encontra ou cria a pasta no Google Drive."""
    try:
        query = f"name='{folder_name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        results = service.files().list(q=query, spaces='drive', fields='files(id, name)').execute()
        files = results.get('files', [])

        if files:
            print(f"[INFO] Pasta '{folder_name}' encontrada no Drive (ID: {files[0]['id']})")
            return files[0]['id']

        # Se nao existe, cria a pasta
        file_metadata = {
            'name': folder_name,
            'mimeType': 'application/vnd.google-apps.folder'
        }
        folder = service.files().create(body=file_metadata, fields='id').execute()
        print(f"[INFO] Pasta '{folder_name}' criada com sucesso (ID: {folder.get('id')})")
        return folder.get('id')
    except Exception as e:
        print(f"[AVISO] Nao foi possivel verificar/criar pasta: {e}. O arquivo sera salvo na raiz.")
        return None

def upload_apk_to_drive(service, apk_path, folder_id=None):
    """Faz upload resumable do APK e define permissoes de compartilhamento."""
    file_size_mb = os.path.getsize(apk_path) / (1024 * 1024)
    timestamp_str = datetime.now().strftime("%Y%m%d_%H%M%S")
    target_filename = f"Digital_OBDII_debug_{timestamp_str}.apk"

    print(f"\n[UPLOAD] Iniciando envio para o Google Drive: {target_filename} ({file_size_mb:.2f} MB)...")

    file_metadata = {'name': target_filename}
    if folder_id:
        file_metadata['parents'] = [folder_id]

    media = MediaFileUpload(
        apk_path,
        mimetype='application/vnd.android.package-archive',
        resumable=True,
        chunksize=1024 * 1024 * 5 # Chunks de 5MB
    )

    request = service.files().create(
        body=file_metadata,
        media_body=media,
        fields='id, name, webViewLink, webContentLink'
    )

    response = None
    last_print = 0
    while response is None:
        status, response = request.next_chunk()
        if status:
            progress = int(status.progress() * 100)
            if progress >= last_print + 20 or progress == 100:
                print(f" -> Progresso do Upload: {progress}%")
                last_print = progress

    file_id = response.get('id')
    web_view_link = response.get('webViewLink')
    web_content_link = response.get('webContentLink')

    print(f"[SUCESSO] Upload concluido! File ID: {file_id}")

    # Define permissao de acesso para quem tiver o link
    try:
        service.permissions().create(
            fileId=file_id,
            body={'type': 'anyone', 'role': 'reader'},
            fields='id'
        ).execute()
        print("[INFO] Permissao publica de leitura/download via link ativada.")
    except Exception as e:
        print(f"[AVISO] Nao foi possivel alterar permissao publica: {e}")

    download_url = f"https://drive.google.com/uc?export=download&id={file_id}"

    return {
        "file_id": file_id,
        "filename": target_filename,
        "view_link": web_view_link or f"https://drive.google.com/file/d/{file_id}/view",
        "download_link": download_url,
        "size_mb": file_size_mb
    }

def send_notification_email(config, upload_info):
    """Envia o e-mail formatado com o link para download do APK."""
    sender = config.get("sender_email")
    password = config.get("gmail_app_password", "").strip()
    recipient = config.get("recipient_email")

    if not password:
        print("\n" + "="*60)
        print("[ATENCAO] Senha de App do Gmail nao configurada em config.json!")
        print("Para enviar e-mails automaticamente, voce precisa de uma Senha de App do Google.")
        print("1. Acesse: https://myaccount.google.com/apppasswords")
        print("2. Crie uma senha chamada 'Digital OBDII'")
        print("3. Cole a senha de 16 letras no arquivo config.json no campo 'gmail_app_password'")
        print("="*60)
        print(f"\n[LINK DO APK GERADO NO DRIVE]:\n{upload_info['view_link']}\n")
        return False

    now_str = datetime.now().strftime("%d/%m/%Y as %H:%M:%S")

    subject = f"[Digital_OBDII] Novo APK Compilado Disponivel ({upload_info['size_mb']:.1f} MB)"

    text_body = f"""Ola Gregor,

Uma nova compilacao do Digital OBD-II foi concluida com sucesso!

Detalhes do Build:
- Arquivo: {upload_info['filename']}
- Data/Hora: {now_str}
- Tamanho: {upload_info['size_mb']:.2f} MB
- Link de Download (Google Drive):
  {upload_info['view_link']}

Link Direto: {upload_info['download_link']}

---
Enviado automaticamente pelo script de pos-compilacao.
"""

    html_body = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0d11; color: #e0e0e0; margin: 0; padding: 20px; }}
    .card {{ max-width: 580px; margin: 0 auto; background: #161922; border-radius: 16px; border: 1px solid #282c38; overflow: hidden; box-shadow: 0 8px 24px rgba(0,0,0,0.5); }}
    .header {{ background: linear-gradient(135deg, #1a2285, #424df0); padding: 24px; text-align: center; color: white; }}
    .header h1 {{ margin: 0; font-size: 22px; font-weight: 700; }}
    .header p {{ margin: 6px 0 0 0; font-size: 13px; opacity: 0.85; }}
    .content {{ padding: 24px; }}
    .table {{ width: 100%; border-collapse: collapse; margin-bottom: 24px; }}
    .table td {{ padding: 10px 12px; border-bottom: 1px solid #232734; font-size: 14px; }}
    .table td.label {{ color: #8a93a6; font-weight: 600; width: 35%; }}
    .table td.value {{ color: #ffffff; font-weight: 500; }}
    .btn-container {{ text-align: center; margin: 30px 0 15px 0; }}
    .btn {{ display: inline-block; background: #00E676; color: #000000 !important; font-weight: 700; font-size: 16px; text-decoration: none; padding: 14px 32px; border-radius: 12px; box-shadow: 0 4px 14px rgba(0,230,118,0.4); }}
    .btn:hover {{ background: #00c853; }}
    .footer {{ text-align: center; padding: 16px; font-size: 11px; color: #606877; border-top: 1px solid #1f232e; }}
  </style>
</head>
<body>
  <div class="card">
    <div class="header">
      <h1>Digital OBD-II - Build Concluido</h1>
      <p>O arquivo APK foi carregado com sucesso no seu Google Drive</p>
    </div>
    <div class="content">
      <table class="table">
        <tr>
          <td class="label">Arquivo:</td>
          <td class="value"><code>{upload_info['filename']}</code></td>
        </tr>
        <tr>
          <td class="label">Compilacao:</td>
          <td class="value">{now_str}</td>
        </tr>
        <tr>
          <td class="label">Tamanho:</td>
          <td class="value">{upload_info['size_mb']:.2f} MB</td>
        </tr>
        <tr>
          <td class="label">Status:</td>
          <td class="value" style="color: #00E676;">Pronto para Instalacao</td>
        </tr>
      </table>

      <div class="btn-container">
        <a href="{upload_info['view_link']}" class="btn" target="_blank">BAIXAR APK NO GOOGLE DRIVE</a>
      </div>
      <p style="text-align: center; font-size: 12px; color: #8a93a6;">
        Link direto: <br>
        <a href="{upload_info['view_link']}" style="color: #424df0; word-break: break-all;">{upload_info['view_link']}</a>
      </p>
    </div>
    <div class="footer">
      Automacao de Pos-Compilacao Gradle - Digital_OBDII
    </div>
  </div>
</body>
</html>
"""

    msg = MIMEMultipart('alternative')
    msg['Subject'] = subject
    msg['From'] = f"Digital OBD-II CI <{sender}>"
    msg['To'] = recipient

    msg.attach(MIMEText(text_body, 'plain', 'utf-8'))
    msg.attach(MIMEText(html_body, 'html', 'utf-8'))

    print(f"[EMAIL] Enviando e-mail para {recipient} via smtp.gmail.com...")
    try:
        server = smtplib.SMTP_SSL('smtp.gmail.com', 465, timeout=15)
        server.login(sender, password)
        server.sendmail(sender, recipient, msg.as_string())
        server.quit()
        print(f"[SUCESSO] E-mail de notificacao enviado com sucesso para {recipient}!")
        return True
    except Exception as e:
        print(f"[ERRO] Falha ao enviar e-mail: {e}")
        return False

def main():
    print("=" * 60)
    print(" ROTINA DE DISTRIBUICAO AUTOMATICA (Google Drive + E-mail)")
    print("=" * 60)

    config = load_or_create_config()

    # 1. Localiza o APK
    if len(sys.argv) > 1:
        apk_path = os.path.abspath(sys.argv[1])
    else:
        apk_path = os.path.abspath(os.path.join(BASE_DIR, config.get("apk_relative_path", "")))

    if not os.path.exists(apk_path):
        print(f"[ERRO] Arquivo APK nao encontrado em:\n{apk_path}")
        print("Certifique-se de compilar o app antes com: gradlew :app:assembleDebug")
        sys.exit(1)

    print(f"[OK] APK encontrado: {apk_path}")

    # 2. Conecta ao Google Drive
    service = get_drive_service()
    if not service:
        sys.exit(1)

    # 3. Encontra ou cria pasta no Drive
    folder_id = find_or_create_folder(service, config.get("drive_folder_name", "Digital_OBDII_APKs"))

    # 4. Faz Upload do APK
    upload_info = upload_apk_to_drive(service, apk_path, folder_id)

    # 5. Envia Notificacao por E-mail
    send_notification_email(config, upload_info)

    print("\n" + "=" * 60)
    print(" PROCESSO CONCLUIDO COM SUCESSO!")
    print("=" * 60)

if __name__ == "__main__":
    main()
