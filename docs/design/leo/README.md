# Piloto LEO em revisão — 12/10/2026

## Correção de direção — cenários ilustrados

A primeira proposta e os arquivos `preview-compact.png` / `preview.html`
foram rejeitados pelo usuário por distância artística da referência. São
registros históricos, não evidência do estado atual nem direção aprovada.

Foram gerados três cenários independentes com a ferramenta nativa imagegen,
usando `image(8).png` como referência de estilo. Assets de produção:
`drawable-nodpi/leo_scene_solar.png`, `leo_scene_dragon.png` e
`leo_scene_lunar.png`. São pinturas sem textos, campos, cartões ou botões.

Prompts: retrato 2:3, arquitetura oriental e nuvens concentradas nas bordas,
centro preto legível, sem pessoas, UI, logo ou molduras. Solar: nuvens âmbar
e pagodes com luz dourada, sem disco solar. Dragão: rubi/ônix, dragão oriental
sinuoso no canto superior direito e nuvens vermelhas, sem dourado. Lunar:
prata/azul noturno, névoa, pagodes e flores claras, sem disco lunar.

Integração local: `LeoSection(illustrated = true)` apenas nos três seletores
de Casta/Aspecto da aba Dados Pessoais. Imagem decorativa atrás de controles
Compose, dimensionada pelo conteúdo, sem interceptação de toque. Cartões
continuam opacos para legibilidade. Nenhuma lógica ou callback foi alterado.

Cabeçalho agora usa o cenário do tipo e o logo oficial existente, sem alterar
as ações dos menus. Em janelas com altura menor que 480 dp, a marca decorativa
é omitida para preservar espaço. A aba Dados Pessoais também possui cenário
atrás das margens e intervalos; os campos continuam sobre superfícies opacas.

Pendente: refinamento da continuidade entre recortes, adaptação visual ampla,
validação de contraste e capturas Android. Não compilar sem autorização;
não propagar às 15 abas como se este piloto já estivesse aprovado.


Base: `main` em `b51c4ba189c0661f452d722fbe89b46c40f1e9c7`, projeto Exalted.945.
Esta proposta não incrementa a versão e não autoriza compilação.

## Escopo real

Piloto de cabeçalho, navegação e aba 1 — Dados Pessoais, em Solar,
Sangue de Dragão e Lunar. **Não é a remodelação concluída das 15 abas.**
O documento mestre v2.4, seções 30.9–30.12, exige aprovação da composição
piloto e capturas Android antes de propagação. As correções atuais do
repositório são preservadas; a indicação histórica de voltar à 567 não
substitui o pedido atual de usar o código do GitHub.

Referências visualizadas: LEO(4).png, Leo 00(2).png, Leo 01(2).png e
Leo 02(2).png. Documento consultado: Especificação Mestra v2.4.

## Decisões aplicadas

- Emblemas originais de Casta e Aspecto reutilizados, sem alterar os arquivos.
- Molduras duplas, cantos geométricos distintos e superfície escura.
- Solar: ouro; Sangue de Dragão: rubi/ônix; Lunar: prata/azul frio.
- Grade 3+2 para Solar/Dragão e 2+2 para Lunar. Em espaço muito estreito
  ou fonte acima de 1,3x, os grupos de três passam para duas colunas.
- Textos dos cartões podem quebrar linha. Seleção comunica cor, contorno
  e marcador; foco por teclado e pressão têm tratamento separado.
- Navegação conserva nomes, ordem, rolagem e callbacks, com fonte maior,
  linha luminosa na seleção e sem divisórias entre itens.
- Campos Nome, Jogador, Conceito, Descrição da Anima, Idioma e Intimidades
  mantêm suas ações e dados. O botão continua sendo InkButton.
- A moldura é desenho em cache atrás do conteúdo. Não usa medição
  intrínseca, não recorta os controles nem intercepta cliques.

Não foram reintroduzidos sol/lua decorativos no fundo, linha lateral
amarela, símbolo vermelho sobre títulos, ícones na navegação, pessoas
ou ícones novos de equipamentos. A restrição anterior foi interpretada de maneira ampla demais; cenários
com arquitetura, nuvens e profundidade agora fazem parte do piloto.
Não declarar equivalência artística antes da comparação no Android.

## Evidência e limites

`preview-compact.png` é uma **prancha de composição SVG**, com os assets
originais, e não uma captura do APK. `preview.html` é um estudo navegável
dos estados de seleção; `#wide` mostra o arranjo amplo.
Os dois estudos aproximam o layout e não testam o renderizador Compose.

Sintaxe dos seis arquivos Kotlin alterados/adicionados verificada com
Tree-sitter; diff conferido. Isso **não** resolve tipos, não compila o
projeto, não executa testes JVM e não mede acessibilidade/desempenho.

`LeoIdentityPreview.kt` contém nove prévias Compose de seletores reais:
três tipos × larguras 320/390/840 dp; a menor usa fonte 1,5x. São
preparadas, **não executadas** neste ambiente, que não dispõe de SDK/emulador.
As prévias não substituem capturas da tela completa.

## Próximos critérios

1. Avaliar esta direção visual antes de propagá-la.
2. Na compilação autorizada, verificar Kotlin/JVM e abrir os três tipos.
3. Capturar a tela completa em 320/390/840 dp, fonte 1x/1,5x, teclado aberto,
   nomes longos, seleção, foco, Idioma e Intimidades. Comparar com LEO.
4. Corrigir diferenças da interface real e validar uma tela densa.
5. Só então remodelar individualmente as outras 14 abas.

Para reproduzir os estudos (Python/Pillow e Node/sharp):

```sh
python3 docs/design/leo/render_board.py
node -e "require('sharp')('docs/design/leo/preview-compact.svg').png().toFile('docs/design/leo/preview-compact.png')"
```

## Revisão para Exalted.946 — compilação autorizada em 12/10/2026

Os 14 ícones originais foram comparados byte a byte com origin/main e os
vínculos conferidos: 5 Solares, 5 Aspectos, 4 Lunares (incluindo Sem Casta).
Novo contrato SHA-256 protege esses arquivos contra substituições futuras.
A prévia gerada não é fonte de ícones nem captura do aplicativo.
O teste de feedback foi adaptado ao novo componente, mantendo verificação
do callback de aspecto e do feedbackClickable. As linhas da grade agora
são memorizadas por itens/colunas para evitar recriação na recomposição.
Prévias Compose incluem os cenários; ainda não executadas localmente.
Autorização atual do usuário: revisar, otimizar e enviar para compilar.
