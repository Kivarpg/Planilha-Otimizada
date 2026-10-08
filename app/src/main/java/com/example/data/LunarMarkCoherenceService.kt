package com.example.data

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/**
 * Coerência exclusivamente VISUAL entre Forma Espiritual e Sinal Lunar.
 *
 * Não participa da legalidade de Arquétipos/Encantos. Sinais neutros continuam universais;
 * sinais morfológicos explícitos só entram quando a família visual da Forma os suporta.
 */
internal object LunarMarkCoherenceService {
    enum class VisualFamily {
        AQUATICO, AVE, FELINO, CANIDEO, UNGULADO, REPTIL, ANFIBIO,
        QUIROPTERO, ARTROPODE, MOLUSCO, PRIMATA, ROEDOR,
        URSIDEO, MUSTELIDEO, MAMIFERO_MARINHO, DINOSSAURO, MAMIFERO_TERRESTRE, OUTRO
    }

    enum class Compatibility { FORTE, COMPATIVEL, INCOMPATIVEL }

    private val combiningMarks = "\\p{M}+".toRegex()

    private fun n(v: String): String = Normalizer.normalize(v, Normalizer.Form.NFD)
        .replace(combiningMarks, "").lowercase(Locale.ROOT)

    private fun hasNormalized(normalized: String, keys: Array<String>) = keys.any { normalized.contains(it) }
    private fun terms(vararg values: String): Array<String> = values.map(::n).toTypedArray()
    private fun has(s: String, vararg keys: String) = hasNormalized(n(s), keys.map(::n).toTypedArray())

    private fun computeFamilia(animal: SpiritualFormService.Animal): VisualFamily {
        val s = n(animal.portuguese + " " + animal.english)
        return when {
            has(s, "baleia","whale","tubarao","shark","peixe","fish","arraia","ray","orca",
                "lula","squid","polvo","octopus","enguia","eel","cavalo-marinho","seahorse",
                "dugongo","manatee","golfinho","dolphin") -> VisualFamily.AQUATICO
            has(s, "foca","seal","lontra-marinha","sea otter") -> VisualFamily.MAMIFERO_MARINHO
            has(s, "aguia","eagle","albatroz","albatross","cisne","swan","avestruz","ostrich",
                "emu","ema","rhea","pinguim","penguin","pelagornis","gastornis","ave-do-terror",
                "phorusrhacos","titanis","beija-flor","hummingbird","pardal","sparrow","canario",
                "canary","andorinha","swallow","andorinhao","swift","periquito","parakeet",
                "calopsita","cockatiel","tentilhao","finch","corruira","wren","chapim","goldcrest",
                "confuciusornis","protopteryx") -> VisualFamily.AVE
            has(s, "leao","lion","tigre","tiger","onca","jaguar","leopardo","leopard","guepardo",
                "cheetah","sucuarana","cougar","smilodon","homotherium","felis","hoplophoneus",
                "eusmilus","barbourofelis","machairodus","megantereon","dinofelis","xenosmilus",
                "therailurus") -> VisualFamily.FELINO
            has(s, "lobo","wolf","coiote","coyote","dingo","chacal","jackal","raposa","fox",
                "hesperocyon","enhydrocyon") -> VisualFamily.CANIDEO
            has(s, "morcego","bat","volaticotherium") -> VisualFamily.QUIROPTERO
            has(s, "ra","frog","sapo","toad","perereca","salamandra","newt","tritao") -> VisualFamily.ANFIBIO
            has(s, "cobra","snake","cascavel","rattlesnake","mamba","sucuri","anaconda","piton",
                "python","jiboia","boa","surucucu","bushmaster","jacare","caiman","gavial","gharial",
                "tartaruga","turtle","lagarto","lizard","gecko","camaleao","chameleon","anolis",
                "skink","dragao-de-komodo","komodo","quinkana","wonambi","meiolania") -> VisualFamily.REPTIL
            has(s, "borboleta","butterfly","mariposa","moth","abelha","bee","vespa","wasp","formiga",
                "ant","besouro","beetle","escaravelho","scarab","vagalume","firefly","joaninha",
                "ladybug","louva-a-deus","mantis","bicho-pau","stick insect","bicho-folha","leaf insect",
                "grilo","cricket","gafanhoto","locust","cigarra","cicada","libelula","dragonfly",
                "aranha","spider","escorpiao","scorpion","lacraia","centipede","piolho","louse",
                "carrapato","tick","acaro","mite","mosca","fly","mosquito","pernilongo","cupim",
                "termite","pulga","flea","pulg","aphid","tripes","thrips","caddisfly","mayfly",
                "stonefly","lacewing","antlion","sawfly","horntail","midge","gnat","water strider",
                "backswimmer","water boatman","silverfish","springtail","earwig","webspinner") -> VisualFamily.ARTROPODE
            has(s, "caracol","snail") -> VisualFamily.MOLUSCO
            has(s, "chimpanze","bonobo","mandril","gelada","macaco","monkey","bugio","marmoset",
                "sagui","archicebus","teilhardina","eosimias","biretia") -> VisualFamily.PRIMATA
            has(s, "rato","rat","camundongo","mouse","hamster","gerbil","leirao","dormouse",
                "phoberomys","hypolagus") -> VisualFamily.ROEDOR
            has(s, "urso","bear","panda") -> VisualFamily.URSIDEO
            has(s, "lontra","otter","carcaju","wolverine","ratel","badger","cangamba","skunk") -> VisualFamily.MUSTELIDEO
            has(s, "cavalo","horse","jumento","donkey","veado","deer","rena","reindeer","alce","moose",
                "antilope","antelope","gazela","gazelle","boi","ox","vaca","cow","bisao","bison",
                "bufalo","buffalo","cabra","goat","ovelha","sheep","ibex","gnu","wildebeest","oryx",
                "kudu","impala","camelo","camel","dromedario","llama","lhama","alpaca","guanaco",
                "vicunha","zebu","yak","saiga","nilgai","takin","markhor","tahr","mouflon","chamois",
                "pronghorn","waterbuck","lechwe","reedbuck","puku","gerenuk","dik-dik","duiker",
                "steenbok","serow","goral","bharal","eland","bongo","gaur","banteng","anoa","tamaraw") -> VisualFamily.UNGULADO
            has(s, "sauro","saurus","raptor","dinosaur","titan","donte","therium","pteryx","ornis",
                "caihong","yi qi","ambopteryx","anchiornis","aurornis","xiaotingia","serikornis") -> VisualFamily.DINOSSAURO
            else -> VisualFamily.MAMIFERO_TERRESTRE
        }
    }

    private val familyByName: Map<String, VisualFamily> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SpiritualFormService.todasFormas().associate { it.portuguese to computeFamilia(it) }
    }

    fun familia(animal: SpiritualFormService.Animal): VisualFamily =
        familyByName[animal.portuguese] ?: computeFamilia(animal)

    private val aquaticTerms = arrayOf("guelra","gill","peixe","fish-like","tubarao","shark-like",
        "nadadeira","fin-like","coral","oceano","ocean","submers","underwater")
    private val avianTerms = arrayOf("pena","feather","plumagem","plumage","bico","beak-like",
        "ossos ocos","hollow, lightweight bones","asas dobradas","folded wings")
    private val felineTerms = arrayOf("felin","feline","olho de gato","cat's eye","ronron","purr")
    private val ungulateTerms = arrayOf("casco","hoof","chifre","antler")
    private val reptileTerms = arrayOf("reptil","reptilian","cobra","snake-like","bifurcada","bifurcated",
        "fossetas loreais","heat-sensing pits")
    private val amphibianTerms = arrayOf("anfib","amphibian","sapo","toad-like")
    private val arthropodTerms = arrayOf("insect","insectoid","quitin","chitin","besouro","beetle",
        "aranha","spider","fiandeir","spinneret","mariposa","moth-like","antena","antennae")
    private val batTerms = arrayOf("ecolocal","echolocation","morcego","bat-like")

    private fun explicitFamilies(mark: LunarMarksService.Mark): Set<VisualFamily> {
        val s = mark.portuguese + " " + mark.english
        val out = linkedSetOf<VisualFamily>()
        if (has(s, *aquaticTerms)) out += setOf(VisualFamily.AQUATICO, VisualFamily.MAMIFERO_MARINHO)
        if (has(s, "membranas natatorias","webbed fingers","webbing between all toes"))
            out += setOf(VisualFamily.AQUATICO, VisualFamily.MAMIFERO_MARINHO, VisualFamily.ANFIBIO, VisualFamily.AVE)
        if (has(s, *avianTerms)) out += VisualFamily.AVE
        if (has(s, *felineTerms)) out += VisualFamily.FELINO
        if (has(s, *ungulateTerms)) out += VisualFamily.UNGULADO
        if (has(s, *reptileTerms)) out += VisualFamily.REPTIL
        if (has(s, "escama","scale")) out += setOf(VisualFamily.REPTIL, VisualFamily.AQUATICO)
        if (has(s, *amphibianTerms)) out += VisualFamily.ANFIBIO
        if (has(s, *arthropodTerms)) out += VisualFamily.ARTROPODE
        if (has(s, *batTerms)) out += VisualFamily.QUIROPTERO
        if (has(s, "porco-espinho","porcupine")) out += VisualFamily.MAMIFERO_TERRESTRE
        return out
    }

    private val explicitFamiliesByMark: Map<LunarMarksService.Mark, Set<VisualFamily>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LunarMarksService.todas().associateWith(::explicitFamilies)
    }

    private fun compatibilidade(family: VisualFamily, mark: LunarMarksService.Mark): Compatibility {
        val explicit = explicitFamiliesByMark[mark] ?: explicitFamilies(mark)
        if (explicit.isEmpty()) return Compatibility.COMPATIVEL
        if (family in explicit) return Compatibility.FORTE

        // Dinossauros variam muito; sinais reptilianos são compatíveis, e sinais aviários
        // ficam apenas compatíveis (não fortes) para evitar afirmar plumagem universal.
        if (family == VisualFamily.DINOSSAURO &&
            (VisualFamily.REPTIL in explicit || VisualFamily.AVE in explicit)) return Compatibility.COMPATIVEL

        return Compatibility.INCOMPATIVEL
    }

    fun compatibilidade(animal: SpiritualFormService.Animal, mark: LunarMarksService.Mark): Compatibility =
        compatibilidade(familia(animal), mark)

    private data class Pools(
        val fortes: List<LunarMarksService.Mark>,
        val compativeis: List<LunarMarksService.Mark>
    )

    /**
     * Os 300 Sinais são imutáveis. Classificá-los por família uma única vez elimina
     * normalização/regex e varredura integral a cada NPC Lunar gerado.
     */
    private val poolsByFamily: Map<VisualFamily, Pools> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val all = LunarMarksService.todas()
        VisualFamily.entries.associateWith { family ->
            val grouped = all.groupBy { compatibilidade(family, it) }
            Pools(
                fortes = grouped[Compatibility.FORTE].orEmpty(),
                compativeis = grouped[Compatibility.COMPATIVEL].orEmpty()
            )
        }
    }

    internal fun poolCoerente(animal: SpiritualFormService.Animal): List<LunarMarksService.Mark> {
        val pools = poolsByFamily.getValue(familia(animal))
        return pools.fortes.ifEmpty { pools.compativeis }
    }

    fun sortear(animal: SpiritualFormService.Animal, random: Random = Random.Default): LunarMarksService.Mark {
        val pool = poolCoerente(animal)
        check(pool.isNotEmpty()) { "Nenhum Sinal Lunar visualmente coerente para ${animal.portuguese}" }
        return pool.random(random)
    }
}
