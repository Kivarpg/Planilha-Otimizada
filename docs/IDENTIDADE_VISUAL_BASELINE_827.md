# Exalted — baseline preventiva da identidade visual

Base compilada confirmada: Exalted.831 (informada pelo usuario).
Estado: preparacao, sem nova remodelacao artistica.
Nao interpretar esta nota como homologacao visual no dispositivo.

## Simbolos celestes
- Sol e lua devem ser representados sem rostos, olhos, bocas ou qualquer feicao humana; nunca antropomorfizar esses simbolos.

## Invariantes
- Manter as 15 abas, seus nomes, ordem, campos e funcionalidades.
- Dar igual prioridade a Solar, Sangue de Dragao e Lunar.
- Preservar a distincao Casta / Aspecto e as implementacoes externas das abas Conflito e Tradutor.
- Nao reintroduzir decoracoes de fundo, pessoas ou icones de equipamentos removidos.
- Preservar as regras mecanicas de NPCs, Encantos e experiencia.
- Nao alterar componentes de Canvas artisticos indiscriminadamente.
- Nao compilar sem autorizacao explicita.

## Evidencia que falta obter no Android
1. Captura de cada uma das 15 abas nos tres tipos de Exaltado, onde aplicavel.
2. Capturas de pop-ups e arvores de Encantos.
3. Largura reduzida, modo paisagem, fonte ampliada e tela dividida.
4. Interacao de botoes compactos, gestos long press e campos com teclado aberto.
5. Aba 11: geracao de NPC, vitalidade, XP e acoes Exportar/Salvar/Carregar.
6. Aba 14: escala do mapa em modo maximizado.
7. Registro de dimensoes e versao do dispositivo para comparar antes/depois.

## Auditoria complementar da navegacao
- SheetTabsBar utiliza LazyRow com rolagem para a aba selecionada.
- O tamanho dos rotulos varia com largura disponivel e fontScale.
- TabIdentitySurface conserva superfícies neutras; nao reinserir ornamentacao removida.
- Teste VisualIdentityPreparationContractTest protege esses contratos estaticamente.
- Risco residual: textos truncados e alvos de toque so podem ser verificados com medicao visual/instrumentada.

## Auditoria das planilhas salvas
- A lista de planilhas salvas deve mostrar Aspecto para Sangue de Dragao, Casta Lunar para Lunar e Casta Solar para Solar.
- As acoes existentes de carregar e excluir permanecem intactas.
- Teste estatico adicionado; ainda exige verificacao em dispositivo com planilhas dos tres tipos.

## Auditoria complementar de dialogos
- SettingsDialog, CommitmentErrorDialog e PendingMeritBreakDialog mantem altura maxima limitada e rolagem vertical.
- Contratos estaticos protegem a presenca dos limites, mas nao garantem ausencia de sobreposicao com teclado ou janela reduzida.
- Evitar substituir alturas de modo indiscriminado sem capturas em dispositivos.

## Gate antes de remodelar
- Confirmar baseline por screenshots e registrar problemas existentes.
- Definir tokens visuais sem alterar a logica de negocio.
- Aprovar um prototipo piloto em dispositivo antes de aplicar a todas as abas.
- Regressao estrutural via VisualIdentityPreparationContractTest.
- Regressao visual somente depois de configurar ferramenta compativel e referencias aprovadas.

## Limites da evidencia
Os testes de contrato atuais inspecionam texto-fonte; nao medem bounds,
nao demonstram ausencia de sobreposicao e nao substituem instrumentacao.
Uma compilacao bem-sucedida nao certifica fidelidade visual nem ausencia de sobreposicao.

## Preparacao da Exalted.834 (sem compilacao)
- Dialogo Restaurar Backup: o horario da copia recebe largura flexivel, no maximo duas linhas e truncamento, preservando o botao Restaurar.
- Dialogo Planilhas Salvas: nomes longos recebem limite de duas linhas e truncamento para nao disputar espaco com o indicador Ativa.
- Testes estaticos adicionados em VisualIdentityPreparationContractTest; falta executar o CI e validar em emulador com fonte ampliada e nomes longos.
- Nao foram alteradas as regras de restauracao, carregamento ou exclusao de planilhas.

## Preparacao da Exalted.835 (sem compilacao)
- Log de Erros: conteudo limitado a 45% da altura da tela (maximo 480dp) com rolagem vertical, preservando os controles Copiar e Fechar.
- Teste estatico cobre a existencia do limite, rolagem e acoes do dialogo.
- Verificacao no emulador ainda necessaria para relatorios extensos, fonte ampliada e tela dividida.
- CI atual executa testes JVM e gera APK, mas nao executa testes instrumentados no emulador. Automatizacao visual completa exigira infraestrutura adicional, testes de navegacao e imagens de referencia aprovadas.

## Preparacao da Exalted.837 (sem compilacao)
- Navegacao: titulos de abas podem ocupar duas linhas quando fontScale > 1.20, mesmo em telas nao compactas.
- Faixa de Pontos de Bonus/Experiencia: altura 34dp com fontes ampliadas, mantendo 25dp no tamanho normal.
- Contratos estaticos de regressao atualizados para os dois ajustes.
- Pendencias: compilacao Kotlin e suite JVM, depois validacao visual no emulador em escala de fonte ampliada, rotacao e tela dividida. Nenhuma dessas validacoes visuais foi executada nesta rodada.

## Preparacao da Exalted.838 (sem compilacao)
- Compartilhamento: QR Code exportado usa largura disponivel ate 240dp, mantendo proporcao quadrada.
- Importacao: contador de caracteres usa peso flexivel, truncamento e reserva largura para botao Colar.
- Teste estatico de regressao verifica limites, acoes e rolagem da importacao.
- Ainda e necessario executar compilacao e testes JVM, e validar visualmente QR, texto longo, fonte ampliada e teclado no emulador.

## Preparacao da Exalted.839 (sem compilacao)
- Aba 8 / dialogos de Encantos: ao abrir arvore de pre-requisitos, o dialogo de detalhes deixa de ser composto ate a arvore ser fechada; evita duas janelas modais simultaneas.
- Lista de planilhas: metadados Casta/Aspecto e Jogador limitados a duas linhas com reticencias, preservando indicador Ativa.
- Testes estaticos de regressao adicionados para ambos os ajustes.
- Pendente: compilacao Kotlin, suite JVM e validacao em emulador dos dialogos de arvore, retorno aos detalhes e nomes extensos.

## Preparacao da Exalted.840 (sem compilacao)
- Dialogos de detalhes de Encantos (catalogo e personagem): estado `mostrarArvore` passa a ser memorizado por ID do Encanto. Ao trocar o Encanto selecionado sem destruir a composicao, a arvore anterior nao permanece aberta por engano.
- Teste de regressao estatico cobre a associacao do estado aos IDs `def.id` e `charm.id`.
- A alteracao nao modifica regras de Encantos, pre-requisitos, atributos, Habilidades ou conteudo de arvores.
- Pendente: compilar, executar testes JVM e validar troca de Encantos/abertura/fechamento da arvore em emulador.

## Preparacao da Exalted.841 (sem compilacao)
- Aba 8 / dialogos de Encantos: caches `remember` de catalogo e arvore incluem o `viewModel` ativo como chave, evitando reutilizar entradas de instancia anterior.
- Dialogo compartilhado de confirmacao de exclusao: conteudo com nomes extensos fica em coluna rolavel e altura maxima de 35% da tela, limitada a 280dp; preserva os botoes Remover e Cancelar.
- Testes estaticos adicionados para os dois contratos.
- Pendente: compilacao Kotlin, testes JVM e verificacao visual em emulador de exclusao com nome longo e troca de planilha/Encanto.

## Preparacao da Exalted.842 (sem compilacao)
- Lista de planilhas salvas: campo de pesquisa permanece visivel quando existe texto de busca, mesmo que a quantidade de planilhas caia para quatro ou menos. Evita filtro ativo sem campo editavel para limpa-lo.
- Teste estatico cobre a condicao `savedSheets.size > 4 || buscaPlanilhasSalvas.isNotBlank()` e a edicao do filtro.
- Sem mudanca de persistencia, busca por nome, regras de personagem ou identidade visual.
- Pendente: compilar, executar testes JVM e testar em dispositivo a transicao de cinco para quatro planilhas com filtro preenchido.

## Preparacao da Exalted.843 (sem compilacao)
- Janela de planilhas salvas: lista vazia passa a exibir mensagem explicita, sem alterar o armazenamento ou carregamento.
- Busca por planilhas: botao acessivel `Limpar busca` aparece quando o campo contem texto e chama `onBuscaChange("")`.
- Contratos estaticos de regressao cobrem estado vazio, icone de limpeza e preservacao do filtro.
- Pendente: compilacao Kotlin, testes JVM e validacao visual em emulador com lista vazia, filtros sem resultado e teclado aberto.

## Preparacao da Exalted.846 (sem compilacao)
- Configuracoes: labels de estilo de avaliacao, idiomas, vibracao, som e Modo Livre receberam `Modifier.weight(1f)` em suas linhas para respeitar o espaco restante em telas estreitas e com fontes ampliadas.
- Teste estatico de regressao cobre as seis alocacoes de largura.
- Nao houve alteracao de preferencias, regras de negocio, nomes de campos ou botoes.
- Pendente: compilacao Kotlin e testes JVM; inspecao no emulador de fontes ampliadas e tela dividida.

## Preparacao da Exalted.847 (sem compilacao)
- Configuracoes: a linha `Ver Log de Erros` agora reserva a largura flexivel do rotulo com `Modifier.weight(1f)` ao lado do icone de aviso.
- Contrato de regressao ampliado para cobrir o rotulo de log junto das seis linhas ajustadas na Exalted.846.
- Mudanca estritamente de layout, sem alterar o acesso ao log, o estado das preferencias ou a logica de negocio.
- Pendente: compilacao e testes JVM; inspecao real em emulador de largura estreita, fonte ampliada e janela dividida.

## Consolidacao planejada Exalted.848 — fechamento da fase de ajustes conhecidos
- QR Code: geracao do bitmap com IntArray e transferencia unica setPixels, mantendo matriz ZXing, nivel H, margem, cores e dimensoes anteriores.
- QR vazio: nao tenta gerar imagem sem dados; mantem o codigo textual e a opcao de copiar.
- Regressao: contrato estatico cobre transferencia em lote, ausencia de setPixel individual e codigo nao vazio.
- Escopo preservado: 15 abas, tres tipos de Exaltado e regras de NPC/Encantos sem alteracoes nesta consolidacao. Ajustes responsivos das versoes 846 e 847 permanecem.
- Limites: contratos sao verificacoes estaticas, nao testes instrumentados nem comprovacao de desempenho medido. Build 848 e testes JVM ainda pendentes.
- Validacao pratica: abrir as 15 abas nos tres tipos; gerar NPCs e percorrer arvores de Encantos; abrir dialogs com fonte ampliada/tela dividida; testar QR, salvamento/restauracao e mapa/escala.
- Encerramento definitivo exige build/testes aprovados e verificacao funcional/visual em dispositivo; nao equiparar sucesso de CI a aplicativo 100% validado.

## Fase visual apos Exalted.848 — primeira consolidacao sem compilacao
- Paineis existentes: acabamento escuro em degradê vertical, contorno metalico discreto e cantos de 8dp via exaltedSectionPanel. Sem introduzir imagens, emblemas ou campos; demais modificadores neutros preservados.
- Simbolos exclusivos por template: Solar somente sol e raios solares (nunca dragoes ou luas); Sangue de Dragao somente dragao ORIENTAL, serpentino e sem asas ocidentais (nunca sol ou lua); Lunar somente lua/fases lunares (nunca sol ou dragoes). Elementos compartilhados sempre neutros.
- Sol e lua nao podem ter rosto, olhos ou boca. Nao adicionar dragoes ocidentais, wyverns ou silhuetas de lagartos alados.
- Alteracao de acabamento e apenas um primeiro passo visual; nao constitui remodelacao completa das 15 abas. Conferir contraste, toque, tamanho de fonte e limites de cada painel em dispositivo.

## Preparacao visual Exalted.850 (sem build automatico)
- Selecao de templates: cartoes com altura minima de 136dp, emblema e espacamentos responsivos quando a largura util for inferior a 360dp; titulo principal permite duas linhas e reticencias.
- A selecao continua usando icones exclusivos: Solar=tab_icon_sol, Sangue de Dragao=tab_icon_dragao, Lunar=tab_icon_lua. A arte do arquivo tab_icon_dragao ainda precisa de inspecao visual para confirmar anatomia oriental, pois a associacao de recurso nao comprova o desenho.
- Removidas linhas verticais decorativas nas laterais do fundo da selecao, conforme preferencia anterior; sem alterar os cartoes nem o logotipo oficial.
- Testes de contrato cobrem correspondencia de icones, largura responsiva e ausencia das linhas laterais; ainda nao substituem screenshot nem validacao no dispositivo.

## Preparacao visual seguinte a Exalted.850 (ainda sem compilacao)
- A tela de selecao nao deve assumir a identidade Solar antes de o usuario escolher o Tipo de Exaltado. Fundo agora usa gradiente escuro neutro; cada cartao continua com a paleta e o icone de seu proprio tipo.
- Teste de contrato garante ausencia do antigo brilho ExaltedBackdropGlow na tela de selecao e presenca do gradiente neutro.
- Nao houve alteracao das regras de negocio, das tres opcoes, dos callbacks nem dos 15 destinos de navegacao.
- Pendencias para aceite visual: verificar arte do dragao oriental no PNG, telas reais pequenas com fonte ampliada e remodelacao individual das 15 abas. O sucesso de compilacao da 850 nao valida estas pendencias.

## Proxima etapa: Aba 1 — Dados Pessoais (preparacao Exalted.852)
- Na secao de identidade, os tres campos existentes (Nome, Jogador, Conceito) passam para coluna com largura total quando o espaco disponivel e inferior a 390dp; em telas maiores, mantem a linha de tres colunas.
- Os mesmos callbacks updateNome, updateJogador e updateConceito permanecem nos dois modos, sem mudanca de regras ou persistencia.
- Teste estatico protege os dois modos e seus tres campos. Validacao visual e compilacao ainda pendentes.
- Esta e uma melhoria especifica da Aba 1, nao declaracao de remodelacao integral da aba ou das 15 abas.
