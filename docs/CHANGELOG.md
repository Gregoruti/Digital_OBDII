# CHANGELOG - Digital OBD-II

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
