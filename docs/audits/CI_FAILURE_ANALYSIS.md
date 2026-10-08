# Análise do CI — Exalted.137 → Exalted.138

## Evidência usada

Base: log real do GitHub Actions fornecido para `./gradlew testDebugUnitTest --stacktrace`.
Resultado: **345 testes, 14 falhas**.

O resumo do Gradle informa `IllegalStateException`/`AssertionError`, mas não imprime as mensagens internas das exceções. Portanto, este documento distingue o que é observável no código do que permanece dependente da confirmação do próximo CI.

## Correções desta versão

### 1. Validação final de Atributos

A construção-base conhece explicitamente os grupos Primário, Secundário e Terciário e valida 11/9/7 nesse momento.

A ficha final, porém, não persiste qual dos dois grupos não primários foi escolhido como Secundário e qual foi escolhido como Terciário. A validação final da Exalted.138 deixa de tentar reconstruir essa informação.

A validação final continua conferindo:
- os 9 Atributos canônicos;
- valores entre 1 e 5;
- soma global compatível;
- pelo menos um Atributo 5 no grupo Primário;
- invariantes específicos de cada Tipo de Exaltado.

Isso evita transformar uma informação de construção não persistida em uma falsa regra da ficha final.

### 2. Ajuste de combate

O ajuste de Força/Destreza foi reescrito para selecionar diretamente um estado físico válido, preservando o total da categoria Física.

Garantias:
- Arqueirismo/Arremesso: Destreza = 5 e Força <= 2;
- Armas Brancas/Briga: Destreza >= 4;
- pelo menos um Atributo físico em 5;
- totais das três categorias preservados;
- soma global preservada.

A escolha entre estados igualmente próximos continua usando `Random`, mantendo a natureza estocástica do gerador.

### 3. Catálogo parcial de Feitiçaria

A seleção de Feitiçaria Terrestre Mental não lança mais exceção apenas porque o item sorteado não existe em um catálogo parcial.

Quando a definição está presente, a reserva e as regras de seleção continuam funcionando normalmente.

Isso é especialmente importante para testes unitários que fornecem catálogos incompletos.

## ACH-001..004

As correções estruturais anteriores permanecem:
- ACH-001: seleção dos dois grupos não primários por um único embaralhamento;
- ACH-002: reserva/prioridade de Ocultismo quando a Feitiçaria é exigida;
- ACH-003: Sangue de Dragão mantém 5 Habilidades de Aspecto + 5 Favorecidas adicionais fora do Aspecto;
- ACH-004: distribuição Lunar possui fallback para consumir o saldo de PB.

## Verificação

A execução local do Gradle nesta sessão continua indisponível porque o ambiente não consegue resolver `services.gradle.org`.

Portanto, **não é afirmado que os 345 testes foram executados localmente**. A confirmação definitiva desta versão é o próximo `testDebugUnitTest` no GitHub Actions.
