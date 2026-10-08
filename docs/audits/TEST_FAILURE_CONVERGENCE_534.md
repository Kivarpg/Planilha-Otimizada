# Exalted.534 — convergência da falha de testes

O CI da .533 confirmou que fontes/recursos compilam e que a falha real está em
`:app:testDebugUnitTest`. O recorte ainda não contém o nome da classe/teste falho.

Correções:
1. rollback isolado de `FactId`/`SourceId` para data class. A conversão value class
   era a única otimização K2 nova da .533; removê-la reduz a superfície até o gate
   ficar verde.
2. Arquétipo e Foco foram desacoplados: Foco especializa a direção escolhida e não
   reescreve silenciosamente o Arquétipo para Mental. Feitiçaria explícita continua
   forçando a tentativa legal de rota mágica independentemente do Arquétipo.
3. Solar teve o gate de Feitiçaria explícita corrigido para não depender de
   `arquetipo == MENTAL`.
4. O CI agora imprime ao final os `<testcase>`/`<failure>` dos XML JUnit, além do
   resumo Gradle. Se qualquer teste continuar falhando, o próximo log deve conter
   classe, método e assertion.

Nenhuma regra de legalidade foi relaxada.
