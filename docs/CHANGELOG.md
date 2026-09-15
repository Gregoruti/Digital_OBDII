# CHANGELOG - Digital OBD-II

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
