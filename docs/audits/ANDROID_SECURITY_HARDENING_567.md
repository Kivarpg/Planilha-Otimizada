# Exalted.567 — Android security hardening

Base: Exalted.566.

A auditoria confirmou que várias proteções adequadas ao perfil do Exalted já
existiam: sem INTERNET, sem WebView/cliente HTTP, cleartext desabilitado,
backup Android desabilitado, apenas a Activity launcher exportada, permissão
legada de escrita limitada à API 28, armazenamento privado/app-specific e
R8/resource shrinking em release.

## Mudanças

1. Release declara explicitamente `isDebuggable = false` e
   `isJniDebuggable = false`.
2. Falha de exportação de PDF só envia stack trace ao Logcat em build DEBUG.
3. O arquivo privado `ultimo_erro.txt` continua disponível para diagnóstico,
   mas agora é limitado a 262.144 caracteres.
4. `AndroidSecurityBaselineTest` transforma a superfície mínima em contrato:
   - sem INTERNET;
   - sem MANAGE_EXTERNAL_STORAGE;
   - sem REQUEST_INSTALL_PACKAGES;
   - sem SYSTEM_ALERT_WINDOW;
   - allowBackup=false;
   - usesCleartextTraffic=false;
   - armazenamento legado somente até API 28;
   - exatamente um componente explicitamente exportado;
   - release não depurável, minificado e com shrink;
   - stack traces conhecidos no Logcat somente em DEBUG.
5. O teste integra o Engineering Gate.

## Deliberadamente não adicionados

- biometria/FLAG_SECURE: fichas de RPG não justificam bloquear screenshots
  ou impor autenticação;
- criptografia/Keystore: não há segredo de autenticação, token ou credencial;
- Network Security Config/certificate pinning: o app não possui INTERNET;
- antitamper/root detection: custo e fragilidade sem benefício proporcional;
- permissões adicionais: aumentariam a superfície de ataque.
