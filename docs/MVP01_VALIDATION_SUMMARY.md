# Resumo de Validação - MVP-01/02 (v3.2.0)

## 1. Objetivo
Validar a conectividade Bluetooth resiliente, o motor de polling de alta performance e a flexibilidade de multi-layout para Tablet e Central Multimídia.

## 2. Status de Desenvolvimento
- [x] **Comunicação**: Turbo Polling (v3.2.0) com leitura em blocos e timeout de 500ms.
- [x] **Geometria**: Posição Y da barra de RPM fixada em 50.0 (topo).
- [x] **Layout**: Matriz 10x2 (10 backgrounds x 2 dispositivos) implementada e validada.
- [x] **Navegação**: Fluxo Dashboard <-> Perfil <-> Visual operante.

## 3. Plano de Testes Práticos (Checklist para Ademar)
Como o app agora está na **v3.2.0**, realize os seguintes testes no veículo real:

### [A] Teste de Estabilidade Bluetooth
1. [ ] Conectar ao ELM327 e abrir o Dashboard.
2. [ ] Dar a partida no motor. Verificar se a conexão retoma automaticamente após a queda de tensão da ignição.
3. [ ] Observar se há "congelamentos" no ponteiro de RPM durante acelerações bruscas.

### [B] Teste de Multi-Layout
1. [ ] No menu **Personalizar Visual**, selecionar **Tablet** e salvar uma posição para o Velocímetro.
2. [ ] Mudar para **Multimídia (Menor)** e salvar uma posição DIFERENTE.
3. [ ] Alternar entre os modos e confirmar se as posições são preservadas de forma independente.

### [C] Teste de Performance (SPS)
1. [ ] No menu **Ajustes de Performance**, setar RPM para **20ms**.
2. [ ] Abrir o **Benchmark (Log)** e verificar se a entrada de dados do RPM está ocorrendo de forma contínua e rápida.

## 4. Conclusão
O sistema está estável, documentado e pronto para a fase de testes reais. A versão exibida na UI deve ser **v3.2.0**.
