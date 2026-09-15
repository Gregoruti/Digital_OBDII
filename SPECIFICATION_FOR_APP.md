# App Specification
SPECIFICATION_FOR_APP.md abaixo.

markdown
# SPECIFICATION_FOR_APP — [NOME DO APP OBD-II]

> Especificação Funcional e Técnica do Aplicativo
> Última atualização: [DATA] | Mantenedor: [NOME/EQUIPE]
> Documento de leitura obrigatória (ver `GUIDELINES.md`, seção 0.4)
> Stack: Kotlin + Jetpack Compose + Clean Architecture + Hilt + Room + Coroutines/Flow + Bluetooth Classic (SPP)

---

## 1. Visão Geral do Produto

### 1.1 Nome e Proposta
**[Nome do App]** é um aplicativo Android que se conecta a um adaptador **ELM327 (OBD-II Bluetooth Classic)** para realizar leitura em tempo real de parâmetros do veículo (velocidade, RPM, temperatura do motor, tensão da ECU, consumo de combustível, marcha ideal estimada) e exibi-los em um painel digital (dashboard) similar a um cluster automotivo.

### 1.2 Problema que Resolve
Veículos sem painel digital completo (ou com informações limitadas) não expõem dados úteis de consumo, marcha ideal ou diagnóstico em tempo real. O app transforma o smartphone em um **display auxiliar (dashboard companion)**, usando a porta OBD-II já existente no veículo (obrigatória desde 1996 nos EUA, 2010 no Brasil).

### 1.3 Usuário-alvo
- Motoristas entusiastas que desejam monitorar performance/consumo em tempo real.
- Usuários de veículos sem painel digital nativo com essas métricas.
- Público técnico (mecânicos amadores) interessado em diagnóstico básico via PIDs OBD-II.

### 1.4 Fora de Escopo (Non-Goals)
- Leitura/limpeza de códigos de falha (DTC) — **não é MVP inicial**, pode ser roadmap futuro.
- Suporte a adaptadores **BLE** (Bluetooth Low Energy) — versão inicial cobre apenas **Bluetooth Classic/SPP**.
- Suporte a protocolos proprietários de fabricantes (ex.: dados estendidos GM/Ford específicos).
- Diagnóstico de segurança/airbag/ABS (fora do escopo de PIDs de motor padrão SAE J1979).

---

## 2. Arquitetura (alinhada à seção 2 do GUIDELINES.md)

### 2.1 Estrutura de Camadas

presentation/
├── dashboard/
│   ├── DashboardViewModel.kt
│   ├── DashboardUiState.kt
│   └── ui/                      # Composables do painel (gauges, displays digitais)
├── connection/
│   ├── DeviceListViewModel.kt
│   └── ui/                      # Tela de pareamento/seleção do adaptador
├── trips/
│   ├── TripHistoryViewModel.kt
│   └── ui/                      # Histórico de viagens
├── navigation/
│   └── AppNavHost.kt
└── theme/
└── Theme.kt / Typography.kt

domain/
├── model/
│   ├── VehicleSnapshot.kt
│   ├── TripSummary.kt
│   └── ConnectionState.kt
├── repository/
│   ├── ObdRepository.kt
│   └── TripRepository.kt
└── usecase/
├── ConnectToAdapterUseCase.kt
├── ObserveVehicleDataUseCase.kt
├── CalculateIdealGearUseCase.kt
├── CalculateFuelConsumptionUseCase.kt
└── UpdateTripSummaryUseCase.kt

data/
├── bluetooth/
│   ├── BluetoothConnectionManager.kt   # Transporte físico (RFCOMM/SPP)
│   └── Elm327Transport.kt
├── obd/
│   ├── ObdCommand.kt                   # Sealed class de PIDs
│   ├── PidRegistry.kt                  # PIDs suportados pelo veículo (detecção dinâmica)
│   ├── ObdResponseParser.kt
│   └── ObdPollingEngine.kt             # Flow de polling contínuo
├── database/
│   ├── dao/TripDao.kt
│   ├── entities/TripEntity.kt
│   ├── Converters.kt                   # Único arquivo de TypeConverters (ver Guidelines 8.3.1)
│   └── AppDatabase.kt
└── repository/
├── ObdRepositoryImpl.kt
└── TripRepositoryImpl.kt

service/
└── ObdForegroundService.kt             # Mantém conexão Bluetooth em background

di/
├── BluetoothModule.kt
├── RepositoryModule.kt
└── DatabaseModule.kt

shell

### 2.2 Fluxo de Dados

Adaptador ELM327 (Bluetooth SPP)
↓ (RFCOMM socket)
BluetoothConnectionManager
↓ (comandos AT / PIDs hex)
ObdPollingEngine (Flow, Dispatchers.IO)
↓
ObdResponseParser (decodificação hex → valores físicos)
↓
ObdRepositoryImpl (aplica UseCases: marcha ideal, consumo)
↓
DashboardViewModel (StateFlow<DashboardUiState>)
↓
UI Compose (gauges, displays digitais)

yaml

> Todo o fluxo de leitura roda em `Dispatchers.IO`; estado exposto à UI via `StateFlow`, conforme seção 8.4 do `GUIDELINES.md`.

---

## 3. Modelos de Domínio

### 3.1 `VehicleSnapshot`

```kotlin
data class VehicleSnapshot(
    val speedKmh: Int = 0,
    val rpm: Int = 0,
    val coolantTempC: Int = 0,
    val ecuVoltage: Double = 0.0,
    val idealGear: Int = 0,
    val instantConsumptionKmL: Double = 0.0,
    val averageConsumptionKmL: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
3.2 TripSummary
kotlin
data class TripSummary(
    val startTime: Long,
    val distanceKm: Double = 0.0,
    val fuelConsumedL: Double = 0.0
) {
    val elapsedMillis: Long get() = System.currentTimeMillis() - startTime
}
3.3 ConnectionState (sealed class)
kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
3.4 Persistência — TripEntity (Room)
kotlin
@Entity
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val fuelConsumedL: Double,
    val avgConsumptionKmL: Double
)
Segue Regra de Migrations/Entidades sincronizadas (Guidelines 8.3.2): todo campo novo em migration deve ser refletido na entidade correspondente.

4. Especificação de Comunicação (Bluetooth + Protocolo OBD-II)
4.1 Transporte Físico
Tipo: Bluetooth Classic — perfil SPP (Serial Port Profile), via socket RFCOMM.
UUID padrão: 00001101-0000-1000-8000-00805F9B34FB
Não suportado na v1: adaptadores BLE (roadmap futuro, ver seção 8).
4.2 Sequência de Inicialização do ELM327
Ordem	Comando AT	Função
1	ATZ	Reset do adaptador
2	ATE0	Echo off
3	ATL0	Linefeed off
4	ATS0	Espaços off
5	ATH0	Headers off
6	ATSP0	Protocolo automático (fallback conforme veículo)
4.3 Tabela de PIDs Suportados (MVP inicial)
PID	Modo	Métrica	Bytes	Fórmula de decodificação
0C	01	RPM	2	((A*256)+B)/4
0D	01	Velocidade (km/h)	1	A
05	01	Temperatura do arrefecimento (°C)	1	A - 40
42	01	Tensão da ECU (V)	2	((A*256)+B)/1000
10	01	MAF — vazão de ar (g/s)	2	((A*256)+B)/100
11	01	Posição do acelerador (%)	1	A*100/255
PIDs adicionais (nível de combustível 2F, fuel rate direto 5E, temperatura de admissão 0F) ficam documentados como backlog — ver seção 8 (Roadmap).

4.4 Detecção de PIDs Suportados pelo Veículo
Antes de iniciar o polling, consultar 0100, 0120, 0140 para descobrir quais PIDs o veículo realmente expõe.
Caso um PID essencial (ex.: RPM) não esteja disponível, exibir estado de erro amigável ao usuário (não travar o app).
4.5 Regras de Parsing
Resposta bruta no formato 41 0C 1A F8 → descartar os 2 primeiros bytes (echo modo+PID).
Tratar respostas NO DATA, ERROR, ? como leitura nula (não crashar a UI).
Timeout obrigatório por comando (withTimeoutOrNull, recomendado 500ms–1s) — adaptador pode não responder.
4.6 Frequência de Polling
RPM e velocidade: alta prioridade — meta de 4-5 leituras/s.
Temperatura, tensão, MAF: baixa prioridade — 1 leitura/s é suficiente.
Meta geral: não exceder 5-10 leituras/s combinadas (limitação física do Bluetooth Classic + ELM327).
5. Regras de Negócio (Use Cases)
5.1 Marcha Ideal Estimada (CalculateIdealGearUseCase)
O OBD-II não fornece marcha diretamente; estimativa via razão RPM/velocidade comparada a uma tabela de relações de câmbio configurável por veículo.
Perfil do veículo (relações de marcha) deve ser persistido (Room ou DataStore) e editável em tela de configuração.
Critério de aceite: se speedKmh == 0 ou rpm == 0, retornar marcha 0 (neutro/parado), nunca erro.
5.2 Consumo de Combustível (CalculateFuelConsumptionUseCase)
Baseado em MAF (PID 10), fórmula estequiométrica padrão (AFR ≈ 14.7 para gasolina/etanol — tornar configurável por tipo de combustível).
Calcular consumo instantâneo (km/L) e médio da viagem.
Fallback: se o veículo expuser PID 5E (fuel rate direto), priorizar esse valor sobre o cálculo via MAF (mais preciso).
5.3 Trip Summary (UpdateTripSummaryUseCase)
Acumula distância e combustível consumido por delta de tempo entre leituras.
Uma viagem é considerada "iniciada" na conexão bem-sucedida ao adaptador e "finalizada" na desconexão ou ação explícita do usuário.
Toda viagem finalizada é persistida em TripEntity para consulta no histórico.
6. Especificação de Interface (UI/UX)
6.1 Tela de Dashboard (principal)
Layout inspirado em cluster automotivo digital: gauge semicircular de RPM/velocidade (Canvas/drawArc) + displays digitais (fonte estilo 7 segmentos) para: velocidade, RPM, marcha ideal, temperatura, tensão, consumo instantâneo/médio, horário, resumo da viagem atual.
Estados visuais obrigatórios: Disconnected, Connecting, Connected, Error (feedback visual claro em cada estado — conforme seção 9.4 do Guidelines, acessibilidade).
@Preview obrigatório para cada componente de gauge/display (Guidelines 8.1).
6.2 Tela de Conexão (pareamento)
Lista de dispositivos Bluetooth pareados (Classic).
Ação explícita de "Conectar" com feedback de progresso (ConnectionState.Connecting).
Tratamento de erro amigável (ex.: "Adaptador não respondeu", "Bluetooth desligado").
6.3 Tela de Histórico de Viagens
Lista de TripEntity ordenada por data (mais recente primeiro), consumida via Flow do TripDao.
Exibir: data/hora, distância, combustível consumido, consumo médio.
6.4 Tela de Configuração de Veículo (perfil)
Cadastro/edição das relações de marcha (gearRatios: List<Double>).
Seleção do tipo de combustível (afeta AFR/densidade usados no cálculo de consumo).
7. Requisitos Não Funcionais
7.1 Permissões (Android 12+)
xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- necessário em versões < 12 para scan -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
Solicitação via ActivityResultContracts.RequestMultiplePermissions() antes de abrir a tela de pareamento.
Fluxo de permissão negada deve ser tratado com tela explicativa (não apenas crash/silêncio).
7.2 Serviço em Foreground
ObdForegroundService mantém a conexão Bluetooth viva mesmo com app em background/tela apagada (uso automotivo real).
Notificação persistente obrigatória (Android 8+).
Dados publicados via SharedFlow/Binder, consumidos pelo DashboardViewModel.
7.3 Resiliência de Conexão
Reconexão automática com retry exponencial em caso de desconexão (veículo fora de alcance, ignição desligada).
Nunca deixar a UI travada em estado de loading infinito — timeout + mensagem de erro clara.
7.4 Performance
Polling não deve bloquear a thread principal (Dispatchers.IO obrigatório — Guidelines 8.4).
Gauges Compose devem evitar recomposição excessiva (usar remember/derivedStateOf conforme Guidelines 8.1).
7.5 Segurança e Privacidade
Nenhum dado do veículo é enviado a servidores externos na v1 (processamento 100% local).
Sem coleta de dados pessoais além do necessário para operação local (histórico de viagens fica no dispositivo).
8. Roadmap de MVPs (alinhado à seção 14 do GUIDELINES.md)
MVP	Descrição	Status
MVP-01	Conexão Bluetooth Classic + inicialização ELM327 + leitura crua de 1 PID (RPM) em log	📋 Planejado
MVP-02	ObdPollingEngine completo + parser + ObdRepository com Flow de VehicleSnapshot	📋 Planejado
MVP-03	Dashboard Compose (gauges + displays digitais) consumindo DashboardViewModel	📋 Planejado
MVP-04	Cálculo de marcha ideal + perfil de veículo configurável (Room/DataStore)	📋 Planejado
MVP-05	Cálculo de consumo (MAF) + Trip Summary em tempo real	📋 Planejado
MVP-06	Persistência de viagens (Room) + tela de histórico	📋 Planejado
MVP-07	ObdForegroundService + reconexão automática + notificação persistente	📋 Planejado
MVP-08	Detecção dinâmica de PIDs suportados (0100/0120/0140) + fallback gracioso	📋 Planejado
MVP-09 (backlog)	Leitura de PIDs adicionais (nível de combustível, fuel rate direto, temp. admissão)	🔭 Futuro
MVP-10 (backlog)	Suporte a leitura/limpeza de DTC (códigos de falha)	🔭 Futuro
MVP-11 (backlog)	Suporte a adaptadores BLE	🔭 Futuro
MVP-12 (backlog)	Exportação de viagens (CSV) / sincronização em nuvem opcional	🔭 Futuro
Cada MVP deve seguir o ciclo completo da seção 6 do GUIDELINES.md (planejamento → implementação → testes → documentação → validação → avanço), com checklist da seção 7 aplicado antes de avançar.

9. Pontos de Atenção Técnica (Riscos Conhecidos)
Risco	Mitigação
Nem todo veículo suporta todos os PIDs	Detecção dinâmica via 0100/0120/0140 antes do polling (MVP-08)
Protocolo automático (ATSP0) pode falhar em alguns veículos	Implementar fallback manual de protocolo (ISO 9141, KWP2000, CAN 11/29 bits)
MAF pode não existir no veículo	Priorizar PID 5E (fuel rate direto) quando disponível; fallback para estimativa via MAF
Marcha ideal é sempre estimativa	Deixar relações de câmbio configuráveis por usuário/veículo; nunca apresentar como valor "oficial"
Tensão da ECU alternativa	Avaliar uso do comando AT RV (tensão do próprio adaptador) como fonte alternativa/mais simples que PID 42
Latência/limitação do Bluetooth Classic	Priorizar PIDs críticos (RPM/velocidade) com maior frequência; demais PIDs com frequência reduzida
Desconexão em uso real (veículo em movimento)	Retry exponencial + ObdForegroundService resiliente (MVP-07)
10. Estratégia de Testes Específica do App (complementa seção 4 do GUIDELINES.md)
ObdResponseParser: testes unitários cobrindo respostas válidas, NO DATA, ERROR, respostas truncadas/malformadas.
CalculateIdealGearUseCase: testes com tabelas de marcha variadas, speedKmh/rpm zerados, valores extremos.
CalculateFuelConsumptionUseCase: testes com MAF zero, AFR customizado, densidade de combustível variável.
BluetoothConnectionManager: testes de integração simulando timeout de leitura (usar fake InputStream/OutputStream).
TripDao: testes de integração com Room em memória (Guidelines 4.4).
UI (Compose): teste crítico do fluxo Connecting → Connected → exibição correta dos valores no dashboard.
11. Glossário
Termo	Definição
ELM327	Chip/adaptador que traduz comandos AT em requisições ao barramento OBD-II do veículo
PID	Parameter ID — código que identifica qual dado está sendo solicitado ao veículo (ex.: 0C = RPM)
SPP	Serial Port Profile — perfil Bluetooth Classic usado para comunicação serial via RFCOMM
MAF	Mass Air Flow — vazão de massa de ar admitida pelo motor, usada para estimar consumo
AFR	Air-Fuel Ratio — razão ar/combustível usada na fórmula de consumo estimado
DTC	Diagnostic Trouble Code — código de falha do veículo (fora do escopo do MVP inicial)
Apêndice A — Placeholders a Substituir
[NOME DO APP OBD-II]
[DATA], [NOME/EQUIPE]
Tabela de PIDs adicionais conforme testes em veículos reais
Relações de marcha padrão (se houver perfis pré-cadastrados por modelo de veículo)