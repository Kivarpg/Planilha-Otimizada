# Aba 11 — Foco + Personalizar (pacote isolado)

Este pacote NÃO deve ser integrado à base principal até a etapa correspondente do roadmap.

## Foco
- Botão rápido ao lado de Físico, Social e Mental.
- Solar/Sangue de Dragão: escolhe uma Habilidade prioritária.
- Lunar: escolhe um Atributo prioritário.
- O restante continua automático.

## Personalizar
Construtor estrutural opcional com cinco decisões:
1. Foco — Habilidade (Solar/Sangue de Dragão) ou Atributo (Lunar).
2. Ataque — Habilidade de combate; Automático disponível.
3. Defesa — Esquiva, Armas Brancas ou Briga; Automático disponível.
4. Secundária — Habilidade adicional prioritária; Automático disponível.
5. Perfil — Automático, Físico, Social ou Mental.

As escolhas são encaminhadas ao gerador por `EncounterCustomization`. O objeto é opcional e as APIs mantêm defaults, preservando as chamadas antigas.

## Regras
- Perfil determina o arquétipo estrutural usado na distribuição quando explicitamente escolhido.
- Foco tem precedência na prioridade do gerador.
- Ataque e Defesa substituem os sorteios correspondentes quando informados.
- Secundária entra nas habilidades estruturalmente relevantes/prioritárias.
- Campos em Automático mantêm o comportamento original.
- Não foi adicionado novo valor a `ArquetipoEncontro`, evitando migração de saves/JSON.

## Validação neste ambiente
O wrapper não pôde iniciar a compilação porque tentou baixar Gradle 9.8.0 de `services.gradle.org`, indisponível por DNS/rede neste runtime. Portanto, a compilação Android deve ser executada no CI/ambiente com Gradle disponível antes da integração.
