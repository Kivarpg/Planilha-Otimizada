# Exalted.490

Candidato cumulativo baseado no Exalted.489.

## Aba 11 — Limite de NPCs Solar/Lunar
- Adiciona `Limite` à caracterização do NPC, imediatamente acima de `Idioma`.
- Exclusivo para Solares e Lunares; Sangue de Dragão não recebe Limite.
- Sorteio usa a mesma fonte de dados da seção Casta da Aba 2.
- Tabela Solar e tabela Lunar foram extraídas dos composables para `EncounterLimitCatalog`, eliminando duplicação.
- O sorteio ocorre depois da definição da casta do NPC e usa a tabela correspondente ao Tipo de Exaltado dessa casta.
- O Limite é persistido no save do NPC e exibido também na saída impressa da caracterização.
- Schema de NPC atualizado de 3 para 4, com migração compatível; saves antigos recebem Limite vazio e não são ressorteados ao carregar.
- Testes adicionados para impedir mistura entre tabelas Solar/Lunar e garantir ausência de Limite para Sangue de Dragão.

## Validação local
- JSONs de assets: válidos.
- Balanceamento estrutural dos Kotlin alterados: válido.
- Gradle local: não executado até compilação por indisponibilidade de rede para o wrapper (`services.gradle.org`).

## Exalted.521 — eficiência Compose
- Remove normalizações `lowercase()` executadas repetidamente pelos comparadores de ordenação nas gavetas da Aba 10 (Vínculos) e no catálogo de Artes Marciais.
- Usa `String.CASE_INSENSITIVE_ORDER`, preservando ordenação sem criar uma nova String normalizada em cada comparação.
- Adiciona teste de regressão para impedir retorno dessas alocações no hot path da UI.


A .584 implementa a regra opcional de segunda Assinatura dos Sangue de Dragão em Essência 5.


A .585 corrige a propagação da regra de Assinatura para os fallbacks de seleção e amplia o diagnóstico de testes do CI.


A .586 normaliza a comparação de Habilidades de Aspecto/Favorecidas na regra de Assinatura e torna o diagnóstico de testes do CI determinístico.


A .587 corrige a sintaxe YAML do workflow mantendo o diagnóstico JUnit em uma única linha Python segura para YAML.


A .588 alinha os testes de integração de +XP à arquitetura atual sem prefetch especulativo, preservando progressão direta e equipamentos manuais.
