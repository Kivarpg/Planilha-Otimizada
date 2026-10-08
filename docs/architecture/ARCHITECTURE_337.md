# Arquitetura 337

## Camadas
- `model`: contratos e estado de domínio, sem UI.
- `data`: catálogos, persistência, geração e serviços de domínio.
- `viewmodel`: coordenação de estado e ações da ficha/encontros.
- `feature/charmtree`: árvore de pré-requisitos isolada como feature.
- `ui/components`: componentes visuais reutilizáveis.
- `ui/tabs`: telas das abas; devem delegar regras para data/viewmodel.
- `iniciativas`: domínio/UI de Conflito, mantido isolado.
- `oldrealm`: tradutor e renderização Old Realm, mantido isolado.

## Regra de dependência
UI -> ViewModel -> Data/Model. Componentes visuais não devem executar persistência ou I/O.
Operações de disco devem permanecer em `Dispatchers.IO`; cálculos caros disparados pela UI devem ser memorizados ou executados fora da thread principal.

## Limpeza
A 337 remove imports comprovadamente não referenciados. Arquivos candidatos a remoção não foram apagados apenas por heurística de nome: em Compose/Kotlin, arquivos podem conter top-level declarations usadas sem referência ao nome do arquivo.
