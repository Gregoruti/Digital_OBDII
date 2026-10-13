package com.engineaudio

import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * EnginePhysics — Motor de Física, Cinemática e Estado Acústico Automotivo.
 *
 * Módulo central de física que processa a telemetria do veículo (real ou simulada)
 * e gera os gatilhos matemáticos realistas para a síntese acústica.
 *
 * RECURSOS IMPLEMENTADOS:
 * 1. Limitador de Giro Sincronizado (Rev Limiter / 2-Step):
 *    - Acoplado diretamente à rotação do Shift Light / Flash Light configurada pelo usuário.
 *    - Barreira de contenção rígida: o RPM NUNCA ultrapassa nem congela no Shift Light.
 *    - Simula o corte de injeção em alta frequência (10-15 Hz) com "bounce" cíclico
 *      de 150 a 250 RPM, gerando a clássica assinatura sonora "ram, ram, ram".
 *
 * 2. Blow-off Valve / Válvula de Alívio (Turbo Stututu):
 *    - Pressurização dinâmica do turbo até 1.5 Bar.
 *    - Disparo duplo:
 *      A) Troca de marcha ascendente (Shift Up) ou queda brusca de RPM (Δ RPM negativo).
 *      B) Alívio rápido do acelerador: Throttle > 0.70 (carga plena) para 0.0 (fechamento borboleta).
 *
 * 3. Pops & Bangs (Estouros de Escapamento / Overrun Burbles):
 *    - Freio motor (desaceleração passiva): Throttle == 0 e RPM em queda gradual.
 *    - Intervalos, pitch e volume 100% orgânicos com gerador pseudo-randômico (evita metralhadora).
 *    - Maior frequência e densidade nas faixas médias-altas de RPM (3500 - 6500 RPM).
 */
class EnginePhysics(
    /** Rotação de marcha lenta (padrão: 750 RPM). */
    var idleRpm: Float = 750f,

    /** Rotação máxima absoluta do motor (fundo de escala / redline física, ex: 8000 RPM). */
    var maxRpm: Float = 8000f,

    /**
     * Rotação de ativação do Shift Light configurada pelo usuário / perfil.
     * Esta variável define a barreira de corte acústico quando a sincronização está ativada.
     */
    var shiftLightTriggerRpm: Float = 6500f,

    /** Se true, o corte de giro ocorre rigorosamente na rotação do Shift Light. */
    var isShiftLightSyncEnabled: Boolean = true,

    /** Se true, calcula pressão de turbo e dispara a válvula de alívio (BOV / espirros). */
    var isTurboEnabled: Boolean = true,

    /** Se true, calcula freio motor e dispara estouros de escape (Pops & Bangs). */
    var isPopsEnabled: Boolean = true,

    /**
     * Intervalo de piscamento do Shift Light / Flash Light em milissegundos (padrão: 100ms).
     * O período acústico de bounce do 2-step ("ram, ram, ram") é correlacionado diretamente a este valor.
     */
    var shiftLightBlinkMs: Int = 100
) {

    /**
     * Snapshot imutável do estado físico e acústico do motor gerado a cada ciclo.
     */
    data class PhysicsState(
        /** RPM resultante após filtragem, barreira e corte de injeção (usado pela síntese de som). */
        val rpm: Float,

        /** RPM bruto de entrada antes do limitador. */
        val rawRpm: Float,

        /** Acelerador [0.0f .. 1.0f]. */
        val throttle: Float,

        /** Marcha atual [0 = Neutro, 1..8]. */
        val gear: Int,

        /** Velocidade do veículo em km/h. */
        val speedKmh: Float,

        /** Pressão de turbo simulada em Bar [0.0f .. 1.5f]. */
        val turboPressureBar: Float,

        /** Indica se o corte de injeção / 2-step do limitador está ativamente cortando. */
        val isRevLimiterActive: Boolean,

        /** Indica se o Shift Light / Flash Light está ativo. */
        val isShiftLightActive: Boolean,

        /** Pulso de disparo da válvula de alívio (BOV) no frame atual. */
        val isBovTriggered: Boolean,

        /** Intensidade do espirro do turbo [0.0f .. 1.0f]. */
        val bovIntensity: Float,

        /** Pulso de disparo de estouro no escape (Pop) no frame atual. */
        val isPopTriggered: Boolean,

        /** Volume do estouro [0.0f .. 1.0f]. */
        val popVolume: Float,

        /** Pitch do estouro [0.85f .. 1.15f]. */
        val popPitch: Float
    )

    // ─── Callbacks de Eventos Acústicos ──────────────────────────────────────
    var onLimiterBounce: ((bounceRpm: Float, isCutPhase: Boolean) -> Unit)? = null
    var onBovTriggered: ((intensity: Float) -> Unit)? = null
    var onPopTriggered: ((volume: Float, pitch: Float) -> Unit)? = null

    // ─── Estados Internos de Física ──────────────────────────────────────────
    private val rng = Random()

    private var currentRpm: Float = idleRpm
    private var prevRpm: Float = idleRpm
    private var prevThrottle: Float = 0f
    private var prevGear: Int = 1
    private var lastUpdateTimeNanos: Long = 0L

    // Dinâmica de Turbo
    private var currentTurboPressure: Float = 0.0f // Bar (0.0 a 1.5)
    private var bovCooldownTimerSec: Float = 0.0f

    // Dinâmica do Limitador (2-Step Bounce)
    private var isLimiterCutting: Boolean = false
    private var limiterPhaseTimerSec: Float = 0.0f
    private var currentBounceDropTarget: Float = 200.0f // 150 a 250 RPM

    // Dinâmica de Pops & Bangs (Overrun)
    private var popCooldownTimerSec: Float = 0.0f
    private var overrunActiveTimerSec: Float = 0.0f

    /**
     * Atualiza a física do motor usando o relógio do sistema para cálculo do tempo delta (dt).
     */
    fun update(
        rawRpm: Float,
        throttle: Float,
        gear: Int = 1,
        speedKmh: Float = 0f,
        isShiftLightActiveSignal: Boolean = false
    ): PhysicsState {
        val now = System.nanoTime()
        val dt = if (lastUpdateTimeNanos == 0L) {
            0.016f // 60 FPS inicial
        } else {
            val elapsedSec = (now - lastUpdateTimeNanos) / 1_000_000_000f
            elapsedSec.coerceIn(0.001f, 0.100f) // Limita entre 1ms e 100ms
        }
        lastUpdateTimeNanos = now
        return update(dt, rawRpm, throttle, gear, speedKmh, isShiftLightActiveSignal)
    }

    /**
     * Passo de integração física com dt explícito em segundos.
     */
    fun update(
        dtSeconds: Float,
        rawRpm: Float,
        throttle: Float,
        gear: Int = 1,
        speedKmh: Float = 0f,
        isShiftLightActiveSignal: Boolean = false
    ): PhysicsState {
        val clampedThrottle = throttle.coerceIn(0f, 1f)
        val clampedGear = max(0, gear)
        val clampedRawRpm = max(rawRpm, idleRpm)

        // ─────────────────────────────────────────────────────────────────────
        // 1. LIMITADOR DE GIRO SINCRONIZADO (REV LIMITER / 2-STEP)
        // ─────────────────────────────────────────────────────────────────────
        // Barreira efetiva: se a sincronização com Shift Light estiver ligada,
        // a barreira DEVE ser o shiftLightTriggerRpm configurado pelo usuário,
        // e NÃO o fundo de escala máximo do painel (maxRpm)!
        val effectiveLimiterRpm = if (isShiftLightSyncEnabled) {
            shiftLightTriggerRpm.coerceIn(idleRpm + 500f, maxRpm)
        } else {
            maxRpm
        }

        // A condição de ativação do corte dispara se o RPM atingir a rotação do Shift Light
        // OU se o sinal do Flash Light estiver ativo via painel.
        val reachedShiftLightBarrier = (clampedRawRpm >= effectiveLimiterRpm) ||
                (isShiftLightSyncEnabled && isShiftLightActiveSignal && clampedRawRpm > idleRpm + 1000f)

        var isRevLimiterActive = false
        var computedRpm = clampedRawRpm

        // =====================================================================
        // [BARREIRA DO SHIFT LIGHT INTERCEPTA A SUBIDA DO RPM AQUI]
        // Quando a rotação atinge a barreira do Shift Light (shiftLightTriggerRpm),
        // o RPM é rigorosamente interceptado: ele NÃO continua subindo além do
        // limite, nem simplesmente congela estático.
        // A injeção é cortada em pulsos rápidos de 10-15 Hz: o RPM quica caindo
        // entre 150 e 250 RPM (fase de corte) e sobe novamente contra a barreira,
        // gerando a sonoridade de "ram, ram, ram".
        // =====================================================================
        if (reachedShiftLightBarrier) {
            isRevLimiterActive = true
            limiterPhaseTimerSec += dtSeconds

            // =====================================================================
            // [CORRELAÇÃO ACÚSTICA DIRETA COM A FREQUÊNCIA DO FLASH LIGHT]
            // A frequência do corte de giro acústico (2-Step / Rev Limiter) é sincronizada
            // com a frequência de piscamento do Shift Light (shiftLightBlinkMs).
            // - shiftLightBlinkMs = 100ms (padrão) -> ciclo de ~100ms (~10 Hz de "ram, ram")
            // - shiftLightBlinkMs = 200ms          -> ciclo de ~200ms (~5 Hz de "ram... ram... ram")
            // =====================================================================
            val totalCycleDuration = (shiftLightBlinkMs / 1000f).coerceIn(0.040f, 0.500f)
            val cutPhaseDuration = totalCycleDuration * 0.55f // 55% corte de injeção, 45% recuperação

            if (!isLimiterCutting) {
                // Início do ciclo de corte (bater na barreira do Shift Light)
                isLimiterCutting = true
                limiterPhaseTimerSec = 0.0f
                // Sorteia a profundidade do quique: queda entre 150 e 250 RPM
                currentBounceDropTarget = 150f + rng.nextFloat() * 100f // [150.0 .. 250.0 RPM]
                onLimiterBounce?.invoke(effectiveLimiterRpm, true)
            }

            if (limiterPhaseTimerSec <= cutPhaseDuration) {
                // FASE DE CORTE DE INJEÇÃO: RPM quica para baixo da barreira
                val progress = (limiterPhaseTimerSec / cutPhaseDuration).coerceIn(0f, 1f)
                val rpmDrop = currentBounceDropTarget * progress
                computedRpm = effectiveLimiterRpm - rpmDrop
            } else if (limiterPhaseTimerSec <= totalCycleDuration) {
                // FASE DE RECUPERAÇÃO: RPM volta a subir contra a barreira do Shift Light
                val progress = ((limiterPhaseTimerSec - cutPhaseDuration) / (totalCycleDuration - cutPhaseDuration)).coerceIn(0f, 1f)
                val rpmDrop = currentBounceDropTarget * (1.0f - progress)
                computedRpm = effectiveLimiterRpm - rpmDrop
            } else {
                // Reinicia ciclo do 2-step (novo pulso de corte)
                limiterPhaseTimerSec = 0.0f
                currentBounceDropTarget = 150f + rng.nextFloat() * 100f
                computedRpm = effectiveLimiterRpm
                onLimiterBounce?.invoke(effectiveLimiterRpm, true)
            }
        } else {
            // Abaixo da barreira do Shift Light: opera normalmente sem corte
            isLimiterCutting = false
            limiterPhaseTimerSec = 0.0f
            computedRpm = clampedRawRpm.coerceIn(idleRpm, effectiveLimiterRpm)
        }

        // ─────────────────────────────────────────────────────────────────────
        // 2. BLOW-OFF VALVE / VÁLVULA DE ALÍVIO (TURBO STUTUTU)
        // ─────────────────────────────────────────────────────────────────────
        var isBovTriggered = false
        var bovIntensity = 0.0f

        if (bovCooldownTimerSec > 0f) {
            bovCooldownTimerSec -= dtSeconds
        }

        if (isTurboEnabled) {
            // A) Acúmulo de Pressão do Turbo (0.0 a 1.5 Bar)
            // Sob aceleração com carga, a pressão sobe em direção a 1.5 Bar
            val targetBoostBar = if (clampedThrottle > 0.30f && computedRpm > 2200f) {
                val throttleFactor = (clampedThrottle - 0.30f) / 0.70f
                val rpmFactor = ((computedRpm - 2200f) / (maxRpm - 2200f)).coerceIn(0f, 1f)
                1.50f * throttleFactor * (0.6f + 0.4f * rpmFactor)
            } else {
                0.0f
            }

            if (targetBoostBar > currentTurboPressure) {
                // Carretel enchendo (spool up)
                currentTurboPressure += (targetBoostBar - currentTurboPressure) * min(1.0f, dtSeconds * 3.5f)
            } else {
                // Vazamento lento se soltar levemente o pé
                currentTurboPressure -= (currentTurboPressure - targetBoostBar) * min(1.0f, dtSeconds * 2.0f)
            }
            currentTurboPressure = currentTurboPressure.coerceIn(0f, 1.5f)

            // B) Detecção de Gatilho da Válvula de Alívio
            val deltaRpm = computedRpm - prevRpm
            val isShiftUp = (clampedGear > prevGear && prevGear > 0)
            val isSevereRpmDrop = (deltaRpm < -500f && prevRpm > 2600f)

            // CONDIÇÃO A: Troca de marcha ascendente OU queda brusca de RPM engatando marcha maior
            val conditionAShiftUp = (isShiftUp || isSevereRpmDrop) && currentTurboPressure > 0.25f

            // CONDIÇÃO B: Acelerador passa de > 0.70 (alta carga) para ~0.0 (fechamento rápido da borboleta)
            val isThrottleSnapClosed = (prevThrottle > 0.70f && clampedThrottle <= 0.05f)
            val conditionBThrottleDrop = isThrottleSnapClosed && currentTurboPressure > 0.30f

            if (bovCooldownTimerSec <= 0f && (conditionAShiftUp || conditionBThrottleDrop)) {
                isBovTriggered = true
                bovIntensity = (currentTurboPressure / 1.5f).coerceIn(0.40f, 1.0f)
                bovCooldownTimerSec = 0.350f // Cooldown de 350ms para evitar retrigger
                currentTurboPressure = 0.0f // Despressurização instantânea pela BOV
                onBovTriggered?.invoke(bovIntensity)
            }
        } else {
            currentTurboPressure = 0.0f
        }

        // ─────────────────────────────────────────────────────────────────────
        // 3. POPS & BANGS (ESTOUROS DE ESCAPAMENTO / OVERRUN BURBLES)
        // ─────────────────────────────────────────────────────────────────────
        var isPopTriggered = false
        var popVolume = 0.0f
        var popPitch = 1.0f

        if (popCooldownTimerSec > 0f) {
            popCooldownTimerSec -= dtSeconds
        }

        if (isPopsEnabled) {
            // CONDIÇÃO DE FREIO MOTOR: Throttle == 0 (pé totalmente fora) E RPM em queda gradual
            val isEngineBraking = (clampedThrottle <= 0.02f) && (computedRpm < prevRpm) && (computedRpm > idleRpm + 800f)

            if (isEngineBraking) {
                overrunActiveTimerSec += dtSeconds

                // Dispara apenas quando o temporizador randômico expirar
                if (popCooldownTimerSec <= 0f) {
                    // Randomização de intervalo, pitch e volume (elimina o som de metralhadora repetitiva)
                    val rpmRatio = ((computedRpm - 1800f) / (maxRpm - 1800f)).coerceIn(0f, 1f)

                    // Mais frequentes em faixas médias-altas de RPM (ex: 3500 - 6500 RPM)
                    val nextIntervalSec = when {
                        computedRpm > 5200f -> {
                            // Faixa Alta: rajadas rápidas e intensas (80ms a 160ms)
                            0.080f + rng.nextFloat() * 0.080f
                        }
                        computedRpm > 3400f -> {
                            // Faixa Média-Alta: estouros frequentes e secos (120ms a 240ms)
                            0.120f + rng.nextFloat() * 0.120f
                        }
                        else -> {
                            // Faixa Média-Baixa: borbulhas espaçadas e graves (220ms a 400ms)
                            0.220f + rng.nextFloat() * 0.180f
                        }
                    }

                    // Probabilidade de pipoco a cada ciclo (maior em alta rotação)
                    val triggerProbability = 0.55f + 0.40f * rpmRatio
                    if (rng.nextFloat() <= triggerProbability) {
                        isPopTriggered = true

                        // Volume proporcional à rotação + variação randômica orgânica
                        val baseVol = 0.45f + 0.50f * rpmRatio
                        popVolume = (baseVol * (0.80f + rng.nextFloat() * 0.40f)).coerceIn(0.35f, 1.0f)

                        // Pitch orgânico variável (0.88f a 1.12f)
                        popPitch = 0.88f + rng.nextFloat() * 0.24f

                        onPopTriggered?.invoke(popVolume, popPitch)
                    }

                    popCooldownTimerSec = nextIntervalSec
                }
            } else {
                overrunActiveTimerSec = 0.0f
                // Se acelerou de novo, limpa temporizadores
                if (clampedThrottle > 0.05f) {
                    popCooldownTimerSec = 0.0f
                }
            }
        }

        // Armazena histórico do frame
        prevRpm = computedRpm
        prevThrottle = clampedThrottle
        prevGear = clampedGear
        currentRpm = computedRpm

        return PhysicsState(
            rpm = computedRpm,
            rawRpm = clampedRawRpm,
            throttle = clampedThrottle,
            gear = clampedGear,
            speedKmh = speedKmh,
            turboPressureBar = currentTurboPressure,
            isRevLimiterActive = isRevLimiterActive,
            isShiftLightActive = reachedShiftLightBarrier,
            isBovTriggered = isBovTriggered,
            bovIntensity = bovIntensity,
            isPopTriggered = isPopTriggered,
            popVolume = popVolume,
            popPitch = popPitch
        )
    }

    /**
     * Reseta os estados internos de física e telemetria para marcha lenta.
     */
    fun reset() {
        currentRpm = idleRpm
        prevRpm = idleRpm
        prevThrottle = 0f
        prevGear = 1
        currentTurboPressure = 0f
        bovCooldownTimerSec = 0f
        isLimiterCutting = false
        limiterPhaseTimerSec = 0f
        popCooldownTimerSec = 0f
        overrunActiveTimerSec = 0f
        lastUpdateTimeNanos = 0L
    }
}
