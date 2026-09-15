# Plano de Implementação - MVP-05.9: Início Rápido e Persistência Bluetooth

Este plano detalha a inversão do fluxo do app para carregar o Dashboard imediatamente ao abrir, utilizando o último adaptador OBD-II pareado.

## User Review Required

- **Auto-Conexão**: Ao abrir o Dashboard, o app tentará se conectar automaticamente ao último MAC Address salvo. Se falhar, mostrará um ícone de alerta ou erro.
- **Navegação**: O usuário precisará entrar em Perfil -> Bluetooth para trocar de adaptador.

## Proposed Changes

### [Domain - Model]

#### [VehicleProfile.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/domain/model/VehicleProfile.kt)
- Adicionar `lastConnectedDeviceAddress: String? = null`.
- Adicionar `lastConnectedDeviceName: String? = null`.

### [Presentation - Navigation]

#### [AppNavHost.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/navigation/AppNavHost.kt)
- Alterar `startDestination` de `Screen.DeviceList` para `Screen.Dashboard`.

### [Presentation - Dashboard]

#### [DashboardViewModel.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/dashboard/DashboardViewModel.kt)
- Ao carregar o perfil, se houver um `lastConnectedDeviceAddress`, chamar o repositório para iniciar a conexão Bluetooth automaticamente.

#### [DashboardScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/dashboard/ui/DashboardScreen.kt)
- Adicionar feedback visual se a auto-conexão estiver em curso ou se falhar.

### [Presentation - Profile]

#### [VehicleProfileScreen.kt](file:///D:/Softwares/Digital_OBDII/app/src/main/java/com/example/digital_obd_ii/presentation/profile/ui/VehicleProfileScreen.kt)
- Adicionar um novo `IconButton` no TopAppBar com o ícone `Icons.Default.Bluetooth`.
- Este botão navega para `Screen.DeviceList`.

---

## Verificação Técnica

1.  **Etapa 1**: Atualizar modelo de dados para salvar o MAC Address do Bluetooth.
2.  **Etapa 2**: Alterar rota inicial na Navegação.
3.  **Etapa 3**: Implementar gatilho de auto-conexão no DashboardViewModel.
4.  **Etapa 4**: Integrar botão de Bluetooth no menu de configurações.

## Plano de Verificação Manual
- Selecionar um dispositivo Bluetooth manualmente uma última vez.
- Fechar o app totalmente.
- Abrir o app: ele deve cair direto no Dashboard e começar a piscar "Conectando" ou mostrar dados se o adaptador estiver por perto.
- Ir em Perfil -> Ícone Bluetooth e verificar se a lista de dispositivos abre corretamente.
