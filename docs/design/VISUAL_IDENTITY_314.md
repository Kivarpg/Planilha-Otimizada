# Exalted.314 — Remodelação visual individual das abas 1–12

Escopo: somente apresentação. Nenhum campo, nome, categoria, botão ou regra de negócio foi criado, removido ou renomeado.

## Abas remodeladas e arquivos específicos alterados
1. Dados Pessoais — `PersonalDataTab.kt`
2. Casta / Aspecto — `CasteTabSolar.kt`, `CasteTabLunar.kt`, `AspectoTab.kt`
3. Atributos — `AttributesTab.kt`
4. Habilidades — `AbilitiesTab.kt`
5. Combate — `CombatTab.kt`
6. Méritos — `MeritsTab.kt`
7. Equipamentos — `EquipmentTab.kt`
8. Encantos — `CharmsTab.kt`
9. Planilha — `SummaryTab.kt`
10. NPCs — `NPCsTab.kt`
11. Encontros — `EncounterGeneratorTab.kt`
12. Conflito — `iniciativas/IniciativasTab.kt`

## Sistema visual aplicado
Cada uma das telas acima aplica explicitamente a moldura/atmosfera de conteúdo da identidade 314 no seu container raiz. Cards existentes presentes nesses arquivos receberam acabamento visual adicional quando compatível, sem alterar o conteúdo. O componente puramente visual `TabIdentitySurface.kt` concentra o desenho da moldura, halo, filetes e acabamento dos painéis para manter consistência entre as 12 abas e respeitar a paleta ativa de Solar, Sangue de Dragão e Lunar.

## Validação
A estrutura do pacote e as alterações dos arquivos foram verificadas. A compilação não pôde ser executada neste ambiente porque o Gradle Wrapper 9.8.0 não está em cache e `services.gradle.org` não está acessível.
