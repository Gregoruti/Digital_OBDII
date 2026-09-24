# Lista de Itens para Validação em Veículo Real

Este documento consolida as funcionalidades testadas e validadas em ambiente real (Veículo + Adaptador ELM327/OBD-II + Central Multimídia / Tablet Android), bem como pendências críticas mapeadas para as próximas versões.

---

## 1. Conexão Bluetooth & Handshake
- [x] **Auto-Conexão ao Iniciar (v3.9.1)**: Validado no veículo real / fechamento e reabertura do App.
- [x] **Reconexão Automática em Segundo Plano (v3.9.4)**: Validado no simulador/veículo real com retomada do streaming no Painel após interrupção.
- [x] **Fluidez e Redução de Latência Visual (v3.9.7)**: Validado que as animações em 60ms e a leitura bloqueante acabaram com o lag do ponteiro.
- [ ] **Inicialização Autônoma (Handshake a Frio)**: Em `v4.1.0`, foi aplicada a engenharia reversa do RevHeadz (`AT AL`, Timeouts Dinâmicos). **Validar:** Desligar o carro, remover o adaptador, plugar novamente e abrir *apenas* o Digital OBD-II para ver se ele consegue acordar a ECU sozinho em `AT SP 0`.

---

## 2. Indicadores do Painel e Calibração
- [x] **Remoção do PID 0x015E (Fuel Rate) (v3.9.6)**: Eliminado envio de `015E` para extinguir o timeout de 500ms `NODATA`. Consumo calculado 100% via `MAF`.
- [x] **Aumento de Frequência de Polling (15-20Hz) (v3.9.6)**: Mapeamento de `Throttle` (50ms) e prioridade máxima para RPM (ciclos ímpares) garantiram atualização imediata na tela.
- [ ] **Calibração de Consumo (Fator MAF) (v4.0.0)**: Testar na tela de calibração se a proporção `App / Real` calcula e ajusta de fato o consumo instantâneo no Painel após salvar.
- [ ] **Ajuste Fino do Velocímetro (+% Offset)**: Testar em rodovia comparando a velocidade digital do App (+4% default) com o painel de instrumentos analógico do veículo.

---

## 3. Desempenho e Estabilidade
- [ ] **Taxa de Amostragem (Hz)**: Monitorar o log de diagnósticos e a resposta em milissegundos (SPS) dos sensores `RPM`, `SPEED`, `MAF`, `TEMP`, `VOLTS`.
- [ ] **Penalty Box (Circuit Breaker)**: Garantir que PIDs não suportados pelo veículo sejam ignorados por 30s sem travar a leitura do RPM.
