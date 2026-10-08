# Relatório de Auditoria de Segurança e Consumo — Exalted

**Versão auditada:** Exalted 0.79 / ZIP `Exalted.079.zip`  
**Data da auditoria:** 23/09/2026  
**Escopo:** código-fonte do projeto Android, Manifest, configuração de build, armazenamento local, execução em segundo plano, permissões, rede, importação/exportação, backups e pontos que podem afetar bateria, RAM ou armazenamento.  
**Objetivo:** deixar um registro técnico permanente para programadores futuros e servir como checklist para novas versões.

> **Importante:** esta é uma auditoria do projeto-fonte. O APK final não foi submetido a análise binária/dinâmica nesta etapa. Portanto, o relatório não deve ser interpretado como certificação de segurança do APK assinado.

---

## 1. Resumo executivo

### Resultado geral

**Atualização 0.79:** o principal ponto de atenção de RAM identificado na auditoria anterior — o mapa HD — foi tratado com renderização por tiles. O arquivo original de 5780×3740 permanece intacto no APK, mas o aplicativo deixa de decodificá-lo inteiro como um único bitmap. Em zoom baixo usa o mapa leve; a partir de zoom de alta resolução, carrega apenas regiões visíveis em tiles de 512×512, com cache LRU limitado.

**Nenhum mecanismo foi identificado no código auditado que indique capacidade de causar dano físico à bateria, armazenamento UFS/eMMC, processador, tela ou outros componentes do dispositivo.**

A superfície de segurança observada é relativamente pequena:

- não há permissão `INTERNET` no Manifest;
- `android:usesCleartextTraffic="false"` está configurado;
- não foram encontrados `Service`, `BroadcastReceiver`, `WorkManager` ou `AlarmManager` próprios;
- não há WebView;
- não foram encontradas bibliotecas de rede como OkHttp/Retrofit nas dependências principais;
- não há GPS/localização, câmera, microfone, SMS ou contatos;
- o armazenamento de NPC usa diretório específico do aplicativo;
- a única permissão declarada é `WRITE_EXTERNAL_STORAGE`, limitada a `maxSdkVersion="28"`;
- o backup do sistema exclui as `SharedPreferences` do aplicativo;
- o codec de compartilhamento possui limite de aproximadamente 8 MiB para dados descomprimidos;
- o salvamento automático usa debounce, evitando uma gravação para cada pequena alteração;
- o backup periódico é rotativo e limitado, sem crescimento infinito conhecido.

### Classificação resumida

| Área | Classificação | Observação |
|---|---|---|
| Dano físico ao aparelho | **Baixo / não identificado** | Não existe API ou rotina capaz de causar esse tipo de dano diretamente. |
| Bateria | **Baixo** | Não há processamento contínuo conhecido; existe backup periódico em coroutine. |
| Armazenamento | **Baixo** | Há gravações locais, mas não foi encontrado crescimento ou escrita infinita. |
| RAM | **Baixo–Médio** | O mapa HD pode causar pico temporário de memória. |
| Rede | **Muito baixo** | Sem `INTERNET` no Manifest e sem cliente HTTP identificado. |
| Execução em segundo plano | **Baixo** | Não há componentes persistentes dedicados. |
| Permissões | **Baixo** | Uma permissão antiga de armazenamento é limitada à API 28. |
| Arquivos do usuário | **Baixo** | Uso predominantemente em sandbox/diretório específico do app. |
| Entrada malformada | **Baixo, com ressalvas** | Codecs possuem validações e limite de descompressão. |
| Privacidade | **Baixo, no código auditado** | Não foi identificado envio de dados para servidores. |
| APK final | **Não concluído** | Falta análise do artefato compilado e teste dinâmico. |

---

## 2. Metodologia

A auditoria foi feita por inspeção estática do projeto `Exalted.078.zip`, procurando principalmente:

1. permissões declaradas no `AndroidManifest.xml`;
2. componentes Android exportados;
3. APIs de rede;
4. execução em segundo plano;
5. loops e timers;
6. operações de leitura/escrita de arquivos;
7. rotinas de backup;
8. armazenamento externo;
9. geração e leitura de arquivos;
10. codecs de importação/exportação;
11. bibliotecas e dependências;
12. uso potencial de RAM;
13. mecanismos de logging;
14. configurações de backup do Android;
15. pontos de entrada externos.

Também foram considerados os problemas encontrados na auditoria funcional anterior do Exalted 0.77 e as correções aplicadas no 0.78.

### Limitação de build

A compilação Gradle completa não pôde ser validada neste ambiente porque o Gradle 9.7.1 tentou acessar `services.gradle.org` e o ambiente não possuía acesso de rede para concluir o download. Portanto:

- a integridade estrutural dos arquivos foi verificada;
- o ZIP foi validado;
- arquivos Kotlin importantes foram submetidos a verificações estruturais de chaves/parênteses;
- **o build Gradle completo não foi executado com sucesso neste ambiente**.

Programadores futuros devem considerar a compilação e testes instrumentados obrigatórios antes de considerar uma versão como liberada.

---

## 3. Android Manifest e permissões

O Manifest atual contém apenas a seguinte permissão:

```xml
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### Interpretação

Essa permissão está limitada ao Android 8/9 (API 26–28). Ela é usada pelo fluxo legado de salvamento de imagens na aba de tradução.

A partir de Android 10 (API 29), o aplicativo utiliza o modelo de armazenamento com escopo/MediaStore, não exigindo a mesma permissão para esse caso.

### Permissões que NÃO devem aparecer sem justificativa futura

O projeto não deve passar a solicitar, sem necessidade funcional claramente documentada:

- `INTERNET`;
- localização precisa/aproximada;
- câmera;
- microfone;
- contatos;
- SMS;
- telefone;
- Bluetooth;
- acesso amplo a arquivos (`MANAGE_EXTERNAL_STORAGE`);
- instalação de pacotes;
- acesso a notificações especiais;
- acessibilidade;
- VPN;
- serviços de acessibilidade ou captura de tela.

Qualquer nova permissão deve ser adicionada somente quando houver uma funcionalidade que realmente necessite dela e deve ser registrada neste documento.

---

## 4. Componentes Android

O Manifest declara como componente principal:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
```

Ela é `exported=true` porque possui o filtro:

```xml
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.LAUNCHER" />
```

Isso é esperado para a Activity inicial do aplicativo.

### Não encontrados como componentes próprios

- `Service`;
- `ForegroundService`;
- `BroadcastReceiver`;
- `ContentProvider` personalizado;
- `Activity-alias`;
- `WorkManager`;
- `AlarmManager`;
- `JobScheduler`.

### Regra para futuras versões

Qualquer novo componente `exported=true` deve ter uma justificativa documentada aqui. Preferir `exported=false` quando o componente não precisar ser iniciado por outro aplicativo ou pelo sistema.

---

## 5. Rede e Internet

### Resultado

**Nenhuma comunicação de rede foi identificada no código auditado.**

O Manifest não declara:

```xml
android.permission.INTERNET
```

As dependências principais não incluem cliente HTTP conhecido como:

- OkHttp;
- Retrofit;
- Volley;
- Ktor client.

Também não foram identificados usos relevantes de:

- `HttpURLConnection`;
- sockets;
- WebView;
- Firebase;
- chamadas REST/HTTP.

O Manifest também possui:

```xml
android:usesCleartextTraffic="false"
```

Isso mantém desabilitado o tráfego HTTP sem criptografia caso alguma capacidade de rede venha a ser adicionada no futuro.

### Regra para futuras versões

Se a Internet for adicionada:

1. justificar a necessidade;
2. adicionar `INTERNET` somente quando necessário;
3. documentar todos os hosts/serviços externos;
4. usar HTTPS;
5. não transmitir ficha ou dados pessoais sem consentimento explícito;
6. não adicionar SDK de analytics/telemetria silenciosamente;
7. revisar as dependências antes do release.

---

## 6. Execução em segundo plano e bateria

### Backup periódico

O projeto possui um backup periódico descrito na interface como aproximadamente a cada **5 minutos**.

A implementação usa coroutine com `delay`, e o código foi construído para não manter esse trabalho executando desnecessariamente quando o aplicativo está em segundo plano.

O intervalo de cinco minutos não significa que o aplicativo fique consumindo CPU durante cinco minutos. A coroutine permanece suspensa durante o `delay` e só executa quando chega o momento de verificar/salvar.

### Auto-save

O salvamento principal utiliza debounce de aproximadamente **400 ms**.

Modelo:

```text
alteração de estado
        ↓
   debounce 400 ms
        ↓
   salvamento
```

Isso reduz gravações repetitivas durante sequências rápidas de alterações.

### Avaliação

**Não foi identificado loop de CPU contínuo ou atividade persistente de alta frequência capaz de indicar consumo anormal de bateria.**

### Melhoria recomendada

No futuro, o backup periódico pode ser ligado/desligado explicitamente ao ciclo de vida em primeiro plano em vez de depender apenas da lógica existente dentro da coroutine. Isso é uma melhoria arquitetural, não uma correção urgente de segurança.

---

## 7. Armazenamento e desgaste de memória flash

O aplicativo grava dados locais de ficha, backups, históricos e arquivos de NPC.

Não foi identificado:

- arquivo que cresce indefinidamente;
- loop de gravação contínua;
- logging permanente de alta frequência;
- download de arquivos repetitivo;
- geração automática infinita de arquivos.

### Backups

Os backups periódicos são rotativos e limitados, evitando crescimento ilimitado.

### Histórico de combate

O histórico possui limite conhecido no código, reduzindo a possibilidade de crescimento indefinido.

### NPCs

Os arquivos de NPC são armazenados em:

```kotlin
context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
```

Esse é um diretório específico do aplicativo.

### Conclusão

O volume de escrita observado é compatível com um aplicativo de ficha que salva estado local. Não há evidência de padrão de escrita que represente risco relevante de desgaste do armazenamento do aparelho.

---

## 8. RAM e mapa HD

### Estado anterior (0.78)

Este era o principal ponto de atenção de desempenho encontrado. O mapa possui **5780 × 3740 pixels** e aproximadamente **3,7 MB** no JPEG. A implementação anterior pré-carregava uma imagem grande em RAM e aplicava limite de dimensão de aproximadamente 4096 pixels. Mesmo com `RGB_565`, isso podia manter dezenas de MB ocupados por uma única imagem.

### Correção aplicada na 0.79 — renderização por tiles

O arquivo original **não foi reduzido nem substituído**. `mapa_creation_hd.jpg` continua com 5780×3740. A alteração é somente na forma de decodificação e renderização.

`MapaAltaResolucaoCache` agora:

1. mantém o arquivo HD original no APK;
2. usa `BitmapRegionDecoder` para decodificar regiões;
3. divide logicamente a imagem em tiles de **512×512 pixels**;
4. carrega somente tiles necessários para a região visível;
5. usa `RGB_565` nos tiles, pois o mapa JPEG não possui canal alfa;
6. mantém cache LRU limitado a **32 tiles**;
7. remove tiles antigos quando o limite é atingido;
8. mantém `mapa_creation` como base/fallback enquanto os tiles HD são carregados;
9. ativa HD a partir de zoom aproximadamente **1,35×**;
10. executa a decodificação em `Dispatchers.IO`, evitando bloquear a UI.

### Impacto esperado na RAM

O mapa inteiro possui aproximadamente 21,6 milhões de pixels. Um bitmap `ARGB_8888` integral poderia consumir aproximadamente **86 MB** somente em dados de pixels.

Um tile de 512×512 em `RGB_565` ocupa aproximadamente **512 KiB** de dados de pixels. O limite de 32 tiles mantém o cache de pixels na ordem de **16 MiB**, antes de overheads de objetos e do renderer/GPU. Na prática, nem todos os 32 tiles estarão sempre residentes.

O ganho fundamental é que a RAM deixa de depender diretamente do tamanho total do mapa e passa a depender da região que o usuário está visualizando.

### Comportamento

```text
zoom 1,0× até ~1,35×
    → mapa leve

zoom > ~1,35×
    → mapa leve permanece como fallback
    → tiles HD da região visível são carregados
    → tiles antigos saem pelo LRU

zoom alto
    → apenas uma pequena região do mapa original precisa permanecer na RAM
```

### Proteções

A decodificação de cada tile trata `OutOfMemoryError` e exceções. Se um tile falhar, a base leve continua disponível. O cache também é limitado para impedir crescimento indefinido.

### Limitação e manutenção futura

`BitmapRegionDecoder` é uma API antiga/deprecada em níveis recentes do Android, embora continue disponível nos níveis suportados pelo projeto. Em uma futura migração gráfica, avaliar `ImageDecoder` ou outra solução tiled moderna, preservando estes requisitos: **arquivo original em resolução máxima, carregamento regional, cache limitado e fallback visual**.

Testar obrigatoriamente em aparelhos com 2–3 GB de RAM e, se possível, medir `dumpsys meminfo`/Android Studio Memory Profiler durante zoom e pan prolongados.

## 9. Arquivos de NPC

O `NpcSaveLoadService` utiliza o diretório específico do aplicativo:

```kotlin
context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
```

com fallback para `context.filesDir`.

Os arquivos possuem extensão `.save` e são filtrados pela aplicação.

### Pontos positivos

- não há necessidade de acesso amplo ao armazenamento;
- o diretório é associado ao aplicativo;
- a leitura é feita somente dos arquivos selecionados/listados pela própria aplicação;
- o carregamento gera novo UUID para o NPC, evitando colisão de ID.

### Ponto de atenção futuro

Arquivos salvos externamente devem ser tratados como **entrada não confiável**. Nunca assumir que um `.save` foi produzido pelo próprio aplicativo.

Toda alteração futura no codec deve manter:

- validação de formato;
- limites de tamanho;
- tratamento de JSON inválido;
- ausência de loops recursivos não limitados;
- mensagens de erro controladas.

---

## 10. Importação e compartilhamento

O sistema de compartilhamento/compressão possui proteção contra expansão descontrolada do conteúdo comprimido.

Existe um limite de aproximadamente:

**8 MiB de dados descomprimidos.**

Isso é importante porque uma string comprimida pequena poderia, em teoria, representar uma quantidade muito maior de dados depois da descompressão.

### Regras para futuras alterações

Nunca remover os limites de:

- tamanho do código de entrada;
- tamanho descomprimido;
- profundidade/complexidade quando aplicável;
- número de elementos importados;
- tamanho de strings individuais.

Qualquer codec novo deve ser testado contra:

1. entrada vazia;
2. entrada truncada;
3. checksum inválido;
4. versão desconhecida;
5. JSON inválido;
6. conteúdo excessivamente grande;
7. conteúdo comprimido malicioso;
8. campos desconhecidos;
9. valores numéricos extremos.

---

## 11. Backup do Android e privacidade

O projeto possui regras explícitas de extração/backup.

`SharedPreferences` é excluído tanto de:

- cloud backup;
- device transfer.

Isso evita que o mecanismo de backup do sistema copie automaticamente esse estado privado.

A exportação/compartilhamento explícito continua sendo responsabilidade do usuário através das funções do aplicativo.

### Regra futura

Se o aplicativo passar a guardar novas informações sensíveis fora de `SharedPreferences`, revisar imediatamente:

- `data_extraction_rules.xml`;
- `backup_rules.xml`;
- armazenamento de arquivos;
- exportação/compartilhamento.

---

## 12. Logs e tratamento de erros

O projeto utiliza `BuildConfig.DEBUG` em pontos de logging, evitando que determinados logs de diagnóstico sejam tratados como comportamento de produção.

Existe também registro de último erro para determinadas falhas não tratadas.

### Regra futura

Nunca registrar em produção:

- senha;
- token;
- conteúdo completo de ficha se contiver informação privada;
- caminho absoluto desnecessário;
- dados pessoais;
- conteúdo de arquivos importados;
- chaves ou segredos.

Logs devem ser limitados e removíveis/rotativos.

---

## 13. Dependências

As dependências principais observadas incluem:

- AndroidX Compose;
- AndroidX Activity;
- AndroidX Lifecycle/ViewModel;
- Kotlin Coroutines;
- ZXing;
- bibliotecas de teste como JUnit, Robolectric e Kotlin Test.

Não foi identificada dependência principal de rede ou analytics.

### Regra para futuras versões

Antes de adicionar uma dependência:

1. verificar manutenção do projeto;
2. verificar licença;
3. verificar permissões que a dependência pode introduzir;
4. verificar transitive dependencies;
5. verificar vulnerabilidades conhecidas;
6. evitar bibliotecas desnecessárias;
7. documentar a razão da inclusão.

A revisão de dependências deve ser repetida a cada atualização importante do projeto.

---

## 14. Configuração de release

O projeto possui:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
}
```

Isso é positivo para uma build de produção, pois permite reduzir código e recursos não utilizados.

A build `debug` permanece sem minificação, o que é esperado para desenvolvimento.

O projeto utiliza:

- `compileSdk = 36`;
- `targetSdk = 36`;
- `minSdk = 26`;
- Java 17.

### Regra futura

Antes de publicar uma release:

- gerar APK/AAB de release;
- verificar assinatura;
- instalar o artefato exato gerado;
- revisar Manifest final;
- verificar permissões finais;
- executar testes;
- testar import/export;
- testar restauração de backup;
- testar em aparelho de baixa RAM;
- testar rotação/recriação da Activity;
- verificar consumo de bateria em uso prolongado.

---

## 15. Riscos encontrados e estado

### Risco A — gravação excessiva por alteração

**Estado:** mitigado.

O auto-save usa debounce de aproximadamente 400 ms.

---

### Risco B — backup periódico em segundo plano

**Estado:** mitigado no desenho atual.

A lógica verifica o estado do aplicativo antes de efetuar o backup.

**Melhoria futura:** amarrar explicitamente o ciclo do backup ao lifecycle de primeiro plano.

---

### Risco C — crescimento infinito de backups

**Estado:** mitigado.

Os backups são rotativos e limitados.

---

### Risco D — expansão maliciosa de conteúdo comprimido

**Estado:** mitigado.

Há limite de aproximadamente 8 MiB após descompressão.

---

### Risco E — pico de RAM do mapa

**Estado:** mitigado parcialmente.

Há downsampling e limite de dimensão, mas o mapa continua sendo o principal ponto de atenção de memória.

---

### Risco F — acesso amplo ao armazenamento

**Estado:** não identificado.

A única permissão antiga de armazenamento é limitada à API 28.

---

### Risco G — comunicação de rede silenciosa

**Estado:** não identificada.

Não há `INTERNET` no Manifest nem cliente HTTP principal identificado.

---

## 16. O que NÃO foi certificado por esta auditoria

Os seguintes itens ainda precisam ser realizados para uma auditoria de release completa:

### 16.1 Análise do APK final

Verificar o APK compilado com ferramentas como:

- `apkanalyzer`;
- `aapt2 dump permissions`;
- `apkanalyzer manifest permissions`;
- JADX, quando necessário;
- análise de DEX;
- inspeção das bibliotecas nativas.

### 16.2 Teste dinâmico

Instalar o APK em aparelho/emulador e verificar:

- CPU;
- RAM;
- wake locks;
- rede;
- gravações de armazenamento;
- processos;
- serviços ativos;
- consumo de bateria.

### 16.3 SAST/dependências

Executar uma ferramenta de análise de vulnerabilidades e dependências na build final.

### 16.4 Build reproduzível

Gerar novamente o APK a partir do código e comparar o artefato quando houver infraestrutura apropriada.

### 16.5 Testes de fuzzing

Aplicar entradas inválidas e grandes aos codecs:

- ShareCode;
- NPC save;
- JSON;
- importadores.

---

## 17. Checklist obrigatório para futuros programadores

Antes de cada release:

- [ ] Não adicionar `INTERNET` sem justificativa.
- [ ] Não adicionar WebView sem revisão de segurança.
- [ ] Não adicionar `Service`/`Worker`/`AlarmManager` sem documentar impacto na bateria.
- [ ] Não adicionar `BroadcastReceiver` exportado sem necessidade.
- [ ] Não solicitar permissões novas sem documentá-las.
- [ ] Manter `usesCleartextTraffic="false"`.
- [ ] Manter limites de tamanho dos importadores.
- [ ] Manter limite de descompressão do ShareCode.
- [ ] Manter limite dos backups.
- [ ] Não criar logs ilimitados.
- [ ] Não gravar continuamente em arquivos.
- [ ] Testar o mapa em aparelho com pouca RAM.
- [ ] Testar restauração de backup.
- [ ] Testar arquivos `.save` corrompidos.
- [ ] Testar códigos de compartilhamento corrompidos.
- [ ] Rodar testes unitários.
- [ ] Rodar testes instrumentados quando disponíveis.
- [ ] Compilar uma build `release` real.
- [ ] Inspecionar o Manifest final do APK.
- [ ] Verificar permissões do APK final.
- [ ] Verificar dependências novas.
- [ ] Instalar e testar o APK exato que será distribuído.
- [ ] Fazer teste de uso prolongado antes de uma release grande.

---

## 18. Regras de segurança que não devem ser removidas

Estas proteções são consideradas parte da arquitetura de segurança do aplicativo:

1. `android:usesCleartextTraffic="false"`.
2. Ausência de `INTERNET` enquanto o aplicativo não precisar de rede.
3. Limitação de `WRITE_EXTERNAL_STORAGE` a API 28.
4. Armazenamento específico do aplicativo para arquivos de NPC.
5. Limites de tamanho nos codecs.
6. Limite de aproximadamente 8 MiB para descompressão do ShareCode.
7. Backups rotativos com quantidade limitada.
8. Debounce do auto-save.
9. Validação de dados importados.
10. Não confiar cegamente em arquivos `.save` externos.
11. Exclusão das `SharedPreferences` dos mecanismos automáticos de backup configurados.
12. Uso de `BuildConfig.DEBUG` para diagnóstico de desenvolvimento.

Qualquer alteração nessas regras deve ser tratada como alteração de segurança e registrada no histórico do projeto.

---

## 19. Conclusão da auditoria 0.78

**Classificação geral do código auditado: baixo risco para o dispositivo.**

Não foi encontrado mecanismo que indique intenção ou capacidade de causar dano físico à bateria, armazenamento, processador ou demais componentes do telefone.

Também não foi identificado mecanismo de comunicação de rede, coleta remota ou execução persistente em segundo plano no escopo examinado.

O principal ponto técnico a monitorar é o uso de RAM na tela do mapa e, secundariamente, a evolução do sistema de backup automático e das rotinas de escrita.

A auditoria deve ser repetida quando ocorrer qualquer uma destas mudanças:

- inclusão de Internet;
- inclusão de SDK externo;
- inclusão de analytics;
- alteração do sistema de arquivos;
- alteração de backup;
- inclusão de execução em segundo plano;
- alteração do codec de importação;
- alteração do Manifest;
- atualização grande das dependências;
- mudança do mapa/imagens;
- publicação de uma nova arquitetura de sincronização.

**Este documento deve permanecer no repositório e ser atualizado junto com cada alteração relevante de segurança, desempenho de memória, armazenamento, permissões, rede ou componentes Android.**

---

## 20. Registro de auditorias futuras

| Versão | Data | Auditor | Resultado | Alterações relevantes |
|---|---|---|---|---|
| 0.78 | 23/09/2026 | OpenAI / revisão estática | Baixo risco no código auditado | Auditoria inicial de segurança, bateria, armazenamento, permissões e rede |
| 0.79 | 23/09/2026 | OpenAI / revisão estática | RAM do mapa HD mitigada | Renderização tiled 512×512, cache LRU de 32 tiles, resolução original preservada |
| | | | | |
| | | | | |
| | | | | |

---

### Referências técnicas

As conclusões sobre sandbox, permissões, armazenamento e componentes Android devem ser comparadas com a documentação oficial atual do Android durante futuras auditorias:

- Android App Sandbox: https://source.android.com/docs/security/app-sandbox
- App fundamentals/components: https://developer.android.com/guide/components/fundamentals
- Permissions overview: https://developer.android.com/guide/topics/permissions/overview
- Security best practices: https://developer.android.com/privacy-and-security/security-best-practices
- Storage best practices: https://developer.android.com/training/data-storage

As URLs acima são referências para manutenção técnica; elas não substituem a análise do código e do APK de cada release.


---

## 21. Correção de compilação — 0.80.1 / 0.81

Uma execução de `testDebugUnitTest` no CI reportou erro de compilação em `FeiticosTab.kt`:

`Unresolved reference 'isLunar'`.

A causa era simples: o arquivo utilizava a extensão `String.isLunar()` sem importar `com.example.model.isLunar`.

Correção aplicada nesta revisão:

`import com.example.model.isLunar`

A alteração não muda a lógica funcional de Feitiços; apenas torna explícita a dependência da extensão já existente em `CharacterType.kt`.

O ambiente desta revisão não conseguiu repetir o build completo porque o Gradle Wrapper tentou baixar `gradle-9.7.1-bin.zip` de `services.gradle.org` e a rede do ambiente não permitiu a conexão. O log fornecido para esta correção, entretanto, confirma que o build chegou ao `compileDebugKotlin` e que o erro reportado era especificamente o `isLunar` ausente.

## 22. Persistência assíncrona e mitigação de I/O — 0.82

A revisão 0.82 introduziu uma camada de coordenação para as gravações da planilha ativa e dos backups periódicos.

### Alterações

- `SheetPersistenceCoordinator` executa a persistência em `Dispatchers.IO`.
- Alterações rápidas que já passaram pelo debounce do ViewModel são coalescidas: somente o snapshot mais recente pendente é mantido.
- As gravações da planilha ativa são serializadas, evitando que uma gravação antiga termine depois de uma nova e sobrescreva o estado mais recente.
- `SheetRepository.loadActiveSheet()` considera o snapshot pendente mais recente, preservando consistência de leitura após uma gravação enfileirada.
- `SheetRepository.getAllSheets()` incorpora a planilha ativa pendente ao resultado do índice enquanto a gravação assíncrona ainda não terminou.
- Backups periódicos passaram a usar `BackupPersistenceCoordinator`, mantendo serialização/JSON e escrita fora da thread de UI.
- A proteção não substitui o debounce de 400 ms: o debounce continua reduzindo a frequência das solicitações e o coordinator atua como segunda barreira contra concorrência e I/O no thread chamador.

### Efeito esperado

A UI deixa de executar diretamente a persistência da planilha ativa e do backup periódico. O armazenamento recebe menos gravações redundantes e as gravações não competem entre si.

### Limitação conhecida

`SharedPreferences` continua sendo o mecanismo de persistência existente. A revisão não migra o projeto para Room/DataStore nem altera o formato dos dados persistidos, evitando uma migração estrutural desnecessária.

### Histórico

| Versão | Data | Auditor | Resultado | Alterações relevantes |
|---|---|---|---|---|
| 0.82 | 23/09/2026 | OpenAI / revisão estática | I/O da planilha ativa e backup mitigado | Coordinator em `Dispatchers.IO`, coalescência e serialização das gravações |

## 23.0.83 — Correções de I/O, robustez de arquivos e manutenção

- Persistência da planilha ativa e backups continuam em `Dispatchers.IO`, com
  coordenação sincronizada para evitar a corrida entre `submit()` e o fim do worker.
- Coordenadores agora possuem encerramento explícito no `SheetViewModel.onCleared()`.
- Persistência de NPC foi movida para APIs `suspend` em `Dispatchers.IO`.
- Arquivos `.save` de NPC usam gravação temporária + rename, limite de 4 MiB,
  validação do diretório permitido e sanitização básica do nome do arquivo.
- Leitura de NPC rejeita arquivos fora do diretório esperado, extensões inválidas
  e arquivos acima do limite antes de decodificar o conteúdo.
- Exportação de PDF de NPC foi movida para `Dispatchers.IO` e passou a usar arquivo
  temporário antes da substituição final.
- Leitura do log de erro da UI foi retirada do caminho síncrono de composição e
  transferida para `Dispatchers.IO`.
- Adicionados testes unitários básicos para serialização/coalescência da persistência
  da planilha ativa.
- Limitação de validação: o build completo não pôde ser executado neste ambiente,
  pois o Gradle Wrapper 9.7.1 tentou acessar `services.gradle.org` e o ambiente
  estava sem resolução de DNS/rede. A validação desta versão inclui inspeção
  estrutural e verificações estáticas locais.


## 23.0.84 — Correção de compilação do mapa

O relatório de CI da versão 0.83 apontou chamadas de APIs Compose dentro de `MapaInterativo` sem o contexto `@Composable` (`MapTab.kt`). A função foi marcada explicitamente com `@Composable`, preservando a lógica de renderização e eliminando o erro de compilação reportado nas linhas 715, 726, 727 e 733.

A validação local desta correção não pôde concluir o build porque o ambiente atual não conseguiu baixar o Gradle 9.7.1 de `services.gradle.org` (`UnknownHostException`).

## 24.0.85 — Persistência e I/O reforçados

Esta revisão reforça a camada de persistência sem alterar o formato das fichas.

- `SheetPersistenceCoordinator` passou a ignorar snapshots já pendentes ou já persistidos e mantém o worker protegido por seção sincronizada.
- `BackupPersistenceCoordinator` passou a coalescer também snapshots já persistidos, reduzindo gravações redundantes.
- O salvamento de NPC continua fora da UI, em `Dispatchers.IO`, com nome sanitizado, limite de 4 MiB, arquivo temporário e validação do diretório no carregamento.
- O carregamento de NPC na interface passou a ser assíncrono; um clique não bloqueia a composição e cliques concorrentes ficam desabilitados durante a leitura.
- A rotina de exportação de PDF de NPC permanece em `Dispatchers.IO` e usa arquivo temporário.

A validação desta revisão permanece limitada à análise estática/estrutural quando o ambiente não consegue obter o Gradle 9.7.1.

## 25.0.86 — Cache HD do mapa endurecido

- Tiles HD visíveis agora são tratados como conjunto protegido durante a poda do LRU.
- O LRU evita expulsar um tile que pertence ao viewport solicitado; tiles antigos não visíveis continuam sujeitos à remoção.
- Cancelamento da coroutine de carregamento remove as chaves marcadas como `loading`, evitando entradas presas que impediriam nova tentativa.
- O arquivo HD original e sua resolução não são alterados.
- O mapa leve continua como fallback quando o zoom está abaixo do limiar HD.

O objetivo é reduzir o risco de um tile atualmente visível desaparecer do cache sem que uma nova requisição seja disparada.

## 26.0.87 — Regressão e integridade

Foram ampliados os testes de persistência e de importação/exportação:

- cobertura do `BackupPersistenceCoordinator` para coalescência e deduplicação;
- cobertura de ida e volta do `ShareCodeCodec` para Lunar, incluindo casta, forma espiritual e sinal;
- cobertura de ida e volta para Sangue de Dragão, incluindo tipo e aspecto;
- os testes existentes de persistência da planilha ativa permanecem preservados.

Esta camada não altera regras de negócio; ela documenta e protege comportamentos que já foram corrigidos nas versões anteriores.


## 27.0.88 — Correções de compilação e fechamento da cadeia de melhorias

O relatório de CI fornecido para a revisão anterior confirmou que o Gradle 9.7.1 foi executado com sucesso até a compilação Kotlin, mas apontou três erros concretos:

- `MainActivity.kt`: smart cast inválido de propriedade delegada `erroAnterior`.
- `EncounterCombatEquations.kt`: `return` proibido dentro do lambda de `withContext`.
- `EncounterNpcCardSections.kt`: chamada suspend de carregamento de NPC dentro de callback síncrono.

Correções aplicadas:

- `MainActivity.kt` agora copia o estado para uma variável local estável antes de passá-lo à tela de erro.
- `gerarPdfNpc()` usa diretamente a expressão `try/catch` como resultado do `withContext`, mantendo todo o I/O em `Dispatchers.IO`.
- O carregamento de NPC foi movido para `LaunchedEffect(arquivoEmCarregamento)`, deixando o `feedbackClickable` responsável apenas por selecionar o arquivo.
- A identificação textual dos NPCs agora preserva explicitamente Solar, Lunar e Sangue de Dragão, inclusive no nome dos arquivos salvos.

O build local desta revisão não pôde ser executado porque este ambiente não conseguiu resolver `services.gradle.org` para baixar o Gradle 9.7.1. O relatório de CI fornecido pelo usuário é a evidência usada para as três correções de compilação.

## 28.0.89 — Otimizações de performance seguras

- `SheetTabsBar` e `MainSheetScreen`: o cálculo de Pontos de Habilidade restantes passou a ser memoizado por snapshot da ficha, evitando recomputação em recomposições que não alteram a ficha.
- `AbilitiesTab`: listas filtradas de Habilidades e Artes Marciais passaram a usar `remember(sheet, subTabIndex)`, evitando `filter`/seleção repetidos durante recomposições visuais.
- `BackupPersistenceCoordinator`: removida atribuição duplicada no `close()`, mantendo a mesma semântica e reduzindo ruído no caminho de encerramento.
- O cache HD do mapa, o carregamento assíncrono de catálogos e a persistência coalescida de 0.85–0.88 foram preservados; nenhuma redução de resolução ou alteração de regra de jogo foi feita.
- Esta rodada é deliberadamente conservadora: prioriza menos recomposição e alocação sem introduzir mudanças arquiteturais de alto risco antes da compilação.

## 28.0.90 — Correção de compilação dos testes

O CI reportou erro em `PersonalDataActionsLanguageTest.kt`: o teste ainda construía `ExperienceActions` com `MutableStateFlow<CharacterSheet>` e `MutableStateFlow<String?>`, enquanto a implementação atual aceita somente `MutableStateFlow<String?>` (`commitmentError`). O teste foi atualizado para usar a assinatura vigente. A tentativa de validação local foi bloqueada pela indisponibilidade de rede para baixar Gradle 9.7.1 (`UnknownHostException: services.gradle.org`).


## 28.0.91 — Aba 7: Equipamento

Ajuste exclusivamente visual nos diálogos de cadastro de equipamento: remoção dos labels redundantes solicitados e renomeação dos botões de catálogo. Não foram alterados campos, ações, persistência, regras ou catálogos. A estrutura de `verticalScroll` foi preservada como proteção de layout para telas menores; a redução de conteúdo deve evitar a necessidade de rolagem no cenário normal.

## 28.0.92 — Aba 8: Artes Marciais e árvore de pré-requisitos

- A caixa `Artes Marciais` da Aba 8 agora usa o mesmo gesto de toque longo das demais caixas de Encantos, abrindo a árvore correspondente.
- A árvore de `Artes Marciais` usa exclusivamente os Encantos cujo campo `mins` começa com `Arte Marcial`, evitando misturar Encantos de Habilidades comuns.
- O catálogo de Artes Marciais passou a participar da fonte de dados da árvore de pré-requisitos usada pelos detalhes de Encantos.
- O JSON `estilos_artes_marciais.json` foi auditado: 28 estilos e 279 Encantos; todas as 22 referências de pré-requisito que não correspondiam exatamente ao nome cadastrado foram corrigidas para o nome efetivo do Encanto no próprio estilo.
- Após a correção, todas as referências `pre_requisitos` do JSON resolvem para Encantos existentes no mesmo estilo.
- Não foram alterados custos, requisitos de Essência/Habilidade, descrições ou regras de aquisição; apenas referências textuais de pré-requisito inconsistentes e o comportamento de navegação da árvore.
- A compilação completa local continua bloqueada neste ambiente por `UnknownHostException: services.gradle.org`; a validação estrutural Kotlin e a validação automática das referências do JSON foram concluídas com sucesso.

## 28.0.93 — Ajuste visual da Aba 11

- Alteração exclusivamente de layout/formatação na identificação de gênero dos NPCs.
- O símbolo masculino/feminino permanece junto ao nome e não ocupa a área dos controles Exportar/Salvar/Carregar.
- PDF usa a mesma convenção visual: `Nome ♂` ou `Nome ♀`.
- Sem alteração de permissões, armazenamento, rede ou lógica de persistência.

## Exalted.094 — Aba 11 Lunar
- Reorganizada a apresentação de NPCs Lunares para a sequência fixa: Nome, Tipo, Casta, Essência, Forma Espiritual, Sinal e Idioma.
- Removido o controle de novo sorteio do Sinal na interface; o Sinal permanece o valor gerado e persistido na criação do NPC.
- A exportação em PDF foi alinhada à mesma ordem.

## 28.0.96 — Aba 8: interação e árvore de pré-requisitos
- `CharmAbilityButton` usa `feedbackCombinedClickable`, reduzindo divergências de tratamento entre toque curto e long press.
- A árvore de pré-requisitos foi ajustada para desenhar ramificações de múltiplos pré-requisitos no sentido visual de progressão.
- Foi removida uma referência circular no JSON de Artes Marciais (`Ataque de Quebra de Articulações` → `Técnica de Travamento de Articulação`).
- Auditoria do catálogo: 279 Encantos e 303 referências internas de pré-requisitos resolvidas, sem ciclos detectados após a correção.
- A validação completa com Gradle não foi possível neste ambiente por indisponibilidade de resolução DNS para `services.gradle.org`; a suíte deve ser confirmada no CI.

## 28.0.97 — Árvore de Encantos orientada pelos pré-requisitos do JSON

- A construção da árvore deixou de usar `split(",")` como regra de sintaxe.
- Referências são resolvidas automaticamente contra o catálogo inteiro por nome ou ID, preservando nomes de Encantos que contenham vírgulas, ponto e vírgula ou outros sinais.
- O ID estável do JSON/modelo passou a ser a identidade interna do nó; o nome continua sendo a referência humana do campo `pre_requisitos`.
- O campo `pre_requisitos` continua sendo a fonte de verdade das ligações; nenhum vínculo é inventado a partir de habilidade, Essência, ordem do JSON ou posição na lista.
- JSONs futuros no mesmo schema podem fornecer `pre_requisitos` como String ou como array de Strings.
- Referências que não podem ser resolvidas permanecem sinalizadas no resultado da árvore em vez de serem silenciosamente descartadas.
- Requisitos genéricos de quantidade, como "quaisquer quatro Encantamentos...", não são transformados artificialmente em arestas.


## 28.0.98 — Aba 11: Força de Vontade e títulos

- A trilha compartilhada de Força de Vontade foi ajustada para aceitar clique em qualquer caixa adquirida após a conclusão da planilha.
- A operação continua centralizada em `WillpowerTrackLogic`, preservando a sequência: caixa vazia consome o primeiro ponto disponível; caixa marcada remove o último ponto marcado.
- A mudança elimina o bloqueio que ocorria ao tocar a última caixa e permite desfazer quando todas as caixas estão cheias.
- “Força de Vontade” e “Trilha de Vitalidade” da Aba 11 usam o mesmo componente visual `EncounterCardTitle` das seções Atributos, Habilidades, Méritos, Ações e Encantos.

## 29.0.99 — ANR em persistência de NPCs de Encontro

O log real do dispositivo apresentou `ANR ... Input dispatching timed out` para `MainActivity`, com ~140% de CPU do processo durante a janela do bloqueio. A causa provável e reproduzível no código era a serialização de toda a lista de NPCs de Encontro no thread da UI através de `EncounterNpcStore.save()`.

A persistência foi movida para `Dispatchers.IO` com coalescência de snapshots em `EncounterNpcPersistenceCoordinator`. Alterações rápidas agora não repetem serialização desnecessária e não bloqueiam o processamento de eventos de toque.

## 28.0.100 — ANR ao abrir árvore de Artes Marciais

O log de execução fornecido mostrou um ANR em `MainActivity`, com `Input dispatching timed out` após 5 s e CPU do processo do aplicativo chegando a aproximadamente 140%. O caminho reproduzido pelo usuário foi Aba 8 → Artes Marciais → visualizar a árvore de um estilo.

Correções aplicadas:
- a construção do grafo da árvore deixou de ocorrer no thread principal da composição; ela agora é executada em `Dispatchers.Default` por `LaunchedEffect`;
- enquanto o grafo é calculado, a interface mostra estado de carregamento, evitando bloquear o processamento de entrada;
- a árvore de Artes Marciais passou a usar o mesmo resolvedor de pré-requisitos orientado pelo JSON utilizado pela árvore geral, eliminando o parser paralelo baseado em `split(",")`;
- os nós usam IDs estáveis do catálogo e as ligações são resolvidas pelos pré-requisitos efetivamente encontrados no catálogo;
- o popup de Artes Marciais não mantém mais dois `AlertDialog` simultâneos ao abrir a árvore: a árvore substitui temporariamente o popup de estilos e retorna a ele ao fechar;
- a alteração reduz o risco de ANR e também evita a combinação de janelas modais que acompanhava a abertura da árvore.

As mensagens `EGL_BAD_MATCH`, `glUtilsParamSize`, `VibratorService` e `TcpSocketTracker` presentes no log são tratadas como ruído/limitações do ambiente de execução, enquanto o sinal relevante para o aplicativo é o ANR de `Input dispatching timed out`.

A suíte Android completa não pôde ser executada neste ambiente porque `services.gradle.org` não está acessível; a validação estrutural dos fontes Kotlin foi realizada.

## Exalted.101 — ANR na árvore de Artes Marciais

O ANR reportado ao abrir `Aba 8 > Artes Marciais > estilo > Encanto > i > Árvore` foi analisado. O detalhe de um Encanto de Arte Marcial estava recebendo, para a árvore, o catálogo combinado de Encantos do personagem (centenas de entradas), embora o Encanto possua um ID com o formato `martial_<estilo>::<encanto>`. Isso fazia o resolvedor de pré-requisitos trabalhar sobre um catálogo muito maior do que o necessário.

Correções:
- Encantos marciais agora usam somente os Encantos do próprio estilo ao abrir sua árvore.
- A construção do grafo da árvore de pré-requisitos passou para `Dispatchers.Default`, evitando trabalho de CPU no thread de UI.
- O diálogo de detalhes e o diálogo da árvore não são mais exibidos simultaneamente.
- A resolução continua baseada integralmente em `pre_requisitos` e IDs do JSON.

O log caracteriza o evento como ANR (`Input dispatching timed out`) com CPU elevada no processo do aplicativo; as mensagens EGL/BlueStacks são tratadas separadamente e não foram usadas como causa única do ANR.

## 29. Exalted.102 — Auditoria de performance / trabalho na Main

### Objetivo
Revisão focada em localizar processamento CPU-bound, I/O e transformação de catálogos que ainda poderiam ocorrer no thread principal, sem alterar regras de jogo ou apresentação visual.

### Pontos identificados e tratados
1. **Construção da árvore de pré-requisitos a partir de `List<Encanto>`** — `paraArvoreDePreRequisitos()` era chamado antes do `LaunchedEffect` em `CharmDetailsDialogs`. Isso fazia conversão do catálogo + resolução de pré-requisitos na Main. A API da árvore passou a receber `List<Encanto>` e executar conversão/resolução em `Dispatchers.Default`.
2. **Resolvedor de pré-requisitos** — o parser reconstruía `groupBy`, candidatos e ordenação para cada Encanto. Foi introduzido `CharmPrerequisiteReferenceParser.Resolver`, que prepara os candidatos uma vez por catálogo e reutiliza essa estrutura para todas as resoluções da árvore.
3. **Catálogo de Méritos da Aba 11** — `NpcCardMerits` criava `MeritosCatalog(context)` durante composição. A composição agora consulta `viewModel.meritoDefinitionPorNome`, reutilizando o catálogo já mantido pelo ViewModel e evitando parse de JSON durante a renderização de cada card.
4. **Índice de planilhas salvas na criação do ViewModel** — `getAllSheets()` pode decodificar várias planilhas e executar migrações. A leitura inicial agora é executada em `Dispatchers.IO` antes de publicar o resultado em `savedSheets`.

### Pontos já protegidos e verificados nesta auditoria
- Persistência da planilha: `SheetPersistenceCoordinator` em `Dispatchers.IO`.
- Backups periódicos: `BackupPersistenceCoordinator` em `Dispatchers.IO`.
- Persistência de NPCs de Encontro: `EncounterNpcPersistenceCoordinator` em `Dispatchers.IO`.
- I/O de arquivos de NPC: `NpcSaveLoadService` em `Dispatchers.IO`.
- Geração de PDF de NPC: `Dispatchers.IO`.
- Exportação de código de planilha/NPC: `Dispatchers.Default` na UI.
- Importação de código: `Dispatchers.Default` na UI.
- Geração de NPCs de encontro: ações pesadas usam `Dispatchers.Default`.
- Construção das árvores de Encantos: `Dispatchers.Default`.
- Listas derivadas da UI que já eram estáveis agora usam `remember(...)` nos pontos revisados.

### Limitação de validação
A suíte `testDebugUnitTest` foi tentada nesta revisão, mas o ambiente não conseguiu baixar Gradle 9.7.1 por `UnknownHostException: services.gradle.org`. Portanto, esta versão foi validada estruturalmente, mas não é declarada como compilada neste ambiente.

### Regra de escopo
Nenhuma mudança desta rodada altera regras de aquisição, elegibilidade, pré-requisitos, valores, catálogo, aparência, layout ou comportamento funcional pretendido. As mudanças são de execução, cache e localização do processamento.

## 30. Exalted.103 — Correção de compilação da árvore

O build externo do Exalted.102 identificou referências não resolvidas em `AbilityCharmTreeDialog.kt`. O código foi ajustado para reutilizar o mapeamento centralizado `paraArvoreDePreRequisitos()`, eliminando as referências diretas que causavam a cascata de erros de inferência de tipos. Nenhuma regra de jogo, catálogo ou comportamento visual foi alterado.

Validação local: o ambiente atual não possui acesso a `services.gradle.org`, portanto o Gradle 9.7.1 não pôde ser baixado para execução do teste completo.


## 31. Revisão técnica — Exalted.104

- Remoção de código privado comprovadamente morto e imports sem uso identificados por análise estática.
- Encerramento explícito do coordinator de persistência de NPCs de Encontros no ciclo de vida do `SheetRepository`, evitando coroutine/scope vivo após o fechamento do repositório.
- Limpeza de duplicação trivial no encerramento do backup periódico.
- Normalização defensiva dos identificadores usados pela árvore de Encantos para reduzir exceções por inconsistências de whitespace no catálogo.
- Construção da árvore permanece fora da Main thread (`Dispatchers.Default`).
- Nenhuma alteração de permissões, rede, armazenamento ou regras funcionais foi introduzida nesta revisão.


## 31. Exalted.105 — árvore de Artes Marciais
- Corrigido o acesso do catálogo por ID de estilo, evitando lista vazia no diálogo de árvore de Artes Marciais.
- Adicionado estado terminal para raiz inexistente, evitando carregamento visual indefinido.

## 32. Exalted.106 — Aba 11: Encontros
- O controle de geração continua protegido pelo mesmo limite de 10 NPCs e pelo mesmo fluxo assíncrono do ViewModel; apenas a superfície visual foi substituída por uma imagem clicável.
- A imagem de geração usa branco fixo para contraste em Solar, Sangue de Dragão e Lunar.
- O switch global `Personalizado/Aleatório` foi removido. A geração não sorteia mais Nome/Gênero/Arquétipo por causa de um modo oculto; utiliza os valores atualmente selecionados na interface.
- O botão direto `Aleatório` de Gênero continua disponível como seleção explícita do usuário.


## 33. Exalted.107 — Correção do asset da Aba 11
- O asset visual do acionador de geração de NPC foi substituído pelo PNG transparente fornecido nesta rodada.
- O recurso permanece local ao aplicativo e não introduz permissões, rede, I/O adicional ou mudança no fluxo de geração.
- A imagem é exibida sem tintura programática, preservando as cores e o canal alpha do arquivo fornecido.


## 34. Exalted.108 — conexões das árvores de Encantos
- Corrigido o roteamento visual das setas da árvore para evitar segmentos ocultos por cartões intermediários.
- Conexões de níveis consecutivos usam `Path` contínuo; conexões que saltam níveis usam corredores laterais dentro do canvas.
- Aumento de espaçamento é exclusivamente visual e não altera regras, catálogo, pré-requisitos ou progressão dos Encantos.
- Nenhuma permissão, rede, armazenamento ou execução em background foi adicionada.


## 35. Exalted.109 — detalhes de equipamento na Aba 11
- O long press da linha `Equipamento` apenas consulta dados já presentes em memória (`CharacterSheet` e `NpcEncontro`) e exibe um popup; não adiciona rede, arquivos, permissões ou execução em background.
- Quando há correspondência com equipamento da Aba 7, são reutilizados os valores persistidos da planilha, inclusive alterações manuais.
- Para equipamentos exclusivos da geração do Encontro, informações derivadas são calculadas pelas mesmas tabelas compartilhadas de estatísticas da Aba 7.
- A alteração é somente de apresentação/interação; as regras de geração e combate permanecem inalteradas.


## 32. Aba 11 — reorganização visual Exalted.110

Alterações exclusivamente de apresentação/posicionamento: títulos centralizados com espaçamento, botão de entrada em batalha reposicionado e controles de XP reposicionados após as gavetas de Encantos. Nenhuma regra de geração ou persistência foi alterada.


## 32. Exalted.111 — apresentação de equipamentos no PDF

- Corrigida duplicação do peso no campo de equipamento do PDF da Aba 11.
- O nome do equipamento pode já conter o sufixo de peso; a camada de apresentação evita repetir o mesmo sufixo.
- Nenhuma regra de geração, persistência ou catálogo foi alterada.

### 32. Exalted.112 — indicadores visuais de Casta/Aspecto/Favorecidas
- Alteração exclusivamente visual na ficha da Aba 11.
- Solar/Sangue de Dragão: quadrados nas Habilidades, marcados para Casta/Aspecto ou Favorecidas.
- Lunar: quadrados nos Atributos, marcados para Casta ou Favorecidos.
- Nenhum novo estado persistido e nenhuma regra de geração alterada.

## 32. Exalted.113 — integridade visual dos emblemas

- A seleção inicial mantém os três tipos de Exaltado com imagens em `ContentScale.Fit`, agora com dimensões mais conservadoras para evitar clipping em telas com altura reduzida.
- O emblema de identidade no topo da planilha recebeu margem adicional e tamanho lógico menor, evitando que o desenho encoste/corte nos limites da caixa.
- Alteração exclusivamente visual; sem mudança de permissões, persistência, geração de dados ou rede.

## Exalted.114 — Aba 11: indicadores e ajustes de apresentação
- Alteração exclusivamente visual na ficha de Encontros: alinhamento do nome, tipografia/espaçamento dos títulos e preenchimento integral dos indicadores.
- Solar/Sangue de Dragão: Habilidades de Casta/Aspecto sem pontos continuam sendo apresentadas como `Habilidade 0` e recebem o indicador quando pertencentes ao conjunto especial.
- Lunar: somente Atributos recebem indicadores, usando os conjuntos já persistidos em `NpcEncontro`.
- Nenhuma regra de geração, persistência, rede, permissão ou execução em background foi introduzida.

## 32. Exalted.115 — detalhes de equipamento da Aba 11
- O long press da linha `Equipamento` continua sendo local, sem qualquer acesso externo ou persistência adicional.
- A exibição consulta apenas os dados já presentes na `CharacterSheet` (Aba 7) e normaliza nomes/pesos para localizar o equipamento correspondente.
- Nenhuma nova permissão, rede, armazenamento ou regra de geração foi introduzida.


## Exalted.116 — Aba 8: integridade das árvores de Encantos

- A árvore de pré-requisitos e a árvore de estrutura completa permanecem separadas por finalidade, mas usam a mesma resolução de referências do catálogo.
- A árvore completa passou a relacionar nós por ID normalizado, reduzindo ambiguidades causadas por nomes repetidos ou diferenças de apresentação.
- Conectores que atravessam níveis intermediários usam corredores laterais fora dos cartões, reduzindo o risco de segmentos visualmente ocultos.
- A árvore de pré-requisitos mantém expansão defensiva para ciclos/pré-requisitos ausentes e passou a desenhar cada conexão em um único caminho contínuo.
- Não foram adicionadas permissões, rede ou execução em segundo plano.

## Exalted.117 — Trilha de Vitalidade da Aba 11
- Removida a reorganização assíncrona/debounce de 2 segundos da vitalidade dos NPCs; a mutação agora é determinística e imediata.
- A marcação de dano só pode avançar para a próxima caixa livre na ordem visual da trilha.
- Ao expandir a capacidade de vitalidade, o dano existente é compactado para o início da nova trilha, preservando a quantidade de dano dentro do novo limite.
- Não foram introduzidos novos acessos de rede, armazenamento ou execução em segundo plano.


### 32. Exalted.118 — correções de compilação
Correções estritamente tipadas em `EncounterCardTitle` e `EncounterNpcCardSections`, sem alteração das regras funcionais.


### Exalted.119
- Corrigida a conversão de `Map.Entry<String, Int>` para `Pair<String, Int>` em `EncounterNpcCardSections.kt`, eliminando o erro de compilação reportado no CI.


## 33. Trilha de Vitalidade da Aba 11
- Interações de dano não criam tarefas ilimitadas: cada NPC mantém no máximo um job de reorganização pendente, cancelado e reagendado a cada nova interação.
- A reorganização ocorre após 2 segundos sem interação e trabalha somente sobre a lista de caixas do NPC.
- Não há acesso de rede ou execução em segundo plano além do escopo do ViewModel já existente.


## 33. Empacotamento determinístico do APK — Exalted.125
- O artefato debug do CI é nomeado `Exalted.125.apk`.
- A fonte única de verdade é `rootProject.name` em `settings.gradle.kts`; o Gradle valida o padrão `Exalted.<versão>`.
- O workflow não usa curinga para o APK final: verifica e publica exatamente o nome esperado.
- A verificação também impede que `app-debug.apk` seja publicado silenciosamente.

### Exalted.125 — correção de configuração Gradle
- A regra de nomeação do APK usa `tasks.configureEach` em vez de `tasks.named("assembleDebug")`, evitando falha de configuração quando o task Android é registrado tardiamente pelo AGP.
- A validação do nome canônico `Exalted.<versão>` permanece ativa.


## Exalted.125 — configuration cache e empacotamento do APK
- Corrigida a incompatibilidade do renomeador do APK com o configuration cache do Gradle 9.7.1.
- A operação de renomeação agora está isolada em uma task tipada, sem serializar `Project` ou objetos do script Gradle.
- O CI continua verificando a existência exclusiva de `app/build/outputs/apk/debug/Exalted.125.apk`.


## Exalted.126 — revisão, correção e limpeza da lógica de geração de NPCs
- Revisadas as quatro interpretações fornecidas para a lógica de construção da Aba 11, usando o código real da Exalted.125 como fonte final de comportamento.
- Corrigida a prioridade de **Ocultismo** para NPCs Mentais Solar e Sangue de Dragão: quando a rota Mental é usada, Ocultismo agora entra explicitamente no topo da prioridade de Encantos, evitando que o catálogo seja consumido antes da árvore necessária para Feitiçaria.
- Mantida a regra específica do Físico: Ocultismo só recebe essa prioridade adicional quando já está em nível 3+, sem alterar a sequência de sorteios.
- Centralizada a regra de Habilidade defensiva obrigatória em `EncounterGenerationRules.habilidadeDefensivaPara()`, removendo a duplicação nos três geradores.
- Centralizadas constantes das regras de Ocultismo e da chance de Feitiçaria Mental, reduzindo números mágicos sem alterar a fonte de aleatoriedade usada pelo gerador.
- Mantida a arquitetura separada Solar/Sangue de Dragão/Lunar; não foi aplicada a migração ampla para Strategy para evitar mudança desnecessária da sequência aleatória e regressões.
- Mantida a correção já existente do PB Lunar para não escolher uma meta impossível quando não há candidatas suficientes; foram adicionados testes de regressão para as novas regras centralizadas.
- Preservado o renomeador tipado de APK da Exalted.125 e atualizado o nome canônico para `Exalted.126.apk`.
- Nenhuma permissão, rede, armazenamento ou execução em segundo plano foi adicionada.


## Exalted.127 — consolidação adicional da geração de NPCs
- Revisada novamente a lógica de construção da Aba 11 à luz das quatro interpretações fornecidas.
- Não houve alteração de permissões, rede, armazenamento, serviços ou execução em segundo plano.
- Foram centralizadas apenas regras de geração puras: grupos de Atributos, escolha de combate, defesa e sorteio da Feitiçaria Mental.
- A validação estrutural agora verifica também o total de 27 pontos de Atributos e os limites 1..5.
- A correção de Feitiçaria Mental exige que os 4 Encantos de Ocultismo sejam buscados antes da inclusão da Feitiçaria quando a chance de 9/10 for satisfeita.

## Exalted.128 — ajuste estrutural dos atributos Lunares
- Alteração restrita à lógica de geração/validação e apresentação dos Atributos Lunares.
- Nenhuma permissão, acesso de rede, armazenamento, serviço ou execução em segundo plano foi adicionado.
- A separação entre 2 Atributos de Casta e 2 Atributos Favorecidos adicionais reduz ambiguidade de estado e torna a validação estrutural mais explícita.

## Exalted.129 — auditoria estrutural da geração de NPCs
- Alterações restritas à geração, validação e testes da Aba 11; nenhum acesso de rede, permissão, serviço, receiver, armazenamento externo ou execução em segundo plano foi adicionado.
- Criado relatório interno de construção sem persistir dados diagnósticos adicionais no NPC.
- Validação reforçada para impedir estados incoerentes entre Atributos, Habilidades, combate, Especialidades, Corpo de Touro e identidade do Exaltado.
- Adicionada matriz automatizada de 900 construções para regressão de identidade/coerência em sementes variadas.
- Reservas de Encantos continuam limitadas ao orçamento inicial; Feitiçaria reservada consome explicitamente uma das vagas previstas.


## Exalted.130 — correção de compilação

- Correção somente de referências de propriedade no relatório/validação de construção de NPCs.
- Nenhuma permissão, rede, armazenamento, execução em segundo plano ou fluxo de importação/exportação foi alterado.
- Sem impacto esperado na superfície de segurança.
