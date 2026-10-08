# Cores Temáticas dos Botões

## Alteração
Os botões baseados em `InkButton` agora acompanham a paleta do tipo de Exaltado ativo.

- Solar: dourado/âmbar.
- Sangue de Dragão: vermelho/rubi.
- Lunar: prata.
- Botões destrutivos (`Danger`) continuam vermelhos por semântica de ação.

O estado selecionado também usa a paleta temática ativa, em vez de uma cor dourada fixa.

A implementação utiliza os estados reativos existentes em `Color.kt`, evitando capturar a paleta Solar quando a planilha é trocada para outro tipo.
