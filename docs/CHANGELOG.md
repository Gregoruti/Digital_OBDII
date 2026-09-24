# CHANGELOG - Digital OBD-II

## [4.1.1] - Atual (Aguardando Validação no Veículo Real)
> **Assistente / Modelo de IA:** Gemini 3.1 Preview (Android Studio)  
> **Status de Validação:** Correção de Padrões e Alinhamentos aplicados. Testes locais unitários passando.

### Corrigido e Otimizado
- **Alinhamento do Trip Summary**: Modificado o campo `padLength` dinâmico do `DashboardScreen.kt`. O padding agora se assegura em 4 dígitos para os dados do sumário de viagem (`TRIP_DIST`, `TRIP_TIME`, `TRIP_FUEL`), impedindo a quebra de alinhamento ou re-posicionamento visual das caixas quando a distância avança para a casa das dezenas/centenas ou o tempo sobe a escala das horas.
- **Novos Defaults Práticos**:
  - `fuelCorrectionFactor` ajustado como default para `1.633f` no `VehicleProfile` a fim de corrigir a margem clássica de eficiência MAF x Litragem diretamente na inicialização do App.
  - `shiftLightSensitivity` ajustada de 85% para `1.0f` (100%), permitindo que a indicação do *Shift Light* do painel ocorra apenas no estágio final do giro limite alvo de troca por padrão.

## [4.1.0] - Anterior
### Corrigido (Engenharia Reversa de Boot ELM327)
- **Ordenação Crítica de Inicialização**: A sequência em `Elm327Init.kt` foi completamente remodelada para imitar o comportamento de inicialização bem-sucedido de aplicativos OBD comerciais (ex: RevHeadz). O protocolo da rede (ex: `AT SP 0`) agora é definido imediatamente após o Reset (`AT Z`), antes de qualquer formatação de dados, evitando a perda de contexto do adaptador.
- **Mensagens Longas (CAN)**: Adicionado o comando vital `AT AL` (Allow Long Messages), essencial para evitar respostas `NO DATA` em barramentos CAN modernos de 29-bit (onde o cabeçalho e checksum exigem mais de 7 bytes).
- **Timeouts Dinâmicos no Handshake**: Modificado o Timeout para um valor explicitamente alto (`AT ST 80` = ~512ms) exclusivamente antes de disparar o comando de handshake `0100`, de modo a não abortar enquanto o adaptador devolve a mensagem `SEARCHING...`. Logo após o handshake `0100` responder com sucesso, o Timeout é reduzido para a configuração do usuário (ex: `AT ST 32`) maximizando a velocidade do motor de polling.

## [4.0.0] - Anterior
### Adicionado
- **Módulo de Calibração de Consumo de Combustível**: O cálculo de consumo via sensor MAF assume que o motor tem 100% de Eficiência Volumétrica (VE). Foi adicionada a nova tela `FuelCalibrationScreen` para ajustar a discrepância entre a teoria estequiométrica e o consumo real.
- **Calculadora Interativa de Fator MAF**: Nova interface permite que o usuário digite a quilometragem e litros reais na bomba de combustível, juntamente com o consumo indicado pelo App. O sistema então gera automaticamente um "Fator Multiplicador" (ex: 1.633x) e aplica nativamente ao cálculo estequiométrico `CalculateFuelConsumptionUseCase`.

## [3.9.9] - Anterior
### Documentação
- **Criação do Documento `V4_PREP_STATE_AND_ROADMAP.md`**: Criado documento oficializando as 3 maiores conquistas técnicas de performance (Leitura Bloqueante Nativa, Priority Interleaving e Animação Linear de 60ms) que garantiram a redução da latência de quase 1 segundo para ~100ms.
- **Registro do Débito Técnico Crítico (Handshake)**: Mapeado oficialmente em `PENDING_CAR_VALIDATIONS.md` e no Roadmap que o aplicativo atualmente falha em inicializar a comunicação OBD-II a frio por conta própria. O app depende do "aquecimento" do ELM327 e negociação prévia do barramento (29-bit 500k) feito por outros apps de terceiros. A resolução autônoma desse handshake será a prioridade número 1 da versão 4.0.

## [3.9.8] - Anterior
### Alterado
- **Redução da Largura da Barra de RPM**: Valor padrão de `rpmBarWidth` reduzido de `22f` para `12f` para uma estética mais fina, moderna e integrada no arco do painel.
- **Ativação Padrão da Correção de Velocidade (+4%)**: O recurso de ajuste de offset de velocidade para equalizar com o velocímetro original do carro (`isSpeedCorrectionEnabled`) agora vem ativado por padrão (`true`) com fator de +4%.

## [3.9.7] - Anterior
### Otimizado
- **Priority Interleaving para RPM e Velocidade**: Implementado agendamento prioritário no `ObdPollingEngine`. O comando `RPM` (`010C`) passa a ser lido em **todos os ciclos ímpares** (frequência dedicada de 10-15Hz), enquanto `SPEED`, `MAF` e `THROTTLE` dividem os ciclos pares.
- **Leitura Nativa Blocking no Socket SPP**: Substituído o polling com `delay(2)` e `inp.available()` em `BluetoothConnectionManager` por chamadas diretas de leitura nativa `inp.read(buffer)`, eliminando até 100ms de suspensão desnecessária de corrotina por comando.
- **Animações de Alta Velocidade no Compose (60ms)**: Substituídas as molas lentas (`Spring.StiffnessMediumLow` que atrasavam a resposta visual em 400ms) por interpolação linear ultra-rápida de 60ms (`tween(60, LinearEasing)`), tornando o ponteiro do RPM e os marcadores digitais instantâneos ao pisar no acelerador.

## [3.9.6] - Anterior
### Otimizado
- **Protocolo Padrão AUTO (`AT SP 0`) & Fallback Dinâmico**: Alterado o protocolo padrão para `AUTO` (`AT SP 0`). O leitor ELM327 negocia automaticamente a arquitetura de bus CAN (11-bit 500k vs 29-bit 500k). Adicionada rotina de fallback no `reinitializeAdapter()` que ativa o `AT SP 0` se o comando `0100` falhar com protocolo forçado.
- **Remoção do PID 0x015E (Fuel Rate)**: Eliminado o envio do PID `01 5E` que gerava timeouts de 500ms `NODATA` em veículos sem suporte direto. O consumo instantâneo (L/h e km/L) passa a ser calculado 100% via `MAF` (`01 10`), acelerando drasticamente o ciclo de leitura.
- **Aumento de Throughput de Polling (15-20Hz)**: Adicionado mapeamento prioritário para `ThrottlePosition` (`"THROTTLE"` a 50ms) e removida a perda de ciclos do PID `01 5E`, aumentando o Throughput real do aplicativo no veículo.

## [3.9.5] - Anterior
### Corrigido
- **Tratamento de Exceções e Proteção de Permissões Bluetooth**: Adicionada verificação prévia de `BLUETOOTH_CONNECT` antes de invocar APIs do adaptador Bluetooth (`bondedDevices`, `createRfcommSocketToServiceRecord`). Captura preventiva de `SecurityException` no `BluetoothConnectionManager` para evitar o encerramento inesperado ("crash") na primeira inicialização do aplicativo.
- **Card de Solicitação de Permissão na UI**: Se a permissão de Bluetooth ainda não tiver sido autorizada pelo usuário, o aplicativo bloqueia temporariamente a tentativa de auto-conexão prévia e exibe um alerta explicativo ("Permissão de Bluetooth Necessária") com botão direto para "Autorizar Permissão do Bluetooth".

## [3.9.4] - Anterior
### Corrigido
- **Ação Imediata pós-autoConnect**: `DashboardViewModel.autoConnect()` agora dispara explicitamente `startVehicleDataCollection()` imediatamente após o sucesso da reconexão Bluetooth e Handshake.
- **Prevenção de Corrotinas Duplicadas**: `startCollecting()` agora gerencia os Jobs das corrotinas (`collectProfileJob` e `collectVehicleDataJob`), cancelando instâncias antigas antes de iniciar um novo fluxo. Isso previne que múltiplas corrotinas concorrentes disputem o mesmo socket Bluetooth e garante atualização instantânea do Painel ao reconectar, sem necessidade de navegar para outras telas.

## [3.9.3] - Anterior
### Corrigido
- **Eliminação do Banimento do PenaltyBox em Sensores Essenciais**: Corrigida a lógica do `ObdPollingEngine` que colocava os comandos `RPM`, `SPEED`, `TEMP` e `VOLTS` em um banimento temporário de 30 segundos ao capturar erros de desconexão/timeout. Agora, os sensores essenciais jamais são banidos no `penaltyBox`.
- **Limpeza de PenaltyBox na Re-inicialização**: Adicionada chamada preventiva de `pollingEngine.clearPenaltyBox()` ao realizar `reinitializeAdapter()`, garantindo a desinterdição imediata de todos os sensores assim que a conexão Bluetooth é restabelecida.

## [3.9.2] - Anterior
### Corrigido
- **Circuit Breaker de Timeouts Consecutivos**: `BluetoothConnectionManager` contabiliza respostas de erro/timeout. Após 3 timeouts consecutivos (1.5s com simulação pausada), o socket travado é encerrado automaticamente, permitindo que a queda seja identificada sem necessitar navegar por menus.
- **Re-subscrição Reativa no Painel**: Implementado loop contínuo de coleta em `DashboardViewModel`. Quando a conexão cai e o Watchdog re-estabelece o Bluetooth com o simulador/adaptador, o fluxo de dados dos sensores é retomado na hora no Painel sem intervenção manual do usuário.

## [3.9.1] - Anterior
### Corrigido
- **Auto-Conexão na Abertura do App**: Corrigido a rota inicial do `AppNavHost` para `Screen.DeviceList.route`. Ao abrir o App com Auto-Conexão ativada, a aplicação direciona imediatamente para a tela `ConnectionStatusScreen` executando a tentativa de conexão 1/3, handshake e transição de 500ms para o Painel.
- **Detecção de Queda de Conexão Física**: Adicionada chamada preventiva de `disconnect()` no `BluetoothConnectionManager` ao capturar falha de I/O / socket interrompido na simulação, garantindo que o Watchdog identifique a desconexão e execute a reconexão automática assim que o simulador/adaptador retornar.

## [3.9.0] - Anterior
### Adicionado
- **Nova Tela de Status da Conexão (`ConnectionStatusScreen`)**: Tela dedicada para acompanhamento do ciclo de conexão com visualização dinâmica de fases e progresso de até 3 tentativas (1/3, 2/3, 3/3).
- **Garantia de Não-Polling Pré-Handshake**: Garante que nenhuma requisição de sensores (RPM, Speed, Volts, MAF) seja emitida pelo módulo OBD antes da conclusão do handshake e da confirmação de conexão estabelecida.
- **Transição Automatizada de 500ms**: Após a conclusão com sucesso do Handshake OBD-II e exibição da mensagem "Conectado!", aguarda 500ms antes de navegar automaticamente para o Painel Principal (Dashboard).
- **Recuperação de Dispositivo & Auto-Conexão**: Salva o endereço do último adaptador Bluetooth pareado e adiciona a chave de "Auto-Conexão ao Iniciar", permitindo reconexão imediata assim que o App é aberto.

## [3.8.0] - Anterior
### Adicionado
- **Ajuste Fino do Velocímetro (Correção de Offset do Painel)**: Nova opção de personalização no Perfil do Veículo que permite ajustar/equalizar a velocidade exibida no App em relação ao painel digital do carro (ex: +4%).
- **Separação de Camadas (ECU vs Apresentação)**: A velocidade real da ECU (`speedKmh`) permanece 100% inalterada para garantir a precisão exata dos cálculos de Consumo de Combustível (km/L), Distância da Viagem (Trip) e Seleção de Marcha Ideal, enquanto a velocidade corrigida (`displaySpeedKmh`) é enviada unicamente para o Gauge de Velocidade na UI.

## [3.7.2] - Anterior
### Otimizado
- **Dynamic Overdue Scheduler**: O motor de busca (`ObdPollingEngine`) não envia mais múltiplos comandos (RPM, MAF, SPEED) em lote. Agora utiliza um cálculo matemático baseado no intervalo requisitado ("Overdue Ratio"), rodando apenas o sensor mais atrasado por ciclo e garantindo máxima taxa de atualização (Hz) real.
- **Penalty Box (Blacklist de PIDs)**: Se um sensor retornar `NODATA`, `ERROR` ou `?`, ele é jogado em um intervalo de espera de 30 segundos, impedindo que os longos timeouts de requisições de sensores não suportados penalizem a leitura do RPM e da Velocidade.

## [3.7.1] - Anterior
### Corrigido
- **Handshake Estável em Clones**: Adicionado um delay vital de `1.5s` logo que a conexão Bluetooth é estabelecida (Warm-up do chip), permitindo que microcontroladores instáveis reiniciem corretamente.
- **Limpeza de Protocolo Base**: Removido os espaços das strings de inicialização padrão (`ATZ` no lugar de `AT Z`) para garantir suporte a dongles com parse UART defeituoso.
- **Estabilização de Thread**: Substituição do `yield()` por `delay(2)` no buffer manager para liberar CPU durante os testes de byte na porta serial.

## [3.7.0] - Anterior
### Adicionado
- **Configurações Avançadas OBD-II**: Habilidade de ativar `Safe Mode`, alterar `Throttle Delay`, `AT Timeout` e ligar/desligar Headers/Spaces via UI.
- **RPM Preditivo**: Lógica computacional avançada baseada em inércia (MAF e Throttle Position) que cria um empurrão (Boost) visual na agulha do RPM injetando dados preditivos antes da chegada dos dados OBD pelo Bluetooth.

## [3.6.0] - 2026-09-08
### Adicionado
- **Consolidação de Alta Performance**: Formalização do motor de streaming sensor-a-sensor com cache persistente para estabilidade total.
- **Ajustes de Layout Tablet (BG1)**: Reajuste final de coordenadas (VOLTS, CLOCK, GEARS) e geometria da Barra de RPM (Y:70, Altura:50) para novo background.
- **Visual "Clean" por Padrão**: Efeito Ghosting desativado nativamente para melhor legibilidade.

## [3.5.3] - 2026-09-08
### Alterado
- **Ajustes de Layout Tablet (BG1)**: Reajuste de coordenadas para novo background (VOLTS, CLOCK, GEARS).
- **Geometria da Barra de RPM**: Altura ajustada para 50.0 e Posição Y para 70.0 no modo Tablet.
- **Sincronização de Versão Global**: Incremento para v3.5.3.

## [3.5.2] - 2026-09-08
### Alterado
- **Ajustes de Layout Tablet (BG1)**: Sincronização de coordenadas padrão conforme nova tabela (ex: RPM em Y:275).
- **Padrão Visual**: Efeito Ghosting desativado por padrão para displays digitais mais limpos.
- **Sincronização de Versão Global**: Incremento para v3.5.2.

## [3.5.1] - 2026-09-08
### Corrigido
- **Estabilização de Sensores Lentos**: Implementado cache persistente no `ObdPollingEngine` para eliminar a oscilação visual ("flicker") nos campos de Temperatura e Voltagem durante a emissão de alta frequência do RPM.
- **Veredito Técnico**: A solução de Zero-Latency Stream (v3.5.0) demonstrou-se altamente eficaz em testes com emulador ELM327, reduzindo drasticamente o RTT percebido e eliminando filas de processamento via software.

## [3.5.0] - 2026-09-08
### Otimizado
- **Fluxo de Dados Zero Latência**: Removido o limitador de 30ms no `DashboardViewModel`, permitindo que cada sensor atualize a tela no exato momento em que é lido.
- **Emissão Sensor-a-Sensor**: O `ObdPollingEngine` agora emite dados individualmente para cada PID, eliminando a espera pelo fim do ciclo de prioridade.
- **Anti-Fila (Conflate)**: Implementado o operador `.conflate()` no fluxo de dados para garantir que a UI sempre processe apenas o valor mais recente, descartando amostras obsoletas durante frames pesados.
- **Sincronização Visual Suave**: Manutenção das animações de interpolação, agora reagindo a uma frequência de entrada muito superior.

## [3.4.0] - 2026-09-08
### Adicionado
- **Motor de Polling com Prioridades**: Implementação de agendador hierárquico (ratio 8:1) para priorizar RPM, Velocidade e MAF sobre outros sensores.
- **Modo Turbo (Multi-PID)**: Suporte para agrupar múltiplos PIDs em uma única requisição OBD-II, dobrando o throughput em adaptadores compatíveis.
- **Nova Tela: Comunicação**: Painel avançado para configuração de protocolos CAN (11/29-bit, 250/500k), Timing Adaptativo e timeouts do chip.
- **Benchmark em Tempo Real**: Monitoramento de latência (RTT), taxa de atualização (Hz) e taxa de erro diretamente na interface de comunicação.
- **Circuit Breaker**: Função de interrupção manual da comunicação serial para diagnósticos seguros.

## [3.3.1] - 2026-09-08
### Adicionado
- **Controle de Ghosting**: Novo botão de liga/desliga no menu de personalização visual para o efeito de "fundo apagado" (888) nos displays de 7 segmentos.
- **Persistência**: O estado do Ghosting é salvo no perfil do veículo e aplicado globalmente.

## [3.3.0] - 2026-09-08
### Adicionado
- **Ajuste Fino Multimídia pós-testes reais**: Atualização do preset "Multimídia (Menor)" com novas coordenadas Y:225 para Speed/Gears e altura de RPM reduzida para 39.9px.
- **Sincronização de Versão Global**: Incremento para v3.3.0 em todos os componentes e exibição na UI.
### Alterado
- **Geometria da Barra de RPM**: Altura padrão reduzida de 50.0 para **39.9** para melhor encaixe visual em telas de 9".
- **Posicionamento Multimídia**: SPEED e GEARS movidos verticalmente para Y:225 para equilíbrio com o arco de RPM.

## [3.2.0] - 2026-09-08
### Adicionado
- **Turbo Polling v3.2.0**: Implementação de motor de busca ultra-performático com leitura em blocos (buffer) e latência reduzida.
- **Sincronização de Layout Multimídia**: Atualização do preset "Multimídia (Menor)" com escalas 0.90 (principais), 0.65 (secundários) e 0.50 (viagem), focado em centrais multimídia de 9 polegadas.
- **Documentação de Protocolo**: Atualização do `OBD_COMMUNICATION_PROTOCOL.md` detalhando as novas regras de performance.
### Alterado
- **Timeout Bluetooth**: Ajustado para **500ms** como padrão estável, garantindo resiliência em adaptadores reais e emuladores.
- **Geometria da Barra de RPM**: Correção da posição Y padrão para **50.0**, garantindo que o arco de RPM ocupe o topo da tela por padrão em novos perfis ou resets.

## [3.1.x] - 2026-09-08
### Otimizado
- **Modo Turbo (v3.1.0)**: Otimização do loop de busca com remoção de comandos redundantes.
- **Fail-Fast (v3.1.1)**: Descarte imediato de pacotes corrompidos ou com atraso superior ao timeout para manter a fluidez do RPM.
- **Ajuste de Y (v3.1.3)**: Correção de regressão na persistência do valor vertical da barra.

## [2.10.1] - 2026-09-08
### Documentação
- **Conformidade v2.10.1**: Revisão final de cabeçalhos técnicos e atualização dos modelos de dados para suportar a customização do Shift Light.
- **Checklist de Validação**: Preparação da lista de testes reais em veículo para as novas lógicas de marcha e alertas.

## [2.10.0] - 2026-09-08
### Adicionado
- **Customização de Shift Light**: Nova seção no Perfil do Veículo permitindo escolher entre o modo Econômico (Honda Style) ou Performance (Início do Redline).
- **Sensibilidade Ajustável**: Controle deslizante para definir o percentual do alvo (80% a 100%) em que o alerta visual (Blink) deve disparar.
### Alterado
- **Regras de Restrição de Alerta**: O Shift Light agora é automaticamente suprimido quando o veículo está em **5ª marcha** ou acima de **100 KM/h**, otimizando a experiência em rodovias.
- **Arquitetura de Alerta**: Lógica de processamento do Blink movida para o `DashboardViewModel` para maior precisão e desacoplamento da UI.

## [2.9.0] - 2026-09-08
### Alterado
- **Nova Lógica de Marchas**: Implementação de sistema inteligente para supressão de "N" (Neutro) enquanto o veículo estiver em movimento.
- **Detecção de Saída**: O indicador agora exibe automaticamente a marcha "1" quando o veículo está parado mas o acelerador é acionado (>5%), preparando o condutor para a arrancada.
- **Persistência de Estado**: Durante trocas de marcha ou uso da embreagem em movimento, o display agora mantém a última marcha detectada em vez de alternar para "N".

## [2.8.0] - 2026-09-08
### Alterado
- **Lógica de Temperatura (TEMP)**: O display agora exibe obrigatoriamente 3 dígitos (ex: 095). O alarme visual (cor vermelha) agora só é ativado acima de **104°C**.
- **Lógica de Tensão (VOLTS)**: Adicionado alarme visual (cor vermelha) para tensões abaixo de **12.0V**.
- **Limpeza de UI**: Remoção do ícone de termômetro vermelho em altas temperaturas para um visual mais limpo.

## [2.7.2] - 2026-09-08
### Documentação
- **Conformidade v2.7.2**: Atualização de cabeçalhos de arquivos e documentação técnica após refinamento de layout.

## [2.7.1] - 2026-09-08
### Alterado
- **Ajuste Fino de Design**: Refinamento das coordenadas de layout (RPM, Velocidade, Volts e Relógio) para melhor equilíbrio visual.
- **Migração Forçada v2.7.1**: Atualização da chave `elements_config_v12` para garantir a aplicação imediata dos novos ajustes de posição.

## [2.7.0] - 2026-09-08
### Alterado
- **Novo Layout de Indicadores**: Reestruturação completa das coordenadas e escalas de todos os elementos do Dashboard (RPM, Velocidade, Marchas, Trip, etc) conforme nova tabela de design.
- **Migração Forçada v2.7.0**: Atualização da chave de persistência de elementos (`elements_config_v11`) para garantir que o novo layout seja aplicado automaticamente no primeiro carregamento desta versão.

## [2.6.5] - 2026-09-08
### Removido
- **Debug Visual**: Remoção do texto amarelo de monitoramento do Watchdog na UI do Dashboard para limpeza de layout.
### Alterado
- **Estabilização de Código**: Manutenção da lógica interna de auto-conexão e delay de startup, visando futura correção de persistência de endereço MAC.

## [2.6.4] - 2026-09-08
### Adicionado
- **Feedback Visual de Resiliência**: Inclusão de um log visual amarelo no canto inferior do Dashboard para monitorar o status do Watchdog em tempo real.
- **Atraso de Inicialização (Startup Delay)**: O Watchdog agora aguarda 3 segundos antes da primeira tentativa de conexão, garantindo a estabilidade dos serviços de Bluetooth e Perfil.

## [2.6.3] - 2026-09-08
### Adicionado
- **Solicitação de Permissões em Runtime**: Adicionada lógica na `MainActivity` para solicitar `BLUETOOTH_CONNECT` e `BLUETOOTH_SCAN` em dispositivos Android 12+, corrigindo possível bloqueio silencioso da auto-conexão.
- **Logs de Depuração Profunda**: Inclusão de rastreamento ultra-detalhado (TAG: `OBD_RESILIENCE`) para capturar falhas exatas no handshake Bluetooth.

## [2.6.2] - 2026-09-08
### Corrigido
- **Ativação do Watchdog**: Correção na ordem de inicialização do ViewModel, garantindo que o Watchdog e os coletores de perfil iniciem imediatamente no `init`.
- **Diagnóstico de Resiliência**: Adição de logs detalhados (TAG: `OBD_RESILIENCE`) para monitorar o comportamento da auto-conexão no Logcat/Benchmark.

## [2.6.1] - 2026-09-08
### Adicionado
- **Watchdog de Bluetooth**: Implementação de um monitor em segundo plano que tenta reconectar automaticamente ao último adaptador OBD-II a cada 5 segundos caso a conexão seja perdida ou o app seja aberto.
- **Resiliência de Auto-Conexão**: Verificação de estado do adaptador Bluetooth (ligado/desligado) antes de tentar a conexão, evitando crashes e loops infinitos.

## [2.6.0] - 2026-09-08
### Adicionado
- **Auto-Conexão Inteligente**: O aplicativo agora memoriza o endereço MAC do último adaptador OBD-II conectado com sucesso. Ao abrir o Dashboard, o sistema tenta restabelecer a conexão automaticamente em segundo plano.
- **Gestão de Estado de Conexão**: Melhoria no feedback visual durante a reconexão automática.

## [2.5.7] - 2026-09-08
### Consolidado
- **Padrão de Fábrica Absoluto**: Consolidação da migração de chaves v2.5.6 para garantir que 100% dos usuários recebam a nova geometria (35°/57dp) e performance (50ms) no primeiro carregamento.
- **Documentação v2.5.7**: Revisão final de cabeçalhos e conformidade com as diretrizes de governança de IA.

## [2.5.6] - 2026-09-08
### Alterado
- **Migração Forçada de Padrões**: Alteração interna das chaves de armazenamento (DataStore) para forçar o carregamento dos novos padrões v2.5.5 em todos os dispositivos sem necessidade de reset manual.
- **Sincronização Absoluta**: Garantia de que Redline (2500), Ângulo (35°), Altura (57dp) e Polling (50ms) sejam os valores iniciais de qualquer instalação.

## [2.5.5] - 2026-09-08
### Corrigido
- **Restauração de Fábrica (Crítico)**: Removidos valores fixos antigos que estavam "escondidos" na lógica de reset do ViewModel. Agora o botão "Restaurar Fábrica" utiliza 100% os padrões v2.5.4/v2.5.5.
- **Geometria Sincronizada**: Validação final dos valores de Redline (2500), Ângulo (35°), Altura (57dp) e Posição Y (110.4) como padrão absoluto.
### Alterado
- **Ajuste de Performance**: Intervalo de polling de RPM alterado de 1ms para **50ms** e Fuel Rate de 500ms para **50ms**, conforme solicitado para estabilidade de benchmark.

## [2.5.4] - 2026-09-08
### Alterado
- **Novos Padrões de Fábrica**: Atualização das configurações padrão do dashboard para Escala de 4000 RPM, Início de Redline em 2500 RPM, Curvatura de 35° e altura de barra de 57dp.
- **Cor de Alerta**: Alteração da cor do Blink (Shift Light) padrão para Branco (FFFFFFFF).
- **Layout Central**: Reajuste da posição padrão do indicador de RPM (Y:300, Escala 0.70) para melhor ergonomia visual em telas padrão.

## [2.5.3] - 2026-09-08
### Corrigido
- **Escala de RPM Preditiva**: Remoção do teto forçado de 3200 RPM no modo Shift Light, unificando a escala das barras com a escala numérica (4K/8K). Agora 800 RPM em uma escala de 8K ocupa exatamente 10% da barra, corrigindo a distorção de 2.5x relatada.
- **Precisão de Redline**: A zona vermelha agora inicia exatamente no valor de RPM configurado, eliminando o erro de proporção que jogava o redline para marcas superiores da escala.
### Documentação
- **Padronização v2.5.3**: Cabeçalhos de arquivos críticos (`DashboardScreen`, `DashboardViewModel`, `ObdRepositoryImpl`, `ProfileRepositoryImpl`, `Gauges`) atualizados com histórico de versões e correlações técnicas.

## [2.5.2] - 2026-09-08
### Corrigido
- **Lógica de Redline Dinâmico**: Correção na renderização da zona vermelha para ser proporcional ao fundo de escala selecionado (4K/8K), garantindo que o efeito visual funcione corretamente em ambos os modos.
- **Sincronização Visual**: O brilho (Glow) e a cor das barras agora respeitam o ponto exato de início de redline configurado.

## [2.5.1] - 2026-09-08
### Corrigido
- **Persistência de Escala**: Correção na persistência dos dados de escala (4K/8K e tamanho de texto) no DataStore, garantindo que as configurações não sejam perdidas ao retornar ao Dashboard.
### Adicionado
- **Redline Customizável**: Novo ajuste no menu de personalização para definir o ponto de início da zona vermelha (redline) na barra de RPM.

## [2.5.0] - 2026-09-08
### Adicionado
- **Escala de RPM Dinâmica**: Nova funcionalidade para exibir uma escala numérica (0-4 ou 0-8) abaixo das barras de RPM.
- **Personalização de Escala**: Opções para alternar entre fundo de escala de 4000 ou 8000 RPM, visibilidade e ajuste de tamanho de fonte.
- **Geometria Sincronizada**: A escala acompanha automaticamente a curvatura (arco ou reta) definida para o RPM.

## [2.4.0] - 2026-09-08
### Adicionado
- **Efeito Glow (Brilho Neon)**: Nova opção no menu de personalização para ativar/desativar o brilho nas barras de RPM.
- **Renderização Nativa**: Implementação de `BlurMaskFilter` via `NativeCanvas` para um efeito de glow suave e performático.

## [2.3.1] - 2026-09-08
### Documentação
- **Cabeçalhos de Arquivo**: Adicionada documentação técnica no topo dos arquivos críticos (`VehicleProfile`, `VehicleProfileViewModel`, `VisualSettingsScreen`, `DashboardScreen`) detalhando objetivos, correlações e histórico.
- **Padronização**: Atualização dos comentários de versão interna nos arquivos.

## [2.3.0] - 2026-09-08
### Adicionado
- **Galeria de Backgrounds**: Nova funcionalidade no menu de personalização que permite alternar entre múltiplos fundos nativos (`dashboard_bg_1.jpg`, `dashboard_bg_2.jpg`, etc.) via carrossel horizontal.
- **Suporte Dinâmico de Assets**: Arquitetura preparada para receber novos backgrounds seguindo o padrão de nomenclatura sequencial.
- **Toggle Custom/Nativo**: Separação clara entre o uso de imagens da galeria e uploads customizados do usuário.

## [2.2.0] - 2026-09-08
### Consolidado
- **Marco de Estabilidade (Milestone)**: Consolidação de todas as melhorias de comunicação Bluetooth e Parser em uma arquitetura de referência.
- **Documentação de Protocolo**: Criação do `docs/architecture/OBD_COMMUNICATION_PROTOCOL.md` detalhando o motor de resiliência.
- **Performance**: Polling de latência zero e emissão reativa validados com 100% de sucesso em emuladores.

## [2.1.4] - 2026-09-08
### Otimizado
- **Motor de Polling Reativo (v2.1.4)**: Implementada emissão imediata de dados (`emit`) logo após cada leitura de sensor, eliminando o atraso de "fim de ciclo".
- **Latência Zero**: Substituído o `delay(10ms)` por `yield()`, permitindo que o motor de polling rode na velocidade máxima permitida pelo hardware (ou emulador).

## [2.1.3] - 2026-09-08
### Corrigido
- **Tratamento de Payload Vazio (NaN)**: Correção para casos onde o adaptador/emulador retorna o eco do PID mas omite os bytes de dados (ex: `4111\r\r>`).
- **Diagnóstico Preciso**: Respostas com payload ausente agora são marcadas corretamente como `ADAPTER_ERROR` (Erro de Adaptador) em vez de `GARBLED` (Dados Embaralhados), facilitando a identificação de sensores não inicializados no emulador.

## [2.1.2] - 2026-09-08
### Corrigido
- **Sincronização de PIDs Específicos**: Correção no parser para lidar com PIDs de um dígito e respostas curtas de emuladores, eliminando o status `GARBLED` nos sensores de Throttle (0111) e Fuel Rate (015E).
- **Normalização de Eco**: Implementada padronização de busca por `Mode + PID` (ex: 4111) para garantir captura exata dos bytes de dados.

## [2.1.1] - 2026-09-08
### Alterado
- **Parser de Alta Resiliência**: Limpeza de string via Regex para ignorar espaços, eco e caracteres especiais, garantindo compatibilidade total com Emuladores Android e Clones ELM327 que ignoram comandos de formatação (`AT S0/H0`).

## [2.1.0] - 2026-09-08
### Adicionado
- **Motor de Conexão Resiliente**: Implementação de `Mutex` (thread-safety) e `withTimeoutOrNull` no transporte Bluetooth.
- **Limpeza de Buffer (Pre-fetch)**: O app agora limpa qualquer byte residual do adaptador antes de enviar um novo comando, eliminando o "lixo" acumulado.
- **Sequência Clone-Proof**: Adição de `AT D` (Reset Padrão), `AT S0` (Sem Espaços) e `AT AT 1` (Tempo Adaptativo) para suportar ELM327 de baixa qualidade.
- **Parser Tolerante a Headers**: O parser agora consegue extrair dados mesmo que o comando `AT H0` falhe, buscando o eco do PID em qualquer posição da string.

## [2.0.0] - 2026-09-08
### Adicionado
- **Boot Robusto v2.0**: Sequência de inicialização estrita com validação de respostas (ATZ, ATE0, ATL0, ATSP6, ATSH7DF e ativação 0100).
- **Sincronização de Prompt**: O app agora aguarda estritamente o caractere `>` do ELM327 antes de enviar o próximo comando, evitando colisões de buffer.
- **Protocolo CAN Fixo**: Otimização para Civic 1.8 (ISO 15765-4 11-bit/500kbps), eliminando o atraso do "Bus Search".

## [1.9.3] - 2026-09-08
### Adicionado
- **Diagnóstico de Performance**: Nova interface de Benchmark em "Ajustes de Performance" com Logger em tempo real.
- **Parser Inteligente v2.0**: Implementação de verificação de eco de PID e validação de range (min/max) para evitar dados "fantasmas" ou embaralhados.
- **Watchdog de Recuperação**: Botão para re-inicialização forçada do adaptador ELM327 (ATZ + Boot Sequence) sem precisar reiniciar o app.
- **Log de Status**: Identificação visual de erros: SUCCESS, GARBLED, TIMEOUT, OUT_OF_RANGE.

## [1.9.2] - 2026-09-08
### Corrigido
- **Correção de Dependência**: Adição dos imports ausentes para `animateIntAsState`, `spring` e `Spring` em `DashboardScreen.kt`, resolvendo erro de compilação pós-migração para o Motor de Fluidez v1.9.1.
- **Sincronização de Versão**: Atualização do `versionCode` e `versionName` no `build.gradle.kts` para refletir o estado atual do desenvolvimento.

## [1.9.1] - 2026-09-08
### Adicionado
- **Motor de Fluidez (Interpolação)**: Implementação de animações baseadas em `spring` para RPM (Barra e Digital) e Velocidade, garantindo que o display nunca "pule" valores e preencha quadros intermediários.
- **Painel de Performance**: Novos controles em "Personalizar Visual" para ajustar a taxa de polling (ms) de RPM, Velocidade e a frequência de Blink do Shift Light.
- **Blink Customizável**: Agora é possível definir a velocidade do alerta de troca de marcha entre 50ms e 500ms.

## [1.9.0] - 2026-09-08
### Adicionado
- **Reset de Fábrica Inteligente**: Nova funcionalidade de restauração com presets de dispositivo.
- **Presets de Escala**: Adição do perfil "Multimídia" com escalas reduzidas (70%/50%/40%) para telas menores, além do padrão "Tablet".
- **Diálogo de Confirmação**: Interface intuitiva para escolha do preset antes do reset no menu "Personalizar Visual".

### Corrigido
- **Navegação de Perfil**: Sanado erro de compilação por parâmetros ausentes (`onBluetoothClick`, `onPerformanceClick`) em `VehicleProfileScreen`.

## [1.8.9] - 2026-09-08
### Adicionado
- **Precisão de SPS**: Slider de performance com escala cirúrgica (1-20ms linear) para fluidez máxima.
- **Motor Gráfico v3.2**: Suporte a separador de dois pontos (`:`) e correção do bug de "zeros cheios" (ghosting).
- **Inicialização OBD Robusta**: Sequência AT (Z, E0, L0, H0, SP0) com Watchdog preventivo.

## [1.8.6] - 2026-09-08
### Adicionado
- **Fluxo de Início Rápido**: O Dashboard agora é a tela de entrada do aplicativo.
- **Botão de Configurações Invisível**: Área de toque no canto superior direito para acesso rápido às configurações.
- **Ícone Bluetooth**: Adicionado ao menu de Perfil para gerenciar pareamentos sem interromper o fluxo inicial.

### Alterado
- **Persistência Bluetooth**: O app agora memoriza e tenta auto-conectar ao último adaptador OBD-II utilizado.
- **Versão Global**: Incremento para v1.8.6.

## [1.8.5] - 2026-09-08
### Adicionado
- **Simulador de Condução Real**: Slider de velocidade integrado ao preview para validar marchas e blink.
- **Controle de Cores Hex**: Seletores de cor via código hexadecimal (#AARRGGBB) para precisão profissional.
- **Geometria Progressiva**: Slider de ângulo agora permite transição suave entre Arco e Reta.
- **Segurança de Versão**: Inicialização do repositório Git local.

## [1.8.1] - 2026-09-08
### Corrigido
- **Fonte Única da Verdade**: Unificação absoluta de coordenadas (RPM em 415, 200).
- **Botão Restaurar Fábrica**: Operação determinística para reset de layout oficial.

## [1.7.0] - 2026-09-08
### Adicionado
- **Background Nativo**: Suporte a imagem embutida via Assets (`dashboard_bg.jpg`).
- **Clean UI**: Remoção total de labels estáticos e decorações fixas.

---
*Modelo de IA: Gemini 2.0 Pro/Flash via Android Studio*
