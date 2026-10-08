# Exalted.589 — botões móveis e assinatura persistente

## Botões em telas de celular
- `InkButtonSize` foi recalibrado para dimensões nominais menores em telas compactas.
- Dimensões `customWidth/customHeight` agora respeitam o valor pedido, com piso de toque de 48 dp; o piso global anterior de 96 dp inflava controles de ícone, +/- e ajustes numéricos.
- A pincelada deixou de ser escalada de forma independente nos eixos X/Y. Em larguras estreitas ela usa escala uniforme pela altura e recorte central, evitando o aspecto horizontalmente comprimido.
- O ajuste automático de fonte e até duas linhas permanece ativo para rótulos longos.
- Nenhum campo, texto, ação ou regra de negócio foi alterado.

## Assinatura persistente
O Gradle aceita uma chave persistente quando o CI fornece:
- `EXALTED_KEYSTORE_PATH`
- `EXALTED_KEYSTORE_PASSWORD`
- `EXALTED_KEY_ALIAS`
- `EXALTED_KEY_PASSWORD`

O GitHub Actions pode materializar temporariamente o keystore a partir dos secrets:
- `EXALTED_KEYSTORE_BASE64`
- `EXALTED_KEYSTORE_PASSWORD`
- `EXALTED_KEY_ALIAS`
- `EXALTED_KEY_PASSWORD`

O arquivo JKS não é armazenado no repositório. Se os secrets ainda não existirem, o workflow continua compilando com a chave debug e registra `PERSISTENT_SIGNING=false`; isso evita quebrar CI durante a migração, mas a identidade persistente só passa a valer depois da configuração dos quatro secrets.
