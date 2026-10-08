# Exalted.548 — sinergia incremental e continuidade

Base estável: Exalted.547.

Pacote fecha uma lacuna entre Planner e ECS:
- a pontuação de um candidato agora considera tanto os Encantos previamente
  selecionados pelo chamador quanto os Encantos já adicionados pelo próprio plano;
- portanto a sinergia é incremental ao longo da sequência, não congelada no estado inicial;
- legalidade continua exclusivamente no RulesEngine;
- crescimento futuro continua limitado por EncounterBuildQuality;
- BuildPlanner elimina uma segunda avaliação redundante de elegibilidade no mesmo estado,
  carregando o resultado já calculado;
- testes reforçam que potencial futuro limitado não supera uma vantagem atual relevante,
  mas pode diferenciar opções atuais equivalentes.

Não há simulação de combate nem promoção automática de compatibilidade BUILD_ONLY
para SAME_ACTION. O ECS continua avaliando sinergia de construção, enquanto o
CombinationEvaluator permanece responsável por realizabilidade estrutural declarada.
