package com.example.data

/**
 * Classificador conservador de sinergia social.
 *
 * Reconhece relações causais entre efeitos sociais (setup/payoff, suporte a aliado,
 * composição de ações e economia), sem confundir mera proximidade temática com combo.
 */
internal object EncounterSocialSynergy {
    enum class Tag {
        SOCIAL_INFLUENCE, PERSUADE, INSTILL, INSPIRE, THREATEN, BARGAIN, READ_INTENTIONS,
        RESOLVE_ATTACK, GUILE_ATTACK, RESOLVE_BUFF, RESOLVE_DEBUFF, GUILE_BUFF, GUILE_DEBUFF,
        APPEARANCE_BUFF, APPEARANCE_SCALING, SOCIAL_DICE_BUFF, SOCIAL_SUCCESS_BUFF,
        SOCIAL_COST_REDUCTION, SOCIAL_ACTION_DENIAL, SOCIAL_COUNTER, ALLY_SOCIAL_BUFF,
        ACTION_COMPOSITION, ON_SOCIAL_SUCCESS, ON_RESOLVE_SUCCESS, PERSISTENT_SOCIAL_STATE
    }

    private val ACTION_COMPOSITION_PAYOFF_TAGS = setOf(
        Tag.PERSUADE, Tag.INSTILL, Tag.INSPIRE, Tag.READ_INTENTIONS
    )

    private fun normalized(text:String)=text.lowercase().replace('–','-').replace('—','-')
    private fun any(t:String,vararg x:String)=x.any(t::contains)
    private fun rx(t:String,p:String)=Regex(p,RegexOption.IGNORE_CASE).containsMatchIn(t)

    fun tags(rawText:String):Set<Tag> {
        val t=normalized(rawText)
        return buildSet {
            if(any(t,"influência social","influencia social","social influence")) add(Tag.SOCIAL_INFLUENCE)
            if(any(t,"persuadir","persuasão","persuasao","persuade")) add(Tag.PERSUADE)
            if(any(t,"instilar","incutir","instill")) add(Tag.INSTILL)
            if(any(t,"inspirar","inspira","inspire")) add(Tag.INSPIRE)
            if(any(t,"ameaçar","ameaça","ameacar","ameaca","threaten")) add(Tag.THREATEN)
            if(any(t,"barganha","bargain")) add(Tag.BARGAIN)
            if(any(t,"ler intenções","ler intencoes","leitura de intenções","read intentions")) add(Tag.READ_INTENTIONS)

            if(rx(t,"(?:reduz|diminu)[^.!?;]{0,45}(?:perseverança|resolve)")) add(Tag.RESOLVE_DEBUFF)
            if(rx(t,"(?:reduz|diminu)[^.!?;]{0,45}(?:astúcia|astucia|guile)")) add(Tag.GUILE_DEBUFF)
            if(rx(t,"(?:aumenta|eleva|adiciona)[^.!?;]{0,45}(?:perseverança|resolve)")) add(Tag.RESOLVE_BUFF)
            if(rx(t,"(?:aumenta|eleva|adiciona)[^.!?;]{0,45}(?:astúcia|astucia|guile)")) add(Tag.GUILE_BUFF)
            if(rx(t,"(?:contra|superar|vence)[^.!?;]{0,35}(?:perseverança|resolve)")) add(Tag.RESOLVE_ATTACK)
            if(rx(t,"(?:contra|superar|vence)[^.!?;]{0,35}(?:astúcia|astucia|guile)")) add(Tag.GUILE_ATTACK)
            if(rx(t,"(?:aumenta|eleva)[^.!?;]{0,35}aparência")) add(Tag.APPEARANCE_BUFF)
            if(any(t,"aparência sobre","aparencia sobre") || rx(t,"aparência[^.!?;]{0,50}(?:sucessos automáticos|sucessos automaticos)")) add(Tag.APPEARANCE_SCALING)
            if(rx(t,"(?:dados? (?:de )?b[oô]nus|adiciona[^.!?;]{0,20}dados)[^.!?;]{0,60}(?:influência social|persuas|socializa|presença|performance)")) add(Tag.SOCIAL_DICE_BUFF)
            if(rx(t,"sucessos? autom[aá]ticos?[^.!?;]{0,60}(?:influência social|persuas|socializa|presença|performance)")) add(Tag.SOCIAL_SUCCESS_BUFF)
            if(rx(t,"(?:custo|custos)[^.!?;]{0,55}(?:influência social|encantos? de influência)[^.!?;]{0,35}(?:reduz|menor)") || any(t,"custo de todos os encantos de influência social é reduzido")) add(Tag.SOCIAL_COST_REDUCTION)
            if(any(t,"impedir que seu alvo faça um teste de influência social","ação social pretendida","acao social pretendida")) add(Tag.SOCIAL_ACTION_DENIAL)
            if(any(t,"contra-argumento","contra argumento","counterargument")) add(Tag.SOCIAL_COUNTER)
            if(rx(t,"(?:aliado|beneficiário|beneficiario|outro personagem)[^.!?;]{0,80}(?:dados? de b[oô]nus|influência social|perseverança|astúcia|guile|resolve)")) add(Tag.ALLY_SOCIAL_BUFF)
            if(any(t,"com um único teste","com um unico teste","compartilhando os resultados do teste")) add(Tag.ACTION_COMPOSITION)
            if(rx(t,"(?:cada vez|quando|se)[^.!?;]{0,55}(?:sucesso|bem-sucedid)[^.!?;]{0,35}influência social")) add(Tag.ON_SOCIAL_SUCCESS)
            if(rx(t,"(?:aplica|aplicar)[^.!?;]{0,35}(?:perseverança|resolve)")) add(Tag.ON_RESOLVE_SUCCESS)
            if(any(t,"uma cena","indefinida","indefinido","até o fim da cena","pelo resto da cena")) add(Tag.PERSISTENT_SOCIAL_STATE)
        }
    }

    private fun pairScore(a:Set<Tag>,b:Set<Tag>):Int {
        var score=0
        fun either(x:Tag,y:Tag,p:Int) { if((x in a&&y in b)||(y in a&&x in b)) score=maxOf(score,p) }
        fun directional(x:Tag,y:Tag,p:Int) { either(x,y,p) }
        directional(Tag.RESOLVE_DEBUFF,Tag.RESOLVE_ATTACK,24)
        directional(Tag.GUILE_DEBUFF,Tag.GUILE_ATTACK,24)
        directional(Tag.APPEARANCE_BUFF,Tag.APPEARANCE_SCALING,22)
        either(Tag.ACTION_COMPOSITION,Tag.SOCIAL_INFLUENCE,24)
        either(Tag.ALLY_SOCIAL_BUFF,Tag.SOCIAL_INFLUENCE,20)
        either(Tag.SOCIAL_DICE_BUFF,Tag.SOCIAL_INFLUENCE,16)
        either(Tag.SOCIAL_SUCCESS_BUFF,Tag.SOCIAL_INFLUENCE,17)
        either(Tag.SOCIAL_COST_REDUCTION,Tag.SOCIAL_INFLUENCE,14)
        either(Tag.SOCIAL_COUNTER,Tag.RESOLVE_BUFF,15)
        either(Tag.ON_SOCIAL_SUCCESS,Tag.SOCIAL_INFLUENCE,18)
        either(Tag.ON_RESOLVE_SUCCESS,Tag.RESOLVE_BUFF,16)
        either(Tag.READ_INTENTIONS,Tag.GUILE_DEBUFF,13)
        return score
    }

    fun pairAffinity(firstText:String,secondText:String)=pairScore(tags(firstText),tags(secondText))

    fun score(candidateText:String,selectedTexts:List<String>):Int =
        scoreTags(tags(candidateText), selectedTexts.map(::tags))

    /** Mesma pontuação de [score], aceitando classificação já calculada. */
    fun scoreTags(candidate:Set<Tag>,selected:List<Set<Tag>>):Int {
        if(candidate.isEmpty()) return 0
        var score=selected.sumOf { pairScore(candidate,it) }
        if(Tag.ACTION_COMPOSITION in candidate && candidate.any(ACTION_COMPOSITION_PAYOFF_TAGS::contains)) score+=6
        if(Tag.SOCIAL_COST_REDUCTION in candidate && Tag.SOCIAL_DICE_BUFF in candidate) score+=5
        return score
    }
}
