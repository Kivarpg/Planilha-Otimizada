# Exalted.200 — auditoria pós-migração AGP 9.x

## Base

Esta versão parte da Exalted.198, já validada pelo CI pelo mantenedor do projeto.

## Escopo da auditoria

Foi feita uma inspeção estrutural das configurações Gradle/Android após a migração para:

- Android Gradle Plugin (AGP): 9.3.3
- Gradle Wrapper: 9.8.0
- Kotlin: 2.2.10
- JDK configurado no CI: 17
- compileSdk: 36
- targetSdk: 36

## Achado corrigido

A Exalted.198 apresentava uma inconsistência de versionamento do artefato:

- `gradle/libs.versions.toml` já usava AGP 9.3.3.
- `gradle-wrapper.properties` já usava Gradle 9.8.0.
- Porém `settings.gradle.kts` ainda declarava `rootProject.name = "Exalted.197"`.
- O workflow também procurava e publicava `Exalted.197.apk`.

Isso fazia com que a configuração da versão do projeto e o nome esperado do APK não acompanhassem a versão entregue do código.

## Correção em Exalted.200

A fonte canônica de versão do projeto foi atualizada para:

`rootProject.name = "Exalted.200"`

O workflow foi atualizado para validar e publicar:

`app/build/outputs/apk/debug/Exalted.200.apk`

A lógica de geração do APK já deriva o nome do artefato de `rootProject.name`, portanto a alteração mantém uma única fonte de verdade.

## Demais verificações estruturais

Não foram introduzidas alterações funcionais no gerador de NPCs, XP, Charms, Méritos, Especialidades, equipamentos ou interface.

O projeto continua sem o plugin separado `org.jetbrains.kotlin.android`, utilizando o suporte de Kotlin integrado ao AGP 9.x e o plugin Compose já existente.

Não foram encontrados, na configuração Gradle inspecionada, usos ativos das DSLs antigas `kotlinOptions`, `lintOptions`, `dexOptions`, `applicationVariants` ou `variantFilter`.

## Validação

A estrutura do projeto foi validada e o ZIP da versão passou em `unzip -t`.

A execução de `clean assemble` e da suíte Gradle não é declarada como executada neste ambiente; a validação executável permanece no GitHub Actions.
