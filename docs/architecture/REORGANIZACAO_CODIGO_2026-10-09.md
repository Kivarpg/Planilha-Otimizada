# Reorganização estrutural do Exalted — 2026-10-09

## Inventário verificado
- 511 arquivos Kotlin no repositório, incluindo testes.
- app/src/main/java/com/example/data: 148 arquivos.
- app/src/main/java/com/example/ui/tabs: 47 arquivos.
- app/src/main/java/com/example/viewmodel: 21 arquivos.
- app/src/main/java/com/example/model: 18 arquivos.
- app/src/main/java/com/example/ui/components: 17 arquivos.
- app/src/main/java/com/example/iniciativas: 14 arquivos.
- app/src/test/java/com/example/data: 153 arquivos.

## Diretrizes
1. Preservar comportamento e regras de Exalted; não renomear funcionalidades ou abas.
2. Manter Solares, Sangue de Dragão e Lunares com igual prioridade.
3. Não mover arquivos Kotlin em massa: caminhos, package, imports e referências devem ser atualizados juntos.
4. Organizar progressivamente por responsabilidade: model (entidades), data (persistência e catálogos), domain (regras), viewmodel (estado/orquestração), ui/tabs (telas), ui/components (componentes).
5. Separar UI de regras de construção de NPCs e persistência. Não misturar refatoração estrutural com otimizações de algoritmo.
6. Preservar testes de equivalência para Encantos, pré-requisitos, XP, feitiçaria e NPCs.
7. Compilar somente após autorização explícita.

## Alterações executadas nesta rodada
- SheetContentArea.kt: organização de imports, formatação e extração do pincel do fundo opaco.
- Implementações experimentais do panorama removidas anteriormente; não reintroduzir.

## Pendências — executar em lotes
1. Catalogar arquivos por domínio e suas dependências; identificar ciclos de importação.
2. Identificar arquivos duplicados, obsoletos e não referenciados, sem excluir por suposição.
3. Separar os arquivos de UI excessivamente extensos em funções e componentes internos mantendo APIs.
4. Extrair regras de negócio de ViewModels grandes com testes de regressão.
5. Consolidar utilitários duplicados de data e normalizar nomes de arquivos.
6. Executar testes unitários e compilação autorizada; corrigir eventuais regressões.

## Critério de conclusão
A reorganização somente estará concluída quando os lotes forem executados e os testes e a compilação forem aprovados. Este documento não representa conclusão da refatoração total.

## Segunda rodada — checkpoint pré-compilação
- SheetScreen.kt: removida importação não utilizada de OnyxTexturedBackground; composição Scaffold reorganizada sem mudança de parâmetros.
- Arquivos extensos identificados: EncounterNpcCard.kt (74.895 bytes), EncounterGeneratorTab.kt (55.798), MapTab.kt (44.592), WeaponSection.kt (43.622).
- Refatoração da Aba 11 deliberadamente adiada até haver cobertura de testes e validação de comportamento.
- Nenhuma mudança intencional em lógica de XP, Encantos, NPCs ou persistência.

## Próximo portão
**Solicitar uma compilação de validação antes de prosseguir com extrações estruturais maiores.**
A compilação valida sintaxe e dependências, mas não substitui testes de regressão nem validação visual.

## Terceira rodada — manutenção de baixo risco
- SheetContentArea.kt: imports explícitos de Compose, espaçamento e blocos de composição padronizados.
- A estrutura do layout, os gradientes, as dimensões e o gerenciamento de estado permanecem equivalentes.
- Sem movimentação de packages, mudanças em API pública ou lógica de negócio.
- O checkpoint segue aguardando compilação e testes. Não extrapolar esta validação para os 511 arquivos.

## Atualização de dependências estáveis — 2026-10-09
- androidx.activity:activity-compose: 1.9.3 -> 1.13.0 (versão final estável).
- androidx.core:core-splashscreen: 1.0.1 -> 1.2.0 (versão final estável).
- Alterações limitadas a gradle/libs.versions.toml.
- Não atualizar Lifecycle 2.11.0 neste lote: sua linha recente de Compose exige atenção a compileSdk e compatibilidade; avaliar em checkpoint separado.
- Não atualizar Kotlin, AGP, Gradle ou Compose BOM em conjunto.
- Compilação e testes de regressão obrigatórios antes de considerar as atualizações validadas.

## Auditoria Modifier e recomposição — 2026-10-10
Correções concretas: MainSheetScreen (Modifier no Scaffold), SheetTopBar (Modifier no Column), SheetContentArea (Modifier no Box) e EncounterGeneratorTab (Modifier no Column raiz). APIs preservadas por parâmetros opcionais finais; sem mudanças em cálculos de NPC.

Achados de estado: MainSheetScreen observa sheetState via collectAsState; EncounterGeneratorTab observa npcsEncontro, gerandoNpcEncontro e erroNpcEncontro via collectAsState; o estado de seleção da Aba 11 utiliza rememberSaveable. Nem todo texto fixo deve virar mutableStateOf: títulos estáticos são conteúdo; cores derivadas de paleta global precisam ser observáveis na fonte da verdade, não copiadas para remember local. A paleta ExaltedActiveMotif e cores globais são suspeitas de falta de invalidação de recomposição, exigindo inspeção da implementação e dos pontos de atualização.

Escopo: auditoria parcial dos principais pontos de entrada, NÃO confirmação de que todos os composables possuem Modifier. Não foi feita compilação nem teste visual deste lote.

## Auditoria de recomposição — continuação (2026-10-10)
- Color.kt já mantém ExaltedActiveMotif e os tokens dinâmicos em mutableStateOf; a hipótese de que eram variáveis comuns estava incorreta.
- Theme.kt calcula darkColorScheme no corpo do composable e lê os tokens observáveis.
- Achado concreto: MainSheetScreen executava iniciarNovaPlanilha() e aplicarPaletaPorTemplate() como efeitos colaterais dentro de remember(tipoPersonagem), durante a composição.
- Correção: transferidos para LaunchedEffect(viewModel, tipoPersonagem), com chave explícita; nenhuma mudança no algoritmo da paleta.
- ATENÇÃO: a execução agora ocorre após a composição; verificar visualmente a primeira renderização e a troca de template para descartar flash de paleta anterior, e verificar persistência ao retornar ao app.
- Compilação e testes instrumentados ainda não executados. Não presumir equivalência funcional sem validação.

## Auditoria de lifecycle — 2026-10-10
- MainSheetScreen: DisposableEffect passou de chave única lifecycleOwner para chaves lifecycleOwner e viewModel.
- Motivo: evitar callback de foreground/background retendo ViewModel antigo caso o parâmetro seja substituído.
- A inscrição e remoção do LifecycleEventObserver continuam simétricas.
- A proposta de executar aplicarPaletaPorTemplate em SideEffect a cada recomposição foi rejeitada por potencial repetição de escritas de estado e recomposições desnecessárias.
- Pendente: teste de primeira renderização de cada template após a mudança anterior para LaunchedEffect; se houver flash de cor, resolver na fonte de estado de tema, não reescrevendo paleta a cada frame.
- Sem compilação nesta rodada.

## Auditoria Modifier por aba — 2026-10-10
- Inspecionadas assinaturas de AbilitiesTab, MeritsTab, EquipmentTab, CharmsTab, SummaryTab, NPCsTab, MapTab, BattleGroupsTab, CasteTabSolar.
- BattleGroupsTab (aba 13) não aceitava Modifier e usava Modifier.fillMaxSize() no Column raiz. Corrigido com parâmetro opcional e encaminhamento ao Column.
- As demais assinaturas inspecionadas já declaravam Modifier opcional. Isso não prova que todos os nós internos o propagam corretamente.
- Os campos editáveis da aba 13 usam remember/mutableStateOf e battleGroups é observado via collectAsState; valores iniciais e rótulos constantes não exigem UIState próprio.
- Ainda não foi feita validação por build ou instrumentação.

## Auditoria de dialogs e cache de estado — 2026-10-10
- Confirmado encaminhamento de Modifier nas telas AspectoTab, CasteTab, SolarCasteTabContent e LunarCasteTabContent, inclusive ramos sem casta selecionada.
- FeiticosPopup e FeiticoDetailsDialog passaram a receber Modifier opcional e encaminhá-lo ao AlertDialog, preservando gildedDialogBorder.
- FeiticosPopup calculava circulosDesbloqueados com remember(sheet), ignorando troca de instância de SheetViewModel. Chave ajustada para remember(sheet, viewModel).
- O cálculo de círculos ainda depende de sheet; se houver mutação interna sem emissão de novo sheetState, deve ser investigada no ViewModel.
- Sem compilação ou verificação visual; manter status não validado.

## Auditoria de componentes compartilhados — 2026-10-10
- InkButton, InkGhostButton, GildedCard, LongPressCard, SectionHeader: parâmetro Modifier e encaminhamento confirmados por inspeção.
- EncantoQuadroBox: parâmetro Modifier opcional adicionado, encaminhado ao Column raiz.
- ConfirmDeleteDialog e SaveValidationModal: Modifier opcional adicionado, encaminhado ao AlertDialog antes de gildedDialogBorder().
- Valores de textos de confirmação são conteúdo estático ou parâmetros; não converter indiscriminadamente para mutableStateOf.
- Mudanças compatíveis com chamadas posicionais existentes por acrescentar parâmetros opcionais ao final.
- Não compilado; verificar via build quando autorizado.
