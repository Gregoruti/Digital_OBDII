# Protocolo de Comunicação OBD-II Resiliente (Digital OBD-II)

Este documento descreve os padrões técnicos implementados entre as versões 1.9.3 e 2.1.4 para garantir estabilidade e alta performance em adaptadores ELM327 Bluetooth (Clones e Emuladores).

## 1. Arquitetura de Transporte (Camada Data)
Para suportar o barramento CAN do Honda Civic 1.8 (2011), o motor de comunicação utiliza:

- **Sincronização por Mutex**: Uso de `kotlinx.coroutines.sync.Mutex` no `BluetoothConnectionManager` para garantir que comandos nunca se sobreponham no canal serial (Thread-Safety).
- **Limpeza de Buffer (Pre-fetch)**: Antes de cada `write`, o sistema executa um loop de `read` para drenar bytes residuais (lixo de buffer), garantindo que a resposta recebida pertença ao comando enviado.
- **Protocolo Síncrono Estrito**: O envio de um comando só é finalizado ao encontrar o caractere prompt `>` do ELM327 ou atingir um timeout de 2000ms.

## 2. Boot Robusto v2.0
A sequência de inicialização foi otimizada para "Clones" e emuladores, travando o protocolo para evitar o delay de "Bus Search":
1. `AT Z` (Reset) -> Espera "ELM327"
2. `AT D` (Defaults) -> Limpa estados anteriores
3. `AT E0` (Echo Off) -> Remove eco do comando
4. `AT L0` (Linefeeds Off) -> Compacta a resposta
5. `AT S0` (Spaces Off) -> Reduz tráfego serial
6. `AT H0` (Headers Off) -> Foca apenas no Payload
7. `AT AT 1` (Adaptive Timing) -> Ajuste automático de latência
8. `AT SP 6` (Protocol CAN 500k) -> Específico para Civic G8/G9
9. `AT SH 7DF` (Header Broadcast) -> Endereço padrão de diagnóstico
10. `0100` (Handshake) -> Acorda a ECU do motor

## 3. Parser de Alta Resiliência
O `ObdResponseParser` utiliza um motor de busca "Fuzzy" e limpeza via Regex:
- **Regex `[^0-9A-F.]`**: Remove qualquer lixo (espaços, pontos, vírgulas) injetado por adaptadores de má qualidade.
- **Busca por Eco**: O parser localiza o padrão `(Mode + 40) + PID` (ex: `410C` para RPM) em qualquer posição da string, permitindo extração de dados mesmo se o comando de ocultar Headers/Eco falhar.
- **Validação de Payload**: Se o adaptador retornar o Eco mas não os dados (caso de valores `NaN` em emuladores), o sistema identifica como `ADAPTER_ERROR`, evitando que o app processe valores nulos ou quebrados.

## 4. Otimização de Performance (Streaming)
- **Emissão Imediata**: O `ObdPollingEngine` emite dados via `Flow` sensor a sensor, sem esperar o fim do ciclo de polling.
- **Yielding**: Substituição de `delay(10ms)` por `yield()`, permitindo que o polling rode na velocidade limite do hardware Bluetooth (baud rate).

---
*Documento consolidado na v2.2.0*
