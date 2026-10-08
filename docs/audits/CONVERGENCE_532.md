# Exalted.532 — loop de convergência

## Rodada 1 — Build
O recorte do CI chega a `ValidateStep` sem preservar a mensagem principal. O projeto
mantinha configuration cache ativo apesar do histórico documentado de MutationGuard
no AGP 9/Gradle 9.8. Para a suíte de CI, ele foi desativado explicitamente. O runner
é efêmero e o ganho de reutilização entre execuções é pequeno; a prioridade aqui é
reprodutibilidade/diagnóstico.

## Rodada 2 — Foco Feitiçaria
A tradução `Feitiçaria -> Ocultismo/Inteligência` estava duplicada nos três geradores.
Foi centralizada em `EncounterCustomization.focoMecanico`, mantendo Feitiçaria fora
dos domínios canônicos de Habilidade/Atributo. Teste de contrato ampliado.

## Rodada 3 — Estrutura/limpeza
- Feitiçaria permanece exclusiva do Foco principal, não do secundário.
- Varredura de package/path: sem divergências.
- Varredura de declarações principais duplicadas: nenhuma.
- Comentários Lunares obsoletos foram alinhados à nova regra explícita.
- Nenhum FIXME ativo encontrado.
- Ocorrências textuais de TODO foram revisadas; eram majoritariamente a palavra
  portuguesa 'todo/todos', não dívida técnica.

## Diagnóstico futuro
O workflow continua preservando `gradle-testDebugUnitTest.log` como artifact em falha.
