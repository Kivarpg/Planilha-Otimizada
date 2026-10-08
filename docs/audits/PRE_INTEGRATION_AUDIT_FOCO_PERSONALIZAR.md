# Auditoria pré-integração — Aba 11: Foco + Personalizar

## Correções aplicadas
1. Perfil personalizado agora é respeitado de ponta a ponta no Solar: especialidades, feitiçaria e preferência de Méritos usam `arquetipoEfetivo`.
2. Sangue de Dragão: Favorecidas, feitiçaria e preferência de Méritos também usam `arquetipoEfetivo`.
3. Lunar: prioridade de Encantos, seleção Lunar e preferência de Méritos usam `arquetipoEfetivo`.
4. Troca do tipo de Exaltado remove somente um Foco incompatível (Habilidade vs Atributo), evitando estado inválido entre Solar/Sangue de Dragão e Lunar.
5. Adicionado `EncounterCustomizationRegressionTest` para contrato vazio/perfil e separação dos universos de Habilidades/Atributos.

## Compatibilidade
- Nenhum novo valor foi adicionado a `ArquetipoEncontro`.
- `EncounterCustomization` permanece opcional e não altera serialização/saves de NPC.
- Chamadas antigas continuam válidas graças aos parâmetros opcionais no final das assinaturas.

## Validação
- Inspeção estática das referências ao arquétipo original nos três geradores concluída.
- Integridade ZIP validada com `unzip -t`.
- Testes Gradle não puderam iniciar neste ambiente: o wrapper tenta obter Gradle 9.8.0 de services.gradle.org e a resolução DNS está indisponível. Isso é limitação do ambiente, não resultado de compilação.

## Antes do merge principal
Executar no CI/repositório com rede/cache Gradle:
`./gradlew testDebugUnitTest`
