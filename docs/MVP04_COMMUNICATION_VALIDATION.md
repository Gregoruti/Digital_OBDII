# Resumo de Validação - MVP-04 (v3.4.0)

## 1. Objetivo
Validar o novo Motor de Polling Hierárquico e as configurações avançadas de comunicação em hardware real (ELM327 Bluetooth).

## 2. Status de Desenvolvimento
- [x] **Agendador Hierárquico**: Ratio 8:1 implementado (Prioridade para RPM/Speed/MAF).
- [x] **Multi-PID**: Suporte a comandos agrupados (ex: `010C0D101`).
- [x] **Interface de Comunicação**: Nova tela com terminal e benchmark.
- [x] **Circuit Breaker**: Interrupção manual da serial.
- [x] **Protocolos CAN**: Seleção manual de protocolo (AT SP).

## 3. Plano de Testes no Veículo (Checklist para Ademar)

### [A] Handshake e Protocolo
1. [ ] Abrir tela de **Comunicação**.
2. [ ] Selecionar o protocolo `CAN 11-bit 500k` (Padrão Honda).
3. [ ] Observar o log de inicialização: deve conter `AT SP 6` e `41 00` (Handshake).
4. [ ] Verificar se os dados começam a fluir no Dashboard.

### [B] Stress Test e Throughput
1. [ ] Na tela de Comunicação, observar o **Throughput (Hz)**.
2. [ ] Ativar o **Modo Turbo (Multi-PID)**.
3. [ ] Verificar se o Throughput aumenta significativamente.
4. [ ] Observar a **Taxa de Erro %**. Se subir acima de 5%, o adaptador pode não suportar o modo turbo.

### [C] Intercalação (Priority Interleaving)
1. [ ] Definir o **Ratio** para `15:1`.
2. [ ] Verificar se o ponteiro de RPM fica extremamente reativo enquanto o Voltímetro demora mais para atualizar.

### [D] Circuit Breaker
1. [ ] No Dashboard com dados fluindo, voltar para Comunicação e clicar em **PARAR COMUNICAÇÃO**.
2. [ ] Voltar ao Dashboard: os dados devem estar "congelados".
3. [ ] Clicar em **INICIAR COMUNICAÇÃO** e ver a fluidez retornar.

## 4. Conclusão
O motor de comunicação v3.4.0 é o mais avançado até agora. O sucesso deste teste permitirá a calibração final do sistema de telemetria.
