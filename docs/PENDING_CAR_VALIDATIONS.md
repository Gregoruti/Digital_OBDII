# Lista de Itens para Validação em Veículo Real

Este documento consolida as funcionalidades testadas e validadas em ambiente real (Veículo + Adaptador ELM327/OBD-II + Central Multimídia / Tablet Android), bem como pendências críticas mapeadas para as próximas versões.

---

## 1. Conexão Bluetooth & Handshake
- [x] **Auto-Conexão ao Iniciar (v3.9.1)**: Validado no veículo real / fechamento e reabertura do App.
- [x] **Reconexão Automática em Segundo Plano (v3.9.4)**: Validado no simulador/veículo real com retomada do streaming no Painel após interrupção.
- [x] **Fluidez e Redução de Latência Visual (v3.9.7)**: Validado que as animações em 60ms e a leitura bloqueante acabaram com o lag do ponteiro.
- [ ] **⚠️ CRÍTICO: Inicialização Autônoma do Protocolo OBD (Handshake)**: O app atualmente "pega carona" na inicialização feita por outros aplicativos (em 29-bit 500k). Alterações de protocolo e handshakes `0100` a frio não estão conseguindo acordar a ECU sozinhos. Requer refatoração profunda em `Elm327Init.kt` na v4.0.

---

## 2. Otimização de Leitura & Throughput
- [x] **Remoção do PID 0x015E (Fuel Rate) (v3.9.6)**: Eliminado envio de `015E` para extinguir o timeout de 500ms `NODATA`. Consumo calculado 100% via `MAF`.
- [x] **Aumento de Frequência de Polling (15-20Hz) (v3.9.6)**: Mapeamento de `Throttle` (50ms) e prioridade máxima para RPM (ciclos ímpares) garantiram atualização imediata na tela.
- [ ] **Ajuste Fino do Velocímetro (+% Offset)**: Testar em rodovia comparando a velocidade digital do App (+4% default) com o painel de instrumentos analógico do veículo.

---

## 3. Desempenho e Estabilidade
- [ ] **Taxa de Amostragem (Hz)**: Monitorar o log de diagnósticos e a resposta em milissegundos (SPS) dos sensores `RPM`, `SPEED`, `MAF`, `TEMP`, `VOLTS`.
- [ ] **Penalty Box (Circuit Breaker)**: Garantir que PIDs não suportados pelo veículo sejam ignorados por 30s sem travar a leitura do RPM.
