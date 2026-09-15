# Plano de Implementação - MVP-05.8: Barra de RPM Personalizável e Modo Shift Light

Este plano detalha a criação do sistema de "Modo Troca de Marchas" e a personalização geométrica/cromática total da barra de RPM.

## User Review Required

- **Lógica de Blink**: O painel piscará a 10Hz quando o RPM atingir 85% do valor da tabela econômica (ex: 2.295 RPM para 1ª -> 2ª).
- **Escala Shift Light**: Quando o modo está ativo, a barra termina em 3.200 RPM, dando muito mais resolução para trocas econômicas.
- **Geometria Dinâmica**: O usuário pode transformar a barra de um Arco (30°) para uma Linha Reta (0°).

## Proposed Changes

### [Domain - Model]

#### [VehicleProfile.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/domain/model/VehicleProfile.kt)
- Adicionar `isShiftLightMode: Boolean`.
- Adicionar `rpmBarConfig`:
    - `curvatureAngle: Float` (0 = Reta, 30+ = Arco).
    - `barWidth: Float`, `barHeight: Float`.
    - `posY: Float`.
    - `colorActiveBlue: Int`, `colorDimmedBlue: Int`.
    - `colorActiveRed: Int`, `colorDimmedRed: Int`.
    - `colorBlink: Int`.

### [Presentation - Components]

#### [Gauges.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/components/Gauges.kt)
- Refatorar `ArchedRpmGauge` para:
    - Aceitar `curvatureAngle`. Se 0, desenha em linha horizontal.
    - Implementar lógica de `Blink` (usando animação de alfa infinita de 100ms/10Hz).
    - Adaptar escala máxima (8.000 vs 3.200) dinamicamente.

### [Presentation - Settings]

#### [VisualSettingsScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/profile/ui/VisualSettingsScreen.kt)
- Adicionar Seção "Configuração da Barra de RPM":
    - Switch "Modo Troca de Marchas".
    - Sliders: Curvatura, Largura da Barra, Altura da Barra, Posição Y.
    - Seletores de Cores (via Hex ou pré-definidos).

### [Presentation - Dashboard]

#### [DashboardScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/dashboard/ui/DashboardScreen.kt)
- Integrar lógica de 85% da Tabela Honda:
    - 1ª->2ª: Alvo 2700 | Blink >= 2295
    - 2ª->3ª: Alvo 2900 | Blink >= 2465
    - 3ª->4ª: Alvo 2800 | Blink >= 2380
    - 4ª->5ª: Alvo 2900 | Blink >= 2465

---

## Verificação Técnica (Etapas)

1.  **Etapa 1**: Expansão do Modelo de Dados e DataStore.
2.  **Etapa 2**: Motor Geométrico da Barra (Reta vs Arco).
3.  **Etapa 3**: Lógica de Blink e Escala Shift Light (3.200 RPM).
4.  **Etapa 4**: Menu de Customização de Cores e Posição Y.

## Plano de Verificação Manual
- Ativar Modo Shift Light e acelerar (ou simular) até 2.300 RPM em 1ª marcha: a barra deve piscar em vermelho/cor escolhida.
- Mudar Curvatura para 0: a barra deve ficar perfeitamente horizontal.
- Alterar Posição Y: a barra deve subir ou descer no Dashboard.
