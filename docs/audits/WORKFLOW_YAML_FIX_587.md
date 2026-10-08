# Exalted.587
A versão 586 usou um heredoc Python dentro de `run: |`; as linhas do corpo escaparam da indentação do bloco YAML e invalidaram o workflow. A 587 remove o heredoc e usa `python3 -c` em uma única linha, preservando o diagnóstico dos testes sem quebrar a sintaxe YAML.
