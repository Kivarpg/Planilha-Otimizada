### Exalted.182 — Correção da priorização estrutural de Méritos


## Exalted.183 — Roadmap individual de progressão por XP

- Implementado `EncounterProgressionRoadmap` persistido no `NpcEncontro`.
- A criação do NPC calcula antecipadamente os primeiros 12 lotes de progressão para o tipo de Exaltado e arquétipo reais, usando o mesmo algoritmo de seleção já existente.
- O botão `+ Aumentar XP` executa o próximo lote pré-calculado, sem reabrir o `EncounterCharmRouteOptimizer` a cada clique.
- O roadmap armazena os Encantos completos (nome, Habilidade e custo), melhorias de Habilidade/Especialização e custo de XP do lote.
- `− Diminuir XP` retrocede o ponteiro do roadmap junto com a reversão normal do lote.
- Se o roadmap estiver esgotado ou inválido, o algoritmo antigo é usado uma vez e um novo roadmap é preparado para os próximos cliques.
- O roadmap foi incluído no `NpcEncontroJsonCodec`, mantendo compatibilidade com NPCs antigos: saves sem o campo continuam com roadmap vazio.
- Foram adicionadas regressões para equivalência entre a execução planejada e o algoritmo atual, reversibilidade e persistência JSON.
- Nenhuma regra de criação, custo de XP, pré-requisito, PB, Mérito ou UI foi alterada.

- A regressão da Exalted.174 mostrou que a tentativa prioritária embaralhava todos os candidatos não-recompráveis depois de calcular a pontuação estrutural.
- Isso permitia que um Mérito terminal consumisse todo o orçamento antes da cadeia que deveria ser priorizada.
- A tentativa continua aleatória entre empates, mas agora ordena primeiro pela quantidade de caminhos desbloqueados.
- Nenhuma regra de orçamento, pré-requisito, recompra ou catálogo foi alterada.

### Exalted.174 — Otimização estrutural da distribuição de Méritos

- A compra exata de Méritos continua preservando integralmente orçamento, pré-requisitos, limites e possibilidade de recompra.
- Quando um Mérito é pré-requisito de outros Méritos disponíveis no mesmo catálogo, ele passa a receber prioridade na ordem de busca.
- A prioridade é aplicada somente à ordenação da busca/backtracking; não força compras ilegais nem altera a quantidade total de pontos.
- Mantida a aleatoriedade nos empates, evitando transformar a geração em uma sequência fixa.
- Incluída regressão determinística para garantir que uma cadeia legal de Mérito seja aproveitada quando o orçamento comportar a cadeia.
- Nenhuma alteração em Charms, Habilidades, PB, XP, equipamentos, UI ou regras de Solar/Lunar/Sangue de Dragão fora da distribuição de Méritos.

### Exalted.173 — Correção de compatibilidade do teste de layout da árvore

- Corrigido o teste `CharmTreeLayoutTest` para usar a nova assinatura `buildCharmTreeLevels(List<CharmTreeNode>)`.
- A implementação da árvore introduzida na Exalted.172 permanece inalterada.
- Nenhuma alteração em geração de NPC, Charms, XP, BP, Especialidades, equipamentos ou UI.

## Exalted.172 — Integração das alterações de Aba 8, Aba 11 e Aba 12

- Aba 8: Encantos sem pré-requisito agora exibem a árvore completa da mesma Habilidade; para Lunares, do mesmo Atributo.
- Aba 8: conectores convergentes usam um barramento horizontal compartilhado, saindo do centro inferior das caixas e chegando ao centro superior do Encanto dependente.
- Aba 8: corrigida a resolução do ID raiz quando o Encanto exibido e o catálogo usado pela árvore possuem instâncias diferentes.
- Aba 11: long press em Equipamento abre o popup com o equipamento efetivamente sorteado para o NPC e suas estatísticas dos catálogos da Aba 7; o toque simples existente permanece.
- Aba 12: estado de conflito deslocado para liberar espaço do contador de iniciativa.
- Incluídos testes para as árvores completas de Habilidade/Atributo Lunar.
- Não foram alteradas regras de geração de NPC, Charms, XP, BP ou distribuição de Especialidades.

## Exalted.169 — Regressão estrutural Lunar com catálogo real

- Reforçada exclusivamente a suíte de regressão em `EncounterConstructionAuditTest`.
- A matriz existente passa a verificar também que todo Lunar possui exatamente 15 Encantos.
- Para Lunar Físico com Inteligência >= 3, a regressão verifica quatro Encantos de Atributo Mental e `Feitiçaria do Círculo Terrestre` dentro dos 15 slots.
- Nenhuma regra de geração, seleção de Encantos, PB, XP, UI ou catálogo foi alterada.
- Objetivo: impedir que uma correção futura volte a quebrar a reserva/liberação de vagas que foi corrigida nas versões anteriores.


## Exalted.167 — Correção dos fixtures Lunares de Encanto Universal

- A regressão `selecao Lunar respeita bloco inicial do atributo principal` usava Encantos `Universal` sintéticos com `minAtributo = 1`.
- O catálogo real `lunar_charms.json` define Encantos Universais com `min_atributo = 0`; portanto o fixture estava tornando o Universal artificialmente inelegível e deixando apenas 14 dos 15 Encantos disponíveis.
- Corrigidos apenas os fixtures de teste para declarar explicitamente `minAtributo = 0` nos Encantos Universais.
- Nenhuma lógica de geração, XP, PB, seleção de Encantos ou UI foi alterada.

## Exalted.166 — Liberação das vagas após o projeto estrutural Lunar
- Correção pontual da seleção Lunar.
- No Lunar Físico com Inteligência >= 3, as cinco vagas reservadas para 4 Encantos Mentais + Feitiçaria eram corretamente protegidas, mas o preenchimento posterior continuava limitado ao orçamento pré-Feitiçaria.
- Após concluir o projeto, o preenchimento final volta a usar o orçamento completo (`quantidade`), permitindo completar os 15 Encantos.
- Mantidas as invariantes de 4 Mentais + Feitiçaria, a prioridade do Atributo principal e o preenchimento por arquétipo.
- Nenhuma alteração em PB, XP, Skills, UI ou regras de legalidade.


## Exalted.166 — Correção de compilação da consolidação de Encantos da Aba 11

Corrigida a chamada de consolidação visual no PDF da Aba 11: `NpcEncontro.charm` usa `EncantoEncontro`, enquanto o helper das abas jogáveis trabalha com `Encanto`. A versão anterior passou a chamada diretamente para o helper incompatível, causando erro de compilação Kotlin. Foi criado um helper específico para `EncantoEncontro`, mantendo a consolidação de Técnica do Corpo de Touro e deixando Encantos não acumuláveis separados. Nenhuma regra de geração, otimização de Habilidades/PB, XP ou seleção de Encantos foi alterada. Incluído teste de regressão para aquisições repetidas na Aba 11.
## Exalted.163 — Seis ajustes visuais e de apresentação

- Integradas as seis modificações do pacote fornecido, sobre a base já validada da Exalted.163.
- Aba 8: Encanto sem pré-requisito de Encanto passa a abrir a árvore completa da Habilidade/Atributo correspondente.
- Aba 8: conexões das árvores usam centro inferior da caixa de origem e centro superior da caixa de destino.
- Encantos acumuláveis: aquisições repetidas de `Técnica do Corpo de Touro` são agrupadas visualmente como `(xN)` na Aba 8, Aba 9 e PDF, mantendo a persistência individual das aquisições.
- Aba 9: Atributos e Habilidades passam a usar apresentação numérica compacta.
- Aba 9: marcadores quadrados de Casta/Favorecido/Aspecto são exibidos conforme o tipo de Exalt.
- Aba 12: etiqueta de estado é recuada para liberar visualmente o contador de iniciativa.
- Foram incorporados testes para agrupamento de Encantos e resolução das Habilidades de Aspecto do Sangue de Dragão.
- Nenhuma regra de geração, PB, XP, elegibilidade, aquisição ou quantidade de Encantos foi alterada por esta etapa.
- A base de integração foi a Exalted.163 já aprovada pelo CI; a compilação da Exalted.163 deve ser feita pelo GitHub Actions.

# Revisão técnica / limpeza de código morto — Exalted.074

- Removidos 50 imports não utilizados (confirmados por análise estática, um a um) em 25 arquivos, principalmente em app/src/main/java/com/example/ui/components e ui/tabs.
- Removido parâmetro de construtor não utilizado `sheetState` em `ExperienceActions` (viewmodel/ExperienceActions.kt) — a classe recebe cada `CharacterSheet` por parâmetro nas próprias funções, então esse campo nunca era lido; ponto de instanciação em `SheetViewModel.kt` ajustado.
- Removido bloco de preview comentado (desativado) no fim de `ui/components/InkButton.kt`.
- Verificação de integridade estrutural: balanceamento de chaves/parênteses em todos os 207 arquivos .kt de app/src/main — nenhum desbalanceamento encontrado.
- Verificação de declarações duplicadas em nível de arquivo/top-level — nenhuma colisão real encontrada (os poucos candidatos eram extensões de tipos diferentes com o mesmo nome de função, o que é válido em Kotlin).
- Trechos marcados `// SKIN:` não foram tocados, conforme combinado.
- Não foi possível compilar/rodar o Gradle neste ambiente (sem Android SDK e sem acesso à distribuição Gradle), então a revisão foi feita por análise estática (varredura de imports, membros privados não referenciados, blocos comentados e assinaturas duplicadas), não por build real.

# Stress / crash-hardening — Exalted.003

- Proteção contra dimensão inválida no layout High Realm: valores <= 0 retornam layout vazio em vez de lançar exceção.
- Limite defensivo de 20.000 caracteres nos tradutores Old Realm e High Realm para impedir entradas extremas de pressionarem a memória/UI.
- Proteção contra loop infinito em DEFLATE malformado no núcleo de códigos de compartilhamento.
- Teste de estresse de geração dos três tipos de Exaltado, três arquétipos e 300 sementes por ciclo.
- Testes de entrada extrema dos tradutores.
- Teste de ciclo/referência ausente na árvore de pré-requisitos.
- Teste de entradas inválidas no importador de NPCs.
- A camada de geração da Aba 11 já captura exceções e converte falhas em mensagem de erro, sem derrubar a tela.
- Os geradores mantêm validações estruturais internas; essas validações não foram removidas para mascarar estados inválidos.

Observação: o Gradle não pôde executar os testes neste ambiente porque o Gradle 9.7.1 não está instalado localmente e o ambiente não possui acesso à internet para baixar a distribuição.


## Correção 0.80.1
- Corrigido import ausente de `com.example.model.isLunar` em `FeiticosTab.kt`, reportado pelo CI em `testDebugUnitTest`.

## Exalted.085 — Persistência/I-O reforçados
- Deduplicação adicional nos coordinators de planilha e backup.
- Carregamento de NPC salvo executado em coroutine de UI, sem chamada suspend direta dentro de `feedbackClickable`.
- Tratamento separado para arquivo corrompido e falha inesperada durante carregamento.
- Preservadas as proteções de tamanho, caminho e gravação temporária dos NPCs.

## Exalted.086 — Cache HD do mapa
- Evicção do LRU agora preserva tiles do viewport atual.
- Cancelamento de carregamento limpa o conjunto `loading` para permitir novas tentativas.
- Mantida a estratégia de tiles 512x512 e o mapa HD original sem redução de resolução.

## Exalted.087 — Regressão e integridade
- Adicionado `BackupPersistenceCoordinatorTest` para coalescência e deduplicação.
- Ampliado `ShareCodeCodecTest` para preservar tipo Lunar/Sangue de Dragão e dados específicos na ida e volta.
- Mantidos os testes anteriores de persistência da planilha ativa e das regras de personagens.


## Exalted.088 — Correções após CI
- Corrigido smart cast de `erroAnterior` em `MainActivity`.
- Corrigido `return` inválido dentro de `withContext` na geração de PDF.
- Movido carregamento suspend de NPC para `LaunchedEffect`, removendo chamada suspend do callback síncrono.
- Corrigida identificação textual de NPC Lunar no nome do arquivo.
- Mantidas todas as melhorias de persistência, I/O, atomicidade, cache HD e testes das versões 085–087.

## Exalted.089 — Performance segura antes da compilação

- Memoização do cálculo de Pontos de Habilidade restantes em `SheetTabsBar` e `MainSheetScreen`.
- Memoização dos filtros de Habilidades e Artes Marciais em `AbilitiesTab`.
- Limpeza do `close()` do `BackupPersistenceCoordinator`.
- Preservadas as otimizações de mapa HD, catálogos e persistência de Exalted.085–.088.

## Exalted.090 — Correção do teste de ExperienceActions

- Corrigido `PersonalDataActionsLanguageTest` para usar a assinatura atual de `ExperienceActions`, que recebe apenas `commitmentError`.
- O teste estava desatualizado após a extração/refatoração de `ExperienceActions` e causava falha em `compileDebugUnitTestKotlin`.
- Nenhuma alteração funcional no aplicativo foi necessária.


## Exalted.091 — Aba 7: Equipamento

- `WeaponSection.kt`: removidos os títulos `Habilidade`, `Arma`, `Categoria` e `Catálogo` do diálogo de cadastro.
- Botões de catálogo renomeados para `Catálogo de Armas`.
- `ArmorSection.kt`: removidos os títulos `Armadura`, `Categoria` e `Catálogo` do diálogo de cadastro.
- Botões de catálogo renomeados para `Catálogo de Armaduras`.
- Nenhum campo, botão, callback ou regra de cadastro foi alterado além dos textos solicitados.

## Exalted.092 — Aba 8: Artes Marciais

- Corrigido o long press da caixa `Artes Marciais`, usando o mesmo callback de árvore das demais caixas.
- A árvore da caixa `Artes Marciais` filtra os Encantos de Artes Marciais pelo marcador `Arte Marcial` do campo `mins`.
- Catálogo de Artes Marciais incluído na fonte de Encantos usada pela árvore de pré-requisitos.
- Auditado `estilos_artes_marciais.json`: 279 Encantos em 28 estilos.
- Corrigidas 22 referências textuais de `pre_requisitos` que apontavam para nomes inexistentes/inconsistentes dentro do respectivo estilo.
- Validação pós-correção: 0 pré-requisitos não resolvidos.

## Exalted.093 — Ajuste de gênero na Aba 11

- Corrigido o cabeçalho do NPC para manter o símbolo de gênero (♂/♀) imediatamente ao lado do nome, em vez de empurrá-lo para a extremidade direita da linha.
- Os botões `Exportar`, `Salvar` e `Carregar` continuam independentes no canto superior direito e não participam do layout do nome.
- Mantido espaçamento visual entre nome e símbolo.
- Exportação PDF ajustada para usar `Nome ♂/♀`, com espaço entre o nome e o símbolo e sem parênteses, mantendo consistência com a interface.
- Nenhum comportamento de salvamento, carregamento ou exportação foi alterado.

## Exalted.094 — Aba 11: estrutura Lunar
- NPCs Lunares agora exibem Tipo, Casta, Essência, Forma Espiritual, Sinal e Idioma em ordem fixa.
- Removido o botão/ação de sortear novamente o Sinal Lunar.
- PDF alinhado à estrutura da interface.

## Exalted.096 — Aba 8: interação uniforme e árvore de Encantos
- `CharmAbilityButton` passou a usar `feedbackCombinedClickable`, unificando o tratamento de toque curto e long press entre todas as caixas de Habilidades/Artes Marciais.
- A árvore de pré-requisitos passou a ser desenhada no sentido de progressão: pré-requisitos acima e Encanto dependente abaixo, com conectores ramificados para múltiplos pré-requisitos.
- Corrigida uma referência circular no JSON de Artes Marciais: `Ataque de Quebra de Articulações` agora depende de `Técnica de Travamento de Articulação`.
- Auditoria do catálogo de Artes Marciais: 279 Encantos, 303 referências internas de pré-requisitos, 303 resolvidas e 0 ciclos após a correção.

## Exalted.097 — Árvore de Encantos dirigida pelo JSON

- `CharmPrerequisiteReferenceParser`: resolução robusta por nome/ID, sem depender de separadores fixos.
- `CharmTreeMapping`: usa IDs estáveis dos Encantos e deriva as ligações exclusivamente de `preRequisitos`.
- `CharmPrerequisiteTree`: registra referências explícitas não resolvidas e aceita raiz por ID ou nome.
- Catálogos Solar, Lunar, Sangue de Dragão e Artes Marciais: `pre_requisitos` aceita String histórica ou array de Strings.
- Diálogos de detalhes passaram a abrir a árvore usando o ID real do Encanto.
- Testes adicionados para nomes com vírgula, múltiplas ramificações, requisitos genéricos, IDs estáveis e JSON com pré-requisitos em array.


## Exalted.098 — Ajustes Aba 11

- Corrigida a interação da trilha de Força de Vontade para não depender visualmente da caixa “próxima”; a regra é resolvida por `WillpowerTrackLogic`.
- Clique em qualquer caixa adquirida vazia marca o primeiro ponto disponível; clique em caixa marcada remove o último ponto marcado. Isso permite desfazer também quando a trilha está cheia.
- Padronizados os títulos “Força de Vontade” e “Trilha de Vitalidade” com `EncounterCardTitle`, igual a Atributos/Habilidades/Méritos/Ações/Encantos.
- Adicionados testes de regressão para clique na caixa direita e trilha cheia.

## Exalted.099 — Correção de ANR na Aba 11

O log do dispositivo mostrou um ANR (`Input dispatching timed out`) com o processo do aplicativo em ~140% de CPU e mais de 129 mil minor page faults. Não houve exceção Java/Kotlin fatal; o problema foi bloqueio/trabalho excessivo no thread principal.

A investigação encontrou que `EncounterNpcActions.toggleForcaDeVontade()` e outras mutações da Aba 11 chamavam `SheetRepository.salvarNpcsEncontro()` diretamente. `EncounterNpcStore.save()` serializava toda a lista de NPCs em `JSONArray` no mesmo thread que tratava o toque.

Correção: criada `EncounterNpcPersistenceCoordinator`, que serializa e grava em `Dispatchers.IO`, coalescendo alterações rápidas e mantendo apenas o snapshot mais recente pendente. Isso cobre vontade, vitalidade, iniciativa e demais mutações que persistem a lista de NPCs.

## Exalted.100 — Correção do ANR na árvore de Artes Marciais

- Identificado que o problema relatado ocorria ao abrir a árvore de um estilo de Artes Marciais, e não na persistência da Aba 11.
- Construção do grafo movida para `Dispatchers.Default` via `LaunchedEffect`.
- Árvore de Artes Marciais passou a reutilizar `CharmPrerequisiteReferenceParser`, mantendo consistência com o JSON e evitando `split(",")`.
- Nós e pré-requisitos passam a usar IDs estáveis do catálogo.
- O popup de Artes Marciais e o popup da árvore não ficam mais simultaneamente abertos; a árvore substitui o primeiro enquanto está ativa.
- Adicionado estado visual de carregamento durante a preparação do grafo.
- Mantidas as melhorias de performance/persistência do Exalted.099.

## Exalted.101 — Correção do ANR da árvore de Artes Marciais

- Corrigido o escopo do catálogo usado pela árvore ao inspecionar um Encanto marcial: agora somente os Encantos do estilo correspondente entram no grafo.
- Movida a construção do grafo de pré-requisitos para `Dispatchers.Default`.
- Evitada a sobreposição do diálogo de detalhes com o diálogo da árvore.
- Mantida a resolução automática pelos `pre_requisitos` do JSON.

## Exalted.102 — Auditoria de performance

- Movida a conversão `Encanto -> CharmTreeEntry` da árvore de pré-requisitos para `Dispatchers.Default`.
- `CharmPrerequisiteReferenceParser` ganhou `Resolver` reutilizável para evitar reconstrução dos candidatos para cada Encanto.
- `NpcCardMerits` deixou de instanciar `MeritosCatalog` durante composição; reutiliza a consulta do `SheetViewModel`.
- Leitura inicial do índice de planilhas salvas movida para `Dispatchers.IO` no `SheetViewModel`.
- Nenhuma alteração de regras ou visual foi introduzida.
- `testDebugUnitTest` tentado; bloqueado pelo ambiente sem resolução de `services.gradle.org`.

## Exalted.103 — Correção de compilação da árvore de Encantos

Corrigido `AbilityCharmTreeDialog.kt`, que referenciava diretamente `CharmTreeEntry` e `CharmPrerequisiteReferenceParser` sem importar/usar corretamente a camada já existente de árvore.

A construção do grafo agora reutiliza `List<Encanto>.paraArvoreDePreRequisitos()`, mantendo a fonte de verdade em `pre_requisitos` e o resolvedor centralizado. A lógica visual, níveis e ordenação da árvore foram preservados.


## Exalted.104 — revisão técnica e limpeza pós-refatorações

- Removida a função privada `definicaoAtivaPorNome` de `CharmsActions.kt`, confirmada sem referências no código de produção/testes.
- Removidos imports sem uso confirmados em `EncounterNpcCardSections.kt` e `EncounterNpcPersistenceCoordinator.kt`.
- Corrigido o ciclo de vida de `EncounterNpcPersistenceCoordinator`: `SheetRepository.close()` agora também encerra o coordinator de persistência dos NPCs de Encontros.
- Removida atribuição duplicada a `pending` em `BackupPersistenceCoordinator.close()`.
- A árvore de Encantos passou a normalizar `id`/nome antes do `distinctBy`, evitando que diferenças de espaços produzam uma duplicidade artificial e uma exceção durante a construção da árvore.
- O diálogo de árvore passou a limpar o gráfico anterior antes de recalcular em `Dispatchers.Default`, evitando exibir um gráfico obsoleto durante a troca de catálogo e eliminando `!!` desnecessários.
- Verificações estáticas adicionais: delimitadores balanceados em todos os `.kt` de produção e nenhum `private` sem referência identificado pelo scanner local.
- O build Gradle completo continua dependendo do ambiente CI/Android, pois o ambiente local não possui acesso funcional ao download de `gradle-9.7.1`.


## Exalted.105 — correção da árvore de Artes Marciais
- Corrigida a resolução de encantos por `estiloId` em `ArtesMarciaisCatalog.encantosDoEstilo`.
- O fluxo da árvore não permanece mais em carregamento quando o encanto raiz não é encontrado; exibe estado explícito.

## Exalted.106 — Aba 11: Encontros
- O antigo botão textual `Gerar NPC` foi substituído por `0000.webp`, mantendo o mesmo callback de geração, limite, estado de carregamento e integração opcional com a Aba 12 (Conflito).
- A imagem é renderizada em branco fixo (`ColorFilter.tint(Color.White)`) para manter contraste nos fundos escuros de Solar, Sangue de Dragão e Lunar.
- Removido o modo separado `Personalizado/Aleatório` e seu switch. A geração passa a usar diretamente Nome, Gênero, Arquétipo, Origem e Tipo de Exaltado selecionados na interface.
- O botão `Aleatório` de Gênero permanece como escolha direta da interface; ele não representa mais um modo global de geração.
- Removido o asset de d10 anterior, que ficou sem referências após a substituição visual.


## Exalted.107 — Correção do asset de geração da Aba 11
- Substituído o asset `gerar_npc_0000.webp` pelo arquivo PNG transparente fornecido pelo usuário (`0000.png`), mantendo o mesmo nome lógico do recurso Android.
- Removido o `ColorFilter.tint(Color.White)` da imagem para que o novo PNG seja exibido com suas cores e transparência originais.
- O callback, área clicável, limite de NPCs, estado durante a geração e integração com Conflito permanecem inalterados.


## Exalted.108 — correção das conexões das árvores de Encantos
- Reorganizado o roteamento visual das conexões em `AbilityCharmTreeDialog.kt`.
- Conexões entre níveis consecutivos agora são desenhadas como um único `Path`, reduzindo descontinuidades entre segmentos.
- Conexões que atravessam níveis intermediários são desviadas por corredores laterais fora dos cartões, impedindo que os nós escondam partes das setas.
- Aumentado o espaçamento horizontal e vertical dos nós para dar mais área de passagem às conexões.
- A resolução das posições dos pré-requisitos foi otimizada com mapa por nome, sem alterar a árvore ou suas regras.
- A correção é aplicada ao componente genérico de árvore e, portanto, abrange Artes Marciais e demais árvores de Encantos.


## Exalted.109 — Aba 11: detalhes do equipamento no long press
- A linha `Equipamento` do card de NPC continua abrindo o editor no toque normal e, no `long press`, agora apresenta um quadro detalhado com nome e estatísticas relevantes.
- Quando o equipamento correspondente existe na `CharacterSheet` da Aba 7, o quadro reutiliza diretamente os dados cadastrados em `sheet.weapons`/`sheet.armaduras`, incluindo valores editados manualmente, dano mínimo, defesa, comitamento e etiquetas/marcadores.
- Para equipamentos gerados no Encontro que não estejam cadastrados na planilha da Aba 7, o quadro usa os valores do próprio equipamento do NPC e as mesmas tabelas compartilhadas (`WeaponStatsTable`/`ArmorStatsTable`) usadas pela Aba 7 para completar informações derivadas.
- Incluídas informações adicionais relevantes para armas de distância, como modificadores por distância.
- O popup foi ampliado para até 320dp de largura para evitar cortes das estatísticas.
- Nenhuma regra de geração, combate ou edição de equipamento foi alterada.


## Exalted.110 — Reorganização visual da Aba 11

- Títulos de Atributos, Habilidades, Méritos, Ações, Força de Vontade, Trilha de Vitalidade e Encantos centralizados horizontalmente.
- Espaçamento vertical uniforme inserido entre cada título e seu conteúdo.
- “Juntar-se à Batalha” movido para a linha entre Méritos e Ações, alinhado à direita.
- Botões “+ Aumentar XP” e “− Diminuir XP” movidos para a linha imediatamente após a última gaveta de Encantos, alinhados à direita.
- Regras e callbacks de geração, combate, XP e demais ações preservados.


## Exalted.111 — Aba 11: peso duplicado na descrição de equipamentos do PDF

- Corrigida a montagem da linha `Equipamento` no PDF de NPC.
- A geração de Encontros já armazena armas/armaduras de catálogo com o peso no próprio nome (ex.: `Garras de Lâmina (Leve)`).
- O PDF também acrescentava `(${peso})`, produzindo `Garras de Lâmina (Leve) (Leve)`.
- A apresentação agora detecta quando o nome já termina com o peso e não o acrescenta novamente; se não terminar, o peso continua sendo exibido uma vez.
- A correção vale para armas e armaduras e não altera os dados persistidos nem as regras de geração.

## Exalted.112 — Indicadores de Casta/Aspecto/Favorecidas na Aba 11
- A ficha visual do NPC passou a exibir um pequeno quadrado ao lado de cada Habilidade para Solar e Sangue de Dragão.
- O quadrado é marcado quando a Habilidade pertence à Casta/Aspecto ou está entre as Habilidades Favorecidas sorteadas.
- Para Lunares, a mesma indicação é aplicada somente aos Atributos: os 2 Atributos de Casta e os 2 Atributos Favorecidos ficam marcados.
- Não são exibidos quadrados nos Atributos de Solar/Sangue de Dragão nem nas Habilidades de Lunar.
- A origem dos conjuntos é a própria informação já armazenada no `NpcEncontro`; não houve alteração das regras de sorteio.

## Exalted.113 — correção de imagens cortadas na seleção e no topo da planilha

- `TemplateSelectionScreen`: reduzidas a altura dos cards, a área reservada aos emblemas e o tamanho dos emblemas, mantendo `ContentScale.Fit`, para que Solar, Sangue de Dragão e Lunar tenham margem suficiente e a terceira opção não fique cortada em telas menores.
- `SheetTopBar`: ampliada a caixa de identidade e reduzido o tamanho lógico do emblema para garantir margem interna suficiente e impedir corte visual do ícone Solar/Sangue de Dragão/Lunar durante a criação da planilha.
- Nenhuma regra de criação, navegação ou dados foi alterada.

## Exalted.114 — Aba 11: nome, títulos e indicadores de Casta/Aspecto/Favorecidas
- O nome do NPC passou a usar alinhamento à esquerda, mantendo os símbolos de gênero imediatamente à direita.
- Os títulos das seções da Aba 11 receberam +1sp no tamanho padrão e espaçamento vertical maior antes do conteúdo.
- Solar e Sangue de Dragão agora exibem também, com valor `0`, as Habilidades de Casta/Aspecto que não receberam pontos, além das Habilidades já pontuadas.
- Os indicadores de Habilidades (Solar/Sangue de Dragão) e de Atributos (Lunar) passaram de um `X` interno para preenchimento integral do quadrado, seguindo o padrão visual dos quadrados gastos da trilha de Força de Vontade.
- Nenhuma regra de sorteio ou persistência foi alterada.

## Exalted.115 — Aba 11: detalhes do equipamento via long press
- Reforçada a correspondência entre o equipamento do NPC e os registros da Aba 7 (`CharacterSheet.weapons` / `CharacterSheet.armaduras`), normalizando o sufixo de peso em ambos os lados.
- O long press da linha `Equipamento` agora abre um quadro de detalhes dedicado, mantendo o toque normal para o editor de equipamento.
- O quadro reutiliza diretamente os dados cadastrados na Aba 7 quando há correspondência, incluindo nome, habilidade, tipo, peso, iniciativa, decisivo, defesa, precisão, dano, dano mínimo, comitamento, distância e etiquetas para armas, e absorção, dureza, penalidade de mobilidade, comitamento e marcadores para armaduras.
- As regras de geração e combate dos NPCs não foram alteradas.


## Exalted.116 — Aba 8: correção das duas visualizações de árvore

- Mantidas as duas finalidades da Aba 8: a árvore de **pré-requisitos** (centrada no Encanto selecionado) e a árvore de **estrutura completa** (todos os Encantos da Habilidade/estilo).
- Corrigida a árvore completa para resolver conexões por **ID normalizado**, em vez de depender apenas do nome exibido.
- As conexões entre níveis consecutivos continuam no corredor entre os cartões; conexões que saltam níveis agora usam corredores laterais reservados e separados, evitando que cartões intermediários escondam segmentos.
- Ampliada a área horizontal da árvore completa para reservar espaço aos corredores laterais.
- A árvore de pré-requisitos teve o desenho das conexões consolidado em trajetos contínuos com junções arredondadas e pontas de seta na chegada ao Encanto dependente.
- Removida uma duplicação acidental do argumento `description` no indicador de Essência do cartão da árvore completa.
- Nenhuma regra de pré-requisito, aquisição ou catálogo foi alterada.

## Exalted.117 — Trilha de Vitalidade da Aba 11
- A marcação da Trilha de Vitalidade dos NPCs de Encontros passou a ser estritamente sequencial: esquerda para direita dentro de cada linha e, somente após completar a linha, avanço para a linha seguinte.
- A validação usa a ordem visual por penalidade, incluindo caixas extras de Corpo de Touro e caixas adicionais concedidas por Méritos.
- Removido o debounce de 2 segundos que permitia marcar caixas fora de ordem antes da reorganização posterior.
- Ao desmarcar uma caixa ou remover uma caixa extra, os danos restantes são compactados imediatamente para a esquerda, eliminando lacunas.
- Ao aumentar/recalcular a trilha por Corpo de Touro, o número de danos existentes é preservado e redistribuído nas primeiras caixas da ordem visual, sem distribuição proporcional ou criação de lacunas.
- Adicionados testes unitários para progressão estritamente sequencial e preservação/compactação do dano durante a expansão da trilha.


## Exalted.118 — correções de compilação após Exalted.117
- Corrigido cálculo de `fontSize` em `EncounterCardTitle` para evitar uso inválido do operador `+` entre `TextUnit`.
- Tornada explícita a tipagem da lista de habilidades ausentes em `EncounterNpcCardSections`, eliminando inferência ambígua na concatenação e ordenação.


### Exalted.119
- Corrigida a conversão de `Map.Entry<String, Int>` para `Pair<String, Int>` em `EncounterNpcCardSections.kt`, eliminando o erro de compilação reportado no CI.


## Exalted.121 — Aba 11: Trilha de Vitalidade com reorganização por gravidade
- A marcação de dano em Encontros agora aceita toque em qualquer quadrado disponível.
- Cada toque altera imediatamente o símbolo da caixa selecionada, sem obrigar progressão sequencial durante a edição.
- Após 2 segundos sem nova interação, os ferimentos são reorganizados automaticamente.
- A prioridade de ordenação é `*` (agravado) > `X` (letal) > `/` (contundente).
- Os símbolos são preenchidos na sequência visual da trilha: esquerda para direita e de cima para baixo, respeitando os níveis de penalidade.
- O debounce é separado por NPC para que uma trilha não cancele a reorganização pendente de outra.


## Exalted.125 — nome determinístico do APK
- O nome canônico da versão passou a ser `rootProject.name = "Exalted.125"`.
- `app/build.gradle.kts` valida esse formato e renomeia automaticamente o APK debug para `Exalted.125.apk` após `assembleDebug`.
- O processo é idempotente e falha se não encontrar o APK esperado, evitando regressões silenciosas para `app-debug.apk`.
- O GitHub Actions agora verifica explicitamente que `Exalted.125.apk` existe e que `app-debug.apk` não permanece no diretório de saída antes de publicar o artefato.

## Exalted.125 — correção do task assembleDebug no Gradle
- Corrigido `app/build.gradle.kts`: substituído `tasks.named("assembleDebug")` por `tasks.configureEach` com verificação de nome, evitando `UnknownTaskException` durante a configuração quando o task Android é registrado tardiamente pelo AGP.
- Mantida a fonte única de verdade `rootProject.name = Exalted.125` para o nome do APK.
- O APK debug continua sendo renomeado para `Exalted.125.apk` após `assembleDebug`.


## Exalted.125 — correção de configuration cache no renomeador do APK
- Corrigido o erro do CI em `assembleDebug`: a implementação anterior usava `doLast` dentro de `tasks.configureEach`, capturando referências do script/Project e impedindo a serialização do configuration cache.
- Criada a task tipada `RenameDebugApkTask`, com `RegularFileProperty` para entrada/saída e `@TaskAction` sem referências a `Project`.
- `assembleDebug` apenas registra `renameDebugApk` como `finalizedBy`, preservando o nome determinístico `Exalted.125.apk`.
- A fonte única de verdade continua sendo `rootProject.name = "Exalted.125"`.


## Exalted.126 — revisão, correção e limpeza da lógica de geração de NPCs
- Revisadas as quatro interpretações fornecidas para a lógica de construção da Aba 11, usando o código real da Exalted.125 como fonte final de comportamento.
- Corrigida a prioridade de **Ocultismo** para NPCs Mentais Solar e Sangue de Dragão: quando a rota Mental é usada, Ocultismo agora entra explicitamente no topo da prioridade de Encantos, evitando que o catálogo seja consumido antes da árvore necessária para Feitiçaria.
- Mantida a regra específica do Físico: Ocultismo só recebe essa prioridade adicional quando já está em nível 3+, sem alterar a sequência de sorteios.
- Centralizada a regra de Habilidade defensiva obrigatória em `EncounterGenerationRules.habilidadeDefensivaPara()`, removendo a duplicação nos três geradores.
- Centralizadas constantes das regras de Ocultismo e da chance de Feitiçaria Mental, reduzindo números mágicos sem alterar a fonte de aleatoriedade usada pelo gerador.
- Mantida a arquitetura separada Solar/Sangue de Dragão/Lunar; não foi aplicada a migração ampla para Strategy para evitar mudança desnecessária da sequência aleatória e regressões.
- Mantida a correção já existente do PB Lunar para não escolher uma meta impossível quando não há candidatas suficientes; foram adicionados testes de regressão para as novas regras centralizadas.
- Preservado o renomeador tipado de APK da Exalted.125 e atualizado o nome canônico para `Exalted.126.apk`.
- Nenhuma permissão, rede, armazenamento ou execução em segundo plano foi adicionada.


## Exalted.127 — consolidação adicional da lógica de construção da Aba 11
- As quatro novas interpretações foram confrontadas novamente com o código real. Foram adotadas as correções objetivas e seguras, sem migrar os geradores para uma Strategy ampla.
- Corrigida uma inconsistência importante do Solar: o sistema declarava 5 Habilidades Favorecidas no total, mas o gerador montava Supernal + 5 adicionais (até 6). Agora são 5 no total, com o Supernal incluído nessas 5.
- Centralizada a escolha dos grupos de Atributos em `EncounterGenerationRules.gruposDeAtributoPara()`, garantindo um único `shuffle` dos dois grupos secundários e eliminando duplicação entre Solar, Sangue de Dragão e Lunar.
- Centralizada a escolha da Habilidade de combate em `escolherHabilidadeCombate()`, mantendo o Supernal como combate somente quando ele próprio é uma Habilidade de combate e usando sorteio normal nos demais casos.
- Centralizado o sorteio 9/10 da Feitiçaria Mental sem trocar a primitiva de `Random`, preservando a semântica de aleatoriedade existente.
- Corrigido o Solar e o Sangue de Dragão Mental para que, quando a Feitiçaria Mental é sorteada, a seleção inicial também reserve os 4 Encantos de Ocultismo necessários antes de tentar inserir a Feitiçaria. Isso evita a situação anterior de 14 Encantos e nenhuma Feitiçaria apesar do sorteio positivo.
- Reforçada a validação estrutural: 27 pontos de Atributos, intervalo 1..5, pelo menos um Atributo 5 na categoria principal, Solar com exatamente 5 Favorecidas e Supernal dentro delas, e Lunar com quatro Atributos especiais distintos e dois de Casta distintos.
- Adicionados testes de regressão para a distribuição dos grupos de Atributos e para a regra probabilística 9/10.
- Preservada a task tipada de renomeação do APK e atualizada a fonte canônica para `Exalted.127.apk`.
- Nenhuma permissão, rede ou execução em segundo plano foi adicionada.

## Exalted.128 — separação correta entre Atributos de Casta e Atributos Favorecidos Lunares
- Corrigida a interpretação da planilha Lunar: são **2 Atributos de Casta + 2 Atributos Favorecidos adicionais**, totalizando 4 Atributos especiais distintos.
- `NpcEncontro.lunarAtributosCasta` continua armazenando somente os 2 Atributos de Casta.
- `NpcEncontro.habilidadesFavorecidas` passa a armazenar somente os 2 Atributos Favorecidos adicionais do Lunar, em vez de armazenar os 4 atributos combinados.
- A seleção de Encantos, Pontos de Bônus e expansão por experiência continua usando explicitamente a união dos 2 de Casta + 2 Favorecidos quando precisa tratar o conjunto especial completo.
- A regra de alinhamento ao arquétipo foi ajustada para preservar os 2 Atributos de Casta e alterar somente os 2 Favorecidos adicionais.
- A validação estrutural agora exige 2 Favorecidos, 2 de Casta, nenhuma duplicação entre os grupos e 4 atributos especiais distintos no conjunto final.
- A indicação visual dos Atributos especiais Lunares passa a marcar tanto Casta quanto Favorecidos.
- Atualizados os testes da matriz de arquétipos e criado teste específico para a separação dos dois grupos.
- O APK canônico desta versão permanece `Exalted.128.apk`.

## Exalted.129 — formalização das regras de construção e auditoria da Aba 11
- Criado `EncounterConstructionPolicy`: separa explicitamente regras **MANDATORY** (identidade/invariantes) de **PREFERENCE** (prioridades probabilísticas e de arquétipo), evitando que uma preferência seja tratada como falha estrutural.
- Criado `EncounterConstructionReport`/`EncounterConstructionReportService`: produz um relatório interno determinístico da construção final, sem adicionar metadados ao estado persistido do NPC.
- `EncounterValidationService` foi reforçado como segunda camada de coerência entre sistemas: exige os 9 Atributos e 25 Habilidades canônicos, valores de Habilidade 0..5, Especialidades válidas, foco de combate/defesa coerentes e contagem de Corpo de Touro consistente.
- A identidade dos três tipos continua explícita: Solar com 5 Favorecidas incluindo Supernal; Sangue de Dragão com 5 Favorecidas adicionais fora das 5 Habilidades do Aspecto; Lunar com 2 Atributos de Casta + 2 Atributos Favorecidos adicionais, totalizando 4 distintos.
- Mantida a reserva de vaga para Feitiçaria Terrestre quando a regra probabilística/estrutural a exige: a seleção reduz a quantidade-base antes de tentar inserir a Feitiçaria, em vez de adicionar uma compra fora do orçamento.
- Adicionado `EncounterConstructionAuditTest`: matriz de 100 sementes x 3 arquétipos x 3 tipos (900 construções), com validação estrutural e geração do relatório para detectar regressões de coerência.
- Adicionado teste específico da separação entre regras obrigatórias e preferências e da reserva de 14+1 quando Feitiçaria ocupa uma vaga.
- Não foi introduzida otimização determinística dos NPCs: as preferências continuam apenas ordenando/priorizando escolhas, preservando aleatoriedade controlada.
- `settings.gradle.kts` e workflow atualizados para `Exalted.129.apk`.


## Exalted.130 — correção de compilação pós-Exalted.129

- Corrigida referência incorreta `supernalAbility` em `EncounterConstructionReport.kt`; o modelo `NpcEncontro` utiliza `habilidadeSupernal`.
- Corrigidas as duas referências incorretas a `supernalAbility` em `EncounterValidationService.kt`.
- Mantida a validação estrutural do Solar exigindo Supernal presente entre as 5 Favorecidas.
- Nenhuma regra de geração foi alterada por esta correção; trata-se de ajuste de integração com o modelo real.
- Workflow e nome do projeto atualizados para `Exalted.130.apk`.
- Validação local completa não pôde ser executada porque o ambiente não possui acesso a `services.gradle.org` para baixar o Gradle 9.7.1.

## Exalted.133 — segunda revisão técnica da geração de NPCs (Aba 11)

Base: Exalted.130 + os cinco relatórios da segunda revisão.

Correções implementadas:
- Distribuição/ajuste de Atributos preserva invariantes por categoria: 11/9/7,
  além de soma 27 e faixa 1..5. O ajuste da Habilidade de combate permanece
  dentro do grupo Físico e não desloca pontos para Social/Mental.
- Validação estrutural agora verifica grupos distintos, cobertura dos 9 Atributos
  e totais 11/9/7.
- Corrigida a condição de validação do Supernal Solar, que estava logicamente
  invertida.
- Sangue de Dragão valida explicitamente 5 Habilidades do Aspecto + 5 Favorecidas
  adicionais, totalizando 10 Habilidades estruturais distintas e sem Supernal.
- Ausência de Supernal em Sangue de Dragão/Lunar deixou de ser representada por
  string vazia ou pela Habilidade de combate: o parâmetro interno agora é nullable.
  Isso separa identidade de foco de combate sem alterar a regra de geração.
- Fórmulas de Motes centralizadas em EncounterMoteService, preservando as fórmulas
  específicas de Solar/Lunar e Sangue de Dragão e o abatimento de equipamento.
- Auditoria ampliada para 1.000 seeds por cenário (9.000 construções), mais teste
  explícito de determinismo por seed.
- Testes adicionados para invariantes 11/9/7, ajuste de combate e fórmulas de Motes.
- Teste estatístico de Feitiçaria usa seeds misturadas em vez de apenas seeds sequenciais.

Itens que os relatórios classificaram como dependentes de regra/documentação
(LG-04, LG-06, LG-07, LG-09 e BuildTrace/ledger completo) não foram inventados:
foram implementadas apenas as correções sustentadas pelo código e pelas regras já
confirmadas no projeto.

## Exalted.133 — correção pós-CI da Exalted.132

- Corrigida a chamada de `validarTotaisBaseDeAtributos()` que existia na validação final sem implementação correspondente na base entregue.
- A verificação dos totais 11/9/7 foi materializada em uma função própria e continua separada da validação final Lunar, pois PB pode elevar Atributos Lunares após a distribuição-base.
- Corrigido `EncounterConstructionAuditTest`: para Lunar, o teste não exige artificialmente 11/9/7 na ficha final; verifica que a soma final permaneça >= 27. Solar e Sangue de Dragão continuam exigindo 11/9/7.
- Mantida a identidade estrutural dos três tipos e as correções da Exalted.132; nenhuma regra de geração foi removida para silenciar os testes.
- Versão do projeto/APK atualizada para Exalted.133.


## Exalted.133 — correção complementar do CI

- Corrigida a referência de teste `supernalAbility` para o campo canônico `NpcEncontro.habilidadeSupernal`.
- Adicionado teste de regressão para a validação da distribuição-base de Atributos após o ajuste da Habilidade de combate.


## Exalted.134 — correção do CI: referência de Encanto no teste Solar

- Corrigida a referência inválida `it.habilidade` em `SolarEncounterGeneratorRegressionTest.kt`.
- `EncantoEncontro` possui o campo canônico `habilidadeVinculada`; o teste agora usa `it.habilidadeVinculada` para verificar os Encantos de Ocultismo.
- Nenhuma regra de geração foi alterada: a correção é exclusivamente de integração do teste com o modelo atual.
- Revisadas as demais ocorrências de `.habilidade` nos testes: as ocorrências restantes correspondem a `EspecialidadeEncontro.habilidade` e outros modelos que realmente possuem esse campo.
- Projeto/APK atualizado para `Exalted.134.apk`.
- O objetivo é permitir que `:app:compileDebugUnitTestKotlin` prossiga para a execução integral dos testes.

## Exalted.135 — incorporação do feedback da auditoria independente

- Baseline da auditoria formalizado: a revisão parte do `Exalted.134` e esta entrega gera `Exalted.135`, evitando mistura de versões.
- Criado `AUDIT_PROTOCOL.md` com níveis de evidência, severidade independente da natureza do achado, categoria `NÃO VERIFICADO`, modelo de ameaça focado em ShareCode/arquivos externos, prioridade por risco e critério mínimo de conclusão.
- Explicitada a regra de que lacunas de especificação não devem virar testes normativos ou mudanças de comportamento sem confirmação.
- Mantidas como regressões obrigatórias as correções ACH-001 a ACH-004 já incorporadas ao código.
- Adicionado teste de determinismo para os 9.000 cenários (1.000 seeds × 3 arquétipos × 3 tipos), comparando construções completas com a mesma seed.
- Nenhuma regra de negócio foi alterada nesta rodada; as mudanças são de rastreabilidade da auditoria e cobertura de testes.
- APK desta versão: `Exalted.135.apk`.


## Exalted.138 — correção do CI após log de 345 testes / 14 falhas
- Corrigida a validação estrutural final para não inferir os totais 11/9/7 de uma ficha que não persiste qual grupo não primário foi Secundário e qual foi Terciário. A distribuição-base continua validando 11/9/7 antes de PB.
- Reescrito o ajuste de combate para escolher diretamente uma distribuição física válida, preservando o total da categoria e garantindo Destreza 5 + Força <= 2 para Arqueirismo/Arremesso e Destreza >= 4 para Armas Brancas/Briga.
- A seleção de Feitiçaria Terrestre Mental deixou de lançar exceção quando o catálogo estiver parcial; quando a definição existe, a reserva permanece ativa.
- Mantidas as regras de identidade Solar, Sangue de Dragão e Lunar; nenhuma regra de UI/persistência foi removida.
- APK desta versão: `Exalted.138.apk`.

## Exalted.137 — correção do CI após log de 345 testes / 20 falhas
- Corrigida a validação dos totais de Atributos: 11/9/7 são aplicados aos papéis Primário/Secundário/Terciário do arquétipo, não às categorias Física/Social/Mental fixas.
- Corrigida a validação final para exigir 11 no grupo Primário e 16 nos dois grupos não primários, pois a escolha entre 9 e 7 não é persistida no NPC.
- Corrigido o teste de regressão de Atributos para verificar os papéis sorteados, sem impor uma associação fixa entre categoria e total.
- Corrigida a fixture Lunar do teste de ajuste de combate para começar de uma distribuição-base válida de 27 pontos.
- Corrigidos os testes de determinismo para ignorar somente UUIDs transitórios do NPC, Méritos e caixas de vitalidade; o conteúdo semântico continua sendo comparado integralmente.
- Adicionado `CI_FAILURE_ANALYSIS.md` com a análise rastreável do log fornecido.
- Workflow passou a imprimir os XMLs dos testes quando o job falhar, preservando a mensagem real das exceções em futuras execuções.
- Nenhuma regra de geração foi alterada além da correção da validação que estava incompatível com a regra de distribuição por arquétipo.
- APK desta versão: `Exalted.137.apk`.

## Exalted.139 — distribuição de Habilidades da Aba 11

- Habilidades Favorecidas recebem obrigatoriamente pelo menos 1 ponto dos 28 pontos normais.
- Habilidades de Casta/Aspecto não são obrigatórias em nível 1.
- Entre as Habilidades de Casta/Aspecto que possuem Encantos no catálogo, o gerador prioriza o máximo possível de aquisições com pelo menos 1 ponto, preservando o orçamento total de 28 pontos.
- Encantos de Habilidades de Casta (Solar) e de Habilidades do Aspecto (Sangue de Dragão) passam a usar o custo reduzido de 8 XP, mesmo quando a Habilidade não estiver na lista adicional de Favorecidas.
- A validação estrutural confirma o mínimo de 1 ponto para todas as Favorecidas.
- A identidade do projeto foi avançada para Exalted.139.



## Exalted.141 — símbolo de gênero junto ao nome na Aba 11

- O símbolo de gênero deixou de ocupar uma posição independente na linha do cabeçalho do NPC.
- O título agora usa o formato `Nome do NPC (♂)` ou `Nome do NPC (♀)`.
- Quando o gênero não estiver definido, o nome permanece sem símbolo.
- Isso impede que o símbolo seja visualmente associado aos botões `Exportar`, `Salvar` ou `Carregar` posicionados no canto superior direito do card.
- A identidade do projeto foi avançada para Exalted.141.


## Exalted.141 — espaçamento dos títulos da Aba 11

- Aumentado o espaçamento vertical entre os títulos vermelhos e o conteúdo das seções da Aba 11.
- O espaçamento passou de 14.dp para 28.dp nas seções Atributos, Habilidades, Méritos e demais seções equivalentes deste componente.
- Objetivo: reproduzir a separação visual solicitada, evitando que os campos fiquem visualmente colados aos títulos.

## Exalted.142 — reposicionamento do checkbox Conflito na Aba 11

- O checkbox `Conflito` foi movido da parte inferior do formulário para a linha do cabeçalho, entre o campo `Nome` e o botão/imagem de gerar NPC.
- O estado `enviarParaConflito` e o callback `onCheckedChange` foram mantidos sem alteração funcional.
- O callback de geração continua usando exatamente a mesma condição para enviar o NPC à Aba 12.
- Nenhuma regra de geração de NPC, persistência ou funcionamento da Aba 12 foi alterada.
- A identidade do projeto foi avançada para Exalted.142.

## Exalted.145 — Correção de compilação do teste de Favorecidas: rede de segurança de regressão (2026-09-24)

- Nenhuma regra de geração foi alterada.
- Mantidos os testes existentes de 9.000 construções (3 Exaltados × 3 arquétipos × 1.000 seeds).
- Fortalecida a cobertura da regra: Habilidades Favorecidas devem iniciar com pelo menos 1 ponto.
- A regra foi registrada para Solar e Sangue de Dragão; Habilidades de Casta/Aspecto continuam podendo permanecer em 0.
- Próxima fase prevista: validação/isolamento da cadeia de Feitiçaria antes de alterar a seleção de Encantos.
- Execução do Gradle permanece pendente no ambiente atual porque o wrapper tenta baixar Gradle 9.7.1 de services.gradle.org e a rede não está disponível.


### Exalted.145 — Correção de compilação do teste de Favorecidas
- Corrigida a chamada de `assertTrue` em `DragonBloodedEncounterGeneratorTest.kt`.
- A mensagem estava sendo passada na posição do argumento booleano e vice-versa, causando incompatibilidade de tipos no Kotlin/JUnit.
- Nenhuma regra de geração foi alterada.
- O nome do projeto e os artefatos de CI foram alinhados para `Exalted.145`.
- A execução local do Gradle não foi possível neste ambiente porque o wrapper precisou baixar o Gradle 9.7.1 e a rede estava indisponível.


## Exalted.146 — Correção da suíte EncounterCharmSelectionServiceTest (2026-09-24)

- **Falha reportada pela CI:** `EncounterCharmSelectionServiceTest.kt` continha duas declarações completas da mesma classe, com dois blocos `package`/`import` no mesmo arquivo.
- **Sintomas:** import `Test` ambíguo, redeclaração de `EncounterCharmSelectionServiceTest`, imports fora do início do arquivo e referências `charm` não resolvidas.
- **Correção:** consolidação das duas suítes no mesmo arquivo, mantendo os testes Lunares da Fase 2 e os testes da cadeia de Feitiçaria Terrestre. Nenhuma regra de geração foi alterada.
- **Escopo:** somente teste, organização de imports/classe e versionamento do projeto.
- **Validação local:** não declarar execução do Gradle como aprovada sem CI; a confirmação final deve ser feita pelo GitHub Actions.


## Exalted.147 — Correção do projeto de Feitiçaria Terrestre (2026-09-24)

O CI da Exalted.146 compilou corretamente, mas um teste funcional falhou em `EncounterCharmSelectionServiceTest`: o projeto de Feitiçaria deveria adicionar quatro Encantos de Ocultismo e então a Feitiçaria em uma única operação, porém a própria Feitiçaria estava sendo considerada como candidata durante a preparação dos quatro Encantos de Ocultismo.

Correção aplicada em `EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre`: a definição da Feitiçaria agora é explicitamente excluída da lista de candidatos de preparação. Assim, ela só pode entrar depois que os quatro Encantos de Ocultismo distintos forem adquiridos e sua elegibilidade for verificada.

Escopo: correção pontual da lógica da cadeia de Feitiçaria. Nenhuma alteração de UI, PB, regras de habilidades, Lunar ou demais seleção de Encantos.

## Exalted.148 — Correção do nome do APK no workflow de CI (2026-09-24)
- O GitHub Actions da Exalted.147 executou os testes e chegou à etapa `assembleDebug`, mas a verificação do artefato ainda procurava `Exalted.145.apk`.
- `app/build.gradle.kts` já deriva corretamente o nome do APK a partir de `rootProject.name`, que estava em `Exalted.147`; portanto o APK esperado era `Exalted.147.apk`.
- Corrigido `.github/workflows/build_apk.yml` para verificar e publicar `Exalted.147.apk`.
- A versão do projeto foi avançada para `Exalted.148` para manter o versionamento sequencial. O workflow continua parametrizado pelo nome explícito da versão da release atual e deverá ser atualizado a cada nova versão.
- Escopo: somente identificação/publicação do artefato no CI e versionamento do projeto. Nenhuma regra de geração de NPC, Encantos, Feitiçaria, Habilidades, PB ou UI foi alterada.

## Exalted.149 — Correção final da consistência do nome do APK no CI (2026-09-24)

O GitHub Actions da Exalted.148 ainda procurava e enviava `Exalted.147.apk`, enquanto o projeto já estava configurado para gerar o APK a partir de `rootProject.name = "Exalted.148"`.

A correção mantém a regra de nomeação existente e alinha os três pontos:
- `rootProject.name = "Exalted.149"`;
- verificação do CI para `Exalted.149.apk`;
- upload do CI para `Exalted.149.apk`.

Nenhuma regra de geração de NPC, UI, Encantos, Feitiçaria, Pontos de Bônus, Lunar ou testes de negócio foi alterada nesta versão. A alteração é exclusivamente de consistência de versão/nome do artefato do CI.

## Exalted.150 — Cobertura de integração da cadeia de Feitiçaria (2026-09-24)

- O CI da Exalted.149 confirmou a compilação após a correção do nome do APK.
- Antes de avançar para a próxima fase de otimização de Encantos, foi adicionada cobertura de integração para a regra já implementada da Feitiçaria Terrestre.
- Solar Físico: quando Ocultismo >= 3, a construção deve conter a Feitiçaria do Círculo Terrestre e pelo menos quatro Encantos de Ocultismo distintos dela.
- Sangue de Dragão Físico: mesma invariável, usando o catálogo real do gerador.
- As novas verificações percorrem uma matriz ampla de seeds e também exigem que exista pelo menos um caso elegível; isso evita um teste que passe apenas porque nenhuma seed atingiu a condição.
- Nenhuma regra de seleção foi alterada nesta versão; somente testes de regressão e versionamento do projeto/CI.
- Próxima etapa, após confirmação do CI: Fase 3 — avaliação de candidatos de Encantos com horizonte limitado, preservando legalidade, orçamento e determinismo.


## Exalted.151 — Correção de compilação do teste de Feitiçaria (2026-09-24)

- O GitHub Actions da Exalted.150 falhou na compilação de testes em `DragonBloodedEncounterGeneratorTest.kt:74`.
- A causa foi a mesma incompatibilidade de ordem dos argumentos de `assertTrue`: a expressão booleana estava na posição destinada à mensagem `String`, e a mensagem na posição booleana.
- Corrigida somente essa chamada para a assinatura esperada pelo JUnit/Kotlin.
- Nenhuma regra de geração, Feitiçaria, Encantos, Habilidades, Pontos de Bônus, Lunar ou UI foi alterada.
- Projeto e artefatos do CI alinhados para `Exalted.151`.


## Exalted.152 — Correção dos testes de integração da Feitiçaria (2026-09-24)

- O GitHub Actions da Exalted.151 concluiu a compilação dos testes, mas falhou durante a execução de 2 testes: `DragonBloodedEncounterGeneratorTest` e `SolarEncounterGeneratorRegressionTest`.
- O erro do Sangue de Dragão ocorreu em `ApplicationProvider.getApplicationContext` porque o teste não estava configurado com `RobolectricTestRunner`; a suíte foi alinhada ao padrão das demais classes que usam `ApplicationProvider`.
- Os dois testes de Feitiçaria dependiam de uma matriz aleatória produzir um Físico com Ocultismo >= 3. A execução mostrou que essa pré-condição não é garantida pela distribuição aleatória atual, portanto o teste estava validando duas coisas ao mesmo tempo e podia falhar sem indicar defeito no projeto de Feitiçaria.
- Os testes foram reescritos para controlar diretamente a pré-condição `Ocultismo >= 3` e verificar o comportamento real de `EncounterCharmSelectionService.aplicarProjetoFeiticariaTerrestre` usando os catálogos reais Solar e Sangue de Dragão.
- A lógica de geração de NPC, seleção de Encantos, Feitiçaria, Habilidades, Pontos de Bônus, Lunar, UI e regras de jogo não foi alterada.
- Esta versão corrige exclusivamente a infraestrutura/qualidade dos testes para eliminar dependência indevida de aleatoriedade e de contexto Android não inicializado.

## Exalted.153 — Correção do nome canônico da Feitiçaria Terrestre (2026-09-24)

- O CI da Exalted.152 executou 354 testes e falhou em um único teste de integração/regressão da Feitiçaria Solar.
- A falha ocorreu em `SolarEncounterGeneratorRegressionTest.kt:70`, na verificação de presença da Feitiçaria.
- A causa confirmada foi uma divergência literal entre o nome constante usado pelo código e o nome existente no catálogo `encantos_solares.json`: o código usava `Feitiçaria do Círculo Terrestrial`, enquanto o catálogo usa `Feitiçaria do Círculo Terrestre`.
- Como `aplicarProjetoFeiticariaTerrestre()` procura a definição pelo nome constante, a busca retornava nulo e o projeto era devolvido sem aquisições.
- Correção: alinhar `NOME_FEITICARIA_TERRESTRE` ao nome canônico do catálogo e atualizar os testes unitários sintéticos para a mesma grafia.
- Não houve alteração na regra de quantidade de Encantos, nos quatro pré-requisitos de Ocultismo, na seleção de Encantos, PB, Lunar ou UI.


## Exalted.154 — Correção do catálogo de Sangue de Dragão para a Feitiçaria Terrestre (2026-09-24)

- O CI da Exalted.153 executou 354 testes e falhou em um único teste: `DragonBloodedEncounterGeneratorTest > Sangue de Dragao Fisico com Ocultismo 3 ou mais recebe o projeto completo de Feiticaria`, em `DragonBloodedEncounterGeneratorTest.kt:72`.
- A causa confirmada foi uma segunda divergência literal de nomenclatura: o catálogo `encantos_sangue_dragoes.json` continha `Feitiçaria do Círculo Terrestrial`, enquanto o nome canônico usado pelo código e pelo catálogo Solar é `Feitiçaria do Círculo Terrestre`.
- `EncantosSangueDosDragoesCatalog` preserva o nome do JSON na conversão para `EncantoSolarDefinition`; por isso `aplicarProjetoFeiticariaTerrestre()` não encontrava a Feitiçaria no catálogo de Sangue de Dragão e devolvia a seleção sem o projeto.
- Correção exclusiva: alinhar a grafia do encanto no catálogo de Sangue de Dragão ao nome canônico `Feitiçaria do Círculo Terrestre`.
- Não foram alterados a lógica de seleção, a regra dos quatro Encantos de Ocultismo, a quantidade inicial de Encantos, Habilidades, PB, Lunar, UI ou qualquer outra regra de geração.
- Próxima etapa permanece bloqueada até o CI confirmar a regressão completa; somente depois disso avançar para a Fase 3 de otimização de seleção de Encantos.

## Exalted.156 — Seleção de Encantos por rota DAG com busca limitada (2026-09-24)

- Fase 3 iniciada após CI limpo da Exalted.154.
- Criado `EncounterCharmRouteOptimizer`, um selecionador genérico por rota com beam search limitada (largura 6, profundidade 3).
- A avaliação considera elegibilidade atual, Encantos desbloqueados pelo candidato, custo econômico de XP e Essência mínima.
- A seleção inicial Solar/Sangue passou a usar o avaliador de rota dentro das Árvores prioritárias, mantendo os blocos de 3 e todos os pré-requisitos legais.
- A progressão de XP Solar/Sangue passou a escolher entre candidatos elegíveis por rota, preservando o comportamento de acumular XP quando um Encanto elegível ainda não cabe no saldo.
- As 5 Excelências gratuitas do Sangue de Dragão passaram a usar o mesmo avaliador de rota.
- A seleção inicial Lunar passou a usar o avaliador de rota, inclusive na rota dos quatro Encantos de Atributo Mental exigidos antes da Feitiçaria no Lunar Físico com Inteligência >= 3.
- Nenhum orçamento, custo legal, pré-requisito ou quantidade inicial foi alterado.
- A busca é limitada e determinística: não substitui o gerador por otimização global e não altera a origem da aleatoriedade dos empates de Atributos Lunares.
- Próxima validação obrigatória: suíte completa no GitHub Actions, seguida dos testes de regressão de custo 8 XP vs 10 XP e das nove combinações.
- Após a validação do ZIP Exalted.155, a execução prolongada do CI revelou um gargalo de custo: a pontuação percorria o catálogo inteiro por candidato e os estados seguintes reordenavam centenas de candidatos.
- Correção de desempenho: índice determinístico `desbloqueiosPorNome` com avaliação O(1) do desbloqueio; vizinhança de rota pré-calculada e limitada a 48 candidatos; fechamento de até dois níveis de dependências diretas; elegibilidade legal completa permanece aplicada após a pré-seleção.
- Beam width 6, profundidade 3, economia de 8 XP, pré-requisitos, orçamentos e regras Solar/Sangue/Lunar permanecem inalterados.
- O objetivo da alteração é reduzir drasticamente o custo computacional sem transformar a seleção em otimização global.


### Exalted.157
- Corrige regressão da Fase 3 na progressão de XP: o otimizador de rotas usava um `Set<String>` para representar posse de Encantos e, com isso, bloqueava uma segunda aquisição legal do mesmo Encanto repetível.
- O catálogo e as regras legais permanecem inalterados: agora o otimizador recebe explicitamente quais Encantos podem ser adquiridos repetidamente; na progressão Solar, somente `Corpo de Touro` usa essa exceção, preservando a contagem real por categoria.
- Adicionado teste direto do otimizador para garantir que uma aquisição repetível possa abrir um pré-requisito de quantidade (`Quaisquer dois Encantos de Resistência`).
- Nenhuma alteração de UI, BP, XP, catálogo ou regras de elegibilidade além da correção necessária para preservar a semântica já coberta pelo teste existente.

### Exalted.158 — prioridade de Habilidades estruturais para Encantos
- Após o CI limpo da Exalted.157, a Fase 4 começa pela otimização de Habilidades sem alterar o orçamento: os 28 pontos normais continuam exatos e nenhum ponto normal ultrapassa 3.
- Habilidades de Casta/Aspecto que possuem Encantos no catálogo já recebiam 1 ponto inicial; agora também entram na fase de blocos prioritários de 3, podendo receber novas compras antes das Habilidades genéricas.
- A ordem continua respeitando a política do arquétipo, Habilidades Favorecidas e o foco de combate; a mudança apenas acrescenta as Habilidades estruturais relevantes ao mesmo conjunto prioritário.
- Adicionado teste de regressão para garantir que uma Habilidade estrutural relevante possa receber pelo menos 3 pontos quando o orçamento permitir.
- Nenhuma alteração de PB, XP, pré-requisitos de Encantos, quantidade de Encantos, regras de Casta/Aspecto ou UI.

## Exalted.159 — Correção de compilação da regressão de Habilidades

Correção exclusiva de teste: `EncounterDistributionServiceBudgetTest.kt` usava `assertTrue` com os argumentos na ordem incorreta para JUnit/Kotlin. A expressão booleana passou para o primeiro argumento e a mensagem para o segundo. Nenhuma regra de geração foi alterada.


## Exalted.163 — Fase 4: prioridade estrutural tambem nos Pontos de Bonus

- Mantida a regra dos 28 pontos normais e o orçamento fixo de 15 PB.
- Habilidades estruturalmente relevantes para rotas de Encantos agora recebem prioridade também na etapa de PB, permitindo compras repetidas acima do ponto inicial.
- Solar passa suas Habilidades relevantes de Casta; Sangue de Dragão passa as relevantes de Aspecto.
- A prioridade continua respeitando custos de Habilidade Favorecida/Casta versus não favorecida, teto 5 e bloqueios existentes.
- Nenhuma regra de legalidade, quantidade de PB, XP, Encantos, UI ou Lunar foi alterada.
- Incluído teste de regressão para comprovar a prioridade estrutural na etapa de PB.


## Exalted.163 — Correção do projeto estrutural Lunar Físico/Inteligência 3+

O CI da Exalted.161 revelou duas regressões nos testes de seleção inicial Lunar: o bloco do Atributo principal não era garantido e o projeto de 4 Encantos de Atributo Mental + Feitiçaria podia ficar sem espaço quando Corpo de Touro repetível consumia vagas antes do projeto obrigatório. A correção mantém a otimização existente e impede apenas que aquisições repetíveis de Corpo de Touro consumam as vagas reservadas enquanto o projeto estrutural é obrigatório. Após a conclusão do projeto, Corpo de Touro continua disponível no preenchimento das vagas restantes. Nenhuma regra de PB, XP, Solar ou Sangue de Dragão foi alterada.


Exalted.166 — Proteção das invariantes estruturais Lunares
- Corrige as duas regressões do CI em EncounterCharmSelectionServiceTest relacionadas ao preenchimento inicial Lunar.
- A seleção dos pontos estruturais obrigatórios (3 do Atributo principal, 1 Universal e, quando aplicável, 4 Mentais + Feitiçaria Terrestre) não depende mais da heurística de busca em feixe.
- Foi adicionado fallback determinístico por elegibilidade para completar o orçamento quando ainda existirem Encantos legais no catálogo.
- A busca otimizada continua sendo usada nas decisões gerais de preenchimento; não altera regras legais, quantidade de Encantos, PB, XP ou UI.

## Exalted.183 — Correção do cache de estados na busca de Méritos

O teste de priorização estrutural de Méritos continuava falhando porque a tentativa prioritária usava a mesma memória de estados sem solução da busca geral. Quando o prefixo prioritário falhava, o estado inicial ficava marcado e impedia a segunda busca de encontrar a cadeia `Influência 1 -> Sobrenome de Renome 2`. A correção limpa apenas esse cache antes do fallback para a busca geral. Não altera orçamento, pré-requisitos, regras de recompra ou outros sistemas.


## Exalted.183 — Regressão final dos nove cenários

- Após a confirmação do CI da Exalted.178, esta versão adiciona somente uma barreira de regressão integrada para os nove cenários Solar/Sangue de Dragão/Lunar × Físico/Social/Mental.
- Para 30 sementes por cenário, verifica 15 Encantos, exatamente quatro Especialidades distintas e o orçamento final de Méritos: 10 para Solar/Lunar e 18 para Sangue de Dragão.
- Também confirma que Sangue de Dragão não recebe Méritos sobrenaturais.
- Nenhuma regra de geração foi alterada; a finalidade é detectar regressões entre as otimizações de Encantos, Habilidades, Especialidades e Méritos antes da próxima etapa.


## Exalted.183 — Regressão final de Especialidades estruturais

- Após o CI limpo da Exalted.180, esta versão adiciona somente uma regressão integrada para Especialidades nos nove cenários Solar/Sangue de Dragão/Lunar × Físico/Social/Mental.
- Para 30 sementes por cenário, quando houver Habilidades estruturais elegíveis com pontos, verifica que pelo menos uma delas aparece entre as quatro Especialidades.
- Solar usa as Habilidades da Casta presentes no catálogo de Encantos; Sangue de Dragão usa as Habilidades do Aspecto presentes no catálogo; Lunar usa o perfil estrutural do arquétipo.
- Nenhuma regra de geração foi alterada.

## Exalted.183 — Integração do pacote cumulativo das Abas 8, 11 e 12

- Integração realizada sobre a base funcional da Exalted.181.
- Aba 8: árvore completa para Encantos raiz sem pré-requisito, identificação correta da raiz, conectores por centro das caixas e linha compartilhada para dependências.
- Aba 11: consolidação visual de aquisições repetíveis de `Técnica do Corpo de Touro` e correção do long press de Equipamento para usar o equipamento efetivamente sorteado no `NpcEncontro`, com estatísticas das tabelas da Aba 7.
- Aba 12: etiquetas de estado alinhadas em área de largura fixa, preservando a posição do contador de iniciativa.
- Incluídos os testes do pacote para árvore, agrupamento de Encantos e detalhes de equipamento.
- Nenhuma regra de geração de NPC, XP, PB, Méritos ou seleção de Encantos foi alterada pela integração.
