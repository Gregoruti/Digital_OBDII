package com.engineaudio

/**
 * Controle de versão e documentação de build para a Engine de Áudio.
 */
object EngineAudioVersion {
    const val VERSION_NAME = "1.06"
    const val VERSION_CODE = 106
    const val BUILD_DATE = "30/09/2026"

    /**
     * Changelog documentado das alterações realizadas
     */
    val CHANGELOG = listOf(
        "v1.06 (30/09/2026):",
        "• ADIÇÃO DA VARIANTE DE ANTECIPAÇÃO PREDITIVA POR VELOCIDADE (CIVIC LXL 1.8 MANUAL).",
        "• Modula e harmoniza o crossfading dinamicamente com base na velocidade real do veículo (km/h).",
        "• Elimina latência de detecção OBD-II em câmbios manuais sem sensor de marcha física durante trocas de marcha e acionamento de embreagem.",
        "• Curva equal-power calibrada para as relações de marcha do Civic 1.8 (115, 70, 45, 35, 28) com normalização contínua de RMS.",
        "v1.05 (30/09/2026):",
        "• ADIÇÃO DO MODO FAIXA SELECIONADA POR MARCHA (GEAR-LOCKED TRACK MODE - OPÇÃO A).",
        "• Amarra a amostra acústica à marcha exibida no câmbio (N/1ª=Idle, 2ª=Low, 3ª=Mid, 4ª/5ª=High) mantendo o repitch dinâmico do RPM contínuo.",
        "• CONTROLE DE CROSSFADE: Permite alternar entre transição suave entre marchas (Crossfade ON) ou corte seco imediato de amostra (Crossfade OFF).",
        "• SINCRONIZAÇÃO NATIVA COM O DASHBOARD DIGITAL_OBDII: O motor de áudio reflete fielmente a marcha mostrada no painel 7-segmentos.",
        "v1.04 (30/09/2026):",
        "• ADIÇÃO OFICIAL DO AUDI RS3 PERFORMANCE EDITION (2.5L TFSI 5-CILINDROS EM LINHA TURBO).",
        "• Extração e calibração de áudio oficial (banco FMOD FSB5 PCM16 @ 44.1kHz).",
        "• 5 faixas contínuas (idle, low, mid, high, decel) com loop seamless e DSG pops de troca de marcha.",
        "• Suporte a 6 carros oficiais: Nissan GT-R GT3, Audi RS4 V8, Giulia QV, Mustang V8, Maserati MC20 GT2 e Audi RS3 I5.",
        "v1.03 (30/09/2026):",
        "• CORREÇÃO CIRÚRGICA DO WRAP TARGET (Eliminação definitiva do micro-peak).",
        "• Diagnóstico preciso: v1.02 blendava o tail no head mas wrapava para frame 0, mantendo salto residual (|head[959] - head[0]| = 16.418 int16).",
        "• Fix: playhead reinicia no frame xfadeFrames (960) pós-wrap (|head[959] - head[960]| = 34 int16, redução de 99.8%).",
        "• Loop 100% contínuo e inaudível para qualquer rotação (700–1400 RPM) em todos os veículos.",
        "v1.02 (30/09/2026):",
        "• CORREÇÃO DEFINITIVA do micro-peak periódico em marcha lenta (700–1400 RPM).",
        "• Causa raiz: SampleTrack.h fazia wrap abrupto frameCount→0 sem crossfade.",
        "• Fix: pre-baked cosine crossfade de 20ms calculado em load() sobrepõe a",
        "  cauda do buffer; o wrap final é praticamente seamless (delta < 7 int16).",
        "• Aplica a TODOS os carros carregados, não só ao RS4.",
        "v1.01 (29/09/2026):",
        "• Correção cirúrgica do micro-peak na marcha lenta do Audi RS4 Avant V8 (eliminação de spike no sample 55938).",
        "• Alinhamento harmônico de fase no loop circular (r = 0.9880) e nivelamento per-pulso dos 8 cilindros a 700 RPM.",
        "• Adição de controle e exibição de versão na tela de configurações.",
        "v1.00 (29/09/2026):",
        "• Equalização e masterização rigorosa de todos os 5 carros em -11.5 dBFS RMS.",
        "• Filtro de mapeamento exponencial de RPM (Power Curve) desacoplando RPM acústico do visual.",
        "• Modo Som Puro (desativação total de efeitos extras/randômicos).",
        "• Suporte aos 5 carros oficiais: Nissan GT-R GT3, Audi RS4 V8, Giulia Quadrifoglio, Mustang V8 e Maserati MC20 GT2."
    )

    fun getSummary(): String = "Engine Audio v$VERSION_NAME • $BUILD_DATE"
}

