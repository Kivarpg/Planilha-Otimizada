# Auditoria de convergência v12

Esta versão não encerrou após a primeira correção. Foram executadas rodadas sucessivas.

## Rodada 1 — falhas corrigidas

### Pré-requisitos ainda podiam ser valorizados como arestas independentes
Corrigido por `EncounterAcquisitionHypergraph`.
AND/OR/NOT permanecem uma expressão única de aquisição. Valor de unlock é marginal:
só existe quando a aquisição muda a expressão completa de falsa para verdadeira.

### Upgrade/recompra podia somar base + substituição
Corrigido por `EncounterPowerProjection`.
ADD, REPLACE e REMOVE são deltas sobre a projeção efetiva.

### Reset podia ser confundido com disponibilidade ilimitada
Corrigido por `EncounterAvailabilityCycle`.
Reset demonstrado significa apenas reutilização em princípio. Nenhuma frequência é inferida.

### Evidência podia ser duplicada entre par, rota, identidade e cobertura
Corrigido por `EncounterSynergyLedger`, com chave causal única.

### Shared prefix e horizonte de rota
Corrigido por `EncounterRouteState`.
Pré-requisito já possuído não é pago novamente. Destino fora de XP/Essência/número de
aquisições do horizonte não recebe valor futuro.

## Rodada 2 — falha exposta pelas correções

### Hubs genéricos ainda poderiam dominar por cobertura
Corrigido por `EncounterBuildCoherence`.
Agora são grandezas separadas:
- mechanicalStrength
- coverage
- coreFocus
- genericHubPenalty

SUPPORT/UTILITY com muitas mecânicas não define identidade por mera conectividade.

## Rodada 3 — revisão de invariantes

Foram rechecados os invariantes acumulados v3-v12:
- domínio nativo de Exaltado não mistura;
- legalidade precede score;
- Type/janela temporal precedem sinergia;
- Simple+Simple não combina na mesma rodada por padrão;
- exceção explícita aponta para regra específica;
- requisitos não são achatados;
- duração pertence ao efeito;
- variantes exclusivas não coexistem;
- configuração alternativa não é simultaneidade;
- transição não é presumida;
- recursos possuem namespaces e semânticas distintas;
- custo efetivo respeita modificadores;
- ator causal é preservado;
- stacking/caps não são presumidos;
- UNKNOWN não vira TRUE;
- fatos podem expirar/revogar;
- derivação cíclica sem raiz não se autossustenta;
- upgrade substitutivo não duplica base;
- reset não vira engine infinita;
- evidência causal não duplica score;
- rota paga custo marginal real;
- hubs genéricos não definem identidade;
- nenhuma camada escolhe ações de combate.

## Critério de parada

Nesta rodada final não foi encontrada nova falha estrutural que exija outra primitiva
arquitetural com as informações atualmente normalizadas.

Restam tarefas de maturação/integração, não falhas estruturais demonstradas:
1. migrar integralmente strings para IDs tipados;
2. ligar os modelos às definições reais dos cinco catálogos;
3. criar normalizadores/extratores conservadores;
4. executar corpus tests contra Charms reais;
5. calibrar pesos somente depois disso.

A próxima descoberta estrutural, se houver, deve surgir do corpus real e não de adicionar
abstrações especulativas.
