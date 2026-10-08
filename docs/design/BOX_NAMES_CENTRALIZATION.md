# Centralização dos nomes exibidos em caixas

A partir de Exalted.008, os nomes de seleção exibidos nas caixas da Aba 1 — Dados pessoais possuem uma única fonte em:

`app/src/main/java/com/example/model/BoxNames.kt`

## Casta Solar

- Alvorecer
- Zênite
- Crepúsculo
- Noite
- Eclipse

## Aspecto de Sangue de Dragão

- Ar
- Terra
- Fogo
- Água
- Madeira

## Casta Lunar

- Lua Cheia
- Lua Minguante
- Lua Nova
- Sem Casta

Os enums `Casta`, `Aspecto` e `LunarCasta` agora consomem esses nomes em vez de manter cópias literais.

Também foram centralizados os principais rótulos da seção Dados pessoais: Dados pessoais, Casta, Aspecto, Idioma, Intimidades, Nome, Jogador, Conceito, Descrição da Anima e Habilidade do Aspecto.

A descrição de Poder da Anima do Aspecto passou a receber o enum `Aspecto`, eliminando a comparação por strings literais como "Ar", "Terra", "Fogo", "Água" e "Madeira".

A intenção é que futuras mudanças de nomenclatura sejam feitas em um único ponto sem alterar a lógica ou a arte das caixas.
