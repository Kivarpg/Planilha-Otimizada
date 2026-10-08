package com.example.model

/**
 * Banco de dados de custos de Pontos de Bônus (BP) na criação de
 * personagem, organizado por template — pedido explícito do usuário, que
 * forneceu a tabela completa do Lunar (diferente de Solar/Sangue de
 * Dragão) e pediu pra organizá-la junto com os valores já existentes.
 *
 * Os valores de Solar e Sangue de Dragão documentados aqui refletem a
 * lógica JÁ IMPLEMENTADA em SheetCalculations.calculateBpBreakdown() —
 * este arquivo não muda esse comportamento, só documenta os números num
 * lugar único e centralizado, ao lado da tabela nova do Lunar.
 */
object BpCostTable {

    // --- Solar ---
    // Atributo: 8/6/4 pontos base grátis por prioridade (1º/2º/3º);
    // excedente custa 4 BP por ponto (3 BP se a prioridade for 3º).
    // Habilidade: 28 pontos base grátis (favorecidas alocadas por último,
    // sobrando as mais baratas pro excedente); níveis 1-3 fora do pool
    // custam 1 (favorecida) ou 2 (não favorecida) BP; níveis 4-5 custam
    // sempre 1 (favorecida) ou 2 (não favorecida) BP.
    // Especialização: 4 grátis, excedente custa 1 BP cada.
    // Mérito: 10 pontos livres, excedente custa 1 BP por ponto.
    // Encanto de Casta/Favorecido: 4 BP (além do limite de poderes grátis).
    // Encanto fora de Casta/Favorecido: 5 BP.
    // Feitiço/Necromancia (Ocultismo de Casta/Favorecida): 4 BP.
    // Feitiço/Necromancia (Ocultismo fora de Casta/Favorecida): 5 BP.
    // Força de Vontade: 2 BP por ponto acima de 5.
    object Solar {
        const val ATRIBUTO_EXCEDENTE_PADRAO = 4
        const val ATRIBUTO_EXCEDENTE_PRIORIDADE_3 = 3
        const val HABILIDADE_FAVORECIDA = 1
        const val HABILIDADE_NAO_FAVORECIDA = 2
        const val ESPECIALIZACAO = 1
        const val MERITO = 1
        const val ENCANTO_FAVORECIDO = 4
        const val ENCANTO_NAO_FAVORECIDO = 5
        const val FEITICO_FAVORECIDO = 4
        const val FEITICO_NAO_FAVORECIDO = 5
        const val FORCA_DE_VONTADE = 2
    }

    // --- Sangue de Dragão ---
    // Mesma estrutura de Atributos/Habilidades/Especializações/Encantos/
    // Feitiços/Força de Vontade do Solar (usa o Aspecto no lugar de
    // Casta+Habilidades Favorecidas separadas). Méritos são a diferença
    // central: nunca custam Pontos de Bônus (orçamento próprio de 13+5,
    // fora do sistema de BP) — pedido explícito do usuário, já
    // implementado.
    object SangueDeDragao {
        const val ATRIBUTO_EXCEDENTE_PADRAO = Solar.ATRIBUTO_EXCEDENTE_PADRAO
        const val ATRIBUTO_EXCEDENTE_PRIORIDADE_3 = Solar.ATRIBUTO_EXCEDENTE_PRIORIDADE_3
        const val HABILIDADE_FAVORECIDA = Solar.HABILIDADE_FAVORECIDA
        const val HABILIDADE_NAO_FAVORECIDA = Solar.HABILIDADE_NAO_FAVORECIDA
        const val ESPECIALIZACAO = Solar.ESPECIALIZACAO
        const val MERITO = 0 // nunca gasta BP — orçamento próprio de Méritos
        const val ENCANTO_FAVORECIDO = Solar.ENCANTO_FAVORECIDO
        const val ENCANTO_NAO_FAVORECIDO = Solar.ENCANTO_NAO_FAVORECIDO
        const val FEITICO_FAVORECIDO = Solar.FEITICO_FAVORECIDO
        const val FEITICO_NAO_FAVORECIDO = Solar.FEITICO_NAO_FAVORECIDO
        const val FORCA_DE_VONTADE = Solar.FORCA_DE_VONTADE
    }

    // --- Lunar ---
    // Tabela fornecida pelo usuário — estrutura própria, diferente do
    // Solar/Sangue de Dragão: cada categoria tem seu próprio custo por
    // ponto (Atributo/Habilidade não separam níveis 1-3 de 4-5, e
    // Encantos/Feitiços não têm um "custo de excedente" diferente do
    // "custo direto" — é um valor único por unidade em cada categoria).
    object Lunar {
        const val ATRIBUTO_CASTA_OU_FAVORECIDO = 3
        const val ATRIBUTO_NAO_FAVORECIDO = 4
        const val HABILIDADE = 2
        const val ESPECIALIZACAO = 1
        const val MERITO = 1
        const val ENCANTO_CASTA_OU_FAVORECIDO = 4
        const val ENCANTO_NAO_FAVORECIDO = 5
        const val ENCANTO_NAO_FAVORECIDO_SEM_CASTA = 4
        const val FEITICO_INTELIGENCIA_FAVORECIDA = 4
        const val FEITICO_INTELIGENCIA_NAO_FAVORECIDA = 5
        // NOTA: "Habilidade Latente de Animal" (custo 3) não tem campo
        // correspondente na planilha ainda — não implementado por falta de
        // definição do que esse traço representa mecanicamente. Ver
        // conversa com o usuário; precisa de esclarecimento antes de
        // adicionar o campo e o cálculo de custo correspondente.
        const val HABILIDADE_LATENTE_DE_ANIMAL = 3
        const val FORCA_DE_VONTADE = 2
    }
}
