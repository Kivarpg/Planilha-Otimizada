package com.example.viewmodel

import com.example.data.EncantosSolaresCatalog
import com.example.model.ExaltedConstants

/**
 * Regex e índices estáticos usados na avaliação de pré-requisitos e
 * limites de recompra de Encantos (Aba 8).
 *
 * Extraído de CharmsActions (refatoração de organização — sem mudança
 * de comportamento). Compilados uma única vez; usados em toda abertura
 * de popup de Encantos.
 */
// Todas as expressões regulares usadas na avaliação de pré-requisitos e
// limites de recompra de Encantos são compiladas uma única vez aqui —
// essas funções rodam para cada Encanto do catálogo em toda abertura de
// popup, então recompilar o Regex a cada chamada seria desperdício real.
internal val REGEX_OU = Regex("\\s+ou\\s+", RegexOption.IGNORE_CASE)
internal val REGEX_ESSENCIA_MINIMA = Regex("(?i)\\s+de\\s+essência\\s+(\\d+)\\+\\s*$")
internal val REGEX_QUANTIDADE = Regex(
    "(?i)\\b(?:quaisquer|qualquer)\\s+([0-9]+|um|uma|dois|duas|três|tres|quatro|cinco|seis|sete|oito|nove|dez|quinze)\\s+"
)
internal val REGEX_SEPARADOR_MAIS_VIRGULA = Regex("\\s*\\+\\s*|\\s*,\\s*")
internal val REGEX_QUANTIDADE_X = Regex("(?i)^(.+?)\\s*\\(x(\\d+)\\)$")
internal val REGEX_SEPARADOR_GRUPO = Regex("\\s*,\\s*|\\s+ou\\s+|\\s+e\\s+")
internal val REGEX_RECOMPRA_COM_ESSENCIA = Regex("(?i)uma recompra com essência")
internal val REGEX_RECOMPRA_UNICA_M = Regex("(?i)pode ser recomprado uma única vez")
internal val REGEX_RECOMPRA_UNICA_F = Regex("(?i)pode ser recomprada uma única vez")
internal val REGEX_SEGUNDA_RECOMPRA = Regex("(?i)recomprad[oa] uma segunda vez|segunda recompra")
internal val REGEX_TERCEIRA_RECOMPRA = Regex("(?i)recomprad[oa] uma terceira vez|comprar este encanto uma terceira vez|terceira recompra")
internal val REGEX_MENCAO_RECOMPRA = Regex("(?i)recomprad[oa]")
internal val HABILIDADES_SOCIAIS_NORMALIZADAS = setOf(
    "Burocracia", "Linguística", "Performance", "Presença", "Socialização"
).map(EncantosSolaresCatalog::normalize).toSet()

// Índice estático das 25 Habilidades oficiais já normalizadas. Em cada
// avaliação de um pré-requisito de grupo, basta intersectar o texto do grupo
// com este conjunto em vez de percorrer e normalizar as 25 Habilidades.
internal val HABILIDADES_25_NORMALIZADAS = ExaltedConstants.ALL_25_ABILITIES
    .mapTo(HashSet(ExaltedConstants.ALL_25_ABILITIES.size), EncantosSolaresCatalog::normalize)

