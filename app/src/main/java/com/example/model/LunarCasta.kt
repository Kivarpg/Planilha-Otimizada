package com.example.model

/**
 * Castas Lunares (Lunar Castes).
 *
 * Nomes de exibição em português. Os identificadores em inglês (FullMoon,
 * ChangingMoon, NoMoon, Casteless) ficam como nomes de enum para uso interno
 * e futura localização — a UI mostra apenas o displayName em PT-BR por enquanto.
 *
 * Atributos de Casta (Caste Attributes):
 * - Lua Cheia (Full Moon): Destreza, Vigor, Força
 * - Lua Minguante (Changing Moon): Aparência, Carisma, Manipulação
 * - Lua Nova (No Moon): Inteligência, Percepção, Raciocínio
 * - Sem Casta (Casteless): nenhum
 *
 * Lunares usam Atributos Favorecidos (não Habilidades Favorecidas como os Solares).
 */
enum class LunarCasta(val displayName: String, val englishName: String) {
    FullMoon(BoxNames.LunarCaste.FULL_MOON, "Full Moon"),
    ChangingMoon(BoxNames.LunarCaste.CHANGING_MOON, "Changing Moon"),
    NoMoon(BoxNames.LunarCaste.NO_MOON, "No Moon"),
    Casteless(BoxNames.LunarCaste.CASTELESS, "Casteless");

    /**
     * Atributos de Casta associados a esta casta.
     * Retorna lista vazia para Casteless.
     */
    fun casteAttributes(): List<String> = when (this) {
        FullMoon -> listOf("Destreza", "Vigor", "Força")
        ChangingMoon -> listOf("Aparência", "Carisma", "Manipulação")
        NoMoon -> listOf("Inteligência", "Percepção", "Raciocínio")
        Casteless -> emptyList()
    }

    /**
     * Pool de Atributos de Casta para a criação. Castas nomeadas fornecem
     * 3 opções, das quais o jogador escolhe exatamente 2. Sem Casta usa o
     * conjunto completo de 9 Atributos para a mesma seleção de 2.
     */
    fun poolAtributosCasta(): List<String> = when (this) {
        Casteless -> ExaltedConstants.ALL_ATTRIBUTES
        else -> casteAttributes()
    }

    /**
     * Nome do recurso drawable (sem extensão) em res/drawable-nodpi/.
     */
    fun nomeRecursoImagem(): String = when (this) {
        FullMoon -> "caste_lua_cheia"
        ChangingMoon -> "caste_lua_minguante"
        NoMoon -> "caste_lua_nova"
        Casteless -> "caste_sem_casta"
    }

    /**
     * Poderes de Casta (3 marcadores por Casta) — texto fornecido pelo
     * usuário, exibido na Aba 2 (Casta) quando a Casta correspondente é
     * selecionada na Aba 1 (Dados Pessoais).
     */
    fun poderesDeCasta(): List<String> = when (this) {
        FullMoon -> listOf(
            "Por cinco motes, o Lua Cheia adiciona (Essência, máximo 5) dados que não contam com Encantos em jogadas de movimento e feitos de força e ganha (maior que Essência ou 3) absorção natural até seu próximo turno. Isso é grátis na anima de fogueira.",
            "Ele adiciona (o atributo físico mais alto/2, arredondado para cima) Perseverança que não contam com Encanto contra testes de ameaças e outras influências baseadas no medo.",
            "Uma vez por dia, quando ele acerta um ataque decisivo que reinicia sua Iniciativa, ele pode pagar 10 motes, um de Força de Vontade para rolar Juntar-se à Batalha, adicionando (atributo físico mais alto) dados que não contam como Encanto."
        )
        ChangingMoon -> listOf(
            "Por cinco motes, o Lua Minguante confere às suas palavras um fascínio hipnótico, adicionando (Essência/2, arredondado para cima) dados que não contam com Encantos em um teste de influência. Qualquer um que perceba a influência o ouvirá; deixá-lo ou interrompê-lo antes que ele termine custa um ponto de Força de Vontade para resistir durante a cena. O custo deste poder é Silencioso e é dispensado na anima de fogueira.",
            "Com a anima fraca, ele ganha +1 Astúcia que não contam com Encanto e adiciona (Essência/2, arredondado para cima) dados que não contam com Encantos em testes de Furtividade e disfarce.",
            "Uma vez por dia, ele pode pagar três motes, um de Força de Vontade quando faz um teste de influência para ignorar os Laços negativos que seus alvos têm com ele (ou sua forma atual). Esses laços não podem ser usados para reforçar a determinação ou em pontos de decisão."
        )
        NoMoon -> listOf(
            "Por cinco motes, o Lua Nova se envolve em uma penumbra sombria até seu próximo turno, impondo uma penalidade de -2 em testes dependentes de visão contra ela, incluindo a maioria dos ataques. Isso é grátis na anima de fogueira.",
            "Por três motes, ele sente a localização e a natureza geral de um local próximo de poder ou significado oculto — uma propriedade, mansão, santuário espiritual, terra sombria, zona da Wyld, portal sobrenatural, trabalho de feitiçaria, etc. O Narrador não deve escolher o local mais próximo, mas aquele que for mais interessante e narrativamente relevante.",
            "Uma vez por dia, enquanto sua anima está na fogueira, ele pode gastá-la reflexivamente completamente para ganhar um ponto de Força de Vontade, adicionar (Essência + [atributo mental mais elevado]) motes de feitiçaria a um feitiço que ele está moldando ou adicionar uma Excelência completa gratuita (que conta como dados de Encanto) em um teste mental baseado em Atributos que não seja Juntar-se à Batalha."
        )
        Casteless -> listOf(
            "Com a anima fraca, o custo de mudar para formas humanas é descontado em dois pontos.",
            "Na anima de fogueira, o custo de mudar para formas animais é descontado em dois motes. Isso não se aplica a Encantos como Forma Bestial Imponente ou Forma do Gafanhoto Esmeralda.",
            "Uma vez por dia, ele pode usar o poder de uso diário de qualquer Casta Lunar. Ele não pode usá-lo novamente até que tenha usado os poderes de todas as três Castas, ou a sessão termine — por exemplo, um Sem Casta que usa o poder do Lua Cheia deve usar os poderes do Lua Minguante e Lua Nova antes de poder usar o poder do Lua Cheia novamente naquela sessão."
        )
    }
}
