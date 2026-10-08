# Exalted.558 — comparação real Mental puro × Feitiçaria (Solar/DB)

Base: Exalted.557, confirmado compilado.

Integração:
- Solares e Sangue de Dragão agora constroem, quando a exploração automática Mental
  é sorteada e a rota é materializável, dois candidatos determinísticos:
  1. candidato puro, sem prioridade artificial de Ocultismo;
  2. candidato com projeto de Feitiçaria Terrestre.
- a seleção final usa a política introduzida na 557;
- utilidade presente = soma dos níveis reais das Habilidades às quais os Encantos
  selecionados pertencem;
- crescimento/sinergia = ECS incremental, limitado pelo teto já existente em
  EncounterBuildQuality;
- empate automático mantém o candidato puro;
- Focus Feitiçaria explícito e a exceção estrutural histórica do Físico continuam
  fora da competição automática e preservam a materialização quando legal;
- nenhum novo RNG foi adicionado.

A seleção pura usa a mesma seleção determinística de Encantos, mas recalcula a
prioridade do arquétipo com `priorizarOcultismo = false`, evitando comparar uma
rota pura contaminada pela preferência da rota mágica.

Teste adicional confirma que a métrica concreta privilegia conjuntos ligados às
Habilidades efetivamente mais altas.

Lunares não foram alterados nesta etapa; possuem pipeline próprio e serão tratados
separadamente.
