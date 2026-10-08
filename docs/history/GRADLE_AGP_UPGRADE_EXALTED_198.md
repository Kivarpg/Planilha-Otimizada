# Exalted.198 — atualização Gradle / AGP

## Alterações

- Gradle Wrapper: 9.7.1 → 9.8.0.
- Android Gradle Plugin: 9.0.1 → 9.3.3.
- `ktlint-gradle`: 12.1.2 → 14.2.0 para a linha Gradle 9 e suporte ao Kotlin integrado/AGP 9.
- Versão do projeto: `Exalted.196` → `Exalted.198` para manter a numeração sequencial e o nome do APK do CI.

## Compatibilidade verificada por documentação oficial

- AGP 9.3.3 requer no mínimo Gradle 9.5.0 e JDK 17; portanto Gradle 9.8.0 satisfaz o requisito mínimo documentado.
- AGP 9.x usa Kotlin integrado; o projeto não aplica `org.jetbrains.kotlin.android`.
- O plugin Compose permanece separado (`org.jetbrains.kotlin.plugin.compose`) e usa a versão Kotlin já existente no catálogo.
- `ktlint-gradle` 13.1.0 introduziu suporte a Gradle 9; 14.1.0 adicionou suporte a projetos Android com o novo DSL/Kotlin integrado; esta versão usa 14.2.0.

## Validação local

O ambiente de execução não possui acesso DNS/rede a `services.gradle.org` e não possui uma instalação local do Gradle 9.8.0. Por isso não foi possível executar aqui `./gradlew wrapper --gradle-version 9.8.0`, `./gradlew clean assemble` ou os testes Gradle.

O `gradle-wrapper.properties` foi atualizado diretamente para apontar para `gradle-9.8.0-bin.zip`. A validação executável final deve ocorrer no GitHub Actions.
