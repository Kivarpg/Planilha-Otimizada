# Plano visual 329 — referência Solar / Sangue de Dragão / Lunar

## Arquitetura Compose
- `ExaltedVisualBlueprints`: fonte única de tokens por linhagem.
- `exaltedTabIdentity`: cenário periférico, moldura e atmosfera compartilhados pelas 15 abas.
- `exaltedSectionPanel`: painel de conteúdo; conteúdo e callbacks permanecem intactos.
- `SheetTopBar` + `SheetTabsBar`: cabeçalho e navegação tratados como peças da moldura, não como Material app bars.
- `InkButton`: controle padrão do app, sem linhas ornamentais entre botões.
- Canvas/DrawScope para molduras, meandros, nuvens abstratas e brilhos; `Path` para curvas; `Brush` para luz/metal.

## Plano individual das 15 abas
1. Dados Pessoais — cabeçalho de identidade + grupos de campos em painéis de obsidiana.
2. Casta/Aspecto — seletores centrais em cards-selo; Solar/DB/Lunar com motivos próprios.
3. Atributos — três blocos principais, hierarquia forte e dots/ratings integrados.
4. Habilidades — listas densas em painéis, mantendo legibilidade e navegação interna.
5. Combate — iniciativa/vitalidade/defesas como instrumentos de painel, sem estética Material.
6. Méritos — cards ornamentais compactos, leitura vertical clara.
7. Equipamentos — inventário e controles em placas escuras com metal periférico.
8. Encantos — árvore/lista como foco central, requisitos visualmente subordinados.
9. Planilha — resumo em painéis de leitura; vitalidade e Limpar integrados ao mesmo sistema.
10. NPCs — biblioteca em cards funcionais, sem inventar conteúdo.
11. Encontros — gerador dividido em seções claras; InkButtons sem linhas entre si.
12. Conflito — controles de iniciativa e participantes com contraste alto e moldura própria.
13. Grupos de Batalha — blocos de grupo/estatística com mesma gramática de painel.
14. Mapa — moldura periférica; área do mapa preservada e não coberta por decoração.
15. Tradutor — entrada/saída em painel limpo; ornamentação apenas periférica.

## Critérios de conferência
- Centro majoritariamente preto; cor concentrada em moldura, seleção e periferia.
- Solar = ouro quente e halo solar; DB = rubi/nuvens, sem círculo/zigue-zague; Lunar = prata/azul e crescente.
- Cantoneiras e moldura visíveis, sem atravessar campos ou botões.
- Nenhum campo, rótulo, regra ou callback novo.
- Todas as 15 abas usam a mesma infraestrutura, mas componentes internos devem ser refinados individualmente nas próximas passagens.
