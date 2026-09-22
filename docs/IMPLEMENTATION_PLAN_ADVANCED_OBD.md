# Plano de Implementação: Configurações Avançadas de Comunicação OBD-II

## Objetivo
Adicionar resiliência na comunicação com clones ELM327 problemáticos (ex: v2.1), sem alterar a lógica base que já funciona para adaptadores bons. Todas as novas configurações serão atreladas ao `VehicleProfile` e configuráveis via interface gráfica, mantendo os valores padrão estritamente iguais ao comportamento atual.

## 1. Alterações no Modelo de Dados (`VehicleProfile.kt`)

Adicionar novos campos em `VehicleProfile` com valores padrão seguros:

```kotlin
// ... Configurações existentes ...

// NOVOS PARÂMETROS DE COMUNICAÇÃO AVANÇADA (Valores padrão = comportamento atual)
val enableHeaders: Boolean = false,       // ATH1 vs ATH0 (Padrão: false)
val enableSpaces: Boolean = false,        // ATS1 vs ATS0 (Padrão: false)
val initCycleCount: Int = 1,              // Quantidade de "reforços" de inicialização (Padrão: 1)
val interCommandDelayMs: Int = 0,         // Delay após receber '>' antes do próximo comando (Padrão: 0)
val relaxedValidation: Boolean = false    // Relaxamento no parser caso Headers/Espaços causem erro (Padrão: false)
```

## 2. Alterações na Inicialização (`Elm327Init.kt`)

Modificar a geração da sequência dinâmica `getDynamicBootSequence` para respeitar os novos campos do perfil.

```kotlin
fun getDynamicBootSequence(profile: VehicleProfile): List<InitStep> {
    val steps = mutableListOf<InitStep>()
    
    // Repetição baseada em initCycleCount
    repeat(profile.initCycleCount) {
        steps.add(InitStep("AT Z", "ELM327", "Reset Total"))
        steps.add(InitStep("AT D", "OK", "Padrões de Fábrica"))
        steps.add(InitStep("AT E0", "OK", "Echo Off"))
        steps.add(InitStep("AT L0", "OK", "Linefeeds Off"))
        
        // Uso das novas flags
        val spacesCmd = if (profile.enableSpaces) "AT S1" else "AT S0"
        steps.add(InitStep(spacesCmd, "OK", "Espaços " + if(profile.enableSpaces) "On" else "Off"))
        
        val headersCmd = if (profile.enableHeaders) "AT H1" else "AT H0"
        steps.add(InitStep(headersCmd, "OK", "Headers " + if(profile.enableHeaders) "On" else "Off"))
        
        steps.add(InitStep(profile.adaptiveTiming.command, "OK", profile.adaptiveTiming.label))
        steps.add(InitStep("AT ST ${profile.atTimeoutMs.toString(16).uppercase()}", "OK", "Timeout ${profile.atTimeoutMs}ms"))
        steps.add(InitStep(profile.obdProtocol.command, "OK", profile.obdProtocol.label))
    }
    
    steps.add(InitStep("0100", "4100", "Handshake ECU"))
    return steps
}
```

## 3. Atraso Entre Comandos (Throttle) no `BluetoothConnectionManager.kt`

Para resolver a dessincronização severa de buffer nos clones v2.1, devemos respeitar o `interCommandDelayMs`. O delay deve acontecer de forma segura _após_ a recepção de um frame e _antes_ do envio do próximo, ou dentro da função de envio.

Como a classe não tem acesso direto ao `VehicleProfile`, podemos passar esse delay como parâmetro opcional no método `send`, com padrão `0L`.

```kotlin
// Em BluetoothConnectionManager.kt
suspend fun send(command: String, timeoutMs: Long = 500L, interCommandDelayMs: Long = 0L): String = mutex.withLock {
    withContext(Dispatchers.IO) {
        // ... (código existente)
        
        // NOVO: Throttle antes de enviar para não engasgar o chip
        if (interCommandDelayMs > 0L) {
            kotlinx.coroutines.delay(interCommandDelayMs)
        }
        
        // 2. Envia comando (ASCII)
        out.write("$command\r".toByteArray(Charsets.US_ASCII))
        // ...
```
E no `ObdPollingEngine.kt`, recuperar e passar esse valor:
`transport.send(query, interCommandDelayMs = profile.interCommandDelayMs.toLong())`

## 4. Relaxamento no Parser (`ObdResponseParser.kt`)

Ao habilitar Headers, as respostas deixam de iniciar estritamente com `41XX` e passam a conter bytes do transmissor/receptor antes dos dados. A validação do parser precisará opcionalmente usar `contains` ao invés de `startsWith`, caso o usuário habilite `relaxedValidation`.

```kotlin
// Modificar a assinatura para receber o profile ou a flag
fun parse(rawResponse: String, command: ObdCommand, isRelaxed: Boolean = false): Double? {
    val cleanResponse = rawResponse.replace(Regex("[^0-9A-F]"), "")
    
    val expectedMode = (command.mode.toInt(16) + 0x40).toString(16).uppercase()
    val expectedHeader = expectedMode + command.pid.padStart(2, '0') // ex: 410C

    // Validação estrita vs relaxada
    val isValid = if (isRelaxed) {
        cleanResponse.contains(expectedHeader)
    } else {
        cleanResponse.startsWith(expectedHeader)
    }

    if (!isValid) return null
    
    // Substring flexível baseada na posição do Header
    val dataStartIndex = cleanResponse.indexOf(expectedHeader) + expectedHeader.length
    val rawDataHex = cleanResponse.substring(dataStartIndex)
    // ... (restante igual)
}
```

## 5. Interface Gráfica de Configurações (`PerformanceSettingsScreen.kt`)

Adicionar uma nova seção "Configurações OBD-II Avançadas" para expor os seletores de forma segura. Nenhuma lógica anterior será tocada, apenas controles adicionais de UI serão injetados ligando ao ViewModel:

1. **Protocolo OBD-II (Dropdown/Seletor):** Mapear o enum `ObdProtocol` (Auto, ISO 15765-4 CAN, etc.).
2. **Adaptive Timing (Dropdown/Seletor):** Mapear o enum `AdaptiveTiming` (ATAT0, ATAT1, ATAT2).
3. **Timeout ELM327 (Slider):** Ajustar `atTimeoutMs` entre 10 e 255.
4. **Ciclos de Init / Reforço (Slider/Botões):** Ajustar `initCycleCount` (1x a 5x).
5. **Throttle/Delay Inter-comandos (Slider):** Configurar `interCommandDelayMs` de 0ms a 120ms.
6. **Switches (Toggles):**
   - Habilitar Headers (`ATH1`)
   - Habilitar Espaços (`ATS1`)
   - Parser Relaxado (Permite Headers Extras na resposta)

## Checklist de Segurança da Implementação
- [ ] O valor default de *todos* os novos campos mantém a operação idêntica à v3.6.0.
- [ ] O `BluetoothConnectionManager` só aplica o `delay` extra se solicitado expressamente (> 0).
- [ ] A tela de Performance continuará rodando isolada, garantindo que o usuário possa ativar/desativar cada hack (como headers/espaços e delays) e imediatamente ver o log no "OBD Real-time Logger" abaixo.