# Auditoria de Encontros — registro de investigação

## Base e limites

Base remota preservada: `3448757a94c124ab48faf71438bb56a406584332`, Exalted.947 + backgrounds aprovados. Branch: `refactor/encounters-audit-947`. A main `f8dc46fd5e5301c056bf09649bb123621f8b413f` tem três alterações adicionais exclusivamente visuais; não incorporadas porque esta tarefa proíbe alterar UI. Sem divergência mecânica desde o ancestral `b51c4ba`.

Gradle 9.8.0; Kotlin 2.2.10; AGP 9.3.3; JDK 17 no runner. Não há Android SDK nem compilador Kotlin local. O workflow dedicado executa somente `:app:testDebugUnitTest`, não assemble/rename/package APK. A autorização atual permite testes automatizados; a restrição de não iniciar APK permanece.

## Constatações do relatório

| ID | Classificação | Evidência atual |
|---|---|---|
| A1 | Confirmada | EncounterGenerator delega a três geradores específicos. |
| A2 | Confirmada | EncounterConstructionPolicy separa MANDATORY/PREFERENCE. |
| A3 | Confirmada com ressalva | BuildPlanner limita 32 passos e 1.000.000 avaliações pontuadas; isso não limita todas as avaliações de elegibilidade. |
| A4 | Confirmada | RouteOptimizer beam 6, 100.000 unidades, BitSet e WeakHashMap sincronizado. |
| A5 | Confirmada | ExperienceService delega Solar/DB e Lunar; crédito 5 XP. |
| A6 | Confirmada | ExperienceLunar laço 500 tentativas e expansão repetida. |
| A7 | Confirmada com ressalva | Validação consultiva coexistindo com validarIdentidadeEstrutural, que lança exceção nos três geradores. |
| A8 | Confirmada com ressalva crítica | Auditor diferencia ERROR/WARNING, mas reutiliza EncounterRulesEngine.evaluate; não é um oráculo inteiramente independente. |
| A9 | Confirmada | SpellManagement distingue círculos, criação e edição inicial; domínio bloqueia edição após primeiro XP. |
| A10 | Confirmada | PerformanceMetrics registra contagem, média e percentis numa janela de 512. |
| A11 | Confirmada | ProgressionCache LRU sincronizado de 64. |
| A12 | Confirmada com ressalva | Pipeline descreve equipamento, mas também é chamado no XP e na restauração. |
| A13 | Confirmada | GenerationRules mantém 15 Encantos, 28 Habilidades e método Artes Marciais separado. |

## Hipóteses

| ID | Estado | Evidência e próxima prova |
|---|---|---|
| H1 | Lacuna confirmada no chamador | gerarComTratamento calcula quality.valid e publica o NPC sem consultá-lo. Ainda é necessário validar possíveis falsos positivos do auditor antes de ligar bloqueio. |
| H2 | Parcialmente mitigada; custo por medir | Catálogo preparado compartilhado, índices XP reutilizados e tags cacheadas já existem. Medir caminhos antes de acrescentar caches. |
| H3 | Contexto esclarecido | A busca em código de produção não encontra chamador de EncounterCanonicalPlanner; ele chama BuildPlanner, mas não substitui o otimizador ativo. Comparação diferencial continua pendente. |
| H4 | Não confirmado genericamente | Cache por id entrega roadmap com versão de esquema/algoritmo, hash de catálogo e pré-condição de estado. proximo() valida antes de aplicar. Conferir cobertura exata dos campos e caminho legado que aceita fingerprint vazio. |
| H5 | Não confirmado | Laço Lunar existe; só métricas de escolhas, requisitos e profundidade podem demonstrar compra inferior. |
| H6 | Risco confirmado, divergência pendente | UUIDs não dependem da seed; snapshots devem normalizar somente identificadores voláteis. Teste registra divergências sem promover novos goldens. |
| H7 | Degradação silenciosa confirmada para catálogo vazio | garantirInicialNaCriacao retorna o NPC sem feitiço quando não há Terrestres; não inventa compra, mas também não adiciona diagnóstico estruturado nesse retorno. |
| H8 | Em investigação | Há publicação otimista por snapshot no XP e persistência na restauração; suíte e casos adicionais precisam verificar integridade. |

## Problemas adicionais

1. A suíte normal exclui `*AuditTest` e `*StressTest`; EncounterTestSamples limita amostras normais a uma. Sucesso da build 947 não prova a matriz ampliada.
2. `acceptedMechanicalFingerprints` está vazio. Há infraestrutura de golden, mas nenhum conjunto mecânico congelado aceito.
3. O oráculo atual avalia cada Encanto com todos os demais já presentes: não prova ordem cronológica de aquisição e pode mascarar um ciclo. Precisam-se fixtures de ciclos/rotas alternativas e reconstrução incremental independente.
4. BuildPlanner deduplica pelo primeiro conjunto adquirido antes de comparar score. Rotas com o mesmo conjunto podem ter score dependente da ordem; requer teste diferencial, não remoção indiscriminada.
5. BuildPlanner esgota orçamento sem expor diagnóstico no Plan; `return@forEach` também não interrompe toda a sequência de elegibilidade. Limite de CPU não deve ser confundido com prova de inexistência de rota.
6. PerformanceMetrics usa ConcurrentLinkedQueue.size em cada amostra; a janela é limitada, porém a contagem exige travessia. Medir custo real antes de substituir a estrutura.
7. `dragonBloodedXpExpanders` é HashMap não sincronizado; conferir simultaneidade entre fallback/prefetch/limpeza e retenção antes de classificar como corrida demonstrada.

## Gate atual

Baseline em execução, nenhuma mudança em algoritmo/regra de produção. O teste cobre 540 cenários de observação, roundtrip JSON, repetição por seed e um passo XP. Isso NÃO substitui os testes de 10/50/100 XP, estresse 1.000, oráculo independente nem instrumentação Android. Métricas de agrupamento por categoria não são profundidade topológica.

## Execuções de baseline e correções do instrumento

- Run 38344819312: 1.114 testes existentes passaram; teste novo abortou no cenário Solar/Social/Foco/seed 0. A fixture usava `Socializar` em vez de `Socialização`. Entrada inválida contaminava a lista favorecida; isso não prova falha de distribuição para foco válido. Corrigido o instrumento, sem mudança do gerador.
- Run 38385728470: 1.114 testes existentes passaram; novo teste abortou em DB/Mental/automático/seed 0, porque decode remove avisos antigos por design. Diferença observada exclusivamente em alertasValidacao, não nos dados mecânicos. A matriz passa a registrar a migração de avisos e exigir igualdade de todos os demais campos.
- Exceções IllegalStateException são registradas por cenário para completar o inventário; sucesso do teste de observação não significa zero falhas de geração. Consultar summary.txt e cada failure.txt.

## Defeitos adicionais confirmados por inspeção; regressões preparadas

- Cache Lunar `orderedByAttribute`: chave antiga continha catálogo + arquétipo, mas os buckets dependem também dos traits da Forma Espiritual. A ordem de geração podia omitir rotas do próximo NPC. Correção candidata acrescenta cópia imutável dos traits à chave. Teste alterna sem visão noturna / com visão noturna / sem visão noturna e exige seleção do foco legal.
- BuildPlanner não atualizava charmCountByCategory ao projetar uma aquisição. Um requisito agregado podia continuar bloqueado apesar de Encantos simulados suficientes. O mesmo erro existia na projeção do valor futuro do CanonicalPlanner. Projeção compartilhada agora atualiza IDs, nomes e contagens.
- BuildPlanner deduplicava antes do score: A→B podia descartar B→A mais valioso. Novo teste demonstra diferença 11 vs 101 sobre o mesmo conjunto final. Correção mantém maior score com desempate determinístico sobre estado completo.
- Limite do BuildPlanner contava somente disponíveis e não interrompia a sequência de elegibilidade; candidato bloqueado também deve consumir orçamento. A versão candidata conta cada avaliação, interrompe os loops e expõe budgetExhausted/candidateEvaluations. Não liga esse planejador ao gerador ativo.
- Reconstrução canônica repetia duas resoluções por nome, buscas lineares de definição e criação de categorias. A candidata reutiliza contribuições imutáveis por ID do catálogo. Referência antiga congelada em teste diferencial; benchmark alterna ordem antigo/novo no mesmo processo. Nenhuma promessa de ganho até obter os resultados.

Essas mudanças locais ainda aguardam baseline completo e testes de integração. Não constituem conclusão da auditoria.

## Primeiro pacote validado parcialmente — run 38425139228

540 cenários completados, zero exceções, zero erros no auditor e zero divergências de seed. As 55 violações anteriores eram Corpo de Touro com Resistência 0; ambos os catálogos exigem Resistência 1. Agora o mínimo entra na redistribuição dos 28 pontos, antes dos PB, sem ponto gratuito. Mudanças de snapshots: 17 Solar, 38 DB, 2 Lunar. As alterações Solar/DB decorrem da correção obrigatória; Lunar decorre da chave do cache de rotas. Detalhamento por campo em comparison-first-pass.json.

Suíte: 1.123 testes, dois testes antigos falharam por proibir todo rating 1 não Favorecido, contradizendo o mínimo funcional de Resistência. A expectativa foi ajustada somente para os mínimos funcionais documentados (Resistência e Linguística Mental), mantendo os testes de 28 pontos e legalidade de Corpo de Touro.

Microbenchmark alternado, 120 estados por rodada, 10 rodadas: mediana antiga 47,405 ms; indexada 8,625 ms (−81,8%). Isso não demonstra melhora global: nesta execução geração automática mediana foi Solar 108,08 ms (+16,9%), DB 87,88 ms (+15,2%), Lunar 173,70 ms (+23,3%). Runners diferentes, sem controle de CPU/JIT; ainda é preciso investigar custo frio e repetir. Não declarar otimização global bem-sucedida.

A preservação de um Encanto de Destreza junto de uma árvore de Força não é automaticamente segundo foco de ataque: os casos detectados usam Forma do Gafanhoto Esmeralda como ponte estrutural da Forma Espiritual. Não remover essa função por uma simples contagem de categorias.
