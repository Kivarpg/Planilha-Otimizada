# Exalted.544 — correção de compilação da .543

Base: Exalted.543.

O GitHub chegou a `compileDebugUnitTestKotlin` e falhou em
`EncounterRulePolicyContractTest.kt:134` porque o novo teste referenciava
`ExaltedConstants` sem importar `com.example.model.ExaltedConstants`.

Correção: inclusão explícita do import correto. Nenhuma regra de geração, XP,
Encantos, Feitiçaria, Forma Espiritual ou Quimera foi alterada.

A versão do projeto foi atualizada para Exalted.544.
