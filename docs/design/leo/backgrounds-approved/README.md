# Integração dos backgrounds aprovados — 13/10/2026

Base: Exalted.947 (compilação bem-sucedida). Versão não incrementada; sem compilação autorizada nesta etapa.

O ZIP fornecido já contém três PNGs independentes. Foram copiados byte a byte, sem geração, recompressão, redimensionamento ou edição artística:
- Solar → `app/src/main/res/drawable-nodpi/leo_scene_solar.png` (557 × 941).
- Sangue de Dragão → `app/src/main/res/drawable-nodpi/leo_scene_dragon.png` (558 × 941).
- Lunar → `app/src/main/res/drawable-nodpi/leo_scene_lunar.png` (557 × 941).

`LeoIdentity.kt`: LeoScene usa ContentScale.Fit com fundo preto; preserva a imagem inteira e sua proporção. Espaço excedente recebe preto, inclusive em áreas horizontais como o cabeçalho. Não usa Crop nem estica a arte. O mapa exclusivo por tipo foi preservado.

Nenhum botão, fonte, moldura, ícone, regra ou funcionalidade foi alterado. As superfícies escuras e os controles existentes continuam sobre o cenário.

As imagens preview são composições estáticas aproximadas em largura de 390 px, com conteúdo rolável estendido, montadas com os arquivos de produção e ícones originais. NÃO são capturas Android nem validação de execução: não há SDK/emulador disponível. Tipografia e espaçamentos podem diferir. A prancha permite conferir os cenários, proporção, legibilidade aproximada e associação a cada tipo.

Validação: PNGs idênticos aos anexos; somente quatro arquivos de produção alterados; sintaxe Kotlin conferida; nenhuma compilação iniciada.
