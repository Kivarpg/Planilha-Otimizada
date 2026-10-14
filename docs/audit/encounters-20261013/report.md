# Auditoria e refatoração — Aba 11: Encontros

**Estado: validação do último pacote em andamento. Não autoriza compilação do APK.**

Branch: `refactor/encounters-audit-947`, repositório `Kivarpg/Planilha-Otimizada`.
Base: Exalted.947 com os backgrounds aprovados. A auditoria não alterou recursos visuais, catálogos, ícones ou layout.

## Alterações implementadas

| Problema confirmado | Solução | Evidência |
|---|---|---|
| Corpo de Touro adquirido com Resistência 0 | Mínimo funcional redistribuído dentro dos 28 pontos normais de Habilidade | 55 violações no baseline; zero nas matrizes posteriores |
| Cache Lunar ignorava traços da Forma Espiritual | Traços imutáveis incorporados à chave | Regressão alterna NPCs com/sem visão noturna |
| Projeções canônicas esqueciam contagens de Encantos | Projeção compartilhada atualiza IDs, nomes e categorias | Testes de pré-requisitos agregados |
| Deduplicação descartava trajetória mais valiosa | Comparar pontuação antes de reter estado equivalente | Fixture demonstra score 101 preservado sobre 11 |
| Orçamento ignorava candidatos bloqueados | Contagem de toda avaliação e interrupção explícita | Testes com orçamento artificialmente pequeno |
| Estado canônico repetia resolução e categorias | Índice por ID e contribuições sob demanda | Comparação com implementação congelada |
| Homônimos de outros tipos inflavam contagens | Resolução restrita ao Tipo de Exaltado e Artes Marciais | Uma raiz compartilhada não satisfaz requisito de dois Encantos |
| NPC inválido podia ser publicado pelo chamador | Barreira antes de atualizar estado e persistir | Integração com candidato inválido e cancelamento |
| Cache de expansores DB não era concorrente | ConcurrentHashMap e cópia da chave de Favorecidas | Suíte de integração e XP |
| Ausência de feitiço inicial elegível era silenciosa | Aviso estruturado, sem compra fictícia; removido quando há opção | Testes de catálogo vazio e recuperação |
| Auditor de NPC evoluído exigia Essência inicial | Separação criação/progressão; fórmula de XP existente preservada | 100 passos e reversão em nove perfis; mutações inválidas |
| Plano de XP legado sem pré-condição podia ser reutilizado | Rejeitar plano e executar expansão normal | Testes de ambos os aplicadores e estado alterado |
| Seleção inicial não considerava explicitamente cadeias futuras completas | Planejador de preferência com limites, pré-requisitos e profundidade | Fixtures dos três tipos e matriz real |
| Aprofundamento Lunar podia consumir as vagas da outra prioridade física | Reserva de mínimos de ataque e Vigor antes do aprofundamento | Pacote final ainda em validação |

As correções do planejador canônico não o tornam automaticamente o gerador ativo. A preferência futura foi integrada aos seletores efetivamente usados. Solar e Sangue de Dragão preservam a ordem do otimizador de sinergia; suas compras diretas de mínimo/Foco recebem a preferência futura. Lunar mantém rotas alternativas por Forma Espiritual.

## Estratégia e diferenças mecânicas

O planejamento só dá preferência a cadeias demonstráveis com os mínimos atuais de Habilidade/Atributo; Essência elevada é um objetivo futuro, nunca autorização de compra antecipada. Dependências ausentes, ciclos, nomes ambíguos e requisitos agregados não demonstrados não recebem bônus. Se o orçamento de busca termina, a preferência parcial é descartada.

A versão preliminar deslocou uma cadeia defensiva Solar. Isso foi detectado na comparação dos snapshots e corrigido com regressão real da seed 16. No último pacote já validado, os 163 cenários Solar historicamente legais preservaram a profundidade. Lunar passou de 2,633 para 2,778 na média da cadeia nomeada máxima: 34 casos melhoraram, 138 permaneceram iguais e oito diminuíram. Sangue de Dragão teve uma redução entre 142 casos historicamente legais, no Foco Social seed 16, trocando Exame Minucioso das Paixões por Fumaça Sem Chama. Essa troca prepara outro caminho futuro e não comprova superioridade em combate.

As nove trajetórias históricas/novas comparadas após 100 passos de XP inicialmente convergiram na quantidade e profundidade finais. Por isso, não se declara ganho estratégico universal nem se usa quantidade de Encantos como substituto de qualidade. O pacote final Lunar altera novamente essas medidas e precisa ser reavaliado.

A reserva Lunar atende à instrução de equilibrar Vigor e um único ataque principal, Força OU Destreza. Encantos utilitários ligados ao outro Atributo não são automaticamente um segundo foco. Os mínimos não autorizam compras ilegais nem exceder 15 vagas; Feitiçaria mantém sua reserva. Corpo de Touro começa limitado a uma aquisição no Físico, preservando seu teto legal nas recompras posteriores.

## Testes e rastreabilidade

A última execução concluída antes do pacote final foi `38526049352`, commit `f6450ad4cf95a752eab47d9400d7d3fd4c3075fc`: 1139 testes gerais aprovados e job ampliado aprovado. Matriz: 540 cenários, zero exceções, erros de auditoria ou divergências por seed. Estresse: 1380 gerações. XP: 900 passos na matriz e 1800 nas trajetórias pareadas. Os 540 snapshots históricos permanecem congelados e seus hashes são verificados.

O relatório original e a investigação A1–A13/H1–H8 estão em `reference.txt` e `findings.md`. O mapeamento T01–T52 e suas limitações está em `test-coverage.csv`. A lista integral de arquivos está em `modified-files.txt`; os CSVs e comparações preservam dados brutos e diferenças por campo.

## Desempenho

No benchmark pareado da execução 6, 120 estados por rodada, dez rodadas alternadas: mediana antiga 40,249 ms e indexada 8,045 ms, redução de aproximadamente 80% nesse componente. Com preparação fria e 15 estados: 5,460 ms e 5,207 ms, redução de 4,6%. A correção posterior de isolamento por tipo muda o escopo da referência e suas rodadas não devem ser combinadas com essa série.

Tempos globais variaram muito entre runners. Não se atribui uma redução global à refatoração com base apenas nessa comparação. O baseline automático teve p50 Solar 92,461 ms, Sangue de Dragão 76,255 ms e Lunar 140,916 ms. Comparações posteriores incluem percentis e diferenças em arquivos JSON.

A medição adicional de bytes alocados pela thread JVM está pendente. Ela não representa pico de heap, consumo total do aplicativo nem desempenho em celular.

## Limitações e próximas verificações

- O oráculo de aquisição usa avaliador e travessia independentes, mas compartilha o compilador de requisitos. Demonstra uma ordem legal nos ratings finais; não reconstrói todos os ratings históricos de cada compra.
- Ambiguidades de regras ou de recompra não modeladas não receberam interpretações novas. Custos e círculos continuam seguindo as regras existentes.
- Não há prova universal de que uma busca limitada encontre toda rota possível. Reservas obrigatórias usam busca direta e limites são diagnosticados.
- Profundidade de pré-requisitos é um indicador parcial; não mede sozinha ataque, defesa ou eficácia em combate.
- Não foram executados testes em aparelho/emulador, rotação, responsividade real ou pico de memória Android. Dependem da etapa posterior de APK autorizada pelo usuário.
- Novos snapshots são observações, não goldens aprovados automaticamente.
- O pacote final deve passar pela suíte e ter suas medições consolidadas antes de concluir esta entrega.
