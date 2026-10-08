# Roadmap isolado — Aba 11 / Personalizar

Base usada: arquivo da Biblioteca `libfile_ed455d245184819195e97a7fd51b35c1`, registrado como `Exalted.296.zip`.

## Implementado
- Botão `Personalizar` na mesma linha de Físico, Social e Mental, em quarto lugar.
- Solar e Sangue de Dragão: seletor com as 25 Habilidades.
- Lunar: seletor com os 9 Atributos.
- A escolha personalizada substitui o arquétipo visual como foco da geração sem adicionar um novo valor persistido ao enum `ArquetipoEncontro`.
- O foco escolhido é inserido antes das prioridades normais de distribuição.
- Solar: quando possível, a Casta/Supernal é alinhada ao foco escolhido; o foco também participa das Favorecidas e prioridades de PB.
- Sangue de Dragão: o foco é priorizado entre Favorecidas quando não pertence ao Aspecto e entra nas prioridades de distribuição/PB.
- Lunar: o Atributo escolhido determina a categoria primária, é priorizado entre Atributos especiais quando aplicável e recebe o maior valor disponível de sua categoria sem alterar o orçamento total.
- O fluxo normal continua selecionando Habilidade de Ataque e, em seguida, a Habilidade de Defesa correspondente.
- Trocar para Físico/Social/Mental limpa o foco personalizado; trocar o Tipo de Exaltado também limpa a escolha incompatível.

## Compatibilidade
Não foi criado `ArquetipoEncontro.PERSONALIZADO`. Isso evita alteração de JSON/saves e reduz o risco na futura integração com o projeto principal.

## Verificação
Foi adicionado `EncounterPersonalizedFocusTest.kt` para regras de mapeamento e prioridade Lunar.
A compilação Gradle não pôde ser executada neste ambiente porque o wrapper tentou baixar Gradle 9.8.0 de `services.gradle.org` e o runtime está sem resolução/acesso de rede para esse host (`UnknownHostException`). O wrapper não chegou à fase de compilação Kotlin.
