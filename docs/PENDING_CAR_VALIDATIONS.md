# Lista de Itens para Validação em Veículo Real

Este documento consolida as funcionalidades implementadas e testadas via emulador/simulador que necessitam de teste e validação prática em ambiente real (Veículo + Adaptador ELM327/OBD-II + Central Multimídia / Tablet Android).

---

## 1. Conexão Bluetooth & Handshake
- [ ] **Fluxo de Conexão com Tentativas (1/3, 2/3, 3/3)**: Verificar tempo de resposta do chip ELM327 durante a rotina de boot na tela de status.
- [ ] **Warm-up Delay (1.5s)**: Confirmar estabilização do socket SPP em adaptadores de baixo custo (clones v1.5 / v2.1).
- [ ] **Auto-Conexão ao Iniciar**: Validar o fluxo de abertura do app com reconexão automática direta para o último dispositivo salvo.
- [ ] **Transição de Tela (500ms pós-Handshake)**: Confirmar navegação suave para o Dashboard após a mensagem de "Conectado!".

---

## 2. Indicadores do Painel e Ajustes Visuais
- [ ] **Ajuste Fino do Velocímetro (+% Offset)**: Testar em rodovia/rua comparando a velocidade digital do App com o painel de instrumentos do veículo (ex: offset de +4%).
- [ ] **RPM Preditivo**: Avaliar a suavidade da agulha de RPM com acelerações rápidas no pedal.
- [ ] **Barra de RPM (ArchedRpmGauge) & Shift Light**: Validar o piscar de alerta (Blink) em diferentes rotações no modo Econômico e Performance.

---

## 3. Desempenho e Estabilidade
- [ ] **Taxa de Amostragem (Hz)**: Monitorar o log de diagnósticos e a resposta em milissegundos (SPS) dos sensores `RPM`, `SPEED`, `MAF`, `TEMP`, `VOLTS`.
- [ ] **Penalty Box (Circuit Breaker)**: Garantir que PIDs não suportados pelo veículo sejam ignorados por 30s sem travar a leitura do RPM.
