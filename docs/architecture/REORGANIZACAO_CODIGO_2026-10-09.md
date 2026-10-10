# Reorganização estrutural do Exalted — 2026-10-09

## Inventário verificado
- 511 arquivos Kotlin no repositório, incluindo testes.
- app/src/main/java/com/example/data: 148 arquivos.
- app/src/main/java/com/example/ui/tabs: 47 arquivos.
- app/src/main/java/com/example/viewmodel: 21 arquivos.
- app/src/main/java/com/example/model: 18 arquivos.
- app/src/main/java/com/example/ui/components: 17 arquivos.
- app/src/main/java/com/example/iniciativas: 14 arquivos.
- app/src/test/java/com/example/data: 153 arquivos.

## Diretrizes
1. Preservar comportamento e regras de Exalted; não renomear funcionalidades ou abas.
2. Manter Solares, Sangue de Dragão e Lunares com igual prioridade.
3. Não mover arquivos Kotlin em massa: caminhos, package, imports e referências devem ser atualizados juntos.
4. Organizar progressivamente por responsabilidade: model (entidades), data (persistência e catálogos), domain (regras), viewmodel (estado/orquestração), ui/tabs (telas), ui/components (componentes).
5. Separar UI de regras de construção de NPCs e persistência. Não misturar refatoração estrutural com otimizações de algoritmo.
6. Preservar testes de equivalência para Encantos, pré-requisitos, XP, feitiçaria e NPCs.
7. Compilar somente após autorização explícita.

## Alterações executadas nesta rodada
- SheetContentArea.kt: organização de imports, formatação e extração do pincel do fundo opaco.
- Implementações experimentais do panorama removidas anteriormente; não reintroduzir.

## Pendências — executar em lotes
1. Catalogar arquivos por domínio e suas dependências; identificar ciclos de importação.
2. Identificar arquivos duplicados, obsoletos e não referenciados, sem excluir por suposição.
3. Separar os arquivos de UI excessivamente extensos em funções e componentes internos mantendo APIs.
4. Extrair regras de negócio de ViewModels grandes com testes de regressão.
5. Consolidar utilitários duplicados de data e normalizar nomes de arquivos.
6. Executar testes unitários e compilação autorizada; corrigir eventuais regressões.

## Critério de conclusão
A reorganização somente estará concluída quando os lotes forem executados e os testes e a compilação forem aprovados. Este documento não representa conclusão da refatoração total.

## Segunda rodada — checkpoint pré-compilação
- SheetScreen.kt: removida importação não utilizada de OnyxTexturedBackground; composição Scaffold reorganizada sem mudança de parâmetros.
- Arquivos extensos identificados: EncounterNpcCard.kt (74.895 bytes), EncounterGeneratorTab.kt (55.798), MapTab.kt (44.592), WeaponSection.kt (43.622).
- Refatoração da Aba 11 deliberadamente adiada até haver cobertura de testes e validação de comportamento.
- Nenhuma mudança intencional em lógica de XP, Encantos, NPCs ou persistência.

## Próximo portão
**Solicitar uma compilação de validação antes de prosseguir com extrações estruturais maiores.**
A compilação valida sintaxe e dependências, mas não substitui testes de regressão nem validação visual.
