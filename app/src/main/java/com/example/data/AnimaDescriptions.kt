package com.example.data

import com.example.model.Aspecto
import com.example.model.Casta

/**
 * Textos dos poderes de anima de cada Casta Solar e Aspecto de Sangue de
 * Dragão — usados na Aba 2 (Casta/Aspecto), num quadro expansível/retrátil
 * abaixo do símbolo selecionado. Conteúdo fornecido integralmente pelo
 * usuário; não resumir nem reformular.
 */
object AnimaDescriptions {

    fun paraCasta(casta: Casta): String = when (casta) {
        Casta.Dawn -> """
            • No nível de anima "fogueira" (ou superior), o Amanhecer adiciona metade de seu valor de Essência (arredondado para cima) à sua Iniciativa base ao retornar ao valor base após um ataque decisivo bem-sucedido.

            • Por 10 motes, todos os Encantos de combate e movimento do Amanhecer que possuam condições de reinicialização pendentes são reinicializados automaticamente. Esse efeito só pode ser usado uma vez por dia, tornando-se disponível novamente ao nascer do sol.

            • A Casta do Amanhecer inspira terror em seus inimigos. Ela adiciona dados equivalentes a metade de seu valor de Essência (arredondado para cima) a todas as ações de influência social intimidatória. Ela também pode intimidar alvos que normalmente não sentem medo, como autômatos, golens e certos mortos-vivos.
        """.trimIndent()

        Casta.Zenith -> """
            • Por um mote, a anima do Zênite salta de sua mão para um cadáver que ela esteja tocando, incendiando-o com chamas Solares. Esse poder incinera o cadáver e garante que a alma do falecido não retorne como um fantasma faminto. Além disso, o Zênite pode perceber as Intimidades mais fortes do falecido e optar por aceitá-las. Quando estiver pronto, ele pode pagar um mote e tocar o alvo de uma dessas Intimidades para transmitir sentimentos de paz, amor e segurança, libertando-se delas no processo. Ele também pode transferi-las para um objeto, permitindo que este transmita conforto ou um senso de dever ao sujeito. Por outro lado, ele pode tocar alguém que tenha causado sofrimento ao falecido, pagando um mote e rolando ([Atributo Social] + Presença) — com três sucessos automáticos (que não contam como uso de Encanto) — contra a Perseverança do alvo, para transferir a dor sofrida pelo falecido de volta para quem a causou. O Narrador tem liberdade para determinar a duração e a intensidade desse atrito.

            • Por 10 motes e um ponto de Força de Vontade, o Zênite canaliza sua Essência em uma aura de retribuição divina que se manifesta ao redor de um ataque. Esse poder aprimora um ataque decisivo bem-sucedido contra uma criatura das trevas, impedindo que a Iniciativa do Solar retorne ao seu valor base após o ataque. Quando a anima do Zênite estiver no nível "fogueira", o custo desse poder é reduzido em cinco motes e o custo de Força de Vontade é ignorado. Esse poder só pode ser usado uma vez por dia, sendo reinicializado quando o sol atinge o zênite.

            • Por sete motes, o Zênite pode ordenar que um espírito desmaterializado se manifeste, utilizando uma ação de persuasão (Carisma + Presença) com sucessos automáticos iguais à sua (Essência). Todos os espíritos reconhecem inerentemente a autoridade do Sol Invicto nos reis-sacerdotes e sentem-se compelidos como se estivessem sob o efeito de uma Intimidade Definidora, embora ainda possam possuir Laços ou Princípios Definidores que contrabalancem essa vantagem. Se a ação for bem-sucedida, a própria ordem do Zênite puxa o espírito para o mundo material, dispensando a necessidade de pagar o custo para se materializar.
        """.trimIndent()

        Casta.Twilight -> """
            • Por cinco motes, o Crepúsculo projeta uma aura de força pura, ganhando reflexivamente cinco pontos de Dureza por um turno. No nível de fogueira/icônico, esse poder é ativado automaticamente, sem custo. Esse efeito não é cumulativo com outras magias que aumentem a Dureza, mas pode ser utilizado durante uma Quebra de Iniciativa.

            • Por 10 motes e um ponto de Força de Vontade, a anima do Crepúsculo brilha em azul e branco e consome seu corpo ao longo da rodada. Em seu próximo turno, ele desaparece em meio à sua anima e deixa de existir. Se o Exaltado se mover ou sofrer um atordoamento de iniciativa antes da conclusão da ação, ele falha e o efeito é desperdiçado. Após utilizar esse poder, o Crepúsculo não reaparecerá até o pôr do sol seguinte. Quando isso ocorrer, será em um local de poder situado em um raio de 20 kilometros do ponto onde desapareceu, escolhido pelo Narrador. Ele pode surgir em um templo, um domínio, um grande cruzamento, na porta do santuário de um deus, no centro exato de uma cidade, etc.

            • Por 10 motes, o Crepúsculo pode tocar um elemental com Essência 1-3 ou um demônio do primeiro círculo, unindo a Essência da criatura à sua própria anima. Role Inteligência + Ocultismo contra a Perserverança da criatura. Se obtiver sucesso, cria-se um pacto que transforma o espírito em seu familiar, permitindo que ela o tenha como alvo de Encantos de Sobrevivência aplicáveis. Além disso, o Crepúsculo pode invocar o espírito instantaneamente e de forma reflexiva por três motes, trazendo-o através da Essência do mundo para aparecer ao seu lado. Ela pode banir a criatura novamente, de forma reflexiva e sem custo, devolvendo-a às marés de Essência que permeiam a Criação até que seja necessária novamente. O Crepúsculo pode manter até (Essência) espíritos familiares vinculados dessa maneira simultaneamente.
        """.trimIndent()

        Casta.Night -> """
            • Por dois motes, a casta Noite pode atenuar sua anima, tratando todos os gastos de Essência Periférica como se fossem Pessoais por um instante.

            • Por três motes, a casta Noite pode ignorar penalidades de até (o maior valor entre 3 e a Essência) em uma tentativa de Furtividade por um instante. Esses motes são sempre considerados Pessoais, independentemente da reserva de onde são gastos.

            • Quando a anima da casta da Noite atinge o nível de fogueira/icônico, ela envolve e permeia sua forma, ocultando sua silhueta e mascarando seu rosto em chamas. Ela se torna apenas uma silhueta temível envolta em todos os tons do anoitecer. Torna-se impossível discernir sua identidade. Esse é considerado um efeito perfeito e não pode ser penetrado pelo Olho do Sol Inconquistado ou por outras magias de onisciência.
        """.trimIndent()

        Casta.Eclipse -> """
            • Por 10 motes e um ponto de Força de Vontade, o Eclipse santifica um juramento que tenha testemunhado. O Exaltado toca aqueles que prestam o juramento (ou as palavras deles, enquanto estas atravessam o ar), e a marca de sua casta brilha imperceptivelmente. Se desejar, sua anima se intensifica brevemente, rodopiando junto com as palavras e runas pelas quais o Céu lhe concedeu o direito de arbitrar tais questões.

            Aqueles que prestam juramento e violam pactos santificados pelo Eclipse (incluindo o próprio Exaltado) ficam sujeitos a uma maldição terrível. Os detalhes da maldição são criados pelo Narrador, idealmente refletindo o juramento que foi quebrado. Uma mulher que quebra um juramento de hospitalidade pode se ver incapaz de descansar sob qualquer teto por um ano e um dia, enquanto um homem que assassinou aquele a quem jurou proteger pode ver suas defesas falharem completamente na próxima vez que tiver de lutar por sua vida.

            • O Eclipse desfruta de imunidade diplomática ao negociar com os inimigos da Criação. Desde que o Eclipse se aproxime deles para tratar de assuntos legítimos, sombras, espíritos, príncipes demônios do Inferno e o Povo das Fadas não podem atacar o Eclipse ou seus companheiros sem justa causa, e tais criaturas devem respeitar as regras locais de hospitalidade. Tais seres ainda podem tentar provocar membros da embaixada do Solar para que quebrem a paz, anulando assim a proteção deste efeito.

            • O Eclipse representa todos os movimentos sutis da Essência sob o Céu, os poderes estranhos e autorreferenciais que se agitam quando a noite se funde com o dia. Eclipses podem aprender os Encantos de espíritos, do Povo das Fadas e de seres sobrenaturais semelhantes que possuam a palavra-chave "Eclipse", ao custo de oito pontos de experiência cada.
        """.trimIndent()
    }

    /** Aspectos de Sangue de Dragão — chave é o nome do aspecto ("Ar", "Terra", "Fogo", "Água", "Madeira"). */
    fun paraAspecto(aspecto: Aspecto): String = when (aspecto) {
        Aspecto.Ar -> """
            • Por cinco motes, o Aspecto do Ar pode usar sua ação de movimento reflexiva para saltar uma faixa de alcance inteira, vertical ou horizontalmente, sem precisar fazer uma rolagem, e não sofre dano de queda pelo restante da rodada. No nível de fogueira, este poder é gratuito.

            • Por três motes, o Aspecto do Ar pode desviar projéteis com uma rajada de vento, impondo uma penalidade ambiental de −1 a todos os ataques à distância contra ele até o seu próximo turno.

            • Uma vez por dia, quando o Aspecto do Ar realiza uma ação social ou mental não estendida para defender um Princípio, ele adiciona dados de bônus (não provenientes de Encantos) iguais ao valor da Intimidade à rolagem.
        """.trimIndent()

        Aspecto.Terra -> """
            • Por cinco motes, o Aspecto da Terra adiciona (o maior valor entre Essência e 3) à sua Absorção natural e ganha +1 de Dureza até o seu próximo turno; além disso, recebe +1 de Defesa contra ataques de impacto e manobras de agarre como um bônus que não provém de Encantos. No nível de fogueira, este poder é gratuito.

            • Por três motes, o Aspecto da Terra ignora um ponto de penalidade decorrente de ferimentos, veneno ou debilitação por um único tick.

            • O Sangue de Dragão pode gastar dois pontos de Iniciativa ao rolar o fluxo de anima para derrubar todos os inimigos feridos ou empurrá-los para uma faixa de alcance de distância de si.
        """.trimIndent()

        Aspecto.Fogo -> """
            • Por cinco motes, o Aspecto do Fogo torna-se completamente imune a perigos ambientais mundanos baseados em fogo ou calor durante uma cena. Ele adiciona (Essência) à sua Absorção natural e ganha Dureza 2 contra ataques baseados em fogo, como pistolas de fogo. Ele obtém esses benefícios gratuitamente enquanto estiver no nível de fogueira.

            • Uma vez por dia, quando o Aspecto do Fogo realiza uma ação não estendida para defender uma Intimidade baseada em emoção, ele adiciona dados de bônus (não provenientes de Encantos) iguais ao valor da Intimidade à rolagem.

            • O fluxo de anima de um Aspecto do Fogo causa (o maior valor entre Essência e 3) de dano a personagens sem Dureza, em vez de causar dano equivalente a um dado.
        """.trimIndent()

        Aspecto.Agua -> """
            • Por cinco motes, o Aspecto da Água pode usar sua ação de movimento para atravessar a superfície da água como se fosse terra firme, e ignora a penalidade de −3 ao realizar investidas ou desengajamentos em terreno difícil. Ele obtém esses benefícios gratuitamente enquanto estiver no nível de fogueira.

            • Por três motes, o Aspecto da Água adiciona um sucesso (que não conta como uso de um Encanto) a uma jogada para se desengajar, recuar ou resistir a uma tentativa de agarrar.

            • Aspectos da Água podem respirar embaixo d'água como se fosse ar e não sofrem penalidades por estarem submersos.
        """.trimIndent()

        Aspecto.Madeira -> """
            • Por cinco motes, o Aspecto da Madeira torna-se imune a venenos comuns de origem vegetal e faz com que resultados 9 contem como sucessos extras em jogadas para resistir a outros venenos e doenças durante a cena. Ele obtém esses benefícios gratuitamente enquanto estiver no nível de anima "Fogueira".

            • Por três motes, o Aspecto da Madeira torna-se flexível como uma muda ao vento, ignorando um ponto de penalidade na Evasão ou pontos de penalidade iguais à sua Essência em uma ação de movimento por um instante.

            • O fluxo de anima de um Aspecto da Madeira é impregnado de poder tóxico. Um personagem que sofra dano dele é exposto a um veneno com Dano de 2i/turno, Duração de (Essência + Vigor) turnos e uma penalidade de −1.
        """.trimIndent()

    }
}
