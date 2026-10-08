# Exalted.554 — política única de materialização da Feitiçaria terrestre

Base estável: Exalted.553.

A convergência encontrou duplicação entre Solar e Sangue de Dragão na passagem
"explorar rota -> reservar/construir projeto". A mesma regra estava expressa
com variáveis locais diferentes, aumentando o risco de divergência futura.

Alterações:
- `EncounterSorceryRoutePolicy.shouldMaterializeTerrestrialProject` centraliza a fronteira;
- Mental automático exige simultaneamente exploração, Ocultismo mínimo e presença
  do Círculo Terrestre no catálogo;
- a exceção histórica Físico + Ocultismo mínimo foi preservada;
- Solar e Sangue de Dragão usam a mesma decisão para reserva de vagas, mínimo de
  Encantos de Ocultismo e aplicação do projeto;
- removidos estados locais duplicados `feiticariaMentalDef` e
  `feiticariaFisicaNecessaria`;
- testes cobrem exploração desligada, catálogo ausente, rota válida e exceção Física.

Nenhuma alteração em probabilidade 9/10, custo, círculos, orçamento total,
RulesEngine, Auditor ou regras Lunares.
