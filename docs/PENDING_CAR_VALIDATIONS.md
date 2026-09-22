# Lista de Itens para Validação em Veículo Real

Este documento consolida as funcionalidades testadas e validadas em ambiente real (Veículo + Adaptador ELM327/OBD-II + Central Multimídia / Tablet Android).

---

## 1. Conexão Bluetooth & Handshake
- [x] **Auto-Conexão ao Iniciar (v3.9.1)**: Validado no veículo real / fechamento e reabertura do App.
- [x] **Reconexão Automática em Segundo Plano (v3.9.4)**: Validado no simulador/veículo real com retomada do streaming no Painel.
- [x] **Handshake Dinâmico CAN 29-bit / 11-bit (v3.9.6)**: Protocolo padrão definido para `AUTO` (`AT SP 0`) com busca automática do bus CAN do veículo.
- [ ] **Fluxo de Conexão com Tentativas (1/3, 2/3, 3/3)**: Confirmar tempo visual na central multimídia em frio.

---

## 2. Otimização de Leitura & Throughput
- [x] **Remoção do PID 0x015E (Fuel Rate) (v3.9.6)**: Eliminado envio de `015E` para extinguir o timeout de 500ms `NODATA`. Consumo calculado 100% via `MAF`.
- [x] **Aumento de Frequência de Polling (15-20Hz) (v3.9.6)**: Mapeamento de `Throttle` (50ms) e respostas em tempo real otimizadas.
- [ ] **Ajuste Fino do Velocímetro (+% Offset)**: Testar em rodovia/rua comparando a velocidade digital do App com o painel de instrumentos do veículo (ex: offset de +4%).
- [ ] **RPM Preditivo**: Avaliar a suavidade da agulha de RPM com acelerações rápidas no pedal.

---

## 3. Desempenho e Estabilidade
- [ ] **Taxa de Amostragem (Hz)**: Monitorar o log de diagnósticos e a resposta em milissegundos (SPS) dos sensores `RPM`, `SPEED`, `MAF`, `TEMP`, `VOLTS`.
- [ ] **Penalty Box (Circuit Breaker)**: Garantir que PIDs não suportados pelo veículo sejam ignorados por 30s sem travar a leitura do RPM.
