# Protocolo de Auditoria — Exalted.135

## Baseline
- Versão auditada: `Exalted.134` (ZIP de entrada desta atualização).
- Esta revisão gera `Exalted.135`.
- O SHA-256 do ZIP de entrada deve ser registrado pelo processo de entrega.
- Nenhuma conclusão de auditoria deve misturar versões diferentes.

## Modelo de ameaça
A prioridade de segurança é o tratamento de dados externos não confiáveis, principalmente:
1. ShareCode/importação de arquivos recebidos de terceiros;
2. backups/arquivos corrompidos ou truncados;
3. arquivos acessíveis por outros componentes/aplicativos no dispositivo, quando aplicável.

Rede e cenário multiusuário não fazem parte do modelo principal do aplicativo local, salvo se uma funcionalidade futura introduzir isso.

## Evidência
- `N`: não verificado.
- `D`: documentação/especificação revisada.
- `L`: código revisado estaticamente.
- `T`: teste existente executado.
- `T+`: teste novo executado.
- `I`: teste em dispositivo/emulador ou instrumentado.

Ausência de evidência não equivale a `OK`.

## Natureza do achado
- 🔴 BUG CONFIRMADO
- 🟠 POSSÍVEL BUG
- 🟡 LACUNA DE ESPECIFICAÇÃO
- 🔵 RISCO TÉCNICO
- 🟢 OK
- ⚫ SUGESTÃO
- 🧪 LACUNA DE TESTE
- ⚪ NÃO VERIFICADO

## Severidade
A natureza/certeza do achado é separada da gravidade:
- CRÍTICA: perda/corrupção de dados, crash na abertura ou impossibilidade de restaurar/abrir ficha.
- ALTA: crash/ANR em uso normal, regra central produzindo ficha inválida ou falha de segurança explorável.
- MÉDIA: regra secundária incorreta, inconsistência entre abas ou degradação perceptível.
- BAIXA: apresentação/estilo ou inconsistência sem efeito nos dados.

## Regra contra invenção de especificação
Quando a regra esperada não estiver definida, registrar `LACUNA DE ESPECIFICAÇÃO`. Não criar teste normativo nem alterar o comportamento para preencher a lacuna até que a regra seja confirmada.

## Prioridade da auditoria
1. Integridade/perda de dados.
2. Crashes, ANRs e lifecycle.
3. Regras que podem produzir fichas inválidas.
4. Regressões conhecidas.
5. Segurança de importação/arquivos.
6. Desempenho e memória.
7. Melhorias de manutenção/estilo.

## Critério mínimo de conclusão
Uma área só pode ser considerada concluída quando todas as células aplicáveis da matriz tiverem estado/evidência registrados. Não podem permanecer 🔴/🟠 sem correção, decisão ou plano de tratamento documentado. Áreas não testadas devem permanecer `⚪ NÃO VERIFICADO`.

## Regras específicas da Aba 11
Os nove cenários obrigatórios continuam sendo:
- Solar: Físico, Social, Mental;
- Sangue de Dragão: Físico, Social, Mental;
- Lunar: Físico, Social, Mental.

As correções históricas ACH-001 a ACH-004 devem permanecer protegidas por testes:
- uma única permutação para Secundário/Terciário;
- garantia de Ocultismo quando a Feitiçaria exigir;
- 5 Habilidades de Aspecto + 5 Favorecidas adicionais no Sangue de Dragão;
- transbordo de PB Lunar sem descarte silencioso.

Toda alteração que modificar a sequência de consumo de `Random` deve ser identificada como mudança comportamental e acompanhada de testes de regressão.
