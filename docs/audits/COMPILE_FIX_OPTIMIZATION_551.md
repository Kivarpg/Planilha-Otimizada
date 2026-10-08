# Exalted.551 — correção de compilação e micro-otimização

Base: Exalted.550 (falhou em compileDebugKotlin).
Checkpoint estável anterior: Exalted.549.

Falha confirmada: `LunarEncounterGenerator.kt` consumia `arquetipoEfetivo`
antes de sua declaração local.

Correções:
- declaração de `arquetipoEfetivo` movida antes dos consumidores;
- decisão de `EncounterSorceryRoutePolicy` permanece única por geração;
- removido o objeto local intermediário `decisaoFeiticaria`: apenas `.explore` é retido;
- a mesma decisão continua reutilizada nas duas passagens Lunares;
- nenhuma regra de probabilidade, legalidade, círculos, requisitos ou orçamento foi alterada.
