# Plano de Implementação - MVP-05.10: Otimização de Protocolo e Amostragem (SPS)

Este plano detalha a robustez da comunicação ELM327 e o controle granular da taxa de atualização dos sensores para garantir fluidez máxima nos gauges e precisão nos cálculos.

## User Review Required

- **Intervalos de Polling**: O usuário poderá definir o delay (em ms) entre as requisições de cada sensor.
    - RPM (Ideal: 0-20ms)
    - Velocidade (Ideal: 50-100ms)
    - MAF (Ideal: 50ms para precisão de consumo)
    - Outros (Ideal: 500-1000ms)
- **Watchdog de Protocolo**: Implementar um verificador que reenvia `AT E0` e `AT H0` caso o buffer apresente sujeira (eco) persistente.

## Proposed Changes

### [Data - OBD]

#### [Elm327Init.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/data/obd/Elm327Init.kt)
- Atualizar sequência de boot: `AT Z` -> `AT E0` -> `AT L0` -> `AT H0` -> `AT SP 0`.
- Adicionar verificador de integridade de resposta.

#### [ObdPollingEngine.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/data/obd/ObdPollingEngine.kt)
- Refatorar o loop de polling para respeitar os intervalos configurados no Perfil para cada PID.
- Implementar prioridade para o RPM (intercalação: RPM -> Vel -> RPM -> MAF -> RPM -> Outros).

### [Domain - Model]

#### [VehicleProfile.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/domain/model/VehicleProfile.kt)
- Adicionar `pollingIntervals: Map<String, Int>` (em milissegundos).

### [Presentation - Settings]

#### [PerformanceSettingsScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/profile/ui/PerformanceSettingsScreen.kt) [NEW]
- Sliders/Inputs para configurar o delay de cada sensor.
- Visualização de "Samples per Second" estimada baseada no delay escolhido.

### [Presentation - Profile]

#### [VehicleProfileScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/profile/ui/VehicleProfileScreen.kt)
- Adicionar botão para acessar "Ajustes de Performance".

---

## Verificação Técnica (Etapas)

1.  **Etapa 1**: Nova sequência de comandos AT e detecção de Eco.
2.  **Etapa 2**: Expansão do Perfil com intervalos de amostragem.
3.  **Etapa 3**: Motor de Polling por Prioridade (RPM First).
4.  **Etapa 4**: Tela de Ajustes de Performance.

## Plano de Verificação Manual
- Analisar os logs do Bluetooth: os comandos devem vir sem o eco da pergunta (`AT E0` ativo) e sem o cabeçalho `0xE8` (`AT H0` ativo).
- Alterar o delay do RPM para 500ms e observar o gauge ficar "lento". Voltar para 0ms e observar a fluidez total.
