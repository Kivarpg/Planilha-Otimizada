# Seleção final de equipamento — Aba 11

Após a geração do NPC, a Aba 11 abre o catálogo de armas compatível com a Habilidade Principal e, em seguida, o catálogo completo de armaduras Artefato. Há busca textual e opção de manter o equipamento originalmente gerado.

As escolhas usam `EncounterEquipmentService.montarArmaSelecionada` e `montarArmaduraSelecionada`, evitando duplicar estatísticas. `EncounterNpcActions.atualizarArmaduraNpc` foi corrigido para atualizar também absorção de armadura, absorção total e dureza ao trocar a armadura, e as alterações continuam persistidas pelo repositório existente.
