# Exalted.533 — estabilização + K2

## Diagnóstico do CI
Sem configuration cache, o runner avançou até `javaPreCompileDebugUnitTest`.
O recorte do GitHub ainda remove a mensagem primária que fica no meio do log.
O workflow agora, em caso de falha, imprime no FINAL um resumo extraído do log
com `e:`, `error:`, `What went wrong`, `Execution failed`, `Unresolved reference`
e erros de compilação. O log integral continua como artifact.

## K2 de baixo risco
`EncounterProjectedFactState.FactId` e `SourceId` eram data classes de um único
String usadas apenas como IDs tipados. Foram convertidas para `@JvmInline value class`.
A semântica de igualdade/hash permanece por valor e os testes existentes exercitam
Set, lookup, revogação e consumo. Não foi alterada `StableContentId`, pois contém
dois valores e convertê-la não seria uma aplicação equivalente/segura de value class.

## Compose/FIR
Nenhum plugin FIR customizado foi introduzido. Nenhuma flag experimental do Compose
Compiler foi habilitada sem medição. O projeto já está em Kotlin 2.2/K2; esta rodada
prioriza otimizações de representação local e comprováveis.

## Convergência
- package/path: sem divergências;
- delimitadores dos arquivos alterados: balanceados;
- sem mudança nas regras de NPC/Feitiçaria;
- limpeza de formatação no hot state loop.
