# Lista de Itens para Validação em Veículo Real

Este documento consolida as funcionalidades testadas e validadas em ambiente real (Veículo + Adaptador ELM327/OBD-II + Central Multimídia / Tablet Android), bem como pendências críticas mapeadas para as próximas versões.

---

## 1. Conexão Bluetooth & Handshake
- [x] **Auto-Conexão ao Iniciar (v3.9.1)**: Validado no veículo real / fechamento e reabertura do App.
- [x] **Reconexão Automática em Segundo Plano (v3.9.4)**: Validado no simulador/veículo real com retomada do streaming no Painel após interrupção.
- [x] **Fluidez e Redução de Latência Visual (v3.9.7)**: Validado que as animações em 60ms e a leitura bloqueante acabaram com o lag do ponteiro.
- [x] **Inicialização Autônoma (Handshake a Frio)**: Validado que com as diretivas AT AL e SP 0, a ECU acorda e responde sozinha sem a necessidade de um aplicativo terceiro.

---

## 2. Indicadores do Painel e Calibração
- [x] **Remoção do PID 0x015E (Fuel Rate)**: Eliminado envio de `015E` para extinguir o timeout de 500ms `NODATA`. Consumo calculado 100% via `MAF`.
- [x] **Aumento de Frequência de Polling (15-20Hz)**: Mapeamento de `Throttle` (50ms) e prioridade máxima para RPM garantiram atualização imediata na tela.
- [x] **Calibração de Consumo (Fator MAF)**: Ajustes de calibração testados; app reflete com precisão os valores corrigidos de km/L.
- [x] **Ajuste Fino do Velocímetro (+% Offset)**: Velocímetro bate exatamente com o velocímetro analógico após calibração (+4% padrão).

---

## 3. Desempenho, Estabilidade e Interface (Atualizações Recentes)
- [x] **Custom Icons & Layout Flexível (v4.6.x)**: Validado em Multimídia. O posicionamento de imagens customizadas salva e é carregado com precisão; funções clicáveis como `TOGGLE_AUDIO` operam em tempo real.
- [x] **Integração do Motor de Áudio (Engine Sound)**: Validação positiva no carro. Sem lentidão ou travamentos gerados pelo som; alternância rápida via área de toque na tela/ícones flutuantes operando suavemente e consumindo pouca CPU por conta do NDK C++.
- [x] **Taxa de Amostragem (Hz) e Penalty Box**: Latência excelente e imunidade a PIDs desconhecidos; sensores ausentes caem na "box" isolada de forma transparente, não prejudicando a fluidez da RPM e velocidade reais.