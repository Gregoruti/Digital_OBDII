# Análise de Engenharia Reversa (RevHeadz Boot Sequence)

Ao comparar a sequência de inicialização do aplicativo "RevHeadz" com o comportamento do nosso "Digital OBD-II", identificamos divergências cruciais que explicam por que o outro app consegue "acordar" o barramento CAN do veículo e nós não.

## 1. O que o App RevHeadz Faz

Log extraído:
1. `AT Z` (Reset) - *Resposta: ELM327 v2.1*
2. `AT SP 0` (Set Protocol Auto) - *Resposta: OK*
3. `AT AL` (Allow Long messages) - *Resposta: OK* (Nós não usamos isso!)
4. `AT E0` (Echo Off) - *Resposta: OK*
5. `AT L0` (Linefeeds Off) - *Resposta: OK*
6. `AT S0` (Spaces Off) - *Resposta: OK*
7. `AT H0` (Headers Off) - *Resposta: OK*
8. `AT ST 80` (Set Timeout para 128 x 4ms = ~512ms) - *Resposta: OK*
9. `01 00` (Handshake ECU) - *Resposta: 4100FFFFFFFF* (Demorou 150ms! Rápido)
10. `01 0C` (RPM Test) - *Várias requisições sucessivas para aquecer a ECU*
11. `AT ST 32` (Reduz o Timeout drasticamente para 50 x 4ms = ~200ms) - *Otimização de polling pós-handshake!*
12. (Início de Multi-PIDs): `01 0C 11`, `01 0C 0D`...

## 2. O que o Digital OBD-II Fazia (Problemas)

1. **A Ordem Importa**: Nós enviávamos os comandos de formatação (`AT E0`, `AT S0`, etc.) *antes* de definir o protocolo (`AT SP 0`). No ELM327, mudar o protocolo pode causar um reset parcial que perde formatações. O RevHeadz manda o `AT SP 0` logo de cara.
2. **`AT AL` Ausente**: `AT AL` permite mensagens maiores que 7 bytes. Redes CAN modernas (29-bit) *exigem* isso para não truncar pacotes e retornar `NO DATA`.
3. **Timeout Fixo vs Dinâmico**: Nós mandávamos `AT ST 32` (Timeout curto) *antes* do Handshake `01 00`. O Handshake envolve "SEARCHING..." na rede e demora muito na primeira vez. O RevHeadz sabiamente manda um timeout altíssimo (`AT ST 80`, mais de meio segundo) *apenas para o boot*, e depois abaixa para `AT ST 32` para ganhar velocidade no RPM.
4. **Comando `AT D` (Default)**: Nós enviamos `AT D`, que zera quase tudo e é desnecessário se já mandamos `AT Z`.

## 3. Plano de Ação para a v4.1.0

Vamos reescrever a sequência dinâmica `getDynamicBootSequence` no `Elm327Init.kt` para imitar o comportamento exato que acordou a rede CAN no seu carro:

1. `AT Z`
2. `AT SP 0` (O protocolo salvo no perfil, que agora é AUTO por padrão)
3. `AT AL` (CRÍTICO)
4. Formatações (`AT E0`, `AT L0`, `AT S0`, `AT H0`)
5. Timeout Alto Provisório (`AT ST 80` ou `AT ST FF`)
6. Handshake `01 00`
7. (Ajuste para o Timeout final escolhido pelo usuário, default 32) `AT ST 32`