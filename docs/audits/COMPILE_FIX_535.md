# Exalted.535 — correção de compilação

O log da .534 identificou três erros K2 em EncounterGeneratorTab.kt: linhas 436, 440 e 444 referenciavam `focoEfetivo` depois que sua declaração foi removida na refatoração Arquétipo/Foco.

Correção: restaurada a variável local `val focoEfetivo = customizacao?.focoExplicito` imediatamente antes do despacho aos três geradores. O Arquétipo continua independente do Foco; a variável apenas encaminha o Foco explícito para Solar, Sangue de Dragão e Lunar.
