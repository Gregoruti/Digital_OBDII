# Camada de Dados, OBD e Bluetooth

A comunicação via dongle **ELM327** no Android é o gargalo clássico. Todo o pacote `data` é voltado a lidar com latência, resiliência e estabilização do sinal.

## Bluetooth Connection Manager
- Gerencia o socket Bluetooth (SPP_UUID).
- Substitui delay/polling passivo por leitura nativa bloqueante (`inp.read()`) com timeout embutido para maximizar as transferências.
- Executa tentativas de handshake e reconexões (Watchdog).

## Protocolo ELM327 / OBD-II
- **Inicialização (`Elm327Init`)**: Rotina de limpeza da porta serial e negociação do protocolo (AT SP 0 ou ISO 15765-4 11-bit/500k).
- **PIDs Registrados (`ObdCommand`)**: `RPM`, `Speed`, `CoolantTemp`, `ControlModuleVoltage`, `MafRate`, e `ThrottlePosition`.
- **Parser (`ObdResponseParser`)**: Recebe a string raw HEX, trata retornos de erros (`NO DATA`, `?`) e converte os bytes de dados em valores inteiros ou decimais.

## Polling Engine
- O `ObdPollingEngine` não faz um round-robin simples. Utiliza priorização hierárquica e "Priority Interleaving".
- PIDs cruciais como `RPM` são requisitados nos ciclos ímpares (maior frequência), enquanto PIDs secundários são distribuídos nos ciclos pares.
