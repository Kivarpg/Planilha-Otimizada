# Exalted.537 — convergência da política de regras

Checkpoint de origem: Exalted.536, confirmado compilado no GitHub.

## Objetivo
Impedir que restrições obrigatórias e preferências de ranking sejam confundidas.

## Alterações
- `EncounterRulePolicy.filterRequired`: contrato explícito para filtrar LEGALITY/STRUCTURAL antes do ranking.
- `resolvePriority` continua compatível, mas sua documentação agora deixa claro que `exaltStructure`
  recebe somente preferências já legais, não restrições obrigatórias.
- Testes determinísticos cobrem:
  1. intenção explícita > Arquétipo;
  2. Arquétipo > preferência do Tipo de Exaltado;
  3. candidato ilegal não pode vencer mesmo quando aparece como intenção explícita;
  4. deduplicação mantém a primeira precedência;
  5. Supernal/Favorecida não substitui silenciosamente a direção central do Arquétipo.

Nenhuma regra de geração, custo, Encanto ou Feitiçaria foi alterada nesta rodada.
