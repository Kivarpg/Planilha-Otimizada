# Exalted — baseline preventiva da identidade visual

Base compilada confirmada: Exalted.830 (informada pelo usuario; execucao anterior da 829 confirmada no CI).
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
