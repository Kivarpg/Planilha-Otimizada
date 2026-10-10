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

## Auditoria de controles de busca e indicadores — 2026-10-10
- SearchComponents: FilterChip, SkillSearchModal, NumericFilterRow e MultiSelectChips receberam Modifier opcional e encaminhamento ao componente raiz.
- SkillSearchModal: rascunho agora é remember(currentFilter), permitindo recomeçar a partir de um novo filtro recebido. Atenção: se currentFilter mudar enquanto o diálogo está aberto, alterações locais não aplicadas serão descartadas; comportamento deve ser validado com o fluxo de busca.
- CounterControls: RingStepSymbol agora recebe Modifier opcional e o encaminha ao Box raiz.
- RatingDisplayControls: DiamondPip agora recebe Modifier opcional e o encaminha ao Canvas raiz.
- Cores dos controles permanecem derivadas dos tokens observáveis Exalted*.
- Não compilado; testes de interação e UI pendentes.

## Auditoria de controles de combate e navegador SDD — 2026-10-10
- IniciativaAjusteButton: Modifier opcional adicionado e encaminhado a InkButton; larguras e regras de cores preservadas.
- SangueDeDragaoBrowserDialog: Modifier opcional adicionado e encaminhado aos dois AlertDialogs (navegação e detalhes), preservando borda existente.
- DecorativeComponents: MedallionIcon, PriorityShieldBadge, Starfield, OnyxTexturedBackground e OrnateFlourish já declaram Modifier.
- LunarCasteEmblemButton já declara Modifier.
- Ainda pendente inspeção completa dos demais componentes e teste de build. Não compilar automaticamente.

## Varredura das telas principais e SplashScreen — 2026-10-10
- SplashScreen: Modifier opcional adicionado e encaminhado ao BoxWithConstraints raiz. Animação e controle de toque mantidos.
- Varredura das funções principais *Tab em Abilities, Aspecto, Attributes, BattleGroups, Caste, Charms, Combat, EncounterGenerator, Equipment, Map, Merits, NPCs, PersonalData e Summary: todas possuem Modifier opcional. A função EncounterGeneratorTab possui assinatura longa e o Modifier no final.
- FeiticosTab.kt não define uma função *Tab principal; contém popups auditados anteriormente.
- Varredura dos componentes de arquivos ButtonComponents, CardComponents, CounterControls, DecorativeComponents, DialogComponents, InkButton, LunarCasteEmblemButton, NumericRatingControls, RatingDisplayControls, SearchComponents, TextComponents e SplashScreen: funções visuais públicas identificadas agora possuem Modifier opcional.
- Não confundir auditoria estática de assinaturas com verificação completa de comportamento. Pendente: teste de build, teste de troca Solar/SDD/Lunar, revisão de recomposição dos consumidores e validação visual em aparelho.
- Não iniciar compilação sem autorização.

## Auditoria adicional de estado e Modifier — 2026-10-10
- TemplateSelectionScreen: Modifier opcional e encaminhamento ao Box raiz.
- SheetTabsBar: Modifier opcional e encaminhamento ao Column raiz. Caches de saldo de BP e pontos de Habilidade usam remember(sheet, viewModel), não apenas remember(sheet).
- SheetScreenDialogs: SettingsDialog, ReversionConfirmDialog, CommitmentErrorDialog e PendingMeritBreakDialog agora aceitam Modifier opcional e o encaminham ao AlertDialog com a borda temática preservada.
- Atenção: lembrar por identidade de sheet não substitui uma emissão de novo valor de sheetState quando dados internos mudam; o ViewModel deve publicar atualizações.
- Ainda falta inspecionar os diálogos de gerenciamento e os usos de remember com chaves incompletas. Não compilado.

## Auditoria de diálogos de gerenciamento e salvamento — 2026-10-10
- SheetLifecycleDialogs: cinco composables recebem Modifier opcional; DeleteSheetDialog encaminha ao ConfirmDeleteDialog, demais ao AlertDialog.
- BackupListDialog: cache remember(show, viewModel); não há atualização automática de backups criados durante diálogo aberto.
- CodeShareDialogs: CodeExportResultDialog e CodeImportDialog recebem Modifier opcional e encaminham aos AlertDialog.
- FileManagementDialogs/SaveValidationDialog: removidas chamadas de dismissValidationDialog, onSaveSuccessful e showSnackbar do corpo de composição no caso válido; movidas para LaunchedEffect(viewModel, saveValidationEvent), com rememberUpdatedState para callback e snackbar. Modifier encaminhado ao SaveValidationModal.
- Verificação obrigatória: testar salvar válido/inválido, navegação e Snackbar, inclusive mudança de ViewModel. O parâmetro scope foi mantido por compatibilidade com chamadas existentes.
- Sem compilação, conforme instrução do usuário.

## Auditoria de estado observável — 2026-10-10 (rodada seguinte)
- Color.kt: tokens de paleta dinâmica usam mutableStateOf; não foi encontrada ausência geral de estado nas cores globais.
- AbilitiesTab: filtro de habilidades já possui chaves de remember para subTabIndex, casteAbilities, favoredAbilities e abilities; sem alteração.
- SheetTabsBar: removido remember(sheet, viewModel) dos cálculos de saldo de BP e pontos de Habilidade para impedir resultado desatualizado quando a mesma instância de CharacterSheet contém coleções alteradas. O cálculo ocorre apenas nos ramos visíveis da barra e em recomposições pertinentes. Atenção: isso troca cache por correção de dados, podendo aumentar custo de recomposição; medir após compilação e, se necessário, expor saldos derivados via StateFlow do ViewModel.
- Pendente: confirmar que mutações do modelo disparam emissão em sheetState, pois retirar remember não força recomposição por si só.
- Nenhuma build disparada.

## Auditoria do fluxo de emissão e troca de tipo — 2026-10-10
- SheetViewModel publica CharacterSheet por MutableStateFlow; módulos AbilitiesActions, AttributesActions, CombatActions, CharmsActions, HealthActions, PersonalDataActions, EquipmentActions, MeritsActions e MartialArtsActions foram inspecionados e utilizam update/copy para alterações de estado examinadas.
- FileManagementActions usa atribuição de sheetState.value em operações de substituição completa da planilha, o que emite um novo valor quando diferente. Problema real encontrado: apenas loadSheet sincronizava explicitamente a paleta via aplicarPaletaPorTemplate.
- Correção: createNewSheet, aplicarPlanilhaImportada, restaurarBackupPeriodico e deleteSheet também sincronizam a paleta após a troca. A paleta usa mutableStateOf em Color.kt.
- Pendente: teste de alternância Solar/SDD/Lunar por todos os caminhos; confirmar comportamento de recomposição em componentes que memorizam coleções mutáveis; revisar o fluxo inicial e demais operações de reversão.
- Não houve compilação ou teste no dispositivo.

## Auditoria da reversão e inicialização — 2026-10-10
- confirmarReversaoPlanilhaConcluida anteriormente substituía sheetState sem chamar onSheetChanged (sincronização dos grupos de batalha) nem reaplicar paleta. Corrigido com estado revertido local, onSheetChanged(revertida) e aplicarPaletaPorTemplate(revertida.tipoPersonagem).
- MainActivity guarda templateEscolhido por rememberSaveable e MainSheetScreen executa iniciarNovaPlanilha(tipoPersonagem) em LaunchedEffect(viewModel, tipoPersonagem). Risco identificado: em recriação de Activity, um template persistido pode divergir do tipo da planilha ativa carregada pelo ViewModel, causando substituição inesperada. NÃO modificado nesta rodada: precisa definir regra de precedência entre escolha inicial e planilha persistida, e teste de reinicialização.
- A atualização de paleta por vários caminhos deve ser validada em testes de Solar, Sangue de Dragão e Lunar.
- Nenhuma compilação iniciada.

## Correção de precedência de estado na inicialização — 2026-10-10
- Antes: MainSheetScreen chamava iniciarNovaPlanilha(tipoPersonagem) em LaunchedEffect, podendo sobrescrever uma planilha carregada após recriação da Activity quando rememberSaveable preservava um template antigo.
- Agora: MainActivity chama iniciarNovaPlanilha apenas no callback explícito de seleção de template; MainSheetScreen observa sheetState e aplica a paleta em LaunchedEffect(sheet.tipoPersonagem). Planilhas carregadas/restauradas passam a determinar a paleta.
- Atenção: MainSheetScreen mantém o parâmetro tipoPersonagem por compatibilidade com o chamador, mas não o usa para reinicializar dados.
- Validação pendente: iniciar os três templates; alternar tipo via carregar/importar/backup; recriar Activity; confirmar que o ID e os campos da planilha não são perdidos. Sem compilação.

## Investigação de bloqueios de fidelidade visual — 2026-10-10
- TextComponents/AutoSizeText: fonte reduzida ficava memorizada mesmo quando mudavam modifier, style, letterSpacing ou minFontSize. Ampliadas chaves de remember para reiniciar o ajuste nessas mudanças.
- TextComponents/AppText: mesmo risco com fittedFontSize; ampliadas chaves de remember para estilo, dimensões via modifier e atributos tipográficos.
- Problema estrutural pendente: AppText (texto com contorno) aplica o mesmo modifier aos dois Text filhos dentro de Box sem modifier. Modifiers de layout, deslocamento, semântica ou interação podem ser duplicados ou não atuar no contêiner como o chamador espera. Necessária refatoração cuidadosa para modifier no Box e fillMaxWidth/textAlign nos filhos, com teste visual, pois o comportamento anterior corrigia centralização em vários campos.
- InkButton: o Box raiz aplica modifier externo seguido de Modifier.size(width,height) ou fillMaxWidth/height; tamanhos mínimos de 48.dp e tamanhos nominais por InkButtonSize podem prevalecer sobre expectativas de layout e prejudicar reprodução de mockups em Rows estreitas. Necessária matriz de casos (weight, largura fixa, customWidth, texto longo, fontScale).
- GildedCard: ornamentos drawBehind desenhados no próprio Card, inclusive dependência de ExaltedActiveMotif. Uma mudança no layout da aba não elimina automaticamente ornamentos internos. Preservar identidade aprovada; alterações visuais requerem autorização.
- Ainda pendente: varredura de hierarquias, dimensões e testes de layout em todos os componentes e abas; nenhuma compilação.
