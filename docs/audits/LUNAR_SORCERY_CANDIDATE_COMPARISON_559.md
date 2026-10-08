# Exalted.559 — comparação Lunar Mental puro × Feitiçaria

Base: Exalted.558, confirmado compilado.

- Integra a política 557/558 ao pipeline Lunar sem reutilizar a implementação
  Solar/DB, pois Lunares compram Encantos por Atributo e rotas de Arquétipo.
- A seleção autoritativa com Feitiçaria continua usando o Random normal.
- Quando Mental automático explora magia, um candidato puro contrafactual é
  construído com seed local estável derivado da ficha/Focus/traços, sem consumir
  RNG adicional do fluxo principal.
- A qualidade Lunar usa os níveis reais dos Atributos + sinergia ECS limitada
  por EncounterBuildQuality.
- Feitiçaria automática só vence com qualidade estritamente maior.
- Empate mantém o candidato puro.
- Focus explícito continua fora dessa competição automática.
- Físico preserva sua exceção estrutural histórica.
- A decisão de Quimera/segunda Forma da rota autoritativa permanece intacta.

Testes adicionados:
- seed contrafactual determinístico e independente da ordem do Map;
- qualidade Lunar favorece Encantos ligados ao Atributo efetivamente maior.
