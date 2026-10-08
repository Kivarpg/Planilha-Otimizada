# Exalted.585
Correção preventiva do ciclo de testes da 584.
A restrição de Assinatura agora é propagada também à busca exata/beam de recuperação do seletor compartilhado; antes esses fallbacks podiam reconstruir uma seleção sem consultar a política nova. O caminho normal permanece inalterado. Foi acrescentada regressão do filtro otimizado e o CI agora expõe os XMLs que contêm failure/error para que um futuro log não termine apenas em TaskExecutionException.
