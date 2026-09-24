# Estado Atual do Projeto e Preparação para v4.0

> **Documento de Transição - Rumo à v4.0**
> Data: Atualização via Gemini 3.1 Preview (Android Studio)
> Versão atual do repositório: v3.9.9

Este documento resume as conquistas arquiteturais e de performance alcançadas ao longo das versões 3.x e documenta as pendências e desafios técnicos mapeados que servirão de fundação para o desenvolvimento da **versão 4.0**.

---

## 1. Conquistas e Mecanismos Vencedores (O que funcionou muito bem)

Ao longo do desenvolvimento da versão 3.9.x, enfrentamos severos desafios de latência (atraso de quase 1 segundo entre o acelerador do carro e a atualização visual do painel). A fluidez (Zero Lag) foi alcançada com sucesso através de **3 pilares técnicos fundamentais**:

### 1.1 Leitura Bloqueante Nativa (Bluetooth)
- **O Problema Original:** Usar `inp.available() > 0` junto com `delay(2)` (ou `yield()`) na thread de leitura do Bluetooth SPP causava "engasgos", pois o `delay()` no Android suspende a corrotina por tempo incerto (10ms a 20ms dependendo do ciclo de CPU), atrasando a leitura.
- **A Solução Vencedora:** Substituir pelo uso da função nativa bloqueante do Java IO `inp.read(buffer)` dentro de um `Dispatchers.IO`. Essa abordagem pausa a thread nativamente no kernel e acorda no microssegundo exato em que os dados chegam pelo rádio Bluetooth, zerando a latência de processamento interno.

### 1.2 Agendamento por Priority Interleaving (Motor de Polling)
- **O Problema Original:** O Round-Robin varria os 6 sensores igualmente (RPM, Speed, MAF, Throttle, Temp, Volts). Como cada comando OBD via Bluetooth demora cerca de ~80ms a 100ms, o `RPM` só era consultado a cada ~500ms (2 vezes por segundo), o que deixava o ponteiro truncado.
- **A Solução Vencedora:** Implementar o **Priority Interleaving**. O sensor de `RPM` (o mais crítico visualmente) passou a ser lido em **todos os ciclos ímpares**, garantindo uma frequência dedicada de ~10Hz a 15Hz. Os demais sensores dividem os ciclos pares.
- **Otimização Crítica:** A remoção do PID `015E` (Fuel Rate), que não era suportado por muitos veículos e causava timeout (Timeout de 500ms de `NODATA`), foi essencial. O consumo agora é calculado de modo impecável e instantâneo via sensor `MAF`.

### 1.3 Animação Linear Ultra-rápida (Compose UI)
- **O Problema Original:** O Jetpack Compose usava `animateIntAsState` com configuração de mola (`Spring.StiffnessMediumLow`). Embora visualmente suave para elementos estáticos, em um painel automotivo essa "suavização" causava uma sensação de "arrasto" de quase 400ms.
- **A Solução Vencedora:** Substituir a mola por uma animação linear explícita de **60ms** (`tween(60, LinearEasing)`). Os números do display digital e o arco do RPM agora respondem de modo estrito e instantâneo ao pacote de dados recém-chegado.

---

## 2. Débitos Técnicos e Roadmap para a v4.0

### 2.1 Problema Identificado: Inicialização Autônoma de Protocolos (Handshake)
Durante os testes de campo, o app demonstrou o seguinte comportamento com o adaptador físico ELM327:
- **Comportamento Atual:** O app lê dados perfeitamente no protocolo **29-bit 500k**. Contudo, percebeu-se que essa leitura só funciona porque um **outro aplicativo** (de terceiros) foi usado previamente para "aquecer" / "inicializar" o ELM327 neste protocolo.
- **O Problema:** Quando o App "Digital OBD-II" tenta alterar ou forçar a seleção de protocolos por conta própria (ex: mudando a configuração no Perfil e acionando o handshake de boot `AT Z`, `AT SP x`, `0100`), ele **não consegue estabelecer a conexão**. A inicialização real do adaptador falha. O app atualmente "pega carona" no estado deixado na memória do adaptador por outros apps.
- **Objetivo v4.0:** Refatorar a classe `Elm327Init.kt` e estudar os protocolos de aquecimento de ECU. Será necessário entender precisamente a sequência de comandos `AT` necessária para acordar o barramento CAN nativamente (ex: uso de `AT SH 7DF`, configurações de Wakeup, testes exaustivos dos comandos de timeout `AT ST`).

### 2.2 Lista de Pendências Prioritárias para v4.0
1. **Engenharia Reversa de Boot ELM327:** Entender qual comando o app de terceiros está enviando que nosso app não está para inicializar com sucesso redes CAN.
2. **Tratamento de Desconexões Agressivas:** Validar se os 3 timeouts seguidos em `BluetoothConnectionManager` conseguem limpar perfeitamente recursos presos do rádio Bluetooth do celular quando o carro é desligado.
3. **Calibração do Fator de Velocidade:** O Fator Default (+4%) agora vem habilitado, mas precisará de uma calibração fina (possivelmente integrando leituras de GPS do celular no futuro para comparação).
4. **Interface (UX/UI):** Refinar o Painel e organizar a tela de configuração de comunicação que agora abriga muitas flags técnicas (Headers, Spaces, Timeouts).
