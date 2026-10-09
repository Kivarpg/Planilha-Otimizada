package com.example.model

import androidx.compose.runtime.Immutable
import java.util.UUID

// Tipos de Exaltado suportados pelo Gerador de Encontros (Aba 11).
// Cada tipo possui gerador e regras específicas; esta enum identifica o tipo
// do NPC persistido e não deve ser usada como substituta das regras próprias
// de Casta/Aspecto/Atributos Favorecidos.
enum class TipoExaltadoEncontro {
    SOLAR,
    SANGUE_DE_DRAGAO,
    LUNAR
}

// Perfil comportamental do NPC — determina prioridades de distribuição de
// recursos na criação E de aquisição de poderes na progressão por XP.
enum class ArquetipoEncontro {
    FISICO, SOCIAL, MENTAL
}

// Um encanto selecionado (na criação ou via progressão de XP) pro NPC.
// Guarda só nome/habilidade/custo (não o objeto Encanto inteiro do
// catálogo) porque o NPC de encontro é uma planilha simplificada, pensada
// pra consulta rápida durante uma cena — não uma planilha jogável completa
// como CharacterSheet.
data class EncantoEncontro(
    val nome: String,
    val habilidadeVinculada: String,
    val custo: String
)

// Um lote de "+ Aumentar XP" — guarda exatamente o que foi comprado com
// aquele lote específico, pra "− Diminuir XP" poder desfazer só aquilo,
// não uma quantidade arbitrária. Pedido explícito do usuário.
data class EncounterProgressionStep(
    /** Fingerprint do NPC imediatamente antes deste lote ser aplicado. */
    val preconditionFingerprint: String = "",
    /** Fingerprint do catálogo usado para construir este passo do roadmap. */
    val catalogFingerprint: String = "",
    val xpGasto: Int,
    val encantos: List<EncantoEncontro> = emptyList(),
    val habilidadeMelhorada: String? = null,
    val pontosGanhosNaHabilidade: Int = 0,
    val especializacaoAdicionada: String? = null,
    val pontosForcaDeVontadeComprados: Int = 0,
    val lunarAtaqueEscolhido: String? = null,
    val formaEspiritualSecundariaLunar: String? = null,
    val lunarArchetypeTraits: List<String>? = null
)

data class EncounterProgressionRoadmap(
    /** Próximos lotes de XP planejados a partir do estado atual do NPC. */
    val passos: List<EncounterProgressionStep> = emptyList(),
    /** Quantos passos do roadmap já foram executados pelo botão +XP. */
    val proximoPasso: Int = 0,
    /** Versão do formato/contrato do roadmap. Saves antigos são invalidados. */
    val schemaVersion: Int = 1,
    /** Versão lógica do algoritmo que produziu os passos. */
    val algorithmVersion: Int = 1,
    /** Versão lógica dos catálogos usados para produzir os passos. */
    val catalogVersion: Int = 1,
    /** Impressão digital dos catálogos efetivamente usados no planejamento. */
    val catalogFingerprint: String = ""
)

data class HistoricoXpBatch(
    val xpGasto: Int,
    val nomesEncantosAdicionados: List<String>,
    // Habilidade melhorada com a "reserva" desse lote (2 a cada 10 XP) —
    // null se o lote não teve XP suficiente pra reservar nada, ou se não
    // havia habilidade elegível pra melhorar no momento.
    val habilidadeMelhorada: String? = null,
    val pontosGanhosNaHabilidade: Int = 0,
    val especializacaoAdicionada: String? = null,
    val pontosForcaDeVontadeComprados: Int = 0
)

// Um feitiço adquirido via progressão (perfil mental, foco Ocultismo).
data class FeiticoEncontro(
    val nome: String,
    val circulo: String,
    val custo: String
)

// Especialidade — só soma +1 na habilidade correspondente pra fins de
// cálculo; não tem descrição própria (exibida como "Nome da Habilidade (+1)").
data class EspecialidadeEncontro(
    val habilidade: String
)

// Arma equipada — sempre Artefato, exceto Briga (leve Mundana). Guarda os
// valores já prontos pro cálculo de ataque/aparar, sem precisar reconsultar
// a tabela de armas toda vez.
data class ArmaEncontro(
    val nome: String,
    val peso: String, // "Leve" | "Média" | "Pesada"
    val tipo: String, // "Artefato" | "Mundana"
    val precisao: Int,
    val dano: Int,
    val defesa: Int,
    val motesComitados: Int = 0,
    // Etiquetas do catálogo de armas (ex.: "Letal", "Armas brancas",
    // "Alcance") — pedido explícito do usuário: exibidas ao final das
    // estatísticas na planilha do NPC.
    val etiquetas: List<String> = emptyList()
)

// Armadura equipada — todo NPC recebe uma automaticamente (seção 9.2).
// motesComitados é o que fica reservado enquanto ela está equipada;
// desequipar devolve esses motes à reserva periférica.
data class ArmaduraEncontro(
    override val nome: String,
    val peso: String, // "Leve" | "Média" | "Pesada"
    val tipo: String = "Artefato", // "Artefato" | "Mundana"
    override val absorcao: Int,
    override val dureza: Int,
    override val penalidadeMobilidade: Int,
    val motesComitados: Int,
    // Marcadores do catálogo (ex.: "Ocultável", "Silenciosa") — pedido
    // explícito do usuário. Vazio em armaduras mundanas/genéricas.
    override val marcadores: List<String> = emptyList(),
    // Custo em pontos de Mérito quando Artefato — a maioria é 3 (padrão),
    // mas a Armadura de Seda custa 4. Ignorado quando tipo == "Mundana".
    val custoMeritoArtefato: Int = 3
) : ArmaduraComum {
    override val tipoNormalizado: String get() = tipo
    override val pesoNormalizado: String get() = peso
}

// Planilha simplificada de um NPC gerado pela Aba 11. Deliberadamente separada
// de CharacterSheet (a planilha jogável completa) e de Npc (o cadastro manual
// da Aba 9) — este aqui carrega só o necessário pra rodar um encontro sem
// exigir cálculo manual do mestre: valores de combate/atuação já prontos,
// não uma progressão de personagem completa.
// @Immutable: mesma justificativa de CharacterSheet.kt — os campos
// List/Map tornariam a classe "instável" pro Compose por padrão. Todas
// as atualizações usam .copy() (via _npcsEncontro.update {...}), nunca
// mutação in-place, então a garantia da anotação é genuinamente cumprida.
@Immutable
data class NpcEncontro(
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    /** Gênero usado pelo gerador para exibição do símbolo na Aba 11. */
    val genero: String = "",
    val tipoExaltado: TipoExaltadoEncontro = TipoExaltadoEncontro.SOLAR,
    val arquetipo: ArquetipoEncontro = ArquetipoEncontro.FISICO,

    // --- Casta e habilidades (seção 4) ---
    val casta: String = "",
    val habilidadesFavorecidas: List<String> = emptyList(),
    /** Só para Lunares: subconjunto exato dos 2 Atributos de Casta. */
    val lunarAtributosCasta: List<String> = emptyList(),
    val habilidadeSupernal: String = "",

    val attributes: Map<String, Int> = emptyMap(),
    val abilities: Map<String, Int> = emptyMap(),
    // Méritos distribuídos automaticamente na geração da Aba 11.
    // O valor de cada item é o custo em pontos de Mérito.
    val merits: List<Merito> = emptyList(),
    val especialidades: List<EspecialidadeEncontro> = emptyList(),

    val habilidadePrincipal: String = "", // habilidade de combate (seção 6.2)
    val habilidadeDefensiva: String? = null,
    val habilidadeSuporte: String = "",

    val charms: List<EncantoEncontro> = emptyList(),
    /** Estilo marcial principal escolhido pela geração; vazio quando não há rota marcial. */
    val estiloArtesMarciais: String = "",
    /** Estilos posteriores só aparecem após desenvolvimento suficiente do principal. */
    val estilosArtesMarciaisAdicionais: List<String> = emptyList(),
    val feiticos: List<FeiticoEncontro> = emptyList(),
    /** Feitiço que ocupa a vaga inicial/gratuita; os demais Feitiços são independentes. */
    val feiticoInicialNome: String? = null,
    val corpoDeTouroCount: Int = 0, // vezes que Técnica do Corpo de Touro foi adquirida

    // --- Essência e recursos (seções 3, 20) ---
    val essencia: Int = 1,
    /** Idioma inicial do NPC. Idiomas adicionais são obtidos por Méritos. */
    val idioma: String = "",
    /** Exclusivo para Lunares: animal sorteado da Forma Espiritual. */
    val formaEspiritual: String = "",
    val formaEspiritualSecundaria: String = "",
    val lunarArchetypeTraits: List<String> = emptyList(),
    val lunarPrimaryArchetypeTraits: List<String> = emptyList(),
    /** Exclusivo para Lunares: última marca/sinal sorteada manualmente. */
    val sinal: String = "",
    val motesPersonais: Int = 0,
    val motesPerifericos: Int = 0,
    val forcaDeVontade: Int = 0,
    // Quais quadrados de Força de Vontade estão marcados como gastos —
    // pedido explícito do usuário: mesmo estilo de quadrados dinâmicos
    // da Aba 5, que precisa dessa informação separada do total.
    val forcaDeVontadeUsados: Set<Int> = emptySet(),

    // --- Progressão por XP (seções 18, 20) ---
    val xpAtual: Int = 0, // experiência disponível, ainda não gasta
    val xpGastoTotal: Int = 0, // experiência efetivamente gasta (define a Essência)
    /**
     * Torna-se true no primeiro recebimento de 5 XP e nunca volta a false.
     * É separado de xpAtual/xpGastoTotal/histórico para que reduzir ou gastar XP
     * não reabra a edição gratuita do Feitiço inicial.
     */
    val primeiroXpRecebido: Boolean = false,
    // Um item por cada vez que "+ Aumentar XP" foi pressionado — permite
    // que "− Diminuir XP" desfaça exatamente aquele lote específico
    // (encantos e o ponto de habilidade/especialização comprados com
    // ele), não só descontar o número de XP. Pedido explícito do usuário.
    val historicoXpBatches: List<HistoricoXpBatch> = emptyList(),

    // --- Equipamento (seção 9) ---
    val arma: ArmaEncontro? = null,
    val armadura: ArmaduraEncontro? = null,

    // --- Combate (Físico) / Atuação (Social/Mental) — seções 10/11 ---
    val acaoPrincipal: Int = 0, // Ataque Fulminante (Físico) ou Ação Social/Mental Principal
    val acaoDecisiva: Int = 0, // Ataque Decisivo (só Físico)
    val defesaPrimaria: Int? = null, // Aparar — calculado sempre com Briga, mesmo quando Briga = 0
    val esquiva: Int = 0,
    val absorcaoNatural: Int = 0,
    val absorcaoArmadura: Int = 0,
    val absorcao: Int = 0, // total (natural + armadura)
    val dureza: Int = 0,
    val perseveranca: Int = 0,
    val astucia: Int = 0,
    val juntarABatalha: Int = 0,
    val investida: Int = 0,
    val desengajamento: Int = 0,
    /** Penalidade temporária de Clash: -2 para Aparar/Evasão até a próxima ação. */
    val penalidadeClashDefesa: Int = 0,
    /** Redução cumulativa das defesas passivas por ataques recebidos na Aba 12. */
    val penalidadeAtaquesDefesa: Int = 0,

    val healthBoxes: List<CaixaVitalidade> = ExaltedConstants.defaultHealthBoxes(),
    // Contador de iniciativa ajustável durante a cena (Aba 11) — separado
    // do valor-base "iniciativa" (Juntar-se à Batalha), que é fixo,
    // calculado na geração; este aqui sobe/desce durante o combate.
    val iniciativaAtual: Int = 0,
    val dano: String = "",
    val alertasValidacao: List<String> = emptyList(),
    /** Exclusivo para Solares e Lunares: Falha de Virtude/Limite sorteada da tabela da Aba 2. */
    val limite: String = "",
    /** Foco explicitamente escolhido pelo usuário na criação. Automático/Nenhum permanecem null. */
    val focoProgressaoExplicito: String? = null,
    /** Arvore ofensiva escolhida na geracao Lunar fisica, independente do foco do usuario. */
    val lunarAtaqueEscolhido: String? = null
) {
    // Pior penalidade de ferimento marcada na trilha — mesma lógica de
    // CharacterSheet.penalidadeFerimentoAtual(), reduz Aparar/Evasão/
    // Defesa exibidos na Aba 11 sem alterar os valores-base armazenados.
    /** Edição gratuita do Feitiço inicial só existe antes do primeiro lote de XP. */
    fun podeGerenciarFeiticoInicial(): Boolean = !primeiroXpRecebido

    fun penalidadeFerimentoAtual(): Int {
        return com.example.data.EncounterMeritEffectsService.penalidadeFerimentoEfetiva(merits, healthBoxes.piorPenalidadeDeFerimento())
    }

}
