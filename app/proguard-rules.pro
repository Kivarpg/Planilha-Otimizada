# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserva nome de arquivo e número de linha nos stack traces mesmo em
# release minificado — sem isso, o capturador de erros global
# (MainActivity.kt, "Ver Log de Erros") produziria um stack trace com
# classes/métodos ofuscados, difícil de usar pra diagnosticar qualquer
# problema que só aparecesse numa build de release.
-keepattributes SourceFile,LineNumberTable

# Enums desserializados via valueOf() a partir de texto salvo (fichas em
# JSON, preferências) — risco real e concreto: Casta, TipoExaltadoEncontro,
# ArquetipoEncontro e RatingStyle já usam esse padrão hoje
# (CharacterSheet.kt, NpcEncontro.kt, ShareCodeCodec.kt, SheetRepository.kt),
# e novos enums no mesmo estilo tendem a aparecer conforme o app cresce
# (ex.: Aspecto, adicionado recentemente). Diferente da maioria dos bugs
# de R8, um enum quebrado aqui não aparece testando o app normalmente —
# só se manifesta quando alguém tenta abrir uma ficha ou preferência
# salva anteriormente, numa build de release especificamente. A regra é
# ampla (todo enum do app, não só os 4 atuais) de propósito: não depende
# de lembrar de atualizar esta lista toda vez que um enum novo for
# adicionado ao mesmo padrão.
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final java.lang.String name();
}
-keepnames enum com.example.**

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
