# Exalted.825 — plano de validação de layout (pré-remodelação)

Referência: Exalted.825, última compilação confirmada pelo usuário. Este documento é uma auditoria estática e **não** comprova defeitos visuais.

## Evidências do código

- `InkButton.kt`: o botão compartilhado calcula `width` e `height` com piso de 48 dp para customizações explícitas, e aplica `modifier.then(Modifier.widthIn(...).height(...))`.
- `InkButton.kt`: a sobrecarga para conteúdo/ícones fornece `customWidth = 52.dp` e `customHeight = 52.dp`.
- `CharmListComponents.kt`: controles de diminuir, aumentar, detalhes e fixação passam `Modifier.size(40.dp)` à sobrecarga de `InkButton`.
- `DialogComponents.kt`: os botões dos diálogos reutilizam `InkButton`.
- `CompactInkButtonContractTest.kt`: testes estáticos preservam ligações funcionais, mas **não medem** limites reais, acessibilidade ou sobreposição.

## Hipótese a testar, não defeito confirmado

A combinação de `Modifier.size(40.dp)` fornecido pelo chamador com `widthIn(min = 48.dp, ...).height(52.dp)` adicionado pelo botão pode produzir limites medidos diferentes do mínimo esperado, conforme restrições de composição e ordem dos modificadores. Não presumir que `coerceAtLeast(48.dp)` garante um alvo de toque efetivo de 48 dp.

## Matriz de validação instrumental

| Cenário | Alvo | Verificação |
| --- | --- | --- |
| Tela estreita | Aba 8, linhas de Encantos com +/- e detalhes | Sem sobreposição; controles e texto acessíveis |
| Fonte ampliada (1,3x e 1,5x) | Encantos e diálogos | Rótulos legíveis, ações não cortadas |
| Três Tipos de Exaltado | Solar, Sangue de Dragão, Lunar | Mesmo comportamento estrutural, cores preservadas |
| Orientação retrato/paisagem | Abas 5, 8, 9, 11, 12, 14 | Sem perda de ações e sem overflow não rolável |
| Diálogo com teclado aberto | SheetScreenDialogs | Conteúdo rolável e ações alcançáveis |
| Toque e acessibilidade | Controles +/- e detalhes | Medir bounds clicáveis reais e semântica, não só dimensões declaradas |
| Modo dividido | Abas 8 e 11 | Adaptação a largura reduzida |
| Mapa maximizado | Aba 14 | Escala em km visível e calibrada |

## Critérios antes de alterar a identidade visual

1. Capturar screenshots de referência em dispositivo/emulador e guardar configuração de tela/fonte.
2. Acrescentar testes Compose instrumentados de bounds e interação com cenários estreitos, sem trocar todas as bibliotecas.
3. Corrigir somente falhas reproduzidas, preservando as APIs e o desenho do `InkButton`.
4. Validar separadamente todas as 15 abas e os três Tipos de Exaltado.
5. Solicitar autorização do usuário antes de executar compilação/workflow. Commits de documentação devem usar `[skip ci]`.

## Não fazer

- Não concluir que Canvas ou dimensões fixas de espaçadores sejam bugs.
- Não fixar o valor 40 dp em testes de regressão como requisito imutável.
- Não migrar indiscriminadamente a arquitetura ou substituir `InkButton`.
- Não declarar teste de interface aprovado com base apenas em busca textual no código.
