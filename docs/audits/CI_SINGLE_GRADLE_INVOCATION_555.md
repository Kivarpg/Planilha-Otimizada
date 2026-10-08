# Exalted.555 — CI consolidado e diagnosticável

Base funcional: Exalted.554 (execução cancelada; não promovida a checkpoint).
Checkpoint estável anterior: Exalted.553.

Diagnóstico:
- testes da 554 terminaram com sucesso;
- assembleDebug + renameDebugApk terminaram com sucesso;
- o workflow invocava Gradle três vezes, sempre sem configuration cache;
- o Engineering Gate reexecutava dez classes que já pertencem à suíte normal.

Mudanças:
- uma única invocação: `testDebugUnitTest renameDebugApk`;
- `renameDebugApk` já depende de `assembleDebug`;
- o Engineering Gate agora verifica os XMLs das dez classes em vez de reexecutá-las;
- falha se qualquer resultado obrigatório estiver ausente;
- preservados stacktrace, log integral, resumo de falhas e relatórios;
- duração da invocação Gradle passa a ser registrada explicitamente;
- removido warning nullable do teste de compartilhamento do catálogo.

Nenhuma regra de NPC, Feitiçaria, RulesEngine, Auditor ou UI foi alterada.
