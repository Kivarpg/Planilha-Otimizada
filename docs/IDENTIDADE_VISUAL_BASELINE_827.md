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
